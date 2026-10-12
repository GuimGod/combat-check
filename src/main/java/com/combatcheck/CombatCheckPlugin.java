/*
 * Copyright (c) 2017, honeyhoney <https://github.com/honeyhoney>
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import com.google.inject.Provides;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.StructComposition;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * An attention check for the player's own gear. It reminds the player of what they left undone and
 * never anticipates anything a monster will do. With the weapon the player chose to hold, it
 * reminds them of two things:
 *
 * <ul>
 * <li>the melee attack style of that weapon that deals the most damage to the monster being
 * fought, when another one is selected;</li>
 * <li>the pieces that go with that weapon and are still sitting in the inventory (a misclick
 * during a seven-piece switch is easy to miss).</li>
 * </ul>
 *
 * <p>It reads what the client already knows (worn gear, inventory, levels, active prayers, the
 * selected style, the monster attacked) and only draws: it changes no style, equips nothing and
 * clicks nothing. Each reminder can wait a configured number of ticks ({@link AttentionTimer}).
 *
 * <p>Precedents for each part (README, "Precedents"): RuneLite's Attack Styles plugin reads the
 * weapon's styles the same way and warns about them; on the Plugin Hub, Warriors Guild Dummy
 * Helper names the style to use and highlights its button in the combat tab, Dynamic Inventory
 * Tags marks the gear missing from the current switch according to the weapon equipped, and
 * Combat Insight compares damage against the current target.
 */
@Slf4j
@PluginDescriptor(
	name = "Combat Check",
	description = "Attention check: reminds you of the best melee style of the weapon in your hand and of switch pieces left in your inventory. After a delay it also points at a much better weapon you are carrying",
	tags = {"combat", "attack", "style", "stab", "slash", "crush", "switch", "gear", "missing", "reminder", "pvm"}
)
public class CombatCheckPlugin extends Plugin
{
	/** What the overlay draws about the attack style of the weapon in hand. */
	/** A carried weapon whose set would deal much more than what is worn. */
	static final class WeaponAdvice
	{
		int itemId;
		/** The bolts to put on with a carried crossbow, when they are not the ones worn; 0 otherwise. */
		int alsoItemId;
		/** How much more, as the notice writes it. */
		String text;
		double gain;
		/** The bolts shown deal less than the ones on: they are shown only to see the switch coming. */
		boolean worse;
	}

	/** A difference in percent with its sign: "+12%", "+0.4%", "-5%". */
	static String differenceText(double gain)
	{
		if (gain >= 0.05)
		{
			return gainText(gain);
		}
		double loss = -gain;
		return loss < 0.05 ? "0%" : loss < 10 ? String.format(java.util.Locale.ROOT, "-%.1f%%", loss) : String.format(java.util.Locale.ROOT, "-%.0f%%", loss);
	}

	static final class Advice
	{
		int weaponItemId;
		/** Position of the best style in the Combat Options tab, 0 to 3. */
		int bestIndex;
		/** The words of the notice: the style and its damage type. */
		String text;
		/** True when the selected style is the best one (or within the configured margin of it). */
		boolean selected;
		/** How much more the best style deals than the selected one, in percent. */
		double gain;
	}

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private CombatCheckConfig config;

	@Inject
	private CombatCheckOverlay overlay;

	@Inject
	private StyleButtonOverlay buttonOverlay;

	@Inject
	private MissingItemsOverlay missingOverlay;

	private StyleData data;
	@Inject
	private net.runelite.client.plugins.PluginManager pluginManager;

	private final AttentionTimer styleTimer = new AttentionTimer();
	/** The monster being fought, for the effects that depend on it (Slayer task, undead). */
	private StyleData.Monster fought;
	/** The Slayer task id the name below was read for. */
	private int taskId = -1;
	private String taskName;
	private volatile Advice advice;
	private volatile List<Integer> missing = Collections.emptyList();
	private volatile List<Integer> takeOff = Collections.emptyList();
	private volatile WeaponAdvice weaponAdvice;
	private volatile WeaponAdvice boltAdvice;
	private StyleData.Monster healthOf;
	private int healthSeen;
	/** How much more the pieces still to put on (and take off) will add, in percent; NaN when unknown. */
	private volatile double piecesGain = Double.NaN;
	private final AttentionTimer weaponTimer = new AttentionTimer();
	private final AttentionTimer prayerTimer = new AttentionTimer();
	/** The plugin stays silent this long after the last sign of a fight with a player: 100 ticks are a minute. */
	static final int PLAYER_FIGHT_TICKS = 100;
	/** The tick a fight with a player was last seen, or -1. */
	private int lastPlayerFight = -1;
	/** The attack prayer reminder to draw now ("Wrong attack prayer", "No attack prayer"), or null. */
	private volatile String prayerNotice;
	/** "Prayer still on", while a prayer drains out of combat; null otherwise. */
	private volatile String leftOnNotice;
	/** Ticks since the player last fought a monster. */
	private int outOfCombat;
	/** Ticks a draining prayer has been on without a break. */
	private int prayerOnFor;
	/** The weapon reminder waits this long: 5 ticks are 3 seconds. */
	static final int WEAPON_DELAY_FLOOR = 5;
	/** A monster of this combat level or higher is a boss for the attack style wait. */
	static final int BOSS_COMBAT_LEVEL = 150;
	/** How long the last monster attacked counts as the one being fought: 100 ticks are a minute. */
	static final int REMEMBER_TARGET_TICKS = 100;
	/** How long after the last attack the attack prayer reminder stays: 8 ticks are about 5 seconds. */
	static final int PRAYER_AFTER_FIGHT_TICKS = 8;

	/** The bolts reminder stays this long after the last attack: 25 ticks are 15 seconds, enough to step aside or wait out a phase. */
	static final int BOLTS_AFTER_FIGHT_TICKS = 25;
	private int targetId = -1;
	private int targetTicks;
	private final AttentionTimer switchTimer = new AttentionTimer();
	/** The value of the spellbook varbit on the Ancient Magicks. */
	private static final int ANCIENT_SPELLBOOK = 1;
	/** What the player is doing with the weapon in hand, for the conditional effects of armour. */
	private SetEffects.Context context;

	@Inject
	private ConfigManager configManager;

	/** Where the darts last seen in the blowpipe are kept, for each account. */
	private static final String CONFIG_GROUP = "combatcheck";
	private static final String BLOWPIPE_DART_KEY = "blowpipeDart";
	/** The darts a blowpipe takes, by the name its Check message gives them, and their item. */
	private static final String[] DART_NAMES = {"bronze dart", "iron dart", "steel dart", "black dart", "mithril dart",
		"adamant dart", "rune dart", "amethyst dart", "dragon dart"};
	private static final int[] DART_ITEMS = {806, 807, 808, 3093, 809, 810, 811, 25849, 11230};
	private static final java.util.regex.Pattern BLOWPIPE_CHECK =
		java.util.regex.Pattern.compile("^Darts: (.+?)(?: x [\\d,]+)?\\. Scales: .*");
	/** The darts in the blowpipe: their item, 0 for none, -1 while the blowpipe has not been checked on this account. */
	private int blowpipeDart = -1;
	private volatile String blowpipeNotice;

	/** The "check your blowpipe" reminder to draw now, or null. */
	String blowpipeNotice()
	{
		return blowpipeNotice;
	}

	/**
	 * The darts a blowpipe's Check message names ("Darts: Dragon dart x 1,234. Scales: 16,383 (100.0%)."):
	 * their item, 0 for none, -1 when the message is not that one or the darts are not known.
	 */
	static int dartFromCheck(String message)
	{
		java.util.regex.Matcher m = message == null ? null : BLOWPIPE_CHECK.matcher(message.trim());
		if (m == null || !m.matches())
		{
			return -1;
		}
		String name = m.group(1).trim().toLowerCase(java.util.Locale.ROOT);
		if (name.equals("none"))
		{
			return 0;
		}
		for (int i = 0; i < DART_NAMES.length; i++)
		{
			if (name.equals(DART_NAMES[i]))
			{
				return DART_ITEMS[i];
			}
		}
		return -1;
	}

	/** True for the blowpipes whose Check message is read: the Toxic blowpipe and its Blazing recolour. */
	static boolean readsDarts(String weaponName)
	{
		String n = weaponName == null ? "" : weaponName.toLowerCase(java.util.Locale.ROOT);
		return n.contains("toxic blowpipe") || n.contains("blazing blowpipe");
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != net.runelite.api.ChatMessageType.GAMEMESSAGE && event.getType() != net.runelite.api.ChatMessageType.SPAM)
		{
			return;
		}
		String text = net.runelite.client.util.Text.removeTags(event.getMessage());
		if (!text.startsWith("Darts: "))
		{
			return;
		}
		int dart = dartFromCheck(text);
		blowpipeDart = dart;
		if (dart < 0)
		{
			configManager.unsetRSProfileConfiguration(CONFIG_GROUP, BLOWPIPE_DART_KEY);
		}
		else
		{
			configManager.setRSProfileConfiguration(CONFIG_GROUP, BLOWPIPE_DART_KEY, dart);
		}
	}

	/** The darts last seen in this account's blowpipe, and whether to ask for a Check of the one in hand. */
	private void readBlowpipe()
	{
		String saved = configManager.getRSProfileConfiguration(CONFIG_GROUP, BLOWPIPE_DART_KEY);
		int dart = -1;
		if (saved != null)
		{
			try
			{
				dart = Integer.parseInt(saved);
			}
			catch (NumberFormatException e)
			{
				dart = -1;
			}
		}
		blowpipeDart = dart;
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		Item inHand = worn == null ? null : worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		boolean blowpipe = inHand != null && inHand.getId() > 0
			&& readsDarts(itemManager.getItemComposition(inHand.getId()).getName());
		blowpipeNotice = blowpipe && dart < 0 ? "Check your blowpipe" : null;
	}

	@Provides
	CombatCheckConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CombatCheckConfig.class);
	}

	/** The style reminder to draw now, or null. */
	Advice advice()
	{
		return advice;
	}

	/** The switch pieces still in the inventory, in inventory order; empty when there is nothing to remind. */
	List<Integer> missing()
	{
		return missing;
	}

	/** The worn pieces that do less than an empty slot with the weapon in hand; empty when there is none. */
	List<Integer> takeOff()
	{
		return takeOff;
	}

	/** The carried weapon to remind the player of, or null. */
	WeaponAdvice weaponAdvice()
	{
		return weaponAdvice;
	}

	/** The ruby or diamond bolts (e) in the inventory that deal more right now ("Diamond +3%"), or null. */
	WeaponAdvice boltAdvice()
	{
		return boltAdvice;
	}

	/** The attack prayer reminder to draw now, or null. */
	String prayerNotice()
	{
		return prayerNotice;
	}

	/** The "a prayer is still on" reminder to draw now, or null. */
	String leftOnNotice()
	{
		return leftOnNotice;
	}

	/** Whether to say that a prayer was left on: one is, and nothing has been fought for the time asked. */
	static boolean leftOn(boolean drainingPrayerOn, int ticksOutOfCombat, int delay)
	{
		return drainingPrayerOn && ticksOutOfCombat >= delay;
	}

	/** True while the player attacks a monster or a monster attacks the player. */
	private boolean inCombat(Player me)
	{
		if (me.getInteracting() instanceof NPC && canBeFought((NPC) me.getInteracting()))
		{
			return true;
		}
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc != null && npc.getInteracting() == me && canBeFought(npc))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * True for an NPC with an Attack option that is not a follower: a pet walking behind the player, a banker
	 * or a grave the player last clicked on is not a fight.
	 */
	private static boolean canBeFought(NPC npc)
	{
		net.runelite.api.NPCComposition composition = npc.getTransformedComposition();
		if (composition == null || composition.isFollower() || composition.getActions() == null)
		{
			return false;
		}
		for (String action : composition.getActions())
		{
			if ("Attack".equals(action))
			{
				return true;
			}
		}
		return false;
	}

	private volatile List<String> leftOnPrayers = Collections.emptyList();

	/** The prayers that are on and draining, by name ("Piety", "Protect from melee"), for the "still on" reminder. */
	List<String> leftOnPrayers()
	{
		return leftOnPrayers;
	}

	/** True when a prayer that drains is on, other than the ones kept on between fights. */
	private boolean drainingPrayerOn()
	{
		List<String> on = new ArrayList<>();
		for (Prayer p : Prayer.values())
		{
			if (p != Prayer.PRESERVE && p != Prayer.RAPID_HEAL && p != Prayer.RAPID_RESTORE && client.isPrayerActive(p))
			{
				String n = p.name().replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
				on.add(Character.toUpperCase(n.charAt(0)) + n.substring(1));
			}
		}
		leftOnPrayers = on;
		return !on.isEmpty();
	}

	/**
	 * What to say about the attack prayers that are on, for the combat class of the weapon in hand: nothing
	 * when one of that class is on; "Wrong prayer: Rigour" when only another class's is; "Piety off" when
	 * none is. Each of the two is said only when the setting asks for it.
	 *
	 * @param meleeOn  the name of the melee attack prayer that is on, or null (the same for the other two)
	 * @param best     the name of the attack prayer of the class in use, for the "off" reminder
	 */
	/**
	 * The two prayer switches as one setting. Not a method of the config interface: RuneLite answers null
	 * for any method there that is not a config item.
	 */
	static CombatCheckConfig.PrayerReminder prayerReminder(boolean noneOn, boolean wrong)
	{
		return noneOn ? (wrong ? CombatCheckConfig.PrayerReminder.BOTH : CombatCheckConfig.PrayerReminder.NONE_ON)
			: wrong ? CombatCheckConfig.PrayerReminder.WRONG : CombatCheckConfig.PrayerReminder.OFF;
	}

	static String prayerNotice(String combatClass, String meleeOn, String rangedOn, String magicOn, String best,
		CombatCheckConfig.PrayerReminder mode)
	{
		if (combatClass == null || mode == null || mode == CombatCheckConfig.PrayerReminder.OFF)
		{
			return null;
		}
		String right = StyleCalc.MELEE.equals(combatClass) ? meleeOn
			: StyleCalc.RANGED.equals(combatClass) ? rangedOn
			: StyleCalc.MAGIC.equals(combatClass) ? magicOn : "";
		if (right != null)
		{
			return null;
		}
		String wrong = meleeOn != null ? meleeOn : rangedOn != null ? rangedOn : magicOn;
		if (wrong != null)
		{
			return mode == CombatCheckConfig.PrayerReminder.NONE_ON ? null : "Wrong prayer: " + wrong;
		}
		return mode == CombatCheckConfig.PrayerReminder.WRONG ? null : best + " off";
	}

	/** The name of the first of these prayers that is on, or null. */
	private String on(Prayer... prayers)
	{
		for (Prayer p : prayers)
		{
			if (client.isPrayerActive(p))
			{
				String n = p.name().replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
				return Character.toUpperCase(n.charAt(0)) + n.substring(1);
			}
		}
		return null;
	}

	/** The attack prayer to name for a combat class: the top one when the player has it, the class otherwise. */
	private String bestPrayer(String combatClass)
	{
		return bestPrayer(combatClass, client.getRealSkillLevel(Skill.PRAYER),
			client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED) == 1, client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) == 1,
			client.getVarbitValue(VarbitID.PRAYER_AUGURY_UNLOCKED) == 1, client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) == 1);
	}

	/**
	 * The best single attack prayer for a combat class at a Prayer level, with the scrolls the player has read.
	 * Below Chivalry melee takes two prayers at once, so it is named by its class; so is a class with none yet.
	 */
	static String bestPrayer(String combatClass, int level, boolean rigour, boolean deadeye, boolean augury, boolean vigour)
	{
		if (StyleCalc.MELEE.equals(combatClass))
		{
			return level >= 70 ? "Piety" : level >= 60 ? "Chivalry" : "Melee prayer";
		}
		if (StyleCalc.RANGED.equals(combatClass))
		{
			return level >= 74 && rigour ? "Rigour" : level >= 62 && deadeye ? "Deadeye" : level >= 44 ? "Eagle eye"
				: level >= 26 ? "Hawk eye" : level >= 8 ? "Sharp eye" : "Ranged prayer";
		}
		return level >= 77 && augury ? "Augury" : level >= 63 && vigour ? "Mystic vigour" : level >= 45 ? "Mystic might"
			: level >= 27 ? "Mystic lore" : level >= 9 ? "Mystic will" : "Magic prayer";
	}

	/** What the pieces left will add, as the notice writes it ("+18%"), or null when there is no figure. */
	String piecesGainText()
	{
		double g = piecesGain;
		return Double.isNaN(g) || g < 0.05 ? null : gainText(g);
	}

	@Override
	protected void startUp() throws IOException
	{
		data = new StyleData();
		reset();
		overlayManager.add(overlay);
		overlayManager.add(buttonOverlay);
		overlayManager.add(missingOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(buttonOverlay);
		overlayManager.remove(missingOverlay);
		reset();
		data = null;
	}

	/** How far from where the last attack was made the monster stops being the one fought. */
	static final int FORGET_DISTANCE = 20;
	/** Where the player stood at the last attack on the remembered monster. */
	private net.runelite.api.coords.WorldPoint targetAt;

	/** True when the player died or is no longer near where the fight was. */
	static boolean leftTheFight(net.runelite.api.coords.WorldPoint here, net.runelite.api.coords.WorldPoint fightAt, int hitpoints)
	{
		if (hitpoints <= 0)
		{
			return true;
		}
		return here != null && fightAt != null
			&& (here.getPlane() != fightAt.getPlane() || here.distanceTo2D(fightAt) > FORGET_DISTANCE);
	}

	@Subscribe
	public void onGameStateChanged(net.runelite.api.events.GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			// nothing of the last fight is carried to another world or another login
			reset();
			prayerNotice = null;
			leftOnNotice = null;
			blowpipeNotice = null;
			outOfCombat = 0;
			prayerOnFor = 0;
			prayerTimer.reset();
		}
	}

	private void reset()
	{
		advice = null;
		missing = Collections.emptyList();
		takeOff = Collections.emptyList();
		weaponAdvice = null;
		boltAdvice = null;
		piecesGain = Double.NaN;
		targetId = -1;
		styleTimer.reset();
		switchTimer.reset();
		weaponTimer.reset();
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		try
		{
			if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null || data == null)
			{
				advice = null;
				missing = Collections.emptyList();
				takeOff = Collections.emptyList();
				weaponAdvice = null;
				boltAdvice = null;
				prayerNotice = null;
				blowpipeNotice = null;
				piecesGain = Double.NaN;
				return;
			}
			if (fightingPlayer(client.getTickCount()))
			{
				// this plugin is for fighting monsters: against a player it says nothing at all
				reset();
				blowpipeNotice = null;
				prayerNotice = null;
				leftOnNotice = null;
				outOfCombat = 0;
				prayerTimer.reset();
				return;
			}
			readBlowpipe();
			update(client.getTickCount());
		}
		catch (RuntimeException e)
		{
			blowpipeNotice = null;
			// a cache lookup that fails must not break the tick for every other plugin
			log.debug("could not check the gear", e);
			advice = null;
			missing = Collections.emptyList();
			takeOff = Collections.emptyList();
			weaponAdvice = null;
			boltAdvice = null;
			prayerNotice = null;
			piecesGain = Double.NaN;
		}
	}

	/**
	 * True while the player is fighting another player: they are targeting one, or one is targeting them,
	 * now or within the last {@link #PLAYER_FIGHT_TICKS} ticks.
	 */
	private boolean fightingPlayer(int tick)
	{
		Player me = client.getLocalPlayer();
		boolean now = me.getInteracting() instanceof Player;
		if (!now)
		{
			for (Player other : client.getTopLevelWorldView().players())
			{
				if (other != null && other != me && other.getInteracting() == me)
				{
					now = true;
					break;
				}
			}
		}
		if (now)
		{
			lastPlayerFight = tick;
		}
		return stillFightingPlayer(tick, lastPlayerFight);
	}

	/** Whether a fight with a player seen at {@code last} (-1 for never) still silences the plugin at {@code tick}. */
	static boolean stillFightingPlayer(int tick, int last)
	{
		return last >= 0 && tick - last <= PLAYER_FIGHT_TICKS;
	}

	/**
	 * How long the wrong style must stay selected: the configured wait, or none against a monster
	 * below the boss level when the wait is kept for bosses only.
	 */
	static int styleWait(int delay, boolean bossOnly, int combatLevel, int bossLevel)
	{
		return bossOnly && combatLevel < bossLevel ? 0 : delay;
	}

	private void update(int tick)
	{
		Player me = client.getLocalPlayer();
		Actor interacting = me.getInteracting();
		net.runelite.api.coords.WorldPoint here = me.getWorldLocation();
		if (interacting instanceof NPC && data.monster(((NPC) interacting).getId()) != null)
		{
			targetId = ((NPC) interacting).getId();
			targetTicks = 0;
			targetAt = here;
		}
		else if (targetId != -1 && (++targetTicks > REMEMBER_TARGET_TICKS
			|| leftTheFight(here, targetAt, client.getBoostedSkillLevel(Skill.HITPOINTS))
			|| (interacting instanceof NPC && canBeFought((NPC) interacting))))
		{
			// the minute of memory is for the pause between two kills: dead, gone from where the fight was,
			// or attacking a monster that is not in the table, the old one is forgotten at once
			targetId = -1;
		}
		StyleData.Monster monster = targetId == -1 ? null : data.monster(targetId);

		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		int weaponId = -1;
		if (worn != null)
		{
			Item weapon = worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
			if (weapon != null && weapon.getId() > 0)
			{
				weaponId = itemManager.canonicalize(weapon.getId());
			}
		}
		List<StyleData.Style> styles = data.styles(weaponId);
		int[] index = indices(clientStyleNames(client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY)), styles.size());
		int selectedIndex = client.getVarpValue(VarPlayerID.COM_MODE);
		String selectedType = null;
		for (int i = 0; i < styles.size(); i++)
		{
			if (index[i] == selectedIndex)
			{
				selectedType = styles.get(i).type;
			}
		}

		// 1. the attack style of the weapon in hand, against the monster being fought
		Advice style = monster == null || !config.styleReminder() ? null
			: style(monster, worn, weaponId, styles, index, selectedIndex, selectedType);
		boolean mistake = style != null && !style.selected;
		int level = targetId == -1 ? 0 : client.getNpcDefinition(targetId).getCombatLevel();
		boolean due = styleTimer.due(tick, mistake,
			styleWait(config.styleDelay(), true, level, BOSS_COMBAT_LEVEL));
		if (style != null && (mistake ? due : config.alwaysShow()))
		{
			advice = style;
		}
		else
		{
			advice = null;
		}

		// 2. the pieces that go with the weapon in hand and are still in the inventory
		List<Integer> left = Collections.emptyList();
		List<Integer> off = Collections.emptyList();
		double gain = Double.NaN;
		fought = monster;
		SetEffects.Context c = contextFor(weaponId, weaponClass(weaponId, selectedType),
			StyleCalc.melee(selectedType) ? selectedType : null, worn);
		context = c;
		SwitchSet.Piece[] on = pieces(worn);
		List<SwitchSet.Piece> carried = inventoryPieces();
		if (config.missingPieces() && (monster != null || !config.onlyInCombat()))
		{
			left = SwitchSet.missing(c.combatClass, c.meleeType, on, carried);
			if (monster != null)
			{
				left = withoutUselessHelmet(left, on, carried, weaponId, target(monster));
			}
			if (config.takeOffPieces())
			{
				off = SwitchSet.takeOff(c.combatClass, c.meleeType, on, carried);
			}
			if (monster != null && config.showGain() && (!left.isEmpty() || !off.isEmpty()))
			{
				// the same weapon with and without the pieces: a nominal spell is enough for a staff on autocast
				SetCalc.Player stats = stats();
				SetCalc.Target target = target(monster);
				SwitchSet.Piece[] done = SwitchSet.after(c.combatClass, c.meleeType, on, carried);
				if (!config.takeOffPieces())
				{
					for (int slot = 0; slot < done.length; slot++)
					{
						done[slot] = done[slot] == null ? on[slot] : done[slot];
					}
				}
				gain = SetCalc.gain(SetCalc.perTick(loadout(on, weaponId), stats, target, true),
					SetCalc.perTick(loadout(done, weaponId), stats, target, true));
			}
		}
		if (!switchTimer.due(tick, !left.isEmpty() || !off.isEmpty(), config.switchDelay()))
		{
			left = Collections.emptyList();
			off = Collections.emptyList();
			gain = Double.NaN;
		}
		missing = left;
		takeOff = off;
		piecesGain = gain;

		// 3. a carried weapon whose set would deal much more to this monster than what is worn
		// never against a monster that changes form or phase during the fight: which weapon each form calls for is the fight itself
		WeaponAdvice better = monster != null && config.weaponNotice() && !changesForm(monster.name)
			? betterWeapon(monster, worn, weaponId, on) : null;
		boolean wait = !weaponTimer.due(tick, better != null, WEAPON_DELAY_FLOOR);
		weaponAdvice = wait ? null : better;

		// 3b. the other of ruby and diamond bolts (e), when it deals more at the monster's current health
		boltAdvice = monster != null && targetTicks <= BOLTS_AFTER_FIGHT_TICKS && config.boltReminder() && !boltPluginOn()
			? betterBolts(on, carried, weaponId, target(monster)) : null;
		if (boltAdvice == null && config.boltReminder() && !boltPluginOn())
		{
			boltAdvice = boltsToPutOn(on, carried, weaponId);
		}

		// 4. the attack prayer that is on, against the class of the weapon in hand
		String prayer = null;
		if (monster != null && targetTicks <= PRAYER_AFTER_FIGHT_TICKS && c.combatClass != null
			&& prayerReminder(config.prayerNoneOn(), config.prayerWrong()) != CombatCheckConfig.PrayerReminder.OFF)
		{
			prayer = prayerNotice(c.combatClass,
				on(Prayer.PIETY, Prayer.CHIVALRY, Prayer.ULTIMATE_STRENGTH, Prayer.INCREDIBLE_REFLEXES, Prayer.SUPERHUMAN_STRENGTH,
					Prayer.IMPROVED_REFLEXES, Prayer.BURST_OF_STRENGTH, Prayer.CLARITY_OF_THOUGHT),
				on(Prayer.RIGOUR, Prayer.DEADEYE, Prayer.EAGLE_EYE, Prayer.HAWK_EYE, Prayer.SHARP_EYE),
				on(Prayer.AUGURY, Prayer.MYSTIC_VIGOUR, Prayer.MYSTIC_MIGHT, Prayer.MYSTIC_LORE, Prayer.MYSTIC_WILL),
				bestPrayer(c.combatClass), prayerReminder(config.prayerNoneOn(), config.prayerWrong()));
		}
		prayerNotice = prayerTimer.due(tick, prayer != null, config.prayerDelay()) ? prayer : null;

		// 5. a prayer left on after the fight
		outOfCombat = inCombat(me) ? 0 : outOfCombat + 1;
		boolean draining = drainingPrayerOn();
		prayerOnFor = draining ? prayerOnFor + 1 : 0;
		// a prayer just turned on is not one left on: praying ahead of a fight is given the same wait
		leftOnNotice = config.prayerLeftOn() && leftOn(draining, Math.min(outOfCombat, prayerOnFor), config.prayerLeftOnDelay())
			? "Prayer still on" : null;
	}

	/**
	 * The pieces left, without a Slayer helmet that would not deal more than what is on the head now: the
	 * helmet is offered when it is the better choice against this monster, not merely because a task is on.
	 * When the damage cannot be worked out for the weapon in hand, the list is left as it is.
	 */
	private List<Integer> withoutUselessHelmet(List<Integer> left, SwitchSet.Piece[] on, List<SwitchSet.Piece> carried,
		int weaponId, SetCalc.Target target)
	{
		for (SwitchSet.Piece p : carried)
		{
			if (p.mask == 0 || !left.contains(p.itemId) || p.slot < 0 || p.slot >= on.length)
			{
				continue;
			}
			SwitchSet.Piece[] with = on.clone();
			with[p.slot] = p;
			SetCalc.Player stats = stats();
			double now = SetCalc.perTick(loadout(on, weaponId), stats, target, true);
			double then = SetCalc.perTick(loadout(with, weaponId), stats, target, true);
			if (helmetIsUseless(now, then))
			{
				List<Integer> kept = new ArrayList<>(left);
				kept.remove((Integer) p.itemId);
				return kept;
			}
		}
		return left;
	}

	/** True when both figures are known and the set with the helmet deals no more than the one without. */
	static boolean helmetIsUseless(double now, double withHelmet)
	{
		return now >= 0 && withHelmet >= 0 && withHelmet <= now;
	}

	/** What the player is doing with a weapon, for the conditional effects of armour. */
	private SetEffects.Context contextFor(int weaponId, String combatClass, String meleeType, ItemContainer worn)
	{
		String category = data.category(weaponId);
		SetEffects.Context c = new SetEffects.Context();
		c.combatClass = combatClass;
		c.meleeType = meleeType;
		c.weaponName = weaponId > 0 ? itemManager.getItemComposition(weaponId).getName() : "";
		c.castingStaff = "Staff".equals(category) || "Bladed Staff".equals(category);
		c.ancientSpellbook = client.getVarbitValue(VarbitID.SPELLBOOK) == ANCIENT_SPELLBOOK;
		c.autocastElement = c.castingStaff ? SetEffects.autocastElement(client.getVarbitValue(VarbitID.AUTOCAST_SPELL)) : null;
		c.ownedNames = ownedNames(worn);
		c.onTask = onTask(fought);
		c.undead = fought == null ? null : (Boolean) fought.undead;
		String weapon = c.weaponName.toLowerCase(java.util.Locale.ROOT);
		c.ammoFree = StyleCalc.MELEE.equals(combatClass) || StyleCalc.MAGIC.equals(combatClass)
			|| "Thrown".equals(category) || "Chinchompas".equals(category) || weapon.contains("blowpipe")
			|| SetEffects.isCrystalBow(c.weaponName);
		return c;
	}

	/** The monster as the damage formulas need it, with whether the player is on a task of it. */
	private SetCalc.Target target(StyleData.Monster monster)
	{
		SetCalc.Target t = monster.target();
		t.onTask = Boolean.TRUE.equals(onTask(monster));
		t.inWilderness = client.getVarbitValue(net.runelite.api.Varbits.IN_WILDERNESS) == 1;
		t.undead = monster.undead;
		// how much health it has now: the ruby bolts' effect depends on it. A boss with the game's own health display
		// gives it exactly; otherwise the bar's fraction of the table's; and while neither shows (walking away from an
		// attack, the bar hidden), the last health seen on this monster, never its full health
		Actor fighting = client.getLocalPlayer().getInteracting();
		int hud = client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
		int seen = 0;
		if (hud > 0 && data.monster(hud) == monster && client.getVarbitValue(net.runelite.api.gameval.VarbitID.HPBAR_HUD_BASEHP) > 0)
		{
			seen = client.getVarbitValue(net.runelite.api.gameval.VarbitID.HPBAR_HUD_HP);
		}
		if (seen <= 0 && fighting instanceof NPC && data.monster(((NPC) fighting).getId()) == monster
			&& fighting.getHealthRatio() > 0 && fighting.getHealthScale() > 0)
		{
			seen = Math.max(1, (int) ((long) monster.hitpoints * fighting.getHealthRatio() / fighting.getHealthScale()));
		}
		if (seen > 0)
		{
			healthOf = monster;
			healthSeen = seen;
		}
		t.hitpointsNow = healthOf == monster ? healthSeen : 0;
		return t;
	}

	/**
	 * The ruby or diamond bolts (e) in the inventory that would deal more than the ones in the ammunition
	 * slot, to the monster at the health it has now; null when there are none or the worn ones are as good.
	 */
	private WeaponAdvice betterBolts(SwitchSet.Piece[] on, List<SwitchSet.Piece> carried, int weaponId, SetCalc.Target target)
	{
		SwitchSet.Piece ammo = shotAmmo(on, weaponId);
		int worn = ammo == null ? 0 : SetCalc.boltKind(ammo.name);
		if (target.hitpointsNow <= 0 || !"Crossbow".equals(data.category(weaponId)))
		{
			return null;
		}
		SetCalc.Player stats = stats();
		if (worn == 0)
		{
			// no bolts on at all (an empty slot, a blessing, arrows): the better of the carried ruby and diamond bolts
			if (ammo != null && isBolts(ammo.name))
			{
				return null; // other bolts are on: their effect is not compared
			}
			SwitchSet.Piece best = null;
			double most = -1;
			double least = -1;
			for (SwitchSet.Piece p : carried)
			{
				if (SetCalc.boltKind(p.name) == 0)
				{
					continue;
				}
				SwitchSet.Piece[] with = on.clone();
				with[SwitchSet.AMMO] = p;
				double d = SetCalc.perTick(loadout(with, weaponId), stats, target, true);
				least = least < 0 ? d : Math.min(least, d);
				if (d > most)
				{
					most = d;
					best = p;
				}
			}
			if (best == null)
			{
				return null;
			}
			double over = SetCalc.gain(least, most);
			WeaponAdvice advice = new WeaponAdvice();
			advice.itemId = best.itemId;
			advice.gain = Double.isNaN(over) ? 0 : over;
			advice.text = (SetCalc.boltKind(best.name) == SetCalc.DIAMOND ? "Diamond" : "Ruby")
				+ (Double.isNaN(over) || over < 0.1 ? "" : " " + gainText(over));
			return advice;
		}
		double now = SetCalc.perTick(loadout(on, weaponId), stats, target, true);
		// the other kind may also sit in Dizana's quiver, under the slot's bolts: taking those off switches to it
		List<SwitchSet.Piece> others = new ArrayList<>();
		SwitchSet.Piece slot = on.length > SwitchSet.AMMO ? on[SwitchSet.AMMO] : null;
		SwitchSet.Piece quiver = ammo == slot && client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT) > 0
			? piece(client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO)) : null;
		if (quiver != null)
		{
			others.add(quiver);
		}
		others.addAll(carried);
		for (SwitchSet.Piece p : others)
		{
			int kind = SetCalc.boltKind(p.name);
			if (kind == 0 || kind == worn)
			{
				continue;
			}
			SwitchSet.Piece[] with = on.clone();
			with[SwitchSet.AMMO] = p;
			double then = SetCalc.perTick(loadout(with, weaponId), stats, target, true);
			double gain = SetCalc.gain(now, then);
			if (Double.isNaN(gain) || (gain < 0.1 && !config.boltAlwaysShow()))
			{
				return null;
			}
			WeaponAdvice advice = new WeaponAdvice();
			advice.itemId = p.itemId;
			advice.gain = gain;
			// with "all the time" on, the other bolts are shown while they are still the worse ones, in red
			advice.worse = gain < 0.1;
			advice.text = (kind == SetCalc.DIAMOND ? "Diamond " : "Ruby ") + differenceText(gain) + (p == quiver ? " (quiver)" : "");
			return advice;
		}
		return null;
	}

	/**
	 * Whether the monster is of the player's Slayer task: TRUE, FALSE, or null when that is not known.
	 * The task comes from the game itself: how many are left, and the task's name in the game's own table.
	 */
	private Boolean onTask(StyleData.Monster monster)
	{
		if (client.getVarpValue(VarPlayerID.SLAYER_COUNT) <= 0)
		{
			return Boolean.FALSE;
		}
		if (monster == null)
		{
			return null;
		}
		return SetEffects.onTask(taskName(), monster.slayer, data.slayerCategories());
	}

	/** The name of the current Slayer task, from the game's table of tasks; null when it cannot be read. */
	private String taskName()
	{
		int id = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		if (id == taskId)
		{
			return taskName;
		}
		String name = null;
		try
		{
			List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, id);
			if (rows != null && !rows.isEmpty())
			{
				Object[] field = client.getDBTableField(rows.get(0), DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
				name = field != null && field.length > 0 && field[0] != null ? field[0].toString() : null;
			}
		}
		catch (RuntimeException e)
		{
			log.debug("could not read the slayer task", e);
		}
		taskId = id;
		taskName = name;
		return name;
	}

	/**
	 * The carried weapon whose finished set would deal the most over what is worn now, when that is
	 * at least the configured gain; null otherwise, and whenever either side has a rule the plugin
	 * does not know (no figure is better than a wrong one).
	 */
	private WeaponAdvice betterWeapon(StyleData.Monster monster, ItemContainer worn, int weaponId, SwitchSet.Piece[] on)
	{
		SetCalc.Loadout now = loadout(on, weaponId);
		if (!SetCalc.plain(now))
		{
			return null;
		}
		SetCalc.Player stats = stats();
		SetCalc.Target target = target(monster);
		double current = SetCalc.perTick(now, stats, target, false);
		if (current < 0)
		{
			return null;
		}
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return null;
		}
		SetEffects.Context mine = context;
		WeaponAdvice best = null;
		List<Integer> seen = new ArrayList<>();
		try
		{
			for (Item item : inventory.getItems())
			{
				if (item == null || item.getId() <= 0)
				{
					continue;
				}
				int id = itemManager.canonicalize(item.getId());
				var stats0 = itemManager.getItemStats(id);
				if (seen.contains(id) || id == weaponId || stats0 == null || stats0.getEquipment() == null
					|| stats0.getEquipment().getSlot() != SwitchSet.WEAPON)
				{
					continue;
				}
				seen.add(id);
				SetCalc.Loadout bare = new SetCalc.Loadout();
				bare.category = data.category(id);
				String cls = SetCalc.combatClass(bare);
				if (cls == null)
				{
					continue;
				}
				// the pieces as they count with that weapon in hand
				context = contextFor(id, cls, null, worn);
				SwitchSet.Piece weapon = piece(id);
				SwitchSet.Piece[] with = pieces(worn);
				with[SwitchSet.WEAPON] = weapon;
				if (weapon != null && weapon.twoHanded)
				{
					with[SwitchSet.SHIELD] = null;
				}
				List<SwitchSet.Piece> carried = new ArrayList<>();
				for (SwitchSet.Piece p : inventoryPieces())
				{
					if (p.itemId != id)
					{
						carried.add(p);
					}
				}
				SwitchSet.Piece[] after = SwitchSet.after(cls, null, with, carried);
				int bolts = 0;
				double other = -1;
				if ("Crossbow".equals(bare.category))
				{
					// a crossbow shoots the bolts worn or the best of the carried ones; with none, it is no option
					List<SwitchSet.Piece> ammo = new ArrayList<>();
					SwitchSet.Piece ready = shotAmmo(after, id);
					ammo.add(ready);
					ammo.addAll(carried);
					for (SwitchSet.Piece b : ammo)
					{
						if (b == null || b.slot != SwitchSet.AMMO || !isBolts(b.name))
						{
							continue;
						}
						SwitchSet.Piece[] shot = after.clone();
						shot[SwitchSet.AMMO] = b;
						SetCalc.Loadout l = loadout(shot, id);
						double d = SetCalc.plain(l) ? SetCalc.perTick(l, stats, target, false) : -1;
						if (d > other)
						{
							other = d;
							bolts = b == ready ? 0 : b.itemId;
						}
					}
					if (other < 0)
					{
						continue;
					}
				}
				else
				{
					// a bow with no arrows to shoot is no option
					String kind = ammoKind(bare.category, weapon == null ? null : weapon.name);
					SwitchSet.Piece arrows = shotAmmo(after, id);
					if (kind != null && (arrows == null || !has(arrows.name, kind)))
					{
						continue;
					}
					SetCalc.Loadout then = loadout(after, id);
					if (!SetCalc.plain(then))
					{
						continue;
					}
					other = SetCalc.perTick(then, stats, target, false);
				}
				// nothing lands now and something would: the notice says so instead of a percentage
				double gain = current == 0 ? (other > 0 ? Double.POSITIVE_INFINITY : Double.NaN) : SetCalc.gain(current, other);
				if (!Double.isNaN(gain) && gain >= config.weaponMinimumGain() && (best == null || gain > best.gain))
				{
					best = new WeaponAdvice();
					best.itemId = id;
					best.alsoItemId = bolts;
					best.gain = gain;
					best.text = Double.isInfinite(gain) ? "can hit" : gainText(gain);
				}
			}
		}
		finally
		{
			context = mine;
		}
		return best;
	}

	/**
	 * The ammunition the weapon shoots: what is in the ammunition slot, or what Dizana's quiver holds when
	 * the weapon uses that kind and the slot holds none of it (the slot is shot first).
	 */
	private SwitchSet.Piece shotAmmo(SwitchSet.Piece[] pieces, int weaponId)
	{
		SwitchSet.Piece slot = pieces.length > SwitchSet.AMMO ? pieces[SwitchSet.AMMO] : null;
		SwitchSet.Piece weapon = pieces.length > SwitchSet.WEAPON ? pieces[SwitchSet.WEAPON] : null;
		if (client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT) <= 0)
		{
			return slot;
		}
		SwitchSet.Piece quiver = piece(client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO));
		return quiver != null && quiverShoots(data.category(weaponId), weapon == null ? null : weapon.name,
			slot == null ? null : slot.name, quiver.name) ? quiver : slot;
	}

	/** Whether a weapon of this category shoots the quiver's ammunition instead of what is in the slot. */
	static boolean quiverShoots(String category, String weaponName, String slotName, String quiverName)
	{
		String kind = ammoKind(category, weaponName);
		return kind != null && has(quiverName, kind) && !has(slotName, kind);
	}

	/** The word in the name of the ammunition a weapon shoots: "bolts", "arrow", or null when it takes none from the slot. */
	static String ammoKind(String category, String weaponName)
	{
		if ("Crossbow".equals(category))
		{
			return "bolts";
		}
		return "Bow".equals(category) && !shootsNoAmmo(category, weaponName) ? "arrow" : null;
	}

	/** A bow that makes its own arrows: what is in the ammunition slot gives it nothing. */
	/** True for the weapons that are their own ammunition (thrown weapons, the blowpipe, chinchompas). */
	static boolean throwsItself(String category)
	{
		return "Thrown".equals(category) || "Chinchompas".equals(category);
	}

	/** True for what a bow, a crossbow or a ballista shoots. */
	static boolean isShotAmmo(String name)
	{
		return has(name, "arrow") || has(name, "bolts") || has(name, "javelin");
	}

	static boolean shootsNoAmmo(String category, String weaponName)
	{
		String weapon = weaponName == null ? "" : weaponName.toLowerCase(java.util.Locale.ROOT);
		return "Bow".equals(category) && (weapon.contains("crystal bow") || weapon.contains("faerdhinen")
			|| weapon.contains("craw's bow") || weapon.contains("webweaver bow"));
	}

	private static boolean has(String name, String part)
	{
		return name != null && name.toLowerCase(java.util.Locale.ROOT).contains(part);
	}

	/**
	 * With a crossbow in hand and no bolts on, and nothing to compare them against: the carried bolts to put on.
	 * Ruby first (carried, they are the ones to start a fight with), then diamond, then any others.
	 */
	private WeaponAdvice boltsToPutOn(SwitchSet.Piece[] on, List<SwitchSet.Piece> carried, int weaponId)
	{
		if (!"Crossbow".equals(data.category(weaponId)))
		{
			return null;
		}
		SwitchSet.Piece ammo = shotAmmo(on, weaponId);
		if (ammo != null && isBolts(ammo.name))
		{
			return null;
		}
		String[] names = new String[carried.size()];
		for (int i = 0; i < names.length; i++)
		{
			names[i] = carried.get(i).slot == SwitchSet.AMMO ? carried.get(i).name : null;
		}
		int at = preferredBolts(names);
		if (at < 0)
		{
			return null;
		}
		int kind = SetCalc.boltKind(names[at]);
		WeaponAdvice advice = new WeaponAdvice();
		advice.itemId = carried.get(at).itemId;
		advice.text = kind == SetCalc.RUBY ? "Ruby" : kind == SetCalc.DIAMOND ? "Diamond" : "";
		return advice;
	}

	/** The index of the bolts to put on among these item names: ruby (e), then diamond (e), then any other bolts; -1 for none. */
	static int preferredBolts(String[] names)
	{
		int diamond = -1;
		int other = -1;
		for (int i = 0; i < names.length; i++)
		{
			int kind = SetCalc.boltKind(names[i]);
			if (kind == SetCalc.RUBY)
			{
				return i;
			}
			if (kind == SetCalc.DIAMOND && diamond < 0)
			{
				diamond = i;
			}
			else if (kind == 0 && other < 0 && isBolts(names[i]) && !names[i].contains("(unf)"))
			{
				other = i;
			}
		}
		return diamond >= 0 ? diamond : other;
	}

	/**
	 * Bosses that change form or phase during the fight, by the start of their name. Against these no other
	 * weapon is ever suggested: knowing which weapon each form calls for is part of the fight. The pieces that
	 * go with the weapon in hand are still listed, since that follows the player's own choice of weapon.
	 */
	private static final String[] CHANGES_FORM = {
		// raids
		"Verzik Vitur", "Nylocas", "Great Olm", "Vanguard", "Elidinis' Warden", "Tumeken's Warden",
		// bosses with forms or phases that call for another weapon
		"Zulrah", "Dawn", "Dusk", "Kalphite Queen", "Phantom Muspah", "Strange Creature",
		"The Nightmare", "Phosani's Nightmare", "Crystalline Hunllef", "Corrupted Hunllef", "Abyssal Sire",
		"The Hueycoatl", "Yama", "Doom of Mokhaiotl", "Kraken", "Zalcano", "Demonic Brutus", "Maggot King",
		// monsters that change their protection, and groups fought together with a different weakness each
		"Demonic gorilla", "Tortured gorilla", "Tormented Demon",
		"Branda the Fire Queen", "Eldric the Ice King",
		// quests
		"Galvek", "Balance Elemental", "Damis", "Koschei the deathless", "Vanstrom Klause",
	};

	static boolean changesForm(String monsterName)
	{
		if (monsterName == null)
		{
			return false;
		}
		for (String boss : CHANGES_FORM)
		{
			if (monsterName.startsWith(boss))
			{
				return true;
			}
		}
		return false;
	}

	/** True while the Bolt Swap Reminder plugin is installed and on: it gives the same reminder, so this one keeps quiet. */
	private boolean boltPluginOn()
	{
		for (Plugin other : pluginManager.getPlugins())
		{
			if ("Bolt Swap Reminder".equals(other.getName()))
			{
				return pluginManager.isPluginEnabled(other);
			}
		}
		return false;
	}

	static boolean isBolts(String name)
	{
		return name != null && name.toLowerCase(java.util.Locale.ROOT).contains("bolts");
	}

	/** The bonuses of a set of pieces, with the weapon's own data. */
	private SetCalc.Loadout loadout(SwitchSet.Piece[] pieces, int weaponId)
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		List<String> names = new ArrayList<>();
		SwitchSet.Piece slotAmmo = pieces.length > SwitchSet.AMMO ? pieces[SwitchSet.AMMO] : null;
		SwitchSet.Piece ammo = shotAmmo(pieces, weaponId);
		SwitchSet.Piece inHand = pieces.length > SwitchSet.WEAPON ? pieces[SwitchSet.WEAPON] : null;
		// ammunition the weapon does not shoot gives it nothing: bolts under a bow, arrows under a crossbow,
		// anything under a bow that makes its own arrows
		if (ammo != null && ammoKind(data.category(weaponId), inHand == null ? null : inHand.name) != null
			&& !has(ammo.name, ammoKind(data.category(weaponId), inHand == null ? null : inHand.name)))
		{
			ammo = null;
		}
		else if (ammo != null && shootsNoAmmo(data.category(weaponId), inHand == null ? null : inHand.name))
		{
			ammo = null;
		}
		else if (ammo != null && throwsItself(data.category(weaponId)) && isShotAmmo(ammo.name))
		{
			// a blowpipe, darts, knives, chinchompas: what they throw is their own, not what the slot holds
			ammo = null;
		}
		List<SwitchSet.Piece> counted = new ArrayList<>();
		for (SwitchSet.Piece p : pieces)
		{
			// what sits in the ammunition slot is not counted unless it is what the weapon shoots
			if (p != null && p != slotAmmo)
			{
				counted.add(p);
			}
		}
		if (ammo != null)
		{
			counted.add(ammo);
		}
		for (SwitchSet.Piece p : counted)
		{
			l.stab += p.stab;
			l.slash += p.slash;
			l.crush += p.crush;
			l.magic += p.magic;
			l.ranged += p.ranged;
			l.strength += p.strength;
			l.rangedStrength += p.rangedStrength;
			l.magicDamage += p.magicDamage;
			l.inquisitor += p.inquisitor;
			l.mask = Math.max(l.mask, p.mask);
			l.salve = Math.max(l.salve, p.salve);
			l.crystal += SetEffects.crystalWeight(p.name);
			names.add(p.name);
		}
		l.voidMelee = SetEffects.voidKind(names, StyleCalc.MELEE);
		l.voidRanged = SetEffects.voidKind(names, StyleCalc.RANGED);
		SwitchSet.Piece weapon = pieces.length > SwitchSet.WEAPON ? pieces[SwitchSet.WEAPON] : null;
		l.weaponName = weapon == null ? "" : weapon.name;
		l.speed = weapon == null || weapon.speed <= 0 ? 4 : weapon.speed;
		if (readsDarts(l.weaponName) && blowpipeDart > 0)
		{
			// the darts inside the blowpipe, as its last Check named them
			var dart = itemManager.getItemStats(blowpipeDart);
			if (dart != null && dart.getEquipment() != null)
			{
				l.rangedStrength += dart.getEquipment().getRstr();
				l.dartKnown = true;
			}
		}
		l.category = data.category(weaponId);
		l.styles = data.styles(weaponId);
		l.bolt = ammo == null ? 0 : SetCalc.boltKind(ammo.name);
		l.boltEffect = l.bolt == 0 && ammo != null && ammo.name != null && ammo.name.endsWith("(e)");
		// the autocast spell belongs to the weapon in hand: a carried staff has none yet
		ItemContainer wornNow = client.getItemContainer(InventoryID.WORN);
		Item inHandNow = wornNow == null ? null : wornNow.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		boolean held = inHandNow != null && inHandNow.getId() > 0 && itemManager.canonicalize(inHandNow.getId()) == weaponId;
		l.spell = held ? client.getVarbitValue(VarbitID.AUTOCAST_SPELL) : 0;
		SwitchSet.Piece shield = pieces.length > SwitchSet.SHIELD ? pieces[SwitchSet.SHIELD] : null;
		String element = SetEffects.autocastElement(l.spell);
		if (shield != null && element != null && shield.name != null && shield.name.equalsIgnoreCase("Tome of " + element))
		{
			l.tome = element;
			if (SetEffects.tomeInUse(shield.name, context))
			{
				// the piece carries the tome as plain magic damage, to be ranked; the figure applies it as the game does
				l.magicDamage -= SetEffects.TOME_BONUS;
			}
		}
		return l;
	}

	/** The player's boosted levels and the prayers that are on. */
	private SetCalc.Player stats()
	{
		SetCalc.Player p = new SetCalc.Player();
		p.attack = client.getBoostedSkillLevel(Skill.ATTACK);
		p.strength = client.getBoostedSkillLevel(Skill.STRENGTH);
		p.ranged = client.getBoostedSkillLevel(Skill.RANGED);
		p.magic = client.getBoostedSkillLevel(Skill.MAGIC);
		p.prayerAttack = 100 + prayer(Prayer.PIETY, 20, Prayer.CHIVALRY, 15, Prayer.INCREDIBLE_REFLEXES, 15,
			Prayer.IMPROVED_REFLEXES, 10, Prayer.CLARITY_OF_THOUGHT, 5);
		p.prayerStrength = 100 + prayer(Prayer.PIETY, 23, Prayer.CHIVALRY, 18, Prayer.ULTIMATE_STRENGTH, 15,
			Prayer.SUPERHUMAN_STRENGTH, 10, Prayer.BURST_OF_STRENGTH, 5);
		p.prayerRanged = 100 + prayer(Prayer.RIGOUR, 20, Prayer.DEADEYE, 18, Prayer.EAGLE_EYE, 15, Prayer.HAWK_EYE, 10,
			Prayer.SHARP_EYE, 5);
		p.prayerRangedStrength = 100 + prayer(Prayer.RIGOUR, 23, Prayer.DEADEYE, 18, Prayer.EAGLE_EYE, 15, Prayer.HAWK_EYE, 10,
			Prayer.SHARP_EYE, 5);
		p.prayerMagic = 100 + prayer(Prayer.AUGURY, 25, Prayer.MYSTIC_VIGOUR, 18, Prayer.MYSTIC_MIGHT, 15, Prayer.MYSTIC_LORE, 10,
			Prayer.MYSTIC_WILL, 5);
		p.prayerMagicDamage = prayer(Prayer.AUGURY, 4, Prayer.MYSTIC_VIGOUR, 3, Prayer.MYSTIC_MIGHT, 2, Prayer.MYSTIC_LORE, 1);
		p.kandarinDiary = client.getVarbitValue(VarbitID.KANDARIN_DIARY_HARD_COMPLETE) == 1;
		p.hitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);
		return p;
	}

	/** The best melee style of the weapon in hand against the monster, or null when there is nothing to compare. */
	private Advice style(StyleData.Monster monster, ItemContainer worn, int weaponId, List<StyleData.Style> styles,
		int[] index, int selectedIndex, String selectedType)
	{
		if (styles.isEmpty() || !StyleCalc.melee(selectedType))
		{
			return null; // a bow, a staff on autocast: no melee style is in use
		}
		String weaponName = weaponId > 0 ? itemManager.getItemComposition(weaponId).getName() : "";
		StyleCalc.Input in = new StyleCalc.Input();
		if (worn != null)
		{
			for (Item item : worn.getItems())
			{
				if (item == null || item.getId() <= 0)
				{
					continue;
				}
				var stats = itemManager.getItemStats(item.getId());
				if (stats != null && stats.getEquipment() != null)
				{
					in.stab += stats.getEquipment().getAstab();
					in.slash += stats.getEquipment().getAslash();
					in.crush += stats.getEquipment().getAcrush();
					in.strength += stats.getEquipment().getStr();
				}
				in.inquisitor += inquisitorWeight(itemManager.getItemComposition(item.getId()).getName());
			}
		}
		in.attackLevel = client.getBoostedSkillLevel(Skill.ATTACK);
		in.strengthLevel = client.getBoostedSkillLevel(Skill.STRENGTH);
		in.prayerAttack = 100 + prayer(Prayer.PIETY, 20, Prayer.CHIVALRY, 15, Prayer.INCREDIBLE_REFLEXES, 15,
			Prayer.IMPROVED_REFLEXES, 10, Prayer.CLARITY_OF_THOUGHT, 5);
		in.prayerStrength = 100 + prayer(Prayer.PIETY, 23, Prayer.CHIVALRY, 18, Prayer.ULTIMATE_STRENGTH, 15,
			Prayer.SUPERHUMAN_STRENGTH, 10, Prayer.BURST_OF_STRENGTH, 5);
		in.fang = isFang(weaponName);
		in.corpbane = isCorpbane(weaponName);
		in.defenceLevel = monster.level;
		in.defenceStab = monster.stab;
		in.defenceSlash = monster.slash;
		in.defenceCrush = monster.crush;
		in.corporealBeast = "Corporeal Beast".equals(monster.name);
		in.alwaysHit = alwaysHit(monster.name);

		int best = -1;
		double bestScore = 0;
		double selectedScore = -1;
		int meleeTypes = 0;
		String firstType = null;
		for (int i = 0; i < styles.size(); i++)
		{
			StyleData.Style s = styles.get(i);
			if (!StyleCalc.melee(s.type))
			{
				continue;
			}
			if (firstType == null)
			{
				firstType = s.type;
				meleeTypes = 1;
			}
			else if (!firstType.equals(s.type))
			{
				meleeTypes = 2;
			}
			double score = StyleCalc.expectedHit(in, s.type, s.stance);
			if (score > bestScore)
			{
				bestScore = score;
				best = i;
			}
			if (index[i] == selectedIndex)
			{
				selectedScore = score;
			}
		}
		if (best == -1 || selectedScore < 0 || bestScore <= 0)
		{
			return null; // no style of this weapon can hit the monster
		}
		StyleData.Style bestStyle = styles.get(best);
		Advice a = new Advice();
		a.weaponItemId = weaponId;
		a.bestIndex = index[best];
		a.text = bestStyle.name + " (" + bestStyle.type + ")";
		a.gain = selectedScore <= 0 ? 100 : (bestScore / selectedScore - 1) * 100;
		// a gain of nothing is the selected style itself or a tie: never a mistake, even with the minimum at 0
		a.selected = a.gain <= 0;
		if (!a.selected && selectedScore > 0 && config.showGain())
		{
			a.text += " " + gainText(a.gain);
		}
		return a;
	}

	/** How much more the best style deals, as the notice writes it: "+0.4%" under ten percent, "+12%" from there on. */
	static String gainText(double gain)
	{
		return gain < 9.95 ? String.format(java.util.Locale.ROOT, "+%.1f%%", gain) : "+" + Math.round(gain) + "%";
	}

	/**
	 * The combat class of the weapon in hand: the class of the selected style, except that a staff
	 * always counts as magic (its melee styles are not why it is held).
	 */
	private String weaponClass(int weaponId, String selectedType)
	{
		String category = data.category(weaponId);
		if (category != null && category.endsWith("Staff"))
		{
			return StyleCalc.MAGIC;
		}
		return StyleCalc.combatClass(selectedType);
	}

	/** The worn piece of each equipment slot, null where the slot is empty. */
	private SwitchSet.Piece[] pieces(ItemContainer worn)
	{
		SwitchSet.Piece[] pieces = new SwitchSet.Piece[SwitchSet.SLOTS];
		if (worn == null)
		{
			return pieces;
		}
		Item[] items = worn.getItems();
		for (int slot = 0; slot < items.length && slot < pieces.length; slot++)
		{
			if (items[slot] != null && items[slot].getId() > 0)
			{
				pieces[slot] = piece(items[slot].getId());
			}
		}
		return pieces;
	}

	/** The equippable items carried, in inventory order. */
	private List<SwitchSet.Piece> inventoryPieces()
	{
		List<SwitchSet.Piece> pieces = new ArrayList<>();
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return pieces;
		}
		for (Item item : inventory.getItems())
		{
			if (item != null && item.getId() > 0)
			{
				SwitchSet.Piece p = piece(item.getId());
				if (p != null)
				{
					pieces.add(p);
				}
			}
		}
		return pieces;
	}

	/** The names of everything worn and carried. */
	private List<String> ownedNames(ItemContainer worn)
	{
		List<String> names = new ArrayList<>();
		for (ItemContainer container : new ItemContainer[]{worn, client.getItemContainer(InventoryID.INV)})
		{
			if (container == null)
			{
				continue;
			}
			for (Item item : container.getItems())
			{
				if (item != null && item.getId() > 0)
				{
					names.add(itemManager.getItemComposition(itemManager.canonicalize(item.getId())).getName());
				}
			}
		}
		return names;
	}

	/** An item as a switch piece, or null when it cannot be equipped. */
	private SwitchSet.Piece piece(int itemId)
	{
		int id = itemManager.canonicalize(itemId);
		var stats = itemManager.getItemStats(id);
		if (stats == null || stats.getEquipment() == null)
		{
			return null;
		}
		var e = stats.getEquipment();
		SwitchSet.Piece p = new SwitchSet.Piece();
		p.itemId = id;
		p.slot = e.getSlot();
		p.twoHanded = e.isTwoHanded();
		p.stab = e.getAstab();
		p.slash = e.getAslash();
		p.crush = e.getAcrush();
		p.magic = e.getAmagic();
		p.ranged = e.getArange();
		p.strength = e.getStr();
		p.rangedStrength = e.getRstr();
		p.magicDamage = e.getMdmg();
		String name = itemManager.getItemComposition(id).getName();
		p.name = name;
		p.speed = e.getAspeed();
		p.inquisitor = inquisitorWeight(name);
		p.keep = SetEffects.keep(name, context);
		p.carriedForUse = SetEffects.carriedForUse(name);
		p.mask = SetEffects.maskKind(name);
		p.salve = SetEffects.salveKind(name);
		p.blessing = context != null && context.ammoFree && SetEffects.isBlessing(name);
		if (context != null)
		{
			p.magicDamage += SetEffects.magicDamageBonus(name, context);
			p.setBonus = SetEffects.scoreBonus(name, context);
		}
		return p;
	}

	/** The first of these prayers that is on, as its percentage; 0 when none is. */
	private int prayer(Object... prayerThenPercent)
	{
		for (int i = 0; i < prayerThenPercent.length; i += 2)
		{
			if (client.isPrayerActive((Prayer) prayerThenPercent[i]))
			{
				return (Integer) prayerThenPercent[i + 1];
			}
		}
		return 0;
	}

	/**
	 * The name the client gives each Combat Options position of this weapon type ("Accurate",
	 * "Aggressive", ...), with "Other" where the position has no style, or null when the cache
	 * has no list for the type. Same lookup as RuneLite's Attack Styles plugin (script 4525).
	 */
	private String[] clientStyleNames(int weaponType)
	{
		int styleEnum = client.getEnum(EnumID.WEAPON_STYLES).getIntValue(weaponType);
		if (styleEnum == -1)
		{
			return null;
		}
		EnumComposition e = client.getEnum(styleEnum);
		int[] structs = e == null ? null : e.getIntVals();
		if (structs == null)
		{
			return null;
		}
		String[] names = new String[structs.length];
		for (int i = 0; i < structs.length; i++)
		{
			StructComposition s = client.getStructComposition(structs[i]);
			names[i] = s == null ? "Other" : s.getStringValue(ParamID.ATTACK_STYLE_NAME);
		}
		return names;
	}

	/**
	 * Where each style of the bundled table sits in the Combat Options tab. The table lists a
	 * weapon's styles in order without gaps, while the game leaves a position empty on weapons
	 * with three styles (a whip's Deflect is the fourth button): the k-th style goes to the
	 * k-th position the client names something other than "Other". Without the client's list,
	 * or when the two do not line up, the styles keep their own order.
	 */
	static int[] indices(String[] clientNames, int styleCount)
	{
		int[] index = new int[styleCount];
		for (int i = 0; i < styleCount; i++)
		{
			index[i] = i;
		}
		if (clientNames == null)
		{
			return index;
		}
		int[] used = new int[clientNames.length];
		int n = 0;
		for (int i = 0; i < clientNames.length; i++)
		{
			if (clientNames[i] != null && !clientNames[i].isEmpty() && !"Other".equalsIgnoreCase(clientNames[i]))
			{
				used[n++] = i;
			}
		}
		if (n < styleCount)
		{
			return index;
		}
		System.arraycopy(used, 0, index, 0, styleCount);
		return index;
	}

	static int inquisitorWeight(String itemName)
	{
		if (itemName == null)
		{
			return 0;
		}
		if (itemName.startsWith("Inquisitor's great helm"))
		{
			return 1;
		}
		return itemName.startsWith("Inquisitor's hauberk") || itemName.startsWith("Inquisitor's plateskirt") ? 2 : 0;
	}

	/** Monsters the calculator treats as always hit. */
	static boolean alwaysHit(String monsterName)
	{
		return "Tormented Demon".equals(monsterName);
	}

	static boolean isFang(String weaponName)
	{
		return weaponName != null && weaponName.startsWith("Osmumten's fang");
	}

	/** Weapons that deal full damage to the Corporeal Beast when they stab. */
	static boolean isCorpbane(String weaponName)
	{
		if (weaponName == null)
		{
			return false;
		}
		String n = weaponName.toLowerCase();
		return isFang(weaponName) || n.contains("halberd") || (n.contains("spear") && !n.startsWith("blue moon spear"));
	}
}
