/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.runelite.api.HitsplatID;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;

/**
 * The special attacks that lower the monster's levels of the other players on the monster fought whose special attacks do
 * not come through the party (outside the party, or in it without the Special Attack Counter), estimated from what the
 * client shows, and counted only when there is no doubt:
 *
 * <ol>
 * <li>the player's target is that very monster and the player starts the weapon's special attack animation
 * ({@link #weapon(int, int, boolean)}) with that weapon seen in hand;</li>
 * <li>on the tick the hit lands ({@link SpecialWeapon#getHitDelay}, the Special Attack Counter's own: Elder maul and
 * Tonalztics of Ralos 2 ticks after the animation, Dragon warhammer and Bandos godsword 1, as measured against the party's
 * Special Attack Counter), the hitsplats on the monster that are not the player's own say how many of its hits landed at
 * the least ({@link #resolve}); a hitsplat does not say whose it is, and a miss counted as a hit would leave the Defence
 * lower than it is.</li>
 * </ol>
 *
 * A miss lowers nothing, except on Tekton (5%: {@link DefenceDrains}). A player whose special attack has come through the
 * party once is never estimated again with a weapon the Special Attack Counter reports, and an estimate is dropped when
 * that player's message comes after all ({@link DrainTracker#dropEstimated}). A player is known by the game's index only.
 *
 * <p>The Bandos godsword drains by its damage, which a shared tick does not tell: it is estimated only with
 * {@link #bgs(boolean)} on (always, in the plugin). Its special attack animation (7642, or 7643 for the ornament) with the
 * godsword seen in hand, the player aiming at that very monster, and on the tick its hit lands (1 tick after the
 * animation, measured: 20 of 20 of the player's own) exactly one hitsplat on the monster that is not the player's own:
 * that one is the godsword's, and its damage is the drain ({@link DefenceDrains}: Defence first, what is left over on to
 * the other levels). A 0 is a miss: nothing, except on Tekton, where it drains 10 (OSRS Wiki, Bandos godsword: "Against
 * Tekton, it drains his combat skills by 10 (following the stat drain order) if it fails to hit"). The Eye of Ayak and the
 * Barrelchest anchor drain by their damage too, and are read the same way.
 */
final class SpecEstimate
{
	/** Ticks from the Elder maul's special attack animation to its hitsplat (measured: 72 of 72 and 65 of 65). */
	static final int ELDER_MAUL_DELAY = 2;
	/** Ticks from the Dragon warhammer's special attack animation to its hitsplat (measured: 23 of 23 by the damage). */
	static final int DWH_DELAY = 1;
	/** Ticks from the Bandos godsword's special attack animation to its hitsplat (measured: 20 of 20 of the player's own). */
	static final int BGS_DELAY = 1;
	/** Two starts of the animation by the same player closer than this are taken as one attack (the three weapons take 6). */
	static final int MIN_TICKS_BETWEEN = 4;

	/*
	 * The special attack animations, by the game's own names (RuneLite's gameval AnimationID). Ralos: 10914 is the charged
	 * weapon's (two hits), 10916 the uncharged one's (one); its normal throws are 10922 and 10923. Measured against the
	 * party's Special Attack Counter in the Fractured Archive (2026-10-20 to 27): a message 3 ticks after each 10914, at 1
	 * to 7 tiles (407 of 450; the others were the next attack), so its hit 2 ticks after, as the Counter's hit delay says.
	 */
	static final int RALOS_CHARGED_SPECIAL = AnimationID.HUMAN_GLAIVE_RALOS01_CHARGED_SPECIAL;
	static final int RALOS_UNCHARGED_SPECIAL = AnimationID.HUMAN_GLAIVE_RALOS01_UNCHARGED_SPECIAL;
	/** Darklight's special attack, Weaken, which the Arclight shares (DefenceDrains counts them as one). */
	static final int DARKLIGHT_SPECIAL = AnimationID.DARK_SPEC_PLAYER;
	static final int EMBERLIGHT_SPECIAL = AnimationID.HUMAN_WEAPON_EMBERLIGHT_01_SPEC;
	static final int ACCURSED_SPECIAL = AnimationID.HUMAN_SPECIAL_ACCURSED;
	static final int AYAK_SPECIAL = AnimationID.HUMAN_EYE_OF_AYAK_SPECIAL;
	static final int ANCHOR_SPECIAL = AnimationID.BRAIN_PLAYER_ANCHOR_SPECIAL_ATTACK;
	static final int ANCHOR_SPECIAL_LONG = AnimationID.BRAIN_PLAYER_ANCHOR_SPECIAL_ATTACK_LONG;

	/** A special attack estimated and decided: what the drains count, on the tick a party message of it would come. */
	static final class Landed
	{
		final int playerId;
		final int npcIndex;
		final Object npc;
		final SpecialWeapon weapon;
		/**
		 * 1 when it hit, 0 when it missed (counted only on Tekton); for a Bandos godsword the damage of its hitsplat, as the
		 * Special Attack Counter's message gives it.
		 */
		final int hit;
		final boolean tekton;
		/** The tick after its hitsplat: when the Special Attack Counter's message of it would come. */
		final int tick;

		Landed(int playerId, int npcIndex, Object npc, SpecialWeapon weapon, int hit, boolean tekton, int tick)
		{
			this.playerId = playerId;
			this.npcIndex = npcIndex;
			this.npc = npc;
			this.weapon = weapon;
			this.hit = hit;
			this.tekton = tekton;
			this.tick = tick;
		}
	}

	private static final class Pending
	{
		final int playerId;
		final int npcIndex;
		final Object npc;
		final SpecialWeapon weapon;
		final int hitTick;
		final boolean tekton;
		/** How many hitsplats the special attack deals: 2 for the charged Tonalztics of Ralos, 1 for the others. */
		final int splats;
		/** True when the weapon is one the Special Attack Counter reports: a message of it may come. */
		final boolean reported;
		/** The hitsplats on the monster on that tick that are not the player's own. */
		final List<Integer> hits = new ArrayList<>();

		Pending(int playerId, int npcIndex, Object npc, SpecialWeapon weapon, int hitTick, boolean tekton, int splats,
			boolean reported)
		{
			this.playerId = playerId;
			this.npcIndex = npcIndex;
			this.npc = npc;
			this.weapon = weapon;
			this.hitTick = hitTick;
			this.tekton = tekton;
			this.splats = splats;
			this.reported = reported;
		}
	}

	/** The animation and weapon last seen of each player aiming at the monster, by the game's index. */
	private final Map<Integer, String> states = new HashMap<>();
	/** The players whose animation was set this tick: the same animation set again is another attack. */
	private final Set<Integer> animationSet = new HashSet<>();
	/** The tick of each player's last special attack estimated. */
	private final Map<Integer, Integer> lastStart = new HashMap<>();
	/** The players whose special attacks come through the party: never estimated. */
	private final Set<Integer> senders = new HashSet<>();
	private final List<Pending> pending = new ArrayList<>();
	/** True when the Bandos godsword is estimated too (always, in the plugin). */
	private boolean bgs;

	/** The players whose target (Actor.getInteracting) is this very monster, not another of the same id. */
	static <P> List<P> aiming(Iterable<P> players, Function<P, Object> interacting, Object target)
	{
		List<P> aiming = new ArrayList<>();
		if (target == null || players == null)
		{
			return aiming;
		}
		for (P p : players)
		{
			if (p != null && interacting.apply(p) == target)
			{
				aiming.add(p);
			}
		}
		return aiming;
	}

	/**
	 * The special attack an animation is, with the weapon seen in hand; null for any other. The Bandos godsword is one only
	 * when it is estimated ({@link #bgs(boolean)}); see {@link #weapon(int, int, boolean)}.
	 */
	static SpecialWeapon weapon(int animation, int weaponItem)
	{
		return weapon(animation, weaponItem, false);
	}

	/**
	 * The special attack an animation is, with the weapon seen in hand, the Bandos godsword with {@code bgs}: the weapon's
	 * own special attack animation only, never its normal attacks. The Seercull, the bone dagger and the Dorgeshuun
	 * crossbow are not estimated: no animation of theirs is known for sure to be the special attack. Dinh's bulwark lowers
	 * nothing.
	 */
	static SpecialWeapon weapon(int animation, int seenItem, boolean bgs)
	{
		// another dress of a weapon (an ornament kit, a corrupted one: Variants), the Arclight and the Tonalztics without
		// charges, are the weapon whose special attack they have
		SpecialWeapon held = CombatCheckPlugin.specialWeapon(seenItem);
		if (held == null)
		{
			return null;
		}
		switch (held)
		{
			case BANDOS_GODSWORD:
				return bgs && (animation == AnimationID.BGS_SPECIAL_PLAYER
					|| animation == AnimationID.BGS_SPECIAL_ORNATE_PLAYER) ? held : null;
			case ELDER_MAUL:
				return animation == AnimationID.HUMAN_ELDER_MAUL_SPEC ? held : null;
			case DRAGON_WARHAMMER:
				return animation == AnimationID.DRAGON_WARHAMMER_SA_PLAYER ? held : null;
			case TONALZTICS_OF_RALOS:
				return animation == RALOS_CHARGED_SPECIAL || animation == RALOS_UNCHARGED_SPECIAL ? held : null;
			case ARCLIGHT:
			case DARKLIGHT:
				return animation == DARKLIGHT_SPECIAL ? held : null;
			case EMBERLIGHT:
				return animation == EMBERLIGHT_SPECIAL ? held : null;
			case ACCURSED_SCEPTRE:
				return animation == ACCURSED_SPECIAL ? held : null;
			case EYE_OF_AYAK:
				return animation == AYAK_SPECIAL ? held : null;
			case BARRELCHEST_ANCHOR:
				return animation == ANCHOR_SPECIAL || animation == ANCHOR_SPECIAL_LONG ? held : null;
			default:
				return null;
		}
	}

	/**
	 * How many hitsplats the special attack of this animation deals: two for the charged Tonalztics of Ralos (OSRS Wiki,
	 * Tonalztics of Ralos: "Uncharged, it will only hit once"), one for the others.
	 */
	static int splats(int animation)
	{
		return animation == RALOS_CHARGED_SPECIAL ? 2 : 1;
	}

	/**
	 * True for the special attacks that drain by their damage, which a shared tick tells only as a bound: the Bandos
	 * godsword, the Eye of Ayak and the Barrelchest anchor. The others drain by whether each hit landed.
	 */
	static boolean drainsByDamage(SpecialWeapon w)
	{
		return w == SpecialWeapon.BANDOS_GODSWORD || w == SpecialWeapon.EYE_OF_AYAK
			|| w == SpecialWeapon.BARRELCHEST_ANCHOR;
	}

	/**
	 * True when the Special Attack Counter reports a special attack with this item in hand: it knows a weapon by its own
	 * list of items (SpecialWeapon), so an item it does not list (the Tonalztics and the Arclight without charges, a dress
	 * it does not have) sends no message, and is estimated even for a player whose messages come.
	 */
	static boolean reported(int item)
	{
		for (SpecialWeapon w : SpecialWeapon.values())
		{
			for (int id : w.getItemID())
			{
				if (id == item)
				{
					return true;
				}
			}
		}
		return false;
	}

	/** True for a hitsplat of damage dealt (or a 0) to the monster: not a heal, poison, burn and the like. */
	static boolean damage(int type)
	{
		switch (type)
		{
			case HitsplatID.BLOCK_ME:
			case HitsplatID.BLOCK_OTHER:
			case HitsplatID.DAMAGE_ME:
			case HitsplatID.DAMAGE_OTHER:
			case HitsplatID.DAMAGE_ME_CYAN:
			case HitsplatID.DAMAGE_OTHER_CYAN:
			case HitsplatID.DAMAGE_ME_ORANGE:
			case HitsplatID.DAMAGE_OTHER_ORANGE:
			case HitsplatID.DAMAGE_ME_YELLOW:
			case HitsplatID.DAMAGE_OTHER_YELLOW:
			case HitsplatID.DAMAGE_ME_WHITE:
			case HitsplatID.DAMAGE_OTHER_WHITE:
			case HitsplatID.DAMAGE_MAX_ME:
			case HitsplatID.DAMAGE_MAX_ME_CYAN:
			case HitsplatID.DAMAGE_MAX_ME_ORANGE:
			case HitsplatID.DAMAGE_MAX_ME_YELLOW:
			case HitsplatID.DAMAGE_MAX_ME_WHITE:
			case HitsplatID.DAMAGE_ME_POISE:
			case HitsplatID.DAMAGE_OTHER_POISE:
			case HitsplatID.DAMAGE_MAX_ME_POISE:
				return true;
			default:
				return false;
		}
	}

	/** The player's animation was set (AnimationChanged) this tick. */
	void animationSet(int playerId)
	{
		animationSet.add(playerId);
	}

	/** The same, with the player next to the monster: the hit delays of the weapons fought up close. */
	boolean seen(int tick, int playerId, boolean me, int animation, int frame, int weaponItem, int npcIndex, Object npc,
		boolean tekton)
	{
		return seen(tick, playerId, me, animation, frame, weaponItem, npcIndex, npc, tekton, 1);
	}

	/**
	 * One player whose target is the monster fought ({@code npc}, at {@code npcIndex}), {@code distance} tiles from it, as
	 * seen on game tick {@code tick}: true when a special attack is estimated from it. The animation starts on its first
	 * frame, or when it is set again with nothing changed; the player's own are not estimated, nor those of the party's
	 * senders with a weapon the Special Attack Counter reports ({@link #reported}).
	 */
	boolean seen(int tick, int playerId, boolean me, int animation, int frame, int weaponItem, int npcIndex, Object npc,
		boolean tekton, int distance)
	{
		String state = animation + " " + weaponItem;
		boolean changed = !state.equals(states.put(playerId, state));
		boolean again = !changed && animation != -1 && (animationSet.contains(playerId) || frame == 0);
		boolean start = again || (changed && frame == 0);
		SpecialWeapon w = weapon(animation, weaponItem, bgs);
		boolean reported = reported(weaponItem);
		if (!start || w == null || me || reported && senders.contains(playerId))
		{
			return false;
		}
		Integer last = lastStart.get(playerId);
		if (last != null && tick - last < MIN_TICKS_BETWEEN && tick >= last)
		{
			return false;
		}
		lastStart.put(playerId, tick);
		pending.add(new Pending(playerId, npcIndex, npc, w, tick + delay(w, distance), tekton, splats(animation),
			reported));
		return true;
	}

	/** Ticks from the special attack animation to its hitsplat, next to the monster. */
	static int delay(SpecialWeapon w)
	{
		return delay(w, 1);
	}

	/**
	 * Ticks from the special attack animation to its hitsplat, {@code distance} tiles from the monster: the Special Attack
	 * Counter's own hit delay (Elder maul 2, Dragon warhammer and Bandos godsword 1, the Tonalztics of Ralos 2 at any
	 * distance, as measured), which it times the player's own special attack's hitsplat by.
	 */
	static int delay(SpecialWeapon w, int distance)
	{
		// the distance as the game gives it, far for another plane: no special attack reaches past 10 tiles
		return w.getHitDelay(Math.max(1, Math.min(distance, MAX_DISTANCE)));
	}

	/** The farthest distance the hit delay is worked out for. */
	private static final int MAX_DISTANCE = 10;

	/**
	 * Whether the Bandos godsword is estimated too (the plugin always turns it on); turned off, the
	 * ones not yet decided are dropped.
	 */
	void bgs(boolean on)
	{
		bgs = on;
		if (!on)
		{
			pending.removeIf(p -> p.weapon == SpecialWeapon.BANDOS_GODSWORD);
		}
	}

	/** The end of the tick's look at the players: the ones no longer aiming at the monster are forgotten until they do. */
	void endTick(Set<Integer> aimingNow)
	{
		states.keySet().retainAll(aimingNow);
		animationSet.clear();
	}

	/** A hitsplat on a monster on game tick {@code tick}; the player's own are left out. */
	void hitsplat(int tick, Object npc, int amount, int type, boolean mine)
	{
		if (mine || !damage(type))
		{
			return;
		}
		for (Pending p : pending)
		{
			if (p.npc == npc && p.hitTick == tick)
			{
				p.hits.add(amount);
			}
		}
	}

	/**
	 * The special attacks whose hit has landed by game tick {@code now} (every hitsplat of a tick has come by that tick's
	 * GameTick) and that lower something: a hit, or a miss on Tekton. The others are dropped.
	 *
	 * <p>A special attack of {@code n} hitsplats (two for the charged Tonalztics of Ralos, one for the others) is among the
	 * hitsplats that are not the player's own on that tick, so with {@code z} of them 0, at least {@code n - z} of its hits
	 * landed: that many are counted, never more (one of the 0s may be its own), and none with fewer hitsplats than its own.
	 * For one hitsplat that is: all of them above 0. A miss is counted (on Tekton, where it lowers something) only when every
	 * hitsplat is 0. One that drains by its damage ({@link #drainsByDamage}) counts with exactly one hitsplat, which is then
	 * its damage, or with every one above 0 by the smallest, the least it can have dealt.
	 */
	List<Landed> resolve(int now)
	{
		List<Landed> out = new ArrayList<>();
		for (Iterator<Pending> i = pending.iterator(); i.hasNext(); )
		{
			Pending p = i.next();
			if (p.hitTick > now)
			{
				continue;
			}
			i.remove();
			if (p.reported && senders.contains(p.playerId) || p.hits.size() < p.splats)
			{
				continue;
			}
			boolean allHit = p.hits.stream().allMatch(h -> h > 0);
			boolean allMissed = p.hits.stream().allMatch(h -> h == 0);
			if (drainsByDamage(p.weapon))
			{
				int damage = p.hits.size() == 1 ? p.hits.get(0) : allHit ? Collections.min(p.hits) : 0;
				// on Tekton a Bandos godsword's miss drains 10: only with its one hitsplat a 0
				boolean tektonMiss = p.weapon == SpecialWeapon.BANDOS_GODSWORD && p.tekton && p.hits.size() == 1;
				if (damage > 0 || tektonMiss)
				{
					out.add(new Landed(p.playerId, p.npcIndex, p.npc, p.weapon, damage, p.tekton, p.hitTick + 1));
				}
				continue;
			}
			long zeros = p.hits.stream().filter(h -> h == 0).count();
			int landed = (int) Math.max(0, p.splats - zeros);
			if (landed > 0 || (allMissed && p.tekton && missLowers(p.weapon)))
			{
				out.add(new Landed(p.playerId, p.npcIndex, p.npc, p.weapon, landed, p.tekton, p.hitTick + 1));
			}
		}
		return out;
	}

	/** True for the special attacks whose miss still lowers Tekton's Defence (5%: {@link DefenceDrains}). */
	private static boolean missLowers(SpecialWeapon w)
	{
		return w == SpecialWeapon.ELDER_MAUL || w == SpecialWeapon.DRAGON_WARHAMMER;
	}

	/**
	 * A Special Attack Counter message from the player at {@code playerId}: theirs with a weapon it reports come exactly
	 * from now on.
	 */
	void partySpec(int playerId)
	{
		senders.add(playerId);
		pending.removeIf(p -> p.playerId == playerId && p.reported);
	}

	/**
	 * The player at {@code playerId} no longer sends through the party (they left it): their special attacks are estimated
	 * again, as any other player's.
	 */
	void notSender(int playerId)
	{
		senders.remove(playerId);
	}

	/** Another party joined, or none: nobody is known to send through it. */
	void noSenders()
	{
		senders.clear();
	}

	/** Another world, another login, or the option turned off. */
	void clear()
	{
		states.clear();
		animationSet.clear();
		lastStart.clear();
		senders.clear();
		pending.clear();
	}
}
