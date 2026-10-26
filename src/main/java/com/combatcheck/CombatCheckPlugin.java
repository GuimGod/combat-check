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
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.specialcounter.SpecialCounterUpdate;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * An attention check for the player's own gear. It reminds the player of what they left undone and
 * never anticipates anything a monster will do. With the weapon the player chose to hold, it
 * reminds them of these, and of the others the README lists (a better weapon in the inventory, the
 * attack prayer, a prayer left on):
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
 * <p>RuneLite's Attack Styles plugin reads the weapon's styles the same way and warns about them.
 */
@Slf4j
@PluginDescriptor(
	name = "Combat Check",
	description = "Attention check: reminds you of the best melee style of the weapon in your hand and of switch pieces left in your inventory. It also reminds you of a better weapon you are carrying and of your attack prayer",
	tags = {"combat", "attack", "style", "stab", "slash", "crush", "switch", "gear", "missing", "reminder", "pvm"}
)
public class CombatCheckPlugin extends Plugin
{
	/** A carried weapon whose set would deal more than what is worn. */
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
		/** The weapon's name, for the line of the bank's weapon ("Bank: <weapon> +X%"). */
		String name;
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
	private int taskBossId = -1;
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
	/** How much more the gear worn would deal with the attack prayer the reminder names, in percent; NaN when unknown. */
	private volatile double prayerGain = Double.NaN;
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

	/**
	 * How many ticks after the last attack a reminder stays, from its option in seconds: 0 is for as long as the
	 * monster is the one fought (the default of every reminder; the prayer's was 5 seconds and the bolts' 15). The
	 * monster is let go when it dies, a minute passes without an attack, the player walks away or attacks another, and
	 * every reminder goes with it.
	 */
	static int afterAttackTicks(int seconds)
	{
		return seconds <= 0 ? Integer.MAX_VALUE : ShownFor.ticks(seconds);
	}
	private int targetId = -1;
	/** The table's row of the monster fought ({@link #tableMonster}), while {@link #targetId} is not -1. */
	private StyleData.Monster targetRow;
	private int targetTicks;
	private final AttentionTimer switchTimer = new AttentionTimer();
	/** The pieces are shown for a while and then not again until a piece of armour is changed. */
	private final ShownFor piecesShown = new ShownFor();
	/** The same out of a fight: time spent on screen while gearing up does not count against the fight's. */
	private final ShownFor piecesShownIdle = new ShownFor();
	/** "No attack prayer on" is shown for a while and then not again until an attack prayer is turned on. */
	private final ShownFor noPrayerShown = new ShownFor();
	/** The item ids worn, by slot, the last tick: to tell when the armour changes. */
	private int[] wornBefore;
	/** The value of the spellbook varbit on the Ancient Magicks. */
	private static final int ANCIENT_SPELLBOOK = 1;
	/** What the player is doing with the weapon in hand, for the conditional effects of armour. */
	private SetEffects.Context context;

	@Inject
	private ConfigManager configManager;

	/** Where the darts last seen in the blowpipe are kept, for each account. */
	private static final String CONFIG_GROUP = "combatcheck";
	private static final String BLOWPIPE_DART_KEY = "blowpipeDart";
	/** Whether the last Chambers of Xeric raid begun was a Challenge Mode one, for each account. */
	private static final String COX_CHALLENGE_KEY = "coxChallengeMode";
	/** The darts a blowpipe takes, by the name its Check message gives them, and their item. */
	private static final String[] DART_NAMES = {"bronze dart", "iron dart", "steel dart", "black dart", "mithril dart",
		"adamant dart", "rune dart", "amethyst dart", "dragon dart"};
	private static final int[] DART_ITEMS = {806, 807, 808, 3093, 809, 810, 811, 25849, 11230};
	private static final java.util.regex.Pattern BLOWPIPE_CHECK =
		java.util.regex.Pattern.compile("^Darts: (.+?)(?: x [\\d,]+)?\\.(?: Scales: .*)?$");
	/**
	 * The blowpipes whose darts are read, each kept apart: the Toxic blowpipe and its Blazing recolour (key "", the key of
	 * earlier versions), and the blowpipes of Sailing (OSRS Wiki, Camphor, Ironwood and Rosewood blowpipe: "Like the toxic
	 * blowpipe, it must be charged with darts"; options "Wield, Check, Unload"), as the calculator's BLOWPIPE_IDS.
	 */
	private static final String[][] BLOWPIPE_KINDS = {{"toxic blowpipe", ""}, {"blazing blowpipe", ""},
		{"camphor blowpipe", "camphor"}, {"ironwood blowpipe", "ironwood"}, {"rosewood blowpipe", "rosewood"}};
	/**
	 * The darts in each kind of blowpipe ({@link #BLOWPIPE_KINDS}): their item, 0 for none, -1 while that blowpipe has not
	 * been checked on this account.
	 */
	private final java.util.Map<String, Integer> blowpipeDarts = new java.util.HashMap<>();
	/** The kind of blowpipe last checked with its Check option, null for none: a Check message without scales is its. */
	private String blowpipeChecked;
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

	/** The kind of blowpipe an item is ({@link #BLOWPIPE_KINDS}), empty or not; null for any other item. */
	static String blowpipeKind(String itemName)
	{
		String n = itemName == null ? "" : itemName.toLowerCase(java.util.Locale.ROOT);
		for (String[] k : BLOWPIPE_KINDS)
		{
			if (n.contains(k[0]))
			{
				return k[1];
			}
		}
		return null;
	}

	/** True for the blowpipes whose Check message is read, with darts in them ({@link #BLOWPIPE_KINDS}). */
	static boolean readsDarts(String weaponName)
	{
		return blowpipeKind(weaponName) != null && !weaponName.toLowerCase(java.util.Locale.ROOT).contains("(empty)");
	}

	/**
	 * The kind of blowpipe a Check message is about: the Toxic blowpipe's (and the Blazing's) names its Zulrah's scales
	 * ("Darts: Dragon dart x 1,234. Scales: 16,383 (100.0%)."); a message without them is the one of the blowpipe of
	 * Sailing last checked ({@code lastChecked}), which takes no scales; null when that is not known.
	 */
	static String checkedKind(String message, String lastChecked)
	{
		if (message.contains(" Scales: "))
		{
			return "";
		}
		return lastChecked == null || lastChecked.isEmpty() ? null : lastChecked;
	}

	/** Where the darts of a kind of blowpipe are kept for the account. */
	private static String dartKey(String kind)
	{
		return kind.isEmpty() ? BLOWPIPE_DART_KEY : BLOWPIPE_DART_KEY + "." + kind;
	}

	/** A Check of a blowpipe: the next message without scales is about that one. */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!"Check".equals(event.getMenuOption()) || event.getItemId() <= 0)
		{
			return;
		}
		String kind = blowpipeKind(itemManager.getItemComposition(itemOf(event.getItemId())).getName());
		if (kind != null)
		{
			blowpipeChecked = kind;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		Boolean challenge = CoxChallengeMode.read(event.getType(), event.getMessage());
		if (challenge != null)
		{
			coxChallengeSeen = challenge;
			configManager.setRSProfileConfiguration(CONFIG_GROUP, COX_CHALLENGE_KEY, challenge);
			return;
		}
		if (event.getType() != net.runelite.api.ChatMessageType.GAMEMESSAGE && event.getType() != net.runelite.api.ChatMessageType.SPAM)
		{
			return;
		}
		String text = net.runelite.client.util.Text.removeTags(event.getMessage());
		if (!text.startsWith("Darts: "))
		{
			return;
		}
		String kind = checkedKind(text, blowpipeChecked);
		if (kind == null)
		{
			return;
		}
		// darts this plugin does not know are saved as such: the blowpipe gets no figure, and no reminder either
		int dart = dartFromCheck(text);
		blowpipeDarts.put(kind, dart);
		configManager.setRSProfileConfiguration(CONFIG_GROUP, dartKey(kind), dart);
	}

	/** The darts last seen in each of this account's blowpipes, and whether to ask for a Check of the one in hand. */
	private void readBlowpipe()
	{
		java.util.Set<String> unknown = new java.util.HashSet<>();
		for (String[] k : BLOWPIPE_KINDS)
		{
			String saved = configManager.getRSProfileConfiguration(CONFIG_GROUP, dartKey(k[1]));
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
			else
			{
				unknown.add(k[1]);
			}
			blowpipeDarts.put(k[1], dart);
		}
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		Item inHand = worn == null ? null : worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		String name = inHand == null || inHand.getId() <= 0 ? null : itemManager.getItemComposition(itemOf(inHand.getId())).getName();
		boolean blowpipe = readsDarts(name);
		// asked for only while nothing at all is known about the darts, and only when the reminder is on
		blowpipeNotice = blowpipe && unknown.contains(blowpipeKind(name)) && config.blowpipeReminder()
			? "Check your blowpipe" : null;
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

	/** True while the Defence the figures use counts a special attack estimated from what the client shows. */
	boolean drainsEstimated()
	{
		return drainsEstimated;
	}

	/** The attack prayer reminder to draw now, or null. */
	String prayerNotice()
	{
		return prayerNotice;
	}

	/**
	 * What the attack prayer the reminder names would add, as the notice writes it ("+18%", "+0.4%"; "0%" when it
	 * adds nothing), or null when there is no figure or the gain is not shown.
	 */
	String prayerGainText()
	{
		double g = prayerGain;
		return Double.isNaN(g) ? null : differenceText(g);
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

	/** A prayer's name as the notices write it, from RuneLite's ("PROTECT_FROM_MELEE" to "Protect from melee"). */
	static String prayerName(String constant)
	{
		String n = constant.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
		return Character.toUpperCase(n.charAt(0)) + n.substring(1);
	}

	/**
	 * The prayers that are on, with Zeal once when its varbit says it is on: Zeal drains as Piety does (OSRS Wiki,
	 * Zeal). RuneLite's Prayer list has it from 1.13.4 on, and then it is already there; before, only the varbit tells.
	 */
	static List<String> withZeal(List<String> on, boolean zealOn)
	{
		if (zealOn && !on.contains("Zeal"))
		{
			on.add("Zeal");
		}
		return on;
	}

	/** True when a prayer that drains is on, other than the ones kept on between fights. */
	private boolean drainingPrayerOn()
	{
		List<String> on = new ArrayList<>();
		for (Prayer p : Prayer.values())
		{
			if (p != Prayer.PRESERVE && p != Prayer.RAPID_HEAL && p != Prayer.RAPID_RESTORE && client.isPrayerActive(p))
			{
				on.add(prayerName(p.name()));
			}
		}
		leftOnPrayers = withZeal(on, zealOn());
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
		if (StyleCalc.MELEE.equals(combatClass) && hasZeal())
		{
			// Zeal takes Piety's place once unlocked (OSRS Wiki, Zeal)
			return "Zeal";
		}
		return bestPrayer(combatClass, client.getRealSkillLevel(Skill.PRAYER), hasPiety(), hasChivalry(),
			hasRigour(), client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) == 1,
			hasAugury(), client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) == 1);
	}

	/** Rigour: its scroll read and Defence 70 (OSRS Wiki, Rigour: "requiring 74 Prayer and 70 Defence to use"). */
	private boolean hasRigour()
	{
		return unlockedByScroll(client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED) == 1, client.getRealSkillLevel(Skill.DEFENCE));
	}

	/** Augury: its scroll read and Defence 70 (OSRS Wiki, Augury: "a Prayer level of 77 and a Defence level of 70"). */
	private boolean hasAugury()
	{
		return unlockedByScroll(client.getVarbitValue(VarbitID.PRAYER_AUGURY_UNLOCKED) == 1, client.getRealSkillLevel(Skill.DEFENCE));
	}

	/** The Defence Rigour and Augury need besides their Prayer level. */
	static final int RIGOUR_AUGURY_DEFENCE = 70;

	/**
	 * Whether Rigour or Augury can be used: the scroll is read with no requirement at all (OSRS Wiki, Dexterous and Arcane
	 * prayer scroll: "There are no skill requirements to read the scroll"), the prayer needs Defence 70, like Piety's.
	 */
	static boolean unlockedByScroll(boolean scrollRead, int defence)
	{
		return scrollRead && defence >= RIGOUR_AUGURY_DEFENCE;
	}

	/** The Knight Waves training ground is finished at this value of its varbit. */
	private static final int KNIGHT_WAVES_DONE = 8;

	/** Piety: Prayer 70, Defence 70 and the Knight Waves. */
	private boolean hasPiety()
	{
		return unlockedByKnightWaves(client.getRealSkillLevel(Skill.PRAYER), client.getRealSkillLevel(Skill.DEFENCE),
			client.getVarbitValue(VarbitID.KR_KNIGHTWAVES_STATE), 70, 70);
	}

	/** Chivalry: Prayer 60, Defence 65 and the Knight Waves. */
	private boolean hasChivalry()
	{
		return unlockedByKnightWaves(client.getRealSkillLevel(Skill.PRAYER), client.getRealSkillLevel(Skill.DEFENCE),
			client.getVarbitValue(VarbitID.KR_KNIGHTWAVES_STATE), 60, 65);
	}

	static boolean unlockedByKnightWaves(int prayer, int defence, int knightWaves, int prayerNeeded, int defenceNeeded)
	{
		return knightWaves >= KNIGHT_WAVES_DONE && prayer >= prayerNeeded && defence >= defenceNeeded;
	}

	/**
	 * The best single attack prayer for a combat class at a Prayer level, with what the player has unlocked.
	 * Without Piety or Chivalry melee takes two prayers at once, so it is named by its class; so is a class with none yet.
	 */
	static String bestPrayer(String combatClass, int level, boolean piety, boolean chivalry, boolean rigour, boolean deadeye,
		boolean augury, boolean vigour)
	{
		if (StyleCalc.MELEE.equals(combatClass))
		{
			return piety ? "Piety" : chivalry ? "Chivalry" : "Melee prayer";
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
		final StyleData bundled = new StyleData();
		data = bundled;
		reset();
		drains.clear();
		bloatShutDown.clear();
		estimate.clear();
		drainsEstimated = false;
		hydra.clear();
		bork.clear();
		borkArmour.clear();
		specWeapon = null;
		specTarget = null;
		specEnergy = -1;
		hitpointsXp = -1;
		running = true;
		migrateConfig();
		if (config.updateData())
		{
			// a newer copy of the same tables, when the plugin's repository has one: asked for once, off the client thread
			RemoteData.fetch(okHttpClient, RemoteData.bundled(), bundled,
				newer -> clientThread.invokeLater(() ->
				{
					if (running && data == bundled)
					{
						data = newer;
					}
				}));
		}
		overlayManager.add(overlay);
		overlayManager.add(buttonOverlay);
		overlayManager.add(missingOverlay);
	}

	/**
	 * The settings of earlier versions: "Estimate specs outside the party" and "Estimate BGS outside the party" are one
	 * option now, "Estimate specs" (key {@code estimateSpecs}), on when either was on; "Count defence drains" is gone (the
	 * drains are always counted). The old keys are removed.
	 */
	private void migrateConfig()
	{
		String specs = configManager.getConfiguration(CONFIG_GROUP, "estimateSpecs");
		String bgs = configManager.getConfiguration(CONFIG_GROUP, "estimateBgs");
		Boolean merged = mergedEstimate(specs, bgs);
		if (merged != null)
		{
			configManager.setConfiguration(CONFIG_GROUP, "estimateSpecs", merged);
		}
		if (bgs != null)
		{
			configManager.unsetConfiguration(CONFIG_GROUP, "estimateBgs");
		}
		if (configManager.getConfiguration(CONFIG_GROUP, "countDrains") != null)
		{
			configManager.unsetConfiguration(CONFIG_GROUP, "countDrains");
		}
	}

	/**
	 * The value "Estimate specs" takes from the two old options' saved values (null where never saved: the default, on
	 * for the specs and off for the godsword), or null when what is saved for it already holds: on when either was on.
	 */
	static Boolean mergedEstimate(String specs, String bgs)
	{
		return "false".equals(specs) && "true".equals(bgs) ? Boolean.TRUE : null;
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(buttonOverlay);
		overlayManager.remove(missingOverlay);
		reset();
		running = false;
		data = null;
	}

	/** How far from where the last attack was made the monster stops being the one fought. */
	static final int FORGET_DISTANCE = 20;
	/** Where the player stood at the last attack on the remembered monster. */
	private net.runelite.api.coords.WorldPoint targetAt;
	/** The monster itself, to know when it dies or goes away. */
	private NPC targetNpc;
	/** Off a Slayer task of it a dead monster stays remembered this long: 2 ticks are about a second, so it is gone almost at once. */
	static final int AFTER_DEATH_TICKS = 2;
	private int deadTicks;

	/**
	 * How long a dead monster stays remembered, by whether it is of the player's Slayer task: on the task, the option "After
	 * a kill on task" in seconds (it was 10), and with 0, its default, until it is let go the other ways (a minute without
	 * an attack, walking away, another monster attacked); otherwise {@link #AFTER_DEATH_TICKS}.
	 */
	static int afterDeathTicks(Boolean onTask, int taskSeconds)
	{
		return Boolean.TRUE.equals(onTask) ? afterAttackTicks(taskSeconds) : AFTER_DEATH_TICKS;
	}

	/** Counts the ticks since the remembered monster died or left the scene; true once that is too long. */
	private boolean deadTooLong()
	{
		if (targetNpc != null && !targetNpc.isDead())
		{
			return false;
		}
		StyleData.Monster monster = data.monster(targetId);
		return ++deadTicks > afterDeathTicks(monster == null ? null : onTask(monster), config.afterKillOnTask());
	}

	@Subscribe
	public void onNpcDespawned(net.runelite.api.events.NpcDespawned event)
	{
		if (event.getNpc() == targetNpc)
		{
			// gone from the scene, dead or not (the player walked away, or it left): from here it is counted
			// as a dead one, so it is forgotten after the same short time
			targetNpc = null;
		}
		// whatever lowered its Defence is gone with it: a monster seen again is a new one to the game's index
		drains.forget(event.getNpc().getIndex(), event.getNpc());
		hydra.forget(event.getNpc());
		bork.forget(event.getNpc());
		borkArmour.bork(event.getNpc().getId());
	}

	/** An Alchemical Hydra that comes into the scene starts its phase with its damage reduction up. */
	@Subscribe
	public void onNpcSpawned(net.runelite.api.events.NpcSpawned event)
	{
		NPC npc = event.getNpc();
		hydra.form(npc, npc.getId(), client.getTickCount());
		borkArmour.bork(npc.getId());
	}

	/** The Alchemical Hydra's roar when the right vent sprays it: its damage reduction is off ({@link HydraShield}). */
	@Subscribe
	public void onOverheadTextChanged(net.runelite.api.events.OverheadTextChanged event)
	{
		if (event.getActor() instanceof NPC)
		{
			NPC npc = (NPC) event.getActor();
			hydra.says(npc, npc.getId(), event.getOverheadText(), client.getTickCount());
			bork.says(npc, npc.getId(), event.getOverheadText(), client.getTickCount());
			borkArmour.says(npc.getId(), event.getOverheadText());
		}
	}

	/** The Defence drains counted on each monster around: the player's own special attacks and the party's. */
	private final DrainTracker drains = new DrainTracker();
	/**
	 * The Elder maul and Dragon warhammer special attacks of players whose specs do not come through the party, and their
	 * Bandos godsword's with its own option.
	 */
	private final SpecEstimate estimate = new SpecEstimate();
	/** True while the Defence used for the monster fought counts one of those ({@link #drainsEstimated}). */
	private volatile boolean drainsEstimated;
	/** An estimate is taken out when the player's own message of it comes this many ticks after it, or fewer. */
	static final int ESTIMATE_REPLACED_TICKS = 10;
	/** Whether each Alchemical Hydra around has its damage reduction up: its hitsplats are a quarter of the damage. */
	private final HydraShield hydra = new HydraShield();
	/** Bork of the Fractured Archive stunned: his flat armour is 0 and his Defence is not known ({@link BorkStun}). */
	private final BorkStun bork = new BorkStun();
	/** Bork's flat armour and magic resistance by the Smorks' health ({@link BorkArmour}). */
	private final BorkArmour borkArmour = new BorkArmour();
	/** The weapons of the bank last seen open and the line worked out from them ({@link BankHint}). */
	private final BankHint.Memory bank = new BankHint.Memory();
	/** The line of the best weapon in the bank to draw now, or null. */
	private volatile WeaponAdvice bankAdvice;

	WeaponAdvice bankAdvice()
	{
		return bankAdvice;
	}

	/** The bank seen open: its weapons are kept for the line of the best weapon in the bank ({@link BankHint}). */
	@Subscribe
	public void onItemContainerChanged(net.runelite.api.events.ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.BANK || event.getItemContainer() == null)
		{
			return;
		}
		List<Integer> weapons = new ArrayList<>();
		for (Item item : event.getItemContainer().getItems())
		{
			if (item == null || item.getId() <= 0)
			{
				continue;
			}
			int id = itemOf(item.getId());
			var e = equipment(id);
			if (e != null && e.getSlot() == SwitchSet.WEAPON && data != null && data.category(id) != null)
			{
				weapons.add(id);
			}
		}
		bank.bank(weapons);
	}

	/** The varbits of the Fractured Archive's party: one for each of the eight places, 0 where nobody is. */
	private static final int[] TFA_PARTY = {VarbitID.TFA_CLIENT_P0, VarbitID.TFA_CLIENT_P1, VarbitID.TFA_CLIENT_P2,
		VarbitID.TFA_CLIENT_P3, VarbitID.TFA_CLIENT_P4, VarbitID.TFA_CLIENT_P5, VarbitID.TFA_CLIENT_P6, VarbitID.TFA_CLIENT_P7};

	/**
	 * The best weapon of the bank last seen open against the monster fought, with how much more it deals than what is
	 * worn ({@link BankHint}); null outside a fight, inside a raid, with the option off, the bank never seen, or no weapon
	 * of it dealing at least the minimum more. Worked out again only when the monster, what is worn, the bank or the
	 * setting changes.
	 */
	private WeaponAdvice bankWeaponNow(StyleData.Monster monster, ItemContainer worn, int weaponId, SwitchSet.Piece[] on,
		boolean sameClassOnly)
	{
		List<Integer> weapons = bank.weapons();
		if (!config.bankWeapon() || monster == null || weapons == null
			|| !BankHint.allowed(client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1,
				client.getVarbitValue(VarbitID.TOB_CLIENT_PARTYSTATUS), inToaRoom(), members(TFA_PARTY), monster))
		{
			bank.forgetLine();
			return null;
		}
		NPC npc = targetNpc;
		List<Integer> wornIds = new ArrayList<>();
		for (SwitchSet.Piece p : on)
		{
			wornIds.add(p == null ? 0 : p.itemId);
		}
		double minimum = minimumGain(config.bankMinimumGain());
		List<Object> key = java.util.Arrays.asList(monster.id, npc == null ? -1 : npc.getIndex(), wornIds, bank.version(),
			minimum, sameClassOnly);
		if (!bank.changed(key))
		{
			return advice(bank.last());
		}
		BankHint.Result result = null;
		SetCalc.Loadout now = loadout(on, weaponId);
		if (SetCalc.plain(now) && !SetEffects.pointsAtNoOtherWeapon(now.weaponName, now.spell))
		{
			SetCalc.Player stats = stats();
			SetCalc.Target target = target(monster);
			String classNow = SetCalc.combatClass(now);
			double current = SetCalc.perTick(now, SetCalc.withPrayerOf(stats, classNow), target, false);
			int npcId = npc != null ? npc.getId() : monster.id;
			List<BankHint.Candidate> candidates = new ArrayList<>();
			SetEffects.Context mine = context;
			try
			{
				for (int id : weapons)
				{
					String category = data.category(id);
					SetCalc.Loadout bare = new SetCalc.Loadout();
					bare.category = category;
					String cls = SetCalc.combatClass(bare);
					if (id == weaponId || cls == null || sameClassOnly && !cls.equals(classNow) || !classAllowed(npcId, cls))
					{
						continue;
					}
					context = contextFor(id, cls, null, worn);
					SwitchSet.Piece weapon = piece(id);
					if (weapon == null || weapon.slot != SwitchSet.WEAPON || !BankHint.usable(weapon.name))
					{
						continue;
					}
					SwitchSet.Piece[] with = BankHint.withWeapon(pieces(worn), weapon);
					// a bow or a crossbow counts only with ammunition it fires on (worn or in Dizana's quiver)
					if (!BankHint.firesWhatIsOn(id, category, weapon.name, shotAmmo(with, id)))
					{
						continue;
					}
					candidates.add(new BankHint.Candidate(id, weapon.name, loadout(with, id)));
				}
			}
			finally
			{
				context = mine;
			}
			result = BankHint.best(current, candidates, stats, target, minimum);
		}
		bank.remember(key, result);
		return advice(result);
	}

	/** The line of the bank's weapon as the overlay draws it. */
	private static WeaponAdvice advice(BankHint.Result result)
	{
		if (result == null)
		{
			return null;
		}
		WeaponAdvice a = new WeaponAdvice();
		a.itemId = result.itemId;
		a.gain = result.gain;
		a.name = result.name;
		a.text = Double.isInfinite(result.gain) ? "can hit" : gainText(result.gain);
		return a;
	}

	/** The Chambers of Xeric raid the player is in, read each game tick; null outside it. */
	private volatile CoxScaling.Raid raid;
	/**
	 * Whether the last raid begun was a Challenge Mode one, by the game's message ({@link CoxChallengeMode}); kept for
	 * each account, so a relog or a restart inside the raid keeps it. Null until read from the account's profile.
	 */
	private Boolean coxChallengeSeen;
	/** The Theatre of Blood's party, read each game tick; null when it is not known or the option is off. */
	private volatile TobScaling.Raid tobRaid;
	/** The Tombs of Amascut's raid level, party and path levels, read each game tick; null with the option off. */
	private volatile ToaScaling.Raid toaRaid;
	/** The special attack energy last seen, -1 before the first. */
	private int specEnergy = -1;
	/** The Hitpoints experience last seen, and the client cycle it last grew on (an Elder maul hit deals damage). */
	private int hitpointsXp = -1;
	private int hitpointsXpCycle;
	/**
	 * The player's own special attack waiting for its hitsplat: the weapon, the monster, the tick it lands on and how many
	 * hitsplats it deals.
	 */
	private SpecialWeapon specWeapon;
	private NPC specTarget;
	private int specTick;
	private int specSplats;
	private boolean specDealtDamage;
	private final List<Integer> specHits = new ArrayList<>();

	/**
	 * The player's own special attacks are found the way RuneLite's Special Attack Counter finds them: the energy
	 * drops while one of its weapons is held, and the player's hitsplat lands on the monster attacked on the tick the
	 * attack hits. They are counted here, in a party too: the party's message about them is not counted again.
	 */
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() != VarPlayerID.SA_ENERGY)
		{
			return;
		}
		int energy = event.getValue();
		boolean used = specEnergy != -1 && energy < specEnergy;
		specEnergy = energy;
		if (!used)
		{
			return;
		}
		// the player and the monsters are not updated yet: what is attacked is read once they are
		final int serverTick = client.getTickCount();
		clientThread.invokeLater(() ->
		{
			int item = weaponInHand();
			specWeapon = specialWeapon(item);
			specSplats = specialHits(item);
			specHits.clear();
			Actor target = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getInteracting();
			specTarget = specWeapon != null && target instanceof NPC ? (NPC) target : null;
			if (specTarget == null)
			{
				specWeapon = null;
				return;
			}
			specDealtDamage = hitpointsXpCycle == client.getGameCycle();
			int distance = 1;
			net.runelite.api.coords.WorldPoint at = client.getLocalPlayer().getWorldLocation();
			net.runelite.api.coords.WorldArea area = specTarget.getWorldArea();
			if (at != null && area != null)
			{
				distance = area.distanceTo(at);
			}
			specTick = serverTick + specWeapon.getHitDelay(distance);
		});
	}

	/** The item id of the weapon in the player's hand, -1 for none. */
	private int weaponInHand()
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		Item weapon = worn == null ? null : worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		return weapon == null ? -1 : weapon.getId();
	}

	/**
	 * The Special Attack Counter's weapon of an item, or null. Two that it does not list are read as the weapon whose special
	 * attack they have (OSRS Wiki): the Arclight without charges, which "functions identically to Darklight" (Arclight,
	 * Charging), and the Tonalztics of Ralos without charges, whose special attack hits once ({@link #specialHits}).
	 * No party message comes for these two: that plugin does not see them.
	 */
	static SpecialWeapon specialWeapon(int itemId)
	{
		// the item as it is, then as the item it is another dress of (Variants)
		for (int item : new int[]{itemId, Variants.base(itemId)})
		{
			for (SpecialWeapon w : SpecialWeapon.values())
			{
				for (int id : w.getItemID())
				{
					if (id == item)
					{
						return w;
					}
				}
			}
		}
		if (itemId == net.runelite.api.gameval.ItemID.ARCLIGHT_INACTIVE)
		{
			return SpecialWeapon.DARKLIGHT;
		}
		return itemId == net.runelite.api.gameval.ItemID.TONALZTICS_OF_RALOS_UNCHARGED ? SpecialWeapon.TONALZTICS_OF_RALOS
			: null;
	}

	/**
	 * How many hitsplats an item's special attack deals: two for the charged Tonalztics of Ralos, one for anything else,
	 * the uncharged Tonalztics among them (OSRS Wiki, Tonalztics of Ralos: "Uncharged, it will only hit once").
	 */
	static int specialHits(int itemId)
	{
		return itemId == net.runelite.api.gameval.ItemID.TONALZTICS_OF_RALOS_CHARGED ? 2 : 1;
	}

	/**
	 * The hits of a Tonalztics of Ralos' special attack that landed, of the last {@code splats} hitsplats on the tick it
	 * hits (the weapon's come last, as the Special Attack Counter reads them); null while they have not all come.
	 */
	static Integer landedHits(List<Integer> hitsplats, int splats)
	{
		int n = hitsplats.size();
		if (n < splats)
		{
			return null;
		}
		int landed = 0;
		for (int i = n - splats; i < n; i++)
		{
			landed += Math.min(hitsplats.get(i), 1);
		}
		return landed;
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() == Skill.HITPOINTS)
		{
			if (event.getXp() > hitpointsXp)
			{
				hitpointsXpCycle = client.getGameCycle();
			}
			hitpointsXp = event.getXp();
		}
	}

	@Subscribe
	public void onFakeXpDrop(FakeXpDrop event)
	{
		if (event.getSkill() == Skill.HITPOINTS)
		{
			hitpointsXpCycle = client.getGameCycle();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (estimating() && targetNpc != null && event.getActor() == targetNpc)
		{
			net.runelite.api.Hitsplat h = event.getHitsplat();
			estimate.hitsplat(client.getTickCount(), targetNpc, h.getAmount(), h.getHitsplatType(), h.isMine());
		}
		if (specTarget != null && event.getActor() == specTarget && event.getHitsplat().isMine()
			&& client.getTickCount() == specTick)
		{
			specHits.add(event.getHitsplat().getAmount());
		}
	}

	/** Counts the player's own special attack once it has landed, as the Special Attack Counter reads its hit. */
	private void landSpecialAttack()
	{
		if (specWeapon == null || specTarget == null)
		{
			return;
		}
		int tick = client.getTickCount();
		Integer hit = null;
		if (specWeapon == SpecialWeapon.ELDER_MAUL)
		{
			// read from the Hitpoints experience on the tick of the attack, without waiting for the hitsplat
			hit = specDealtDamage ? 1 : 0;
		}
		else if (tick == specTick)
		{
			int n = specHits.size();
			if (specWeapon == SpecialWeapon.TONALZTICS_OF_RALOS)
			{
				// the hits that landed: two hitsplats for the charged weapon, one for the uncharged
				hit = landedHits(specHits, specSplats);
			}
			else
			{
				// the weapon's hitsplat comes last, after a thrall's or vengeance's on the same tick
				hit = n == 0 ? null : specHits.get(n - 1);
			}
			if (hit == null)
			{
				return; // the hitsplat may still come this tick
			}
		}
		else if (tick < specTick)
		{
			return;
		}
		if (hit != null)
		{
			// the damage rolled, where the Alchemical Hydra's reduction cut the hitsplat
			int rolled = HydraShield.rolled(specWeapon, hit, hydra.shielded(specTarget));
			// by the tick its hit lands: an Elder maul is read 2 ticks before it, the others on that tick
			drains.add(specTarget.getIndex(), specTarget, specWeapon, rolled, DefenceDrains.tekton(specTarget.getId()),
				specTick);
		}
		specWeapon = null;
		specTarget = null;
		specHits.clear();
	}

	@Inject
	private PartyService party;

	/**
	 * A party member's special attack, sent by their Special Attack Counter (it is read only while this player has
	 * that plugin on). The player's own comes back the same way and is skipped: it was counted when it landed.
	 */
	@Subscribe
	public void onSpecialCounterUpdate(SpecialCounterUpdate event)
	{
		if (party.getLocalMember() == null || party.getLocalMember().getMemberId() == event.getMemberId())
		{
			return;
		}
		clientThread.invoke(() ->
		{
			if (event.getWorld() != client.getWorld())
			{
				return;
			}
			// this player's special attacks come exactly from now on: never estimated, and an estimate of this one is
			// replaced by it (the party's members are counted once), until they leave the party
			estimate.partySpec(event.getPlayerId());
			partyPlayers.put(event.getMemberId(), event.getPlayerId());
			NPC npc = client.getTopLevelWorldView().npcs().byIndex(event.getNpcIndex());
			if (npc != null)
			{
				drains.dropEstimated(npc.getIndex(), npc, event.getPlayerId(), event.getWeapon(),
					client.getTickCount() - ESTIMATE_REPLACED_TICKS);
				int rolled = HydraShield.rolled(event.getWeapon(), event.getHit(), hydra.shielded(npc));
				drains.add(npc.getIndex(), npc, event.getWeapon(), rolled, DefenceDrains.tekton(npc.getId()),
					partyHitTick(event.getWeapon(), client.getTickCount()));
			}
		});
	}

	/** The game's index of each party member whose special attack came through the party, by their member id. */
	private final java.util.Map<Long, Integer> partyPlayers = new java.util.HashMap<>();

	/** A member leaves the party: their special attacks no longer come through it, so they are estimated again. */
	@Subscribe
	public void onUserPart(UserPart event)
	{
		clientThread.invoke(() ->
		{
			Integer player = partyPlayers.remove(event.getMemberId());
			if (player != null)
			{
				estimate.notSender(player);
			}
		});
	}

	/** Another party, or none: nobody is known to send through it any more. */
	@Subscribe
	public void onPartyChanged(PartyChanged event)
	{
		clientThread.invoke(() ->
		{
			partyPlayers.clear();
			estimate.noSenders();
		});
	}

	/**
	 * The tick a party member's special attack is counted on, from the tick its message came: the drains go in the order
	 * the hits landed (OSRS Wiki, Hit delay: an attack is queued and "processed on an entity" when it hits; Tekton: "If
	 * multiple specials land on the same tick"). The Special Attack Counter sends an Elder maul's on the tick of the attack,
	 * without waiting for its hit, which lands {@link SpecEstimate#ELDER_MAUL_DELAY} ticks later ("The hitsplat is applied
	 * 2t after spec unlike most melee weapons"), and any other weapon's on the tick of its hit: so a godsword that lands
	 * between an Elder maul's attack and its hit is counted first, as in the game.
	 */
	static int partyHitTick(SpecialWeapon weapon, int received)
	{
		return weapon == SpecialWeapon.ELDER_MAUL ? received + SpecEstimate.ELDER_MAUL_DELAY : received;
	}

	/**
	 * A monster that changes into one with another Defence (a new phase) keeps nothing of the drains, nor one whose Defence
	 * the game gives back on the change (the Phantom Muspah's shield, Galvek's phases: {@link DefenceDrains#resets}).
	 */
	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		hydra.form(npc, npc.getId(), client.getTickCount());
		StyleData d = data;
		if (d == null || event.getOld() == null)
		{
			return;
		}
		StyleData.Monster before = d.monster(event.getOld().getId(), event.getOld().getName());
		StyleData.Monster now = tableMonster(npc);
		switch (formChange(before, now, event.getOld().getId(), npc.getId()))
		{
			case UNKNOWN:
				// Ocellus's next phase: the drains go on, but how is not known (DefenceDrains.carriesUnknown)
				drains.carryUnknown(npc.getIndex(), npc);
				break;
			case FORGET:
				drains.forget(npc.getIndex(), npc);
				break;
			default:
				break;
		}
	}

	/** What a change of form does to the drains counted on the monster. */
	enum FormChange
	{
		/** They go on as they were: the same Defence (the Archive Guardian landing and rising). */
		KEEP,
		/** They are dropped: another Defence, or one the game gives back ({@link DefenceDrains#resets}). */
		FORGET,
		/** They go on in a way that is not known ({@link DefenceDrains#carriesUnknown}): the Defence left is "?". */
		UNKNOWN
	}

	/** What the change of the monster from {@code oldId} (row {@code before}) into {@code newId} (row {@code now}) does. */
	static FormChange formChange(StyleData.Monster before, StyleData.Monster now, int oldId, int newId)
	{
		if (DefenceDrains.carriesUnknown(oldId, newId))
		{
			return FormChange.UNKNOWN;
		}
		return before == null || now == null || before.level != now.level || DefenceDrains.resets(oldId, newId)
			? FormChange.FORGET : FormChange.KEEP;
	}

	/**
	 * The monster's levels in this fight: scaled for the raid it is in (the Chambers of Xeric for the party and mode,
	 * the Theatre of Blood for the party, the Tombs of Amascut for the raid level, path and party), then Araxxor's
	 * enrage; then, for the monster fought, lowered by the special attacks counted on it, in the order they landed
	 * ({@link DefenceDrains#left}: none for a fight immune to drains, the floors, what the Nightmares win back).
	 */
	private DefenceDrains.Levels levels(StyleData.Monster monster)
	{
		DefenceDrains.Levels full = fullLevels(monster);
		NPC npc = targetNpc;
		if (npc == null || tableMonster(npc) != monster)
		{
			return full;
		}
		return DefenceDrains.left(monster, npc.getId(), full, drains.of(npc.getIndex(), npc),
			drains.ticksSince(npc.getIndex(), npc, client.getTickCount()));
	}

	/**
	 * The monster's levels in this fight before any special attack: the raid's scaling (the Fractured Archive's bosses with
	 * the hitpoints of their health bar), Araxxor's and the Warden's enrage.
	 */
	private DefenceDrains.Levels fullLevels(StyleData.Monster monster)
	{
		DefenceDrains.Levels full = MonsterLevels.scaled(monster, raid, tobRaid, toaRaid);
		full = MonsterLevels.fromBar(monster, full, archiveHitpoints(monster));
		full = MonsterLevels.enraged(monster, full, healthNow(monster, full.hitpoints));
		return MonsterLevels.wardenEnraged(monster, full, wardenEnraged);
	}

	/** The line of Dinh's bulwark's special attack to draw now, or null. */
	private volatile String bulwarkSpecLine;

	String bulwarkSpecLine()
	{
		return bulwarkSpecLine;
	}

	/**
	 * What Shield Bash would deal now (OSRS Wiki, Dinh's bulwark, "Special attack"): with the bulwark in hand in its
	 * Pummel style (the Block style does not attack) and half the special attack energy left, on the monster fought,
	 * and in a multicombat area on each other monster of the table within 5 tiles that can be attacked, up to 10 in
	 * all. Null otherwise.
	 */
	private String bulwarkSpecLineNow(StyleData.Monster monster, SwitchSet.Piece[] on, int weaponId, String meleeType,
		Player me)
	{
		NPC npc = targetNpc;
		if (monster == null || npc == null || me == null || !StyleCalc.CRUSH.equals(meleeType)
			|| client.getVarpValue(VarPlayerID.SA_ENERGY) < SetCalc.BULWARK_SPEC_COST)
		{
			return null;
		}
		SetCalc.Loadout l = loadout(on, weaponId);
		if (!SetCalc.isBulwark(l.weaponName))
		{
			return null;
		}
		SetCalc.Player p = stats();
		double[] main = SetCalc.bulwarkSpec(l, p, target(monster));
		if (main == null)
		{
			return null;
		}
		List<double[]> others = new ArrayList<>();
		net.runelite.api.coords.WorldPoint at = me.getWorldLocation();
		if (client.getVarbitValue(VarbitID.MULTIWAY_INDICATOR) == 1 && at != null)
		{
			for (NPC other : client.getTopLevelWorldView().npcs())
			{
				if (others.size() >= SetCalc.BULWARK_SPEC_TARGETS - 1)
				{
					break;
				}
				net.runelite.api.coords.WorldArea area = other == null ? null : other.getWorldArea();
				StyleData.Monster m = area == null || other == npc || other.isDead() || !canBeFought(other)
					|| area.distanceTo(at) > SetCalc.BULWARK_SPEC_REACH ? null : tableMonster(other);
				double[] hit = m == null ? null : SetCalc.bulwarkSpec(l, p, otherTarget(m));
				if (hit != null)
				{
					others.add(hit);
				}
			}
		}
		return bulwarkSpecText(config.language(), main, others);
	}

	/** Another monster than the one fought: its levels without the drains counted on the one fought. */
	private SetCalc.Target otherTarget(StyleData.Monster monster)
	{
		SetCalc.Target t = monster.target(fullLevels(monster));
		t.onTask = Boolean.TRUE.equals(onTask(monster));
		t.inWilderness = client.getVarbitValue(net.runelite.api.Varbits.IN_WILDERNESS) == 1;
		t.undead = monster.undead;
		t.toaInvocation = ToaScaling.invocation(monster, toaRaid);
		t.defencelessShare = defencelessShare(monster);
		return t;
	}

	/**
	 * The line of Shield Bash: on the monster fought its two hits ({mean, max} of one hit each, from
	 * {@link SetCalc#bulwarkSpec}), and with others in reach their count and the mean and max of one hit on each (the
	 * mean of their means, the highest of their maxes).
	 */
	static String bulwarkSpecText(CombatCheckConfig.Language language, double[] main, List<double[]> others)
	{
		String mean = String.format(java.util.Locale.ROOT, "%.1f", 2 * main[0]);
		int max = (int) (2 * main[1]);
		if (others.isEmpty())
		{
			return String.format(Words.of(language, Words.Key.BASH_ONE), mean, max);
		}
		double sum = 0;
		double top = 0;
		for (double[] o : others)
		{
			sum += o[0];
			top = Math.max(top, o[1]);
		}
		return String.format(Words.of(language, Words.Key.BASH_MANY), others.size() + 1, mean, max,
			String.format(java.util.Locale.ROOT, "%.1f", sum / others.size()), (int) top);
	}

	/** The "Defence left" line to draw now ("Def 246 -> 158 (3 specs)"), or null. */
	private volatile String defenceLine;

	String defenceLine()
	{
		return defenceLine;
	}

	/**
	 * The "Defence left" line: the Defence before the special attacks and the one the figures use now, and how many
	 * were counted, misses included; "~" before the Defence now when an estimated one is in it. Null with none counted.
	 */
	static String defenceText(CombatCheckConfig.Language language, int full, int left, int specs, boolean estimated)
	{
		return levelText(language, Words.Key.DEFENCE_LEFT, full, left, specs, estimated);
	}

	private static String levelText(CombatCheckConfig.Language language, Words.Key key, int full, int left, int specs,
		boolean estimated)
	{
		if (specs <= 0)
		{
			return null;
		}
		String count = specs == 1 ? Words.of(language, Words.Key.ONE_SPEC)
			: String.format(Words.of(language, Words.Key.SPECS), specs);
		return String.format(Words.of(language, key), full, (estimated ? "~" : "") + left) + " (" + count + ")";
	}

	/**
	 * The line for the weapon in hand: the special attacks lower the monster's levels, not its bonuses. Melee and
	 * ranged roll against its Defence level, so the line gives that. Magic rolls against its Magic level, or against
	 * its Defence level on the monsters whose magic defence is rolled with Defence (the table's rule "D", as SetCalc
	 * rolls it: the Ice demon, Verzik...): there the Defence line too; elsewhere the Magic level when a special attack
	 * lowered it (the Bandos godsword's damage beyond the Defence, the Accursed sceptre, the Seercull), and nothing
	 * when none did: the drains do not help magic.
	 */
	static String defenceTextFor(CombatCheckConfig.Language language, String combatClass, boolean magicFromDefence,
		DefenceDrains.Levels full, DefenceDrains.Levels left, int specs, boolean estimated)
	{
		if (StyleCalc.MAGIC.equals(combatClass) && !magicFromDefence)
		{
			return left.magic < full.magic
				? levelText(language, Words.Key.MAGIC_LEFT, full.magic, left.magic, specs, estimated) : null;
		}
		return defenceText(language, full.defence, left.defence, specs, estimated);
	}

	/**
	 * Whether the line is shown for this monster: one whose Defence can be lowered (not a fight immune to drains, not
	 * Verzik Vitur or Vardorvis, whose floor is their whole Defence), with at least one special attack counted on it.
	 */
	static boolean showsDefence(boolean immune, int floor, int full, int specs)
	{
		return !immune && floor < full && specs > 0;
	}

	/**
	 * Whether the monster's Defence is not known now (the line says "Def ?"): drains carried over a change nobody knows
	 * the rule of (Ocellus's next phase), a special attack counted on a monster no drain is confirmed on (Ezekiel), or
	 * Bork stunned (the examine of the stun was never taken).
	 */
	static boolean defenceUnknown(boolean carriedUnknown, boolean notConfirmed, boolean borkStunned, int specs)
	{
		return carriedUnknown || notConfirmed && specs > 0 || borkStunned;
	}

	/** The line of a Defence not known: "Def ?", with the special attacks counted when there are any. */
	static String unknownDefenceText(CombatCheckConfig.Language language, int specs)
	{
		String text = Words.of(language, Words.Key.DEFENCE_UNKNOWN);
		if (specs <= 0)
		{
			return text;
		}
		return text + " (" + (specs == 1 ? Words.of(language, Words.Key.ONE_SPEC)
			: String.format(Words.of(language, Words.Key.SPECS), specs)) + ")";
	}

	/** Works out the "Defence left" line for the monster fought, or null. */
	private String defenceLineNow(StyleData.Monster monster, String combatClass)
	{
		NPC npc = targetNpc;
		if (!config.showDefence() || monster == null || npc == null || tableMonster(npc) != monster)
		{
			return null;
		}
		int specs = drains.counted(npc.getIndex(), npc);
		if (defenceUnknown(drains.unknown(npc.getIndex(), npc), DefenceDrains.notConfirmed(npc.getId()), borkStunned(monster),
			specs))
		{
			return unknownDefenceText(config.language(), specs);
		}
		DefenceDrains.Levels full = fullLevels(monster);
		boolean immune = DefenceDrains.immune(monster) || DefenceDrains.immune(npc.getId());
		if (!showsDefence(immune, DefenceDrains.floor(npc.getId(), full.defence), full.defence, specs))
		{
			return null;
		}
		return defenceTextFor(config.language(), combatClass, monster.magicDefenceFromDefence, full, levels(monster), specs,
			drains.estimated(npc.getIndex(), npc));
	}

	/**
	 * The Chambers of Xeric's scaling while the player is inside the raid, read from the game: the party size the
	 * raid was scaled for, Challenge Mode (its varbit, or the game's message at the raid's start: the varbit reads 0
	 * for a member who is not the leader) and the party's highest combat level. The highest Hitpoints level has no
	 * varbit: 99 is taken, and 99 Mining for each member (the Guardians' hitpoints). Null outside the raid or with
	 * the option off.
	 */
	private CoxScaling.Raid raidNow()
	{
		if (!config.coxScaling() || client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return null;
		}
		int size = client.getVarbitValue(VarbitID.RAIDS_CLIENT_PARTYSIZE_SCALED);
		if (size <= 0)
		{
			size = client.getVarbitValue(VarbitID.RAIDS_CLIENT_PARTYSIZE);
		}
		size = Math.max(1, size);
		int combat = client.getVarbitValue(VarbitID.RAIDS_CLIENT_HIGHESTCOMBAT);
		boolean challenge = client.getVarbitValue(VarbitID.RAIDS_CHALLENGE_MODE) != 0 || challengeSeen();
		CoxScaling.Raid now = new CoxScaling.Raid(size, challenge, combat > 0 ? combat : 126, 99, 99 * size);
		return now.equals(raid) ? raid : now;
	}

	/** {@link #coxChallengeSeen}, read from the account's profile the first time. */
	private boolean challengeSeen()
	{
		if (coxChallengeSeen == null)
		{
			coxChallengeSeen = "true".equals(configManager.getRSProfileConfiguration(CONFIG_GROUP, COX_CHALLENGE_KEY));
		}
		return coxChallengeSeen;
	}

	/**
	 * Whether the Warden of the Tombs of Amascut's third phase is enraged: the phantom of Amascut behind it opens her mouth
	 * when it does (OSRS Wiki, Phantom (Tombs of Amascut): "once the Warden reaches critical health, she will open her
	 * mouth"; the open form is the NPC RuneLite names TOA_AMASCUT_P3_ENRAGE, 11768). Read each game tick.
	 */
	private boolean wardenEnraged;

	/**
	 * Reads the phantom's form. On entering the enrage the Warden's Defence is set to 180 (OSRS Wiki, Tumeken's Warden):
	 * the drains counted on it before are dropped.
	 */
	private void readWardenEnrage()
	{
		boolean now = false;
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			for (NPC npc : client.getTopLevelWorldView().npcs())
			{
				if (npc != null && npc.getId() == net.runelite.api.gameval.NpcID.TOA_AMASCUT_P3_ENRAGE)
				{
					now = true;
					break;
				}
			}
		}
		if (now && !wardenEnraged && data != null)
		{
			for (NPC npc : client.getTopLevelWorldView().npcs())
			{
				StyleData.Monster m = npc == null ? null : tableMonster(npc);
				if (m != null && m.enragedDefence > 0)
				{
					drains.forget(npc.getIndex(), npc);
				}
			}
		}
		wardenEnraged = now;
	}

	/**
	 * The Smorks' health for Bork's armour ({@link BorkArmour}): from the boss health display when it shows them, and from
	 * their health bar over them. Read each game tick.
	 */
	private void readSmorks()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		int max = client.getVarbitValue(net.runelite.api.gameval.VarbitID.HPBAR_HUD_BASEHP);
		if (client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC) == BorkArmour.SMORKS && max > 0)
		{
			borkArmour.display(client.getVarbitValue(net.runelite.api.gameval.VarbitID.HPBAR_HUD_HP), max);
		}
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc != null && npc.getId() == BorkArmour.SMORKS)
			{
				borkArmour.bar(npc.getHealthRatio(), npc.getHealthScale());
			}
		}
	}

	/** The most members of the Fractured Archive's party seen since its places were first held; 0 outside the raid. */
	private int archivePartyLocked;

	/**
	 * The team the Fractured Archive's bosses are scaled for: its party's places that hold someone, the most since the
	 * raid began ({@link #lockedParty}; the scaling is set at the start of the raid, and a player who dies does not
	 * lower it). Read each game tick.
	 */
	private void archiveParty()
	{
		int members = client.getGameState() == GameState.LOGGED_IN ? members(TFA_PARTY) : 0;
		archivePartyLocked = lockedParty(archivePartyLocked, members > 0, members);
	}

	/**
	 * The hitpoints of a boss of the Fractured Archive in this raid: the most its boss health bar shows, or else the OSRS
	 * Wiki's table for the team ({@link ArchiveScaling}); 0 when neither is known or for a monster outside the raid.
	 */
	private int archiveHitpoints(StyleData.Monster monster)
	{
		if (!monster.fracturedArchive)
		{
			return 0;
		}
		return raidHitpoints(barMax(monster), monster.id, archivePartyLocked);
	}

	/** A boss of the Fractured Archive's hitpoints: the bar's most ({@code bar}) when seen, else the table's for the team. */
	static int raidHitpoints(int bar, int npcId, int team)
	{
		return bar > 0 ? bar : ArchiveScaling.hitpoints(npcId, team);
	}

	/** The varbits of the Theatre of Blood's party: one for each of the five places, 0 where nobody is. */
	private static final int[] TOB_PARTY = {VarbitID.TOB_CLIENT_P0, VarbitID.TOB_CLIENT_P1, VarbitID.TOB_CLIENT_P2,
		VarbitID.TOB_CLIENT_P3, VarbitID.TOB_CLIENT_P4};
	/** The varbits of the Tombs of Amascut's party: one for each of the eight places, 0 where nobody is. */
	private static final int[] TOA_PARTY = {VarbitID.TOA_CLIENT_P0, VarbitID.TOA_CLIENT_P1, VarbitID.TOA_CLIENT_P2,
		VarbitID.TOA_CLIENT_P3, VarbitID.TOA_CLIENT_P4, VarbitID.TOA_CLIENT_P5, VarbitID.TOA_CLIENT_P6, VarbitID.TOA_CLIENT_P7};

	/** How many of these varbits are not 0: the members of a raid's party, by the health orbs the raid shows. */
	private int members(int[] varbits)
	{
		int n = 0;
		for (int varbit : varbits)
		{
			n += client.getVarbitValue(varbit) != 0 ? 1 : 0;
		}
		return n;
	}

	/**
	 * The Theatre of Blood's party, read from the game: the places of its party that hold someone (RuneLite's
	 * THEATRE_OF_BLOOD_ORB varbits, alive or dead), the most since they were first held ({@link #lockedParty}). Null with
	 * the option off or when no place holds anyone: then the hitpoints stay as the table has them. Only the Theatre's own
	 * monsters are scaled with it.
	 */
	private TobScaling.Raid tobNow()
	{
		if (!config.tobScaling())
		{
			return null;
		}
		// the party the raid began with: a player leaving does not lower it, as in the Chambers and the Tombs; the
		// places all empty, the raid is over and nothing is kept
		int members = members(TOB_PARTY);
		tobPartyLocked = lockedParty(tobPartyLocked, members > 0, members);
		int size = tobPartyLocked;
		TobScaling.Raid now = size == 0 ? null : new TobScaling.Raid(size);
		return now != null && now.equals(tobRaid) ? tobRaid : now;
	}

	/** The regions of the Tombs of Amascut's rooms, from the Nexus to the Tomb (the Tombs of Amascut plugin's RaidRoom). */
	private static final int[] TOA_ROOMS = {14160, 15698, 15700, 14162, 14164, 15186, 15188, 14674, 14676, 15184, 15696, 14672};

	/** The most members of the party seen since this raid of the Tombs began; 0 outside the raid. */
	private int toaPartyLocked;
	/** The same for the Theatre of Blood: the most places held since its places were first held; 0 outside it. */
	private int tobPartyLocked;

	/**
	 * The party the Tombs' monsters are scaled for: the most members seen since the raid began, kept until the player
	 * leaves the raid: a player leaving does not lower the monsters' hitpoints, as in the Chambers of Xeric. Outside
	 * the raid, the party as read now.
	 *
	 * @param locked  what was kept so far
	 * @param inRaid  the player is in one of the raid's rooms
	 * @param members the places of the party that hold someone now
	 * @return the party to keep and to scale with
	 */
	static int lockedParty(int locked, boolean inRaid, int members)
	{
		return inRaid ? Math.max(locked, members) : 0;
	}

	/** True when the player is in one of the Tombs of Amascut's rooms. */
	private boolean inToaRoom()
	{
		Player me = client.getLocalPlayer();
		net.runelite.api.coords.LocalPoint lp = me == null ? null : me.getLocalLocation();
		if (lp == null)
		{
			return false;
		}
		int region = net.runelite.api.coords.WorldPoint.fromLocalInstance(client, lp).getRegionID();
		for (int r : TOA_ROOMS)
		{
			if (r == region)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * The Tombs of Amascut's raid level, party and path levels, read from the game (TOA_CLIENT_RAID_LEVEL, the places of
	 * the party that hold someone, and each path's level). Null with the option off. A party that cannot be read is
	 * left out of the hitpoints (0 members). Inside the raid the party is the most seen since it began ({@link
	 * #lockedParty}). Only the raid's own monsters are scaled with it.
	 */
	private ToaScaling.Raid toaNow()
	{
		if (!config.toaScaling())
		{
			return null;
		}
		int members = members(TOA_PARTY);
		boolean inRaid = inToaRoom();
		toaPartyLocked = lockedParty(toaPartyLocked, inRaid, members);
		ToaScaling.Raid now = new ToaScaling.Raid(client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL),
			inRaid ? toaPartyLocked : members,
			client.getVarbitValue(VarbitID.TOA_CLIENT_CRONDIS_LEVEL), client.getVarbitValue(VarbitID.TOA_CLIENT_SCABARAS_LEVEL),
			client.getVarbitValue(VarbitID.TOA_CLIENT_HET_LEVEL), client.getVarbitValue(VarbitID.TOA_CLIENT_APMEKEN_LEVEL));
		return now.equals(toaRaid) ? toaRaid : now;
	}

	/** True with "Estimate specs" on. */
	private boolean estimating()
	{
		return config.estimateSpecs();
	}

	/**
	 * The estimated special attacks whose hit has landed by this tick, counted in the drains by the tick a party message of
	 * them would come; and whether the Defence of the monster fought now counts one. With the option off nothing is kept.
	 */
	private void countEstimates()
	{
		if (!estimating())
		{
			estimate.clear();
			drains.dropEstimated();
			drainsEstimated = false;
			return;
		}
		estimate.bgs(true);
		for (SpecEstimate.Landed l : estimate.resolve(client.getTickCount()))
		{
			// a Bandos godsword's drain is the damage rolled, where the Alchemical Hydra's reduction cut the hitsplat
			int hit = HydraShield.rolled(l.weapon, l.hit, hydra.shielded(l.npc));
			drains.addEstimated(l.npcIndex, l.npc, l.weapon, hit, l.tekton, l.tick, l.playerId);
		}
		NPC npc = targetNpc;
		StyleData.Monster monster = npc == null || data == null ? null : tableMonster(npc);
		drainsEstimated = npc != null && monster != null && drains.estimated(npc.getIndex(), npc)
			&& drains.of(npc.getIndex(), npc) != null && !DefenceDrains.immune(monster) && !DefenceDrains.immune(npc.getId());
	}

	/**
	 * The players whose target is the monster fought, looked at once a tick for the start of an Elder maul or Dragon
	 * warhammer special attack, or a Bandos godsword's ({@link SpecEstimate}). Not
	 * with the game's hitsplat tinting turned off: the hitsplats of the others were measured with it on only.
	 */
	private void lookForSpecs()
	{
		NPC npc = targetNpc;
		Player me = client.getLocalPlayer();
		java.util.Set<Integer> aimingNow = new java.util.HashSet<>();
		if (estimating() && npc != null && me != null && client.getVarbitValue(VarbitID.HITSPLAT_TINT_DISABLED) == 0)
		{
			boolean tekton = DefenceDrains.tekton(npc.getId());
			for (Player p : SpecEstimate.aiming(client.getTopLevelWorldView().players(), Player::getInteracting, npc))
			{
				aimingNow.add(p.getId());
				net.runelite.api.PlayerComposition composition = p.getPlayerComposition();
				int weapon = composition == null ? -1 : composition.getEquipmentId(net.runelite.api.kit.KitType.WEAPON);
				estimate.seen(client.getTickCount(), p.getId(), p == me, p.getAnimation(), p.getAnimationFrame(), weapon,
					npc.getIndex(), npc, tekton);
			}
		}
		estimate.endTick(aimingNow);
	}

	@Subscribe
	public void onAnimationChanged(net.runelite.api.events.AnimationChanged event)
	{
		if (event.getActor() instanceof NPC)
		{
			NPC npc = (NPC) event.getActor();
			bork.animation(npc, npc.getId(), npc.getAnimation(), client.getTickCount());
			if (DefenceDrains.bloat(npc.getId()) && npc.getAnimation() == DefenceDrains.BLOAT_SLEEP_ANIMATION)
			{
				// the Bloat shuts down: its stomp, some ticks on, restores its Defence
				bloatShutDown.put(npc, client.getTickCount());
			}
		}
		if (estimating() && targetNpc != null && event.getActor() instanceof Player)
		{
			estimate.animationSet(((Player) event.getActor()).getId());
		}
	}

	/** True when what the player faces is a monster to fight that is not the remembered one. */
	static boolean attacksAnother(Object facing, Object remembered, boolean canBeFought)
	{
		return canBeFought && facing != remembered;
	}

	/** True for a pet walking behind a player: it can carry a monster's name and is never the monster fought. */
	private static boolean isFollower(NPC npc)
	{
		net.runelite.api.NPCComposition composition = npc.getTransformedComposition();
		return composition != null && composition.isFollower();
	}

	/** Each Pestilent Bloat that shut down, with the tick it did: its stomp is still to come. */
	private final java.util.Map<NPC, Integer> bloatShutDown = new java.util.HashMap<>();

	/** The Bloat's stomp restores its Defence: what was counted on it is dropped (a special attack after it counts). */
	private void bloatStomps()
	{
		int tick = client.getTickCount();
		bloatShutDown.entrySet().removeIf(e ->
		{
			if (!DefenceDrains.bloatStomps(e.getValue(), tick))
			{
				return false;
			}
			drains.forget(e.getKey().getIndex(), e.getKey());
			return true;
		});
	}

	/** The tick the player last attacked a monster of the table, -1 for never, and where: to tell a new fight. */
	private int lastFightTick = -1;
	private net.runelite.api.coords.WorldPoint lastFightAt;

	/**
	 * Whether a fight starting now, after no monster was remembered, is a new one, which gives the pieces their time in
	 * a fight once more: none was fought before, or the last attack was at least a minute ago ({@link
	 * #REMEMBER_TARGET_TICKS}, the monster's memory run out), or it was somewhere else (another plane, or farther than
	 * {@link #FORGET_DISTANCE}: the next room of a raid). The next monster of the same room is not: a Guardian after
	 * the other one, forgotten a second after it died, is attacked a few seconds later where the first one was; nor the
	 * next monster of a task, still remembered when it is attacked.
	 */
	static boolean newFight(int lastTick, net.runelite.api.coords.WorldPoint lastAt, int tick,
		net.runelite.api.coords.WorldPoint here)
	{
		if (lastTick < 0 || lastAt == null || here == null)
		{
			return true;
		}
		return tick - lastTick >= REMEMBER_TARGET_TICKS || here.getPlane() != lastAt.getPlane()
			|| here.distanceTo2D(lastAt) > FORGET_DISTANCE;
	}

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
			defenceLine = null;
			bulwarkSpecLine = null;
			leftOnNotice = null;
			blowpipeNotice = null;
			outOfCombat = 0;
			prayerOnFor = 0;
			prayerTimer.reset();
			// another login: each reminder may be shown once more
			piecesShown.arm();
			piecesShownIdle.arm();
			noPrayerShown.arm();
			wornBefore = null;
			drains.clear();
			bloatShutDown.clear();
			estimate.clear();
			drainsEstimated = false;
			hydra.clear();
			bork.clear();
			borkArmour.clear();
			archivePartyLocked = 0;
			specWeapon = null;
			specTarget = null;
			specEnergy = -1;
			// read again from the profile of the account logged in next
			coxChallengeSeen = null;
		}
	}

	private void reset()
	{
		advice = null;
		missing = Collections.emptyList();
		takeOff = Collections.emptyList();
		weaponAdvice = null;
		bankAdvice = null;
		boltAdvice = null;
		piecesGain = Double.NaN;
		defenceLine = null;
		bulwarkSpecLine = null;
		targetId = -1;
		targetNpc = null;
		styleTimer.reset();
		switchTimer.reset();
		weaponTimer.reset();
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		try
		{
			bloatStomps();
			landSpecialAttack();
			countEstimates();
			readWardenEnrage();
			readSmorks();
			CoxScaling.Raid before = raid;
			raid = client.getGameState() == GameState.LOGGED_IN ? raidNow() : null;
			tobRaid = client.getGameState() == GameState.LOGGED_IN ? tobNow() : null;
			toaRaid = client.getGameState() == GameState.LOGGED_IN ? toaNow() : null;
			archiveParty();
			if (raid != before)
			{
				log.debug("Chambers of Xeric raid: {}", raid);
			}
			if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null || data == null)
			{
				advice = null;
				missing = Collections.emptyList();
				takeOff = Collections.emptyList();
				weaponAdvice = null;
				bankAdvice = null;
				boltAdvice = null;
				prayerNotice = null;
				defenceLine = null;
				bulwarkSpecLine = null;
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
				defenceLine = null;
				bulwarkSpecLine = null;
				leftOnNotice = null;
				outOfCombat = 0;
				prayerTimer.reset();
				return;
			}
			readBlowpipe();
			update(client.getTickCount());
			lookForSpecs();
		}
		catch (RuntimeException e)
		{
			blowpipeNotice = null;
			// a cache lookup that fails must not break the tick for every other plugin
			log.debug("could not check the gear", e);
			leftOnNotice = null;
			advice = null;
			missing = Collections.emptyList();
			takeOff = Collections.emptyList();
			weaponAdvice = null;
			bankAdvice = null;
			boltAdvice = null;
			prayerNotice = null;
			defenceLine = null;
			bulwarkSpecLine = null;
			piecesGain = Double.NaN;
		}
	}

	/**
	 * True while the player is fighting another player: where players can attack each other, they are targeting one, or
	 * one is targeting them, now or within the last {@link #PLAYER_FIGHT_TICKS} ticks.
	 */
	private boolean fightingPlayer(int tick)
	{
		Player me = client.getLocalPlayer();
		boolean targeting = me.getInteracting() instanceof Player;
		boolean targeted = false;
		for (Player other : client.getTopLevelWorldView().players())
		{
			if (other != null && other != me && other.getInteracting() == me)
			{
				targeted = true;
				break;
			}
		}
		// the Wilderness, or wherever the game sets its PvP area varbit (a PvP world): as RuneLite's Player Indicators
		// decides its "PvP" setting (PlayerIndicatorsService: Varbits.IN_WILDERNESS or VarbitID.PVP_AREA_CLIENT is 1)
		boolean pvp = client.getVarbitValue(net.runelite.api.Varbits.IN_WILDERNESS) == 1
			|| client.getVarbitValue(VarbitID.PVP_AREA_CLIENT) == 1;
		if (playerFightNow(pvp, targeting, targeted))
		{
			lastPlayerFight = tick;
		}
		return stillFightingPlayer(tick, lastPlayerFight);
	}

	/**
	 * Whether this tick is one of a fight with a player. The game's "interacting" is also following and trading
	 * (RuneLite API, Actor.getInteracting: "Targetting a player to trade; Following a player"), so outside the places
	 * where players can attack each other a player targeting the player, or targeted, is an alt or a friend following.
	 */
	static boolean playerFightNow(boolean pvpArea, boolean targetingPlayer, boolean targetedByPlayer)
	{
		return pvpArea && (targetingPlayer || targetedByPlayer);
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

	/**
	 * The table's row for an NPC, by its id or else by its name, and for the fight the game is in (an awakened boss of
	 * Desert Treasure II, by the most hitpoints the game's boss health display shows for it); null when the table has
	 * neither.
	 */
	private StyleData.Monster tableMonster(NPC npc)
	{
		return npc == null ? null : data.inFight(data.monster(npc.getId(), npc.getName()), displayedHitpoints(npc));
	}

	/** The monster fought whose boss health bar was seen, the NPC id it had then and the bar's most hitpoints. */
	private NPC barNpc;
	private int barId;
	private int barHitpoints;

	/**
	 * The most hitpoints the game's boss health bar shows for the monster fought, in the form it has now: read while the bar
	 * shows it, and kept while it does not (walking away, the bar hidden); 0 for another monster or a form not seen on the
	 * bar yet (the Archive Guardian's shield in flight and its body on the ground have bars of their own).
	 */
	private int barMax(StyleData.Monster monster)
	{
		NPC npc = targetNpc;
		if (npc == null || !monster.fracturedArchive || tableMonster(npc) != monster)
		{
			return 0;
		}
		int shown = displayedHitpoints(npc);
		if (shown > 0)
		{
			barNpc = npc;
			barId = npc.getId();
			barHitpoints = shown;
		}
		return barNpc == npc && barId == npc.getId() ? barHitpoints : 0;
	}

	/** The most hitpoints the game's boss health display shows for this NPC; 0 when it shows another one or none. */
	private int displayedHitpoints(NPC npc)
	{
		return client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC) == npc.getId()
			? client.getVarbitValue(net.runelite.api.gameval.VarbitID.HPBAR_HUD_BASEHP) : 0;
	}

	private void update(int tick)
	{
		Player me = client.getLocalPlayer();
		Actor interacting = me.getInteracting();
		net.runelite.api.coords.WorldPoint here = me.getWorldLocation();
		boolean wasFighting = targetId != -1;
		if (interacting instanceof NPC && !((NPC) interacting).isDead() && !isFollower((NPC) interacting)
			&& tableMonster((NPC) interacting) != null)
		{
			// the id of the table's row: an id the table does not list is found by the monster's name; the row itself,
			// which is the awakened fight's when the game is in one
			targetRow = tableMonster((NPC) interacting);
			targetId = targetRow.id;
			targetNpc = (NPC) interacting;
			targetTicks = 0;
			deadTicks = 0;
			targetAt = here;
		}
		else if (targetId != -1 && (deadTooLong() || ++targetTicks > REMEMBER_TARGET_TICKS
			|| leftTheFight(here, targetAt, client.getBoostedSkillLevel(Skill.HITPOINTS))
			|| attacksAnother(interacting, targetNpc, interacting instanceof NPC && canBeFought((NPC) interacting))))
		{
			// the monster has been dead for long enough, or the player died, left where the fight was, or attacks
			// a monster that is not in the table: it is forgotten. The minute of memory is only for a monster
			// still alive that the player stopped attacking for a moment. The dead monster itself, still faced
			// by the player, is not "another monster": it is kept for the time a dead one is kept
			targetId = -1;
			targetNpc = null;
		}
		StyleData.Monster monster = targetId == -1 ? null : targetRow;
		if (monster != null)
		{
			if (!wasFighting && newFight(lastFightTick, lastFightAt, tick, here))
			{
				// a new fight: the pieces get their seconds in it again, without a change of armour
				piecesShown.arm();
			}
			if (targetTicks == 0)
			{
				// attacking it this tick: the minute of memory after the last attack is not fighting
				lastFightTick = tick;
				lastFightAt = here;
			}
		}

		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		int weaponId = -1;
		if (worn != null)
		{
			Item weapon = worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
			if (weapon != null && weapon.getId() > 0)
			{
				weaponId = itemOf(weapon.getId());
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
		// the pieces are shown for a while; after that only a change of armour (not of weapon) brings them back
		Item[] wornItems = worn == null ? new Item[0] : worn.getItems();
		int[] wornNowIds = new int[Math.max(wornItems.length, ShownFor.AMMO_SLOT + 1)];
		for (int slot = 0; slot < wornNowIds.length; slot++)
		{
			wornNowIds[slot] = slot < wornItems.length && wornItems[slot] != null ? wornItems[slot].getId() : -1;
		}
		if (ShownFor.armourChanged(wornBefore, wornNowIds) || me.isDead())
		{
			piecesShown.arm();
			piecesShownIdle.arm();
		}
		wornBefore = wornNowIds;
		if (me.isDead())
		{
			noPrayerShown.arm();
		}
		boolean piecesDue = switchTimer.due(tick, !left.isEmpty() || !off.isEmpty(), config.switchDelay());
		// in a fight and out of one each have their time: wielded away from the monster, the pieces still show in the fight
		if (!ShownFor.visible(piecesShown, piecesShownIdle, monster != null, piecesDue,
			config.switchKeep() ? 0 : ShownFor.ticks(config.switchShowFor())))
		{
			left = Collections.emptyList();
			off = Collections.emptyList();
			gain = Double.NaN;
		}
		missing = left;
		takeOff = off;
		piecesGain = gain;

		// 3. a carried weapon whose set would deal much more to this monster than what is worn
		// against a monster that changes form or phase during the fight, only a weapon of the same combat class is
		// pointed at (a better sword for the sword in hand): which class each form calls for is the fight itself
		WeaponAdvice better = monster != null && config.weaponNotice()
			? betterWeapon(monster, worn, weaponId, on, changesForm(monster.name)) : null;
		boolean wait = !weaponTimer.due(tick, better != null, WEAPON_DELAY_FLOOR);
		weaponAdvice = wait ? null : better;
		// 3a. the best weapon of the bank last seen open, outside the raids
		bankAdvice = bankWeaponNow(monster, worn, weaponId, on, monster != null && changesForm(monster.name));

		// 3b. the other of ruby and diamond bolts (e), when it deals more at the monster's current health
		boltAdvice = monster != null && targetTicks <= afterAttackTicks(config.boltsAfterAttack()) && config.boltReminder() && !boltPluginOn()
			? betterBolts(on, carried, weaponId, target(monster)) : null;
		if (boltAdvice == null && config.boltReminder() && !boltPluginOn())
		{
			boltAdvice = boltsToPutOn(on, carried, weaponId);
		}

		// 4. the attack prayer that is on, against the class of the weapon in hand
		String prayer = null;
		String meleePrayer = zealOn() ? "Zeal" : on(Prayer.PIETY, Prayer.CHIVALRY, Prayer.ULTIMATE_STRENGTH, Prayer.INCREDIBLE_REFLEXES, Prayer.SUPERHUMAN_STRENGTH,
			Prayer.IMPROVED_REFLEXES, Prayer.BURST_OF_STRENGTH, Prayer.CLARITY_OF_THOUGHT);
		String rangedPrayer = on(Prayer.RIGOUR, Prayer.DEADEYE, Prayer.EAGLE_EYE, Prayer.HAWK_EYE, Prayer.SHARP_EYE);
		String magicPrayer = on(Prayer.AUGURY, Prayer.MYSTIC_VIGOUR, Prayer.MYSTIC_MIGHT, Prayer.MYSTIC_LORE, Prayer.MYSTIC_WILL);
		boolean noAttackPrayer = meleePrayer == null && rangedPrayer == null && magicPrayer == null;
		if (!noAttackPrayer)
		{
			// an attack prayer was turned on: without one again, the reminder is worth showing once more
			noPrayerShown.arm();
		}
		if (monster != null && targetTicks <= afterAttackTicks(config.prayerAfterAttack()) && c.combatClass != null
			&& client.getBoostedSkillLevel(Skill.PRAYER) > 0
			&& prayerReminder(config.prayerNoneOn(), config.prayerWrong()) != CombatCheckConfig.PrayerReminder.OFF)
		{
			prayer = prayerNotice(c.combatClass, meleePrayer, rangedPrayer, magicPrayer,
				bestPrayer(c.combatClass), prayerReminder(config.prayerNoneOn(), config.prayerWrong()));
		}
		boolean prayerDue = prayerTimer.due(tick, prayer != null, config.prayerDelay());
		// "no attack prayer on" is shown for a while and then keeps quiet; the wrong prayer stays until put right
		if (prayerDue && noAttackPrayer
			&& !noPrayerShown.visible(true, config.prayerKeep() ? 0 : ShownFor.ticks(config.prayerShowFor())))
		{
			prayerDue = false;
		}
		// what the prayer named would add to the gear worn against this monster: Augury can add a couple of percent
		// against a low magic defence and much more against a high one, so the player can tell a loss from a saving
		// (worked out before the notice is set, so the overlay never draws a notice with the last one's figure)
		prayerGain = prayerDue && prayer != null && config.showGain()
			? SetCalc.prayerGain(loadout(on, weaponId), stats(), target(monster)) : Double.NaN;
		prayerNotice = prayerDue ? prayer : null;

		// 4b. the monster's Defence before the special attacks counted on it and now
		defenceLine = defenceLineNow(monster, c.combatClass);
		if (defenceLine == null && config.showDefence() && interacting instanceof NPC
			&& data.unmodelled(((NPC) interacting).getId()))
		{
			// a monster seen but never examined (Dreadnoth's Jellies): its Defence is not known, and no figure is given
			defenceLine = unknownDefenceText(config.language(), 0);
		}
		// 4c. what Dinh's bulwark's special attack would deal now
		bulwarkSpecLine = config.showBulwarkSpec() ? bulwarkSpecLineNow(monster, on, weaponId, c.meleeType, me) : null;

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
		// its levels in this fight: the raid's scaling, then the special attacks counted on it
		DefenceDrains.Levels levels = levels(monster);
		SetCalc.Target t = monster.target(levels);
		t.onTask = Boolean.TRUE.equals(onTask(monster));
		t.inWilderness = client.getVarbitValue(net.runelite.api.Varbits.IN_WILDERNESS) == 1;
		t.undead = monster.undead;
		// the Tombs of Amascut's raid level, which multiplies the defence roll (not for Kephri's overlords)
		t.toaInvocation = ToaScaling.invocation(monster, toaRaid);
		t.hitpointsNow = healthNow(monster, levels.hitpoints);
		t.defencelessShare = defencelessShare(monster);
		if (monster.id == BorkStun.BORK && monster.fracturedArchive)
		{
			// Bork's flat armour and magic resistance by the Smorks' health; stunned by the Smorks' heavy cannonball, his
			// flat armour 0 (OSRS Wiki, Bork)
			BorkArmour.apply(t, monster.flatArmour, borkArmour.quarters(), borkStunned(monster));
		}
		return t;
	}

	/** True while the monster fought is Bork stunned ({@link BorkStun}). */
	private boolean borkStunned(StyleData.Monster monster)
	{
		NPC npc = targetNpc;
		return npc != null && monster != null && monster.id == BorkStun.BORK && tableMonster(npc) == monster
			&& bork.stunned(npc, client.getTickCount());
	}

	/**
	 * The share of the attacks that meet the monster without defence, every one of them landing: the Tormented Demon's,
	 * half until its flames are read from the game (Weapons.TORMENTED_DEMON_DEFENCELESS), for the figures and for the
	 * attack style alike. The one place to give it the game's reading.
	 */
	private static double defencelessShare(StyleData.Monster monster)
	{
		return Weapons.defencelessShare(monster.name);
	}

	/**
	 * How much health the monster has now, 0 when that is not known: the ruby bolts' effect, the Keris partisan of the
	 * sun's and Araxxor's enrage depend on it. A boss with the game's own health display gives it exactly; otherwise the
	 * bar's fraction of {@code hitpoints} (its full hitpoints, scaled for the raid); and while neither shows (walking
	 * away from an attack, the bar hidden), the last health seen on this monster, never its full health.
	 */
	private int healthNow(StyleData.Monster monster, int hitpoints)
	{
		Actor fighting = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getInteracting();
		int hud = client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
		int seen = 0;
		if (hud > 0 && hud == monster.id && client.getVarbitValue(net.runelite.api.gameval.VarbitID.HPBAR_HUD_BASEHP) > 0)
		{
			seen = client.getVarbitValue(net.runelite.api.gameval.VarbitID.HPBAR_HUD_HP);
		}
		// a boss of the Fractured Archive whose hitpoints are not known (no bar seen, no team or no table for it: the row's
		// are the OSRS Wiki's before the raid's scaling): neither is the share of them the health above it shows
		boolean known = !monster.fracturedArchive || archiveHitpoints(monster) > 0;
		if (seen <= 0 && known && fighting instanceof NPC && tableMonster((NPC) fighting) == monster
			&& fighting.getHealthRatio() > 0 && fighting.getHealthScale() > 0)
		{
			seen = Math.max(1, (int) ((long) hitpoints * fighting.getHealthRatio() / fighting.getHealthScale()));
		}
		if (seen > 0)
		{
			healthOf = monster;
			healthSeen = seen;
		}
		return healthOf == monster ? healthSeen : 0;
	}

	/**
	 * The ruby or diamond bolts (e) in the inventory that would deal more than the ones in the ammunition
	 * slot, to the monster at the health it has now; null when there are none or the worn ones are as good.
	 */
	private WeaponAdvice betterBolts(SwitchSet.Piece[] on, List<SwitchSet.Piece> carried, int weaponId, SetCalc.Target target)
	{
		SwitchSet.Piece ammo = shotAmmo(on, weaponId);
		int worn = ammo == null ? 0 : SetCalc.boltKind(ammo.name);
		SwitchSet.Piece inHand = on.length > SwitchSet.WEAPON ? on[SwitchSet.WEAPON] : null;
		if (target.hitpointsNow <= 0 || !"bolts".equals(Weapons.ammoKind(data.category(weaponId), inHand == null ? null : inHand.name)))
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
				if (SetCalc.boltKind(p.name) == 0 || !firesBolts(weaponId, inHand, p))
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
		SwitchSet.Piece quiver = ammo == slot && quiverInUse(wearsQuiver(on), client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT))
			? piece(client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO)) : null;
		if (quiver != null)
		{
			others.add(quiver);
		}
		others.addAll(carried);
		for (SwitchSet.Piece p : others)
		{
			int kind = SetCalc.boltKind(p.name);
			if (kind == 0 || kind == worn || !firesBolts(weaponId, inHand, p))
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
		int target = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		return onTask(client.getVarpValue(VarPlayerID.SLAYER_COUNT), target == BOSS_TASK,
			client.getVarbitValue(VarbitID.SLAYER_MASTER), client.getVarbitValue(net.runelite.api.Varbits.IN_WILDERNESS) == 1,
			monster == null ? null : taskName(), monster == null ? null : monster.name,
			monster == null ? null : monster.slayer, monster != null && monster.awakened, data.slayerCategories());
	}

	/** The game's Slayer task id of the boss tasks (OSRS Wiki, RuneScape:Varplayer/395: "A value of 98 corresponds to boss tasks"). */
	static final int BOSS_TASK = 98;
	/** The Slayer masters by the game's SLAYER_MASTER varbit (OSRS Wiki, RuneScape:Varbit/4067): Krystilia 7, Konar 8. */
	static final int KRYSTILIA = 7;
	static final int KONAR = 8;

	/**
	 * Whether the monster is of the player's Slayer task, from what the game says: TRUE, FALSE, or null when that is not
	 * known. No task left is FALSE; a task of Krystilia's counts only inside the Wilderness (OSRS Wiki, Slayer helmet: the
	 * bonuses "do not apply to monsters outside of the designated assigned area (applicable when receiving tasks from
	 * Krystilia or Konar quo Maten)"); a boss task by the boss the game names ({@link SetEffects#onBossTask}), any other
	 * by the task's category ({@link SetEffects#onTask}). Konar's location is not checked: nothing tells where its area is.
	 *
	 * @param remaining  the game's SLAYER_COUNT
	 * @param bossTask   whether the game's SLAYER_TARGET is the boss task
	 * @param master     the game's SLAYER_MASTER
	 * @param taskName   the task's name in the game's table; for a boss task, the boss's
	 */
	static Boolean onTask(int remaining, boolean bossTask, int master, boolean inWilderness, String taskName,
		String monsterName, java.util.Set<String> monsterCategories, boolean awakened, java.util.Set<String> knownCategories)
	{
		if (remaining <= 0)
		{
			return Boolean.FALSE;
		}
		if (monsterName == null)
		{
			return null;
		}
		if (master == KRYSTILIA && !inWilderness)
		{
			return Boolean.FALSE;
		}
		return bossTask ? SetEffects.onBossTask(taskName, monsterName, awakened, master == KONAR)
			: SetEffects.onTask(taskName, monsterCategories, knownCategories);
	}

	/**
	 * The name of the current Slayer task, from the game's table of tasks; null when it cannot be read. For a boss task,
	 * the boss's: the game's SLAYER_TARGET_BOSSID varbit indexes the boss in its table of boss tasks, whose row points at
	 * the task named (OSRS Wiki, RuneScape:Varplayer/395; read the way RuneLite's Slayer plugin reads it, SlayerPlugin
	 * .updateTask: DBTableID.SlayerTaskSublist by COL_TASK_SUBTABLE_ID, then COL_TASK, then COL_NAME_UPPERCASE).
	 */
	private String taskName()
	{
		int id = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		int boss = id == BOSS_TASK ? client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID) : -1;
		if (id == taskId && boss == taskBossId)
		{
			return taskName;
		}
		String name = null;
		try
		{
			Integer row = null;
			if (id == BOSS_TASK)
			{
				List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID,
					DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0, boss);
				if (rows != null && !rows.isEmpty())
				{
					Object[] task = client.getDBTableField(rows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0);
					row = task != null && task.length > 0 && task[0] instanceof Integer ? (Integer) task[0] : null;
				}
			}
			else
			{
				List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, id);
				row = rows != null && !rows.isEmpty() ? rows.get(0) : null;
			}
			if (row != null)
			{
				Object[] field = client.getDBTableField(row, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
				name = field != null && field.length > 0 && field[0] != null ? field[0].toString() : null;
			}
		}
		catch (RuntimeException e)
		{
			log.debug("could not read the slayer task", e);
		}
		taskId = id;
		taskBossId = boss;
		taskName = name;
		return name;
	}

	/**
	 * The carried weapon whose finished set would deal the most over what is worn now, when that is
	 * at least the configured gain; null otherwise, and whenever either side has a rule the plugin
	 * does not know (no figure is better than a wrong one).
	 */
	private WeaponAdvice betterWeapon(StyleData.Monster monster, ItemContainer worn, int weaponId, SwitchSet.Piece[] on,
		boolean sameClassOnly)
	{
		SetCalc.Loadout now = loadout(on, weaponId);
		if (!SetCalc.plain(now))
		{
			return null;
		}
		if (SetEffects.pointsAtNoOtherWeapon(now.weaponName, now.spell))
		{
			// a weapon taken out for its special attack: going back to the other one is the player's own timing; a weapon
			// or a spell that hits several monsters at once (the Venator bow, a burst or a barrage): the figures are for one
			return null;
		}
		SetCalc.Player stats = stats();
		SetCalc.Target target = target(monster);
		// the weapon in hand is counted the way the other one is: with the attack prayer of its own class
		// and with its own switch finished, so a prayer or a piece left behind is not read as a better weapon
		String classNow = SetCalc.combatClass(now);
		SetCalc.Player statsNow = SetCalc.withPrayerOf(stats, classNow);
		double current = SetCalc.perTick(now, statsNow, target, false);
		if (current < 0)
		{
			return null;
		}
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return null;
		}
		SetCalc.Loadout finished = loadout(SwitchSet.after(classNow, null, on, inventoryPieces()), weaponId);
		if (SetCalc.plain(finished))
		{
			current = Math.max(current, SetCalc.perTick(finished, statsNow, target, false));
		}
		// the time it takes to kill the monster (a wave Ischyros: one of them, small and big in their share), the
		// weapon in hand with its switch finished or not, whichever kills sooner; only when weapons are compared by it,
		// and not on Verzik's first phase
		boolean byTime = config.weaponComparison() == CombatCheckConfig.WeaponComparison.TIME_TO_KILL;
		double ticksNow = !byTime || target.verzikP1 ? Double.NaN : killTicks(now, statsNow, target, monster);
		if (SetCalc.plain(finished) && !Double.isNaN(ticksNow))
		{
			double ticksFinished = killTicks(finished, statsNow, target, monster);
			ticksNow = Double.isNaN(ticksFinished) ? Double.NaN : Math.min(ticksNow, ticksFinished);
		}
		if (target.verzikP1)
		{
			current = verzikP1Rate(current, classNow, now.speed);
			if (current < 0)
			{
				return null;
			}
		}
		SetEffects.Context mine = context;
		WeaponAdvice best = null;
		double minimum = minimumGain(config.weaponMinimumGain());
		List<Integer> seen = new ArrayList<>();
		// the NPC itself, for the forms that pray against a class (Akkha): the table's row may be another id of it
		int npcId = targetNpc != null ? targetNpc.getId() : monster.id;
		try
		{
			for (Item item : inventory.getItems())
			{
				if (item == null || item.getId() <= 0)
				{
					continue;
				}
				int id = itemOf(item.getId());
				var stats0 = equipment(id);
				if (seen.contains(id) || id == weaponId || stats0 == null || stats0.getSlot() != SwitchSet.WEAPON)
				{
					continue;
				}
				seen.add(id);
				SetCalc.Loadout bare = new SetCalc.Loadout();
				bare.category = data.category(id);
				String cls = SetCalc.combatClass(bare);
				if (cls == null || (sameClassOnly && !cls.equals(SetCalc.combatClass(now))) || !classAllowed(npcId, cls))
				{
					continue;
				}
				// the pieces as they count with that weapon in hand, and the attack prayer that goes with it
				context = contextFor(id, cls, null, worn);
				SetCalc.Player with0 = SetCalc.withPrayerOf(stats, cls);
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
				// the set the figure is for, for the time it takes to kill
				SetCalc.Loadout chosen = null;
				if ("Crossbow".equals(bare.category))
				{
					// a crossbow shoots the bolts worn or the best of the carried ones; with none, it is no option
					List<SwitchSet.Piece> ammo = new ArrayList<>();
					SwitchSet.Piece ready = shotAmmo(after, id);
					ammo.add(ready);
					ammo.addAll(carried);
					for (SwitchSet.Piece b : ammo)
					{
						// bolts the crossbow does not fire (dragon bolts in a Rune crossbow) are no option
						if (b == null || b.slot != SwitchSet.AMMO
							|| !Weapons.canFire(id, bare.category, weapon == null ? null : weapon.name, b.itemId, b.name))
						{
							continue;
						}
						SwitchSet.Piece[] shot = after.clone();
						shot[SwitchSet.AMMO] = b;
						SetCalc.Loadout l = loadout(shot, id);
						double d = SetCalc.plain(l) ? SetCalc.perTick(l, with0, target, false) : -1;
						if (d > other)
						{
							other = d;
							bolts = b == ready ? 0 : b.itemId;
							chosen = l;
						}
					}
					if (other < 0)
					{
						continue;
					}
				}
				else
				{
					// a bow with no arrows to shoot is no option, nor with arrows it does not fire (dragon arrows in a Magic
					// shortbow (i)); the Comp ogre bow fires brutal arrows
					String kind = Weapons.ammoKind(bare.category, weapon == null ? null : weapon.name);
					SwitchSet.Piece arrows = shotAmmo(after, id);
					if (kind != null && (arrows == null
						|| !Weapons.canFire(id, bare.category, weapon == null ? null : weapon.name, arrows.itemId, arrows.name)))
					{
						continue;
					}
					SetCalc.Loadout then = loadout(after, id);
					if (!SetCalc.plain(then))
					{
						continue;
					}
					other = SetCalc.perTick(then, with0, target, false);
					chosen = then;
					if (target.verzikP1)
					{
						other = verzikP1Rate(other, cls, then.speed);
					}
				}
				if (target.verzikP1 && !StyleCalc.MELEE.equals(cls))
				{
					// ranged and magic on Verzik's first phase: no figure there (verzikP1Rate)
					continue;
				}
				// nothing lands now and something would: the notice says so instead of a percentage
				double perTickGain = current == 0 ? (other > 0 ? Double.POSITIVE_INFINITY : Double.NaN) : SetCalc.gain(current, other);
				// the time it takes to kill, where both weapons have one: what is dealt beyond the hitpoints is wasted
				double ticksOther = Double.isNaN(ticksNow) || chosen == null ? Double.NaN : killTicks(chosen, with0, target, monster);
				double gain = comparedGain(config.weaponComparison(), perTickGain, ticksNow, ticksOther);
				if (pointsAt(gain, minimum) && (best == null || gain > best.gain))
				{
					best = new WeaponAdvice();
					best.itemId = id;
					best.alsoItemId = bolts;
					best.gain = gain;
					best.text = Double.isInfinite(gain) ? "can hit"
						: gainText(gain) + (byTime && killsSooner(perTickGain, gain, minimum) ? " " + LOW_HP : "");
				}
			}
		}
		finally
		{
			context = mine;
		}
		return best;
	}

	/** The range of the setting "Weapon: minimum gain", in percent. */
	static final double LEAST_MINIMUM_GAIN = 0;
	static final double MOST_MINIMUM_GAIN = 200;
	/** The least gain the notice can show: it reads "+0.1%" (one decimal under 10%, {@link #gainText}). */
	static final double LEAST_SHOWN_GAIN = 0.05;

	/**
	 * The setting "Weapon: minimum gain", in percent: a decimal number (0.5 is half a percent, 0 any gain), kept from 0
	 * to 200 (the panel's spinner has no limits of its own for a decimal number).
	 */
	static double minimumGain(double setting)
	{
		return Double.isNaN(setting) ? 1 : Math.max(LEAST_MINIMUM_GAIN, Math.min(MOST_MINIMUM_GAIN, setting));
	}

	/**
	 * True when a carried weapon's gain is enough to point at it: at least the minimum, and at least what shows as
	 * "+0.1%", so a minimum of 0 never points at a weapon that deals the same.
	 */
	static boolean pointsAt(double gain, double minimum)
	{
		return !Double.isNaN(gain) && gain >= minimum && gain >= LEAST_SHOWN_GAIN;
	}

	/** What the weapon notice adds when the time to kill points at a weapon the figure per tick would not. */
	static final String LOW_HP = "(low HP)";

	/**
	 * The gain the weapon notice uses: how much sooner the other weapon kills the monster, where both have a time to
	 * kill ({@link SetCalc#ticksToKill}); the gain per tick otherwise, and when nothing lands now ("can hit").
	 */
	static double weaponGain(double perTickGain, double ticksNow, double ticksOther)
	{
		if (Double.isNaN(ticksNow) || Double.isNaN(ticksOther) || Double.isInfinite(ticksNow) || Double.isInfinite(ticksOther)
			|| ticksOther <= 0 || Double.isInfinite(perTickGain))
		{
			return perTickGain;
		}
		return (ticksNow / ticksOther - 1) * 100;
	}

	/**
	 * The gain the weapon notice uses with the setting "Compare weapons by": the gain per tick, the damage per second
	 * (the default), or how much sooner the weapon kills ({@link #weaponGain}).
	 */
	static double comparedGain(CombatCheckConfig.WeaponComparison by, double perTickGain, double ticksNow, double ticksOther)
	{
		return by == CombatCheckConfig.WeaponComparison.TIME_TO_KILL ? weaponGain(perTickGain, ticksNow, ticksOther) : perTickGain;
	}

	/** True when the time to kill points at a weapon the gain per tick alone would not point at: the notice says why. */
	static boolean killsSooner(double perTickGain, double gain, double minimum)
	{
		return !pointsAt(perTickGain, minimum) && pointsAt(gain, minimum);
	}

	/**
	 * The ticks the weapon takes to kill the monster from its full hitpoints ({@link SetCalc#ticksToKill}); for a Nylocas
	 * Ischyros of the waves of the Nylocas room, one of them, the small and the big in the share they come in ({@link
	 * NyloWaves}): the melee weapon is held for both sizes, whatever the size of the one fought. NaN when not worked out.
	 */
	private double killTicks(SetCalc.Loadout l, SetCalc.Player p, SetCalc.Target t, StyleData.Monster monster)
	{
		if (monster.fracturedArchive && archiveHitpoints(monster) <= 0)
		{
			// the raid's hitpoints are known from the boss health bar or the team's table: without either, no time to kill
			return Double.NaN;
		}
		int[] rows = NyloWaves.rows(monster.id);
		if (rows == null)
		{
			return SetCalc.ticksToKill(l, p, t);
		}
		StyleData.Monster small = data.monster(rows[0]);
		StyleData.Monster big = data.monster(rows[1]);
		if (small == null || big == null)
		{
			return SetCalc.ticksToKill(l, p, t);
		}
		return NyloWaves.combined(SetCalc.ticksToKill(l, p, waveTarget(small)), SetCalc.ticksToKill(l, p, waveTarget(big)));
	}

	/** A wave Ischyros as one comes in: scaled for the party, at its full hitpoints, nothing counted on it. */
	private SetCalc.Target waveTarget(StyleData.Monster row)
	{
		SetCalc.Target t = row.target(fullLevels(row));
		t.onTask = Boolean.TRUE.equals(onTask(row));
		t.undead = row.undead;
		return t;
	}

	/** The ticks of Verzik's first phase between two of her magic attacks, in which a player hits twice. */
	static final int VERZIK_P1_CYCLE = 14;

	/**
	 * What a weapon deals per tick on Verzik's first phase as the phase is played, from its figure at its own speed:
	 * she charges a magic attack every 14 ticks, "which allows for two hits by any 4 or 5-tick weapon before requiring
	 * to hide behind a pillar" (OSRS Wiki, Theatre of Blood/Strategies, Verzik Vitur - phase 1), so a 4-tick weapon gets
	 * no more attacks than a 5-tick one and the two are compared by what one attack deals (two in each 14 ticks). The
	 * shield's limit on each hit (10 melee, 3 ranged and magic) is in the figure already (SetCalc, Target.verzikP1); the
	 * Dawnbringer has no figure (SetCalc.plain), so it is never pointed at and nothing is pointed at while it is in hand.
	 * -1 for what the wiki gives no rhythm for: a weapon of another speed, or ranged and magic, which the wiki does not
	 * use there (and whose every hit is held at 3).
	 */
	static double verzikP1Rate(double perTick, String combatClass, int speed)
	{
		if (perTick < 0 || !StyleCalc.MELEE.equals(combatClass) || speed < 4 || speed > 5)
		{
			return -1;
		}
		return perTick * speed * 2 / VERZIK_P1_CYCLE;
	}

	/**
	 * The ammunition the weapon shoots: what is in the ammunition slot, or what Dizana's quiver holds when
	 * the quiver is worn in these pieces, the weapon uses that kind and the slot holds none of it (the slot is shot first).
	 */
	private SwitchSet.Piece shotAmmo(SwitchSet.Piece[] pieces, int weaponId)
	{
		int amount = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT);
		SwitchSet.Piece quiver = quiverInUse(wearsQuiver(pieces), amount)
			? piece(client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO)) : null;
		return shotAmmo(pieces, data.category(weaponId), wearsQuiver(pieces), amount, quiver);
	}

	/**
	 * The ammunition a weapon of this category shoots with these pieces on: the slot's, or the quiver's ammunition
	 * ({@code quiverAmmo}, {@code amount} of it) when the quiver is worn ({@link #quiverInUse}) and {@link #quiverShoots}.
	 */
	static SwitchSet.Piece shotAmmo(SwitchSet.Piece[] pieces, String category, boolean quiverWorn, int amount,
		SwitchSet.Piece quiverAmmo)
	{
		SwitchSet.Piece slot = pieces.length > SwitchSet.AMMO ? pieces[SwitchSet.AMMO] : null;
		SwitchSet.Piece weapon = pieces.length > SwitchSet.WEAPON ? pieces[SwitchSet.WEAPON] : null;
		if (!quiverInUse(quiverWorn, amount) || quiverAmmo == null)
		{
			return slot;
		}
		return quiverShoots(category, weapon == null ? null : weapon.name, slot == null ? null : slot.name, quiverAmmo.name)
			? quiverAmmo : slot;
	}

	/**
	 * Whether the ammunition kept in Dizana's quiver is shot: only with the quiver worn, as RuneLite's Ammo plugin reads
	 * the same varps (AmmoPlugin: the cape's ParamID.QUIVER_AMMO_AVAILABLE is 1), and with some of it there. The quiver
	 * keeps its ammunition in the inventory and, maybe, in the bank (OSRS Wiki, Dizana's quiver: the extra slot is shown
	 * "if the quiver is in the inventory or equipped"); in the inventory a ranged switch puts it on (SwitchSet.after).
	 */
	static boolean quiverInUse(boolean quiverWorn, int amount)
	{
		return quiverWorn && amount > 0;
	}

	/** Whether the cape of these pieces is a quiver that shoots its own ammunition (Dizana's quiver, a max cape with it). */
	private boolean wearsQuiver(SwitchSet.Piece[] pieces)
	{
		SwitchSet.Piece cape = pieces.length > CAPE_SLOT ? pieces[CAPE_SLOT] : null;
		return cape != null && cape.itemId > 0
			&& itemManager.getItemComposition(cape.itemId).getIntValue(ParamID.QUIVER_AMMO_AVAILABLE) == 1;
	}

	private static final int CAPE_SLOT = EquipmentInventorySlot.CAPE.getSlotIdx();

	/** Whether a weapon of this category shoots the quiver's ammunition instead of what is in the slot. */
	static boolean quiverShoots(String category, String weaponName, String slotName, String quiverName)
	{
		String kind = Weapons.ammoKind(category, weaponName);
		return kind != null && Weapons.has(quiverName, kind) && !Weapons.has(slotName, kind);
	}

	/** The crossbow in hand fires these bolts (no ruby bolts in the Ascension crossbows, no Ascension bolts in another). */
	private boolean firesBolts(int weaponId, SwitchSet.Piece inHand, SwitchSet.Piece bolts)
	{
		return Weapons.canFire(weaponId, data.category(weaponId), inHand == null ? null : inHand.name, bolts.itemId, bolts.name);
	}

	/**
	 * With a crossbow in hand and no bolts on, and nothing to compare them against: the carried bolts to put on.
	 * Ruby first (carried, they are the ones to start a fight with), then diamond, then any others.
	 */
	private WeaponAdvice boltsToPutOn(SwitchSet.Piece[] on, List<SwitchSet.Piece> carried, int weaponId)
	{
		SwitchSet.Piece inHand = on.length > SwitchSet.WEAPON ? on[SwitchSet.WEAPON] : null;
		if (!"bolts".equals(Weapons.ammoKind(data.category(weaponId), inHand == null ? null : inHand.name)))
		{
			return null;
		}
		SwitchSet.Piece ammo = shotAmmo(on, weaponId);
		if (ammo != null && isBolts(ammo.name))
		{
			return null;
		}
		String[] names = new String[carried.size()];
		String category = data.category(weaponId);
		for (int i = 0; i < names.length; i++)
		{
			SwitchSet.Piece c = carried.get(i);
			// only the bolts the crossbow fires (no dragon bolts in a Rune crossbow)
			names[i] = c.slot == SwitchSet.AMMO && Weapons.canFire(weaponId, category, inHand == null ? null : inHand.name, c.itemId, c.name)
				? c.name : null;
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
		// raids (not Verzik Vitur: each of her phases is plain to see and lasts; her first phase is compared per
		// attack, verzikP1Rate)
		"Nylocas", "Great Olm", "Vanguard", "Elidinis' Warden", "Tumeken's Warden",
		// bosses with forms or phases that call for another weapon
		"Zulrah", "Dawn", "Dusk", "Kalphite Queen", "Phantom Muspah", "Strange Creature",
		"The Nightmare", "Phosani's Nightmare", "Crystalline Hunllef", "Corrupted Hunllef", "Abyssal Sire",
		"The Hueycoatl", "Yama", "Doom of Mokhaiotl", "Zalcano", "Demonic Brutus", "Maggot King",
		// the Fractured Archive: the Archive Guardian flies (only a halberd's melee reaches it) and lands every 20 to 60
		// seconds; Ocellus floats out of melee reach and prays Deflect Melee, Deflect Range and Soul Split; Bork and the
		// Smorks are fought together, each with its own weakness
		"Archive Guardian", "Ocellus", "Bork", "The Smorks",
		// Nex: melee in some phases, ranged in her Shadow phase and against Zaros's Deflect Melee
		"Nex",
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

	/** Akkha's NPC ids (RuneLite's NpcID): his forms that attack and pray with melee, ranged and magic, and his enrage. */
	static final int AKKHA_MELEE = 11790;
	static final int AKKHA_RANGED = 11791;
	static final int AKKHA_MAGIC = 11792;
	static final int AKKHA_ENRAGE = 11795;
	static final int AKKHA_SHADOW_ENRAGE = 11798;

	/**
	 * Whether a carried weapon of this combat class may be pointed at against this NPC: never one of the class Akkha is
	 * praying against (he prays against the style he attacks with: OSRS Wiki, Akkha), and in his enrage, where he and his
	 * shadows pray against magic and missiles together, only melee. The weapon in hand is the player's own choice and is
	 * left as it is.
	 */
	static boolean classAllowed(int npcId, String combatClass)
	{
		switch (npcId)
		{
			case AKKHA_MELEE:
				return !StyleCalc.MELEE.equals(combatClass);
			case AKKHA_RANGED:
				return !StyleCalc.RANGED.equals(combatClass);
			case AKKHA_MAGIC:
				return !StyleCalc.MAGIC.equals(combatClass);
			case AKKHA_ENRAGE:
			case AKKHA_SHADOW_ENRAGE:
				return StyleCalc.MELEE.equals(combatClass);
			default:
				return true;
		}
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
		// ammunition the weapon does not shoot gives it nothing: bolts under a bow, arrows under a crossbow, arrows above
		// the bow's tier (the calculator's list: Weapons.canFire), anything under a bow that makes its own arrows
		if (ammo != null && Weapons.ammoKind(data.category(weaponId), inHand == null ? null : inHand.name) != null
			&& !Weapons.canFire(weaponId, data.category(weaponId), inHand == null ? null : inHand.name, ammo.itemId, ammo.name))
		{
			ammo = null;
		}
		else if (ammo != null && Weapons.shootsNoAmmo(data.category(weaponId), inHand == null ? null : inHand.name))
		{
			ammo = null;
		}
		else if (ammo != null && Weapons.throwsItself(data.category(weaponId)) && Weapons.isShotAmmo(ammo.name))
		{
			// a blowpipe, darts, knives, chinchompas: what they throw is their own, not what the slot holds
			ammo = null;
		}
		List<SwitchSet.Piece> counted = new ArrayList<>();
		for (SwitchSet.Piece p : pieces)
		{
			if (p != null)
			{
				// every piece worn counts for Dinh's bulwark's strength, the ammunition slot's too (the calculator's sum)
				l.defence += p.defence;
			}
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
			names.add(p.name);
		}
		// the crystal armour, the Void, Dharok's, Ahrim's and obsidian sets, the Berserker necklace
		SetEffects.wornSets(l, names);
		l.ammoRangedStrength = ammo == null ? 0 : ammo.rangedStrength;
		SwitchSet.Piece weapon = pieces.length > SwitchSet.WEAPON ? pieces[SwitchSet.WEAPON] : null;
		l.weaponName = weapon == null ? "" : weapon.name;
		l.twoHanded = weapon != null && weapon.twoHanded;
		l.speed = weapon == null || weapon.speed <= 0 ? 4 : weapon.speed;
		Integer blowpipeDart = readsDarts(l.weaponName) ? blowpipeDarts.get(blowpipeKind(l.weaponName)) : null;
		if (blowpipeDart != null && blowpipeDart > 0)
		{
			// the darts inside that blowpipe, as its last Check named them
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
		l.boltEffect = l.bolt == 0 && ammo != null && ammo.name != null && ammo.name.endsWith("(e)")
			&& !SetCalc.harmlessBolts(ammo.name);
		// what some monsters take damage from: broad ammunition, silver bolts, Ice arrows, brutal arrows
		l.ammo = ammo == null ? null : ammo.name;
		l.ammoWorn = slotAmmo == null ? null : slotAmmo.name;
		l.efaritay = names.contains("Efaritay's aid");
		// the autocast spell belongs to the weapon in hand: a carried staff has none yet
		ItemContainer wornNow = client.getItemContainer(InventoryID.WORN);
		Item inHandNow = wornNow == null ? null : wornNow.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		boolean held = inHandNow != null && inHandNow.getId() > 0 && itemOf(inHandNow.getId()) == weaponId;
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
		p.mining = client.getBoostedSkillLevel(Skill.MINING);
		boolean zeal = zealOn();
		p.prayerAttack = zeal ? ArchiveItems.ZEAL_ATTACK : 100 + prayer(Prayer.PIETY, 20, Prayer.CHIVALRY, 15,
			Prayer.INCREDIBLE_REFLEXES, 15, Prayer.IMPROVED_REFLEXES, 10, Prayer.CLARITY_OF_THOUGHT, 5);
		p.prayerStrength = zeal ? ArchiveItems.ZEAL_STRENGTH : 100 + prayer(Prayer.PIETY, 23, Prayer.CHIVALRY, 18,
			Prayer.ULTIMATE_STRENGTH, 15, Prayer.SUPERHUMAN_STRENGTH, 10, Prayer.BURST_OF_STRENGTH, 5);
		p.prayerRanged = 100 + prayer(Prayer.RIGOUR, 20, Prayer.DEADEYE, 18, Prayer.EAGLE_EYE, 15, Prayer.HAWK_EYE, 10,
			Prayer.SHARP_EYE, 5);
		p.prayerRangedStrength = 100 + prayer(Prayer.RIGOUR, 23, Prayer.DEADEYE, 18, Prayer.EAGLE_EYE, 15, Prayer.HAWK_EYE, 10,
			Prayer.SHARP_EYE, 5);
		p.prayerMagic = 100 + prayer(Prayer.AUGURY, 25, Prayer.MYSTIC_VIGOUR, 18, Prayer.MYSTIC_MIGHT, 15, Prayer.MYSTIC_LORE, 10,
			Prayer.MYSTIC_WILL, 5);
		p.prayerMagicDamage = prayer(Prayer.AUGURY, 4, Prayer.MYSTIC_VIGOUR, 3, Prayer.MYSTIC_MIGHT, 2, Prayer.MYSTIC_LORE, 1);
		p.kandarinDiary = client.getVarbitValue(VarbitID.KANDARIN_DIARY_HARD_COMPLETE) == 1;
		p.hitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);
		p.hitpointsMax = client.getRealSkillLevel(Skill.HITPOINTS);
		p.prayerLevel = client.getRealSkillLevel(Skill.PRAYER);
		p.piety = hasPiety();
		p.zeal = hasZeal();
		p.chivalry = hasChivalry();
		p.rigour = hasRigour();
		p.deadeye = client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) == 1;
		p.augury = hasAugury();
		p.vigour = client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) == 1;
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
				int id = itemOf(item.getId());
				var e = equipment(id);
				if (e != null)
				{
					in.stab += Weapons.stab(id, e.getAstab());
					in.slash += e.getAslash();
					in.crush += e.getAcrush();
					in.ranged += e.getArange();
					in.magic += e.getAmagic();
					in.strength += Weapons.strength(id, e.getStr());
				}
				in.inquisitor += Weapons.inquisitorWeight(itemManager.getItemComposition(id).getName());
			}
		}
		if (weaponName.startsWith("Keris partisan of amascut") && !monster.toa)
		{
			// outside the Tombs of Amascut, as SetCalc counts it (the calculator's Equipment.ts)
			in.stab -= 50;
			in.strength -= 22;
		}
		in.attackLevel = client.getBoostedSkillLevel(Skill.ATTACK);
		in.strengthLevel = client.getBoostedSkillLevel(Skill.STRENGTH);
		boolean zeal = zealOn();
		in.prayerAttack = zeal ? ArchiveItems.ZEAL_ATTACK : 100 + prayer(Prayer.PIETY, 20, Prayer.CHIVALRY, 15,
			Prayer.INCREDIBLE_REFLEXES, 15, Prayer.IMPROVED_REFLEXES, 10, Prayer.CLARITY_OF_THOUGHT, 5);
		in.prayerStrength = zeal ? ArchiveItems.ZEAL_STRENGTH : 100 + prayer(Prayer.PIETY, 23, Prayer.CHIVALRY, 18,
			Prayer.ULTIMATE_STRENGTH, 15, Prayer.SUPERHUMAN_STRENGTH, 10, Prayer.BURST_OF_STRENGTH, 5);
		in.fang = Weapons.isFang(weaponName);
		in.corpbane = Weapons.isCorpbane(weaponName);
		in.twoHits = SetCalc.twoHitWeapon(weaponName.toLowerCase(java.util.Locale.ROOT));
		in.defenceLevel = levels(monster).defence;
		in.defenceStab = monster.stab;
		in.defenceSlash = monster.slash;
		in.defenceCrush = monster.crush;
		in.corporealBeast = "Corporeal Beast".equals(monster.name);
		in.alwaysHit = monster.wardenP2 || monster.everyAttackLands;
		in.defencelessShare = defencelessShare(monster);
		in.alwaysMax = monster.alwaysMaxMelee;
		in.oneHit = monster.oneHit;
		in.araxyteMax = Weapons.araxyteMax(monster.name);
		in.noxiousHalberd = weaponName.toLowerCase(java.util.Locale.ROOT).startsWith("noxious halberd");
		// the Tombs of Amascut: the fang's double roll the raid's way, and the raid level's defence roll
		in.toa = monster.toa;
		in.defenceRollInvocation = ToaScaling.invocation(monster, toaRaid);
		in.hueycoatlTail = monster.hueycoatlTail;
		in.verzikP1 = monster.verzikP1;

		// the Obligator's styles differ in speed and in the Thrust's rolls: compared per tick by the figures' own rules
		// (SetCalc.styleFigure), on the monster as fought; uncharged it has no figure, and no style is pointed out
		boolean obligator = ArchiveItems.isObligator(weaponName.toLowerCase(java.util.Locale.ROOT));
		SetCalc.Loadout held = obligator ? loadout(pieces(worn), weaponId) : null;
		SetCalc.Player me = obligator ? stats() : null;
		SetCalc.Target fought = obligator ? target(monster) : null;
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
			double score = obligator ? SetCalc.styleFigure(held, me, fought, s) : StyleCalc.expectedHit(in, s.type, s.stance);
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
					names.add(itemManager.getItemComposition(itemOf(item.getId())).getName());
				}
			}
		}
		return names;
	}

	/**
	 * The item an item in the game is: a placeholder or a note read as the item, and another dress of it (an ornament
	 * kit, a locked one, a recolour: {@link Variants}) read as the item itself, so that every rule and table finds it.
	 */
	int itemOf(int itemId)
	{
		return Variants.base(itemManager.canonicalize(itemId));
	}

	/** An item as a switch piece, or null when it cannot be equipped. */
	private SwitchSet.Piece piece(int itemId)
	{
		int id = itemOf(itemId);
		var e = equipment(id);
		if (e == null)
		{
			return null;
		}
		SwitchSet.Piece p = new SwitchSet.Piece();
		p.itemId = id;
		p.slot = e.getSlot();
		p.twoHanded = e.isTwoHanded();
		// the item data the game gives, but the Keris partisan of amascut's stab and strength (Weapons.stab)
		p.stab = Weapons.stab(id, e.getAstab());
		p.slash = e.getAslash();
		p.crush = e.getAcrush();
		p.magic = e.getAmagic();
		p.ranged = e.getArange();
		p.strength = Weapons.strength(id, e.getStr());
		p.rangedStrength = e.getRstr();
		p.magicDamage = e.getMdmg();
		p.defence = e.getDstab() + e.getDslash() + e.getDcrush() + e.getDrange();
		String name = itemManager.getItemComposition(id).getName();
		p.name = name;
		p.speed = e.getAspeed();
		p.inquisitor = Weapons.inquisitorWeight(name);
		p.keep = SetEffects.keep(name, context);
		p.carriedForUse = SetEffects.carriedForUse(name);
		p.mask = SetEffects.maskKind(name);
		p.salve = SetEffects.salveKind(name);
		p.blessing = context != null && context.ammoFree && SetEffects.isBlessing(name);
		if (context != null)
		{
			p.magicDamage += SetEffects.magicDamageBonus(name, context);
			p.setBonus = SetEffects.scoreBonus(name, context) + SetEffects.bulwarkBonus(p.defence, context);
		}
		return p;
	}

	/**
	 * The bonuses of an item: RuneLite's item data, or, for the Fractured Archive's weapons and bolts it does not have yet,
	 * the OSRS Wiki's ({@link ArchiveItems}); null for an item that cannot be equipped.
	 */
	private net.runelite.client.game.ItemEquipmentStats equipment(int itemId)
	{
		var stats = itemManager.getItemStats(itemId);
		if (stats != null && stats.getEquipment() != null)
		{
			return stats.getEquipment();
		}
		return ArchiveItems.equipment(itemId);
	}

	/** Zeal is on (VarbitID.PRAYER_ZEAL: the prayer is not in RuneLite's Prayer list yet). */
	private boolean zealOn()
	{
		return client.getVarbitValue(VarbitID.PRAYER_ZEAL) == 1;
	}

	/**
	 * Zeal can be used: unlocked with the Horn of Bran (it then replaces Piety) and 84 Prayer, 75 Attack and 75 Strength
	 * (OSRS Wiki, Zeal).
	 */
	private boolean hasZeal()
	{
		return client.getVarbitValue(VarbitID.PRAYER_ZEAL_UNLOCKED) == 1
			&& client.getRealSkillLevel(Skill.PRAYER) >= ArchiveItems.ZEAL_PRAYER
			&& client.getRealSkillLevel(Skill.ATTACK) >= ArchiveItems.ZEAL_ATTACK_STRENGTH
			&& client.getRealSkillLevel(Skill.STRENGTH) >= ArchiveItems.ZEAL_ATTACK_STRENGTH;
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

	@Inject
	private net.runelite.client.callback.ClientThread clientThread;

	@Inject
	private okhttp3.OkHttpClient okHttpClient;

	/** Between startUp and shutDown: tables that arrive after the plugin was turned off are dropped. */
	private volatile boolean running;

	/** "Bug or suggestion" in the settings works as a button: each click opens the report window, whichever way the box ends up. */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("combatcheck".equals(event.getGroup()) && "reportBug".equals(event.getKey()) && event.getNewValue() != null)
		{
			clientThread.invokeLater(() ->
			{
				final NPC monster = targetNpc;
				final String name = monster == null ? null : monster.getName();
				final String context = ReportWindow.context(client, monster);
				javax.swing.SwingUtilities.invokeLater(() -> ReportWindow.open(client.getCanvas(), name, context, okHttpClient));
			});
		}
	}
}
