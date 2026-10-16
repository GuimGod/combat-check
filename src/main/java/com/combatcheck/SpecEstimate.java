/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.runelite.api.HitsplatID;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;

/**
 * The Elder maul and Dragon warhammer special attacks (and the Bandos godsword's, below) of the other
 * players on the monster fought whose special attacks do not come through the party (outside the party, or in it
 * without the Special Attack Counter), estimated from what the client shows, and counted only when there is no doubt:
 *
 * <ol>
 * <li>the player's target is that very monster and the player starts the weapon's special attack animation (Elder maul
 * 11124, Dragon warhammer 1378) with that weapon seen in hand;</li>
 * <li>on the tick the hit lands (Elder maul 2 ticks after the animation, Dragon warhammer 1, as measured in a Chambers of
 * Xeric raid against the party's Special Attack Counter), the hitsplats on the monster that are not the player's own are
 * all above 0 (it hit) or all 0 (it missed); anything else (some 0, some not, or none) is not counted, since the hitsplat
 * does not say whose it is, and a miss counted as a hit would leave the Defence lower than it is.</li>
 * </ol>
 *
 * A miss lowers nothing, except on Tekton (5%: {@link DefenceDrains}). A player whose special attack has come through the
 * party once is never estimated again, and an estimate is dropped when that player's message comes after all
 * ({@link DrainTracker#dropEstimated}). A player is known by the game's index only.
 *
 * <p>The Bandos godsword drains by its damage, which a shared tick does not tell: it is estimated only with
 * {@link #bgs(boolean)} on (always, in the plugin), and only when there is no doubt whose hitsplat it is. Its special attack animation (7642, or
 * 7643 for the ornament) with the godsword seen in hand, the player aiming at that very monster, and on the tick its hit
 * lands (1 tick after the animation, measured: 20 of 20 of the player's own) exactly one hitsplat on the monster that is
 * not the player's own: that one is the godsword's, and its damage is the drain ({@link DefenceDrains}: Defence first,
 * what is left over on to the other levels). Two or more, or none, and it is not counted. A 0 is a miss: nothing, except
 * on Tekton, where it drains 10 (OSRS Wiki, Bandos godsword: "Against Tekton, it drains his combat skills by 10
 * (following the stat drain order) if it fails to hit"). The other special attacks are never estimated.
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
		/** The hitsplats on the monster on that tick that are not the player's own. */
		final List<Integer> hits = new ArrayList<>();

		Pending(int playerId, int npcIndex, Object npc, SpecialWeapon weapon, int hitTick, boolean tekton)
		{
			this.playerId = playerId;
			this.npcIndex = npcIndex;
			this.npc = npc;
			this.weapon = weapon;
			this.hitTick = hitTick;
			this.tekton = tekton;
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
	/** True when the Bandos godsword is estimated too (always, in the plugin: "Estimate specs"). */
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

	/** The special attack an animation is, with the weapon seen in hand, the Bandos godsword with {@code bgs}. */
	static SpecialWeapon weapon(int animation, int weaponItem, boolean bgs)
	{
		if (bgs && (animation == AnimationID.BGS_SPECIAL_PLAYER || animation == AnimationID.BGS_SPECIAL_ORNATE_PLAYER)
			&& (weaponItem == ItemID.BGS || weaponItem == ItemID.BGSG))
		{
			return SpecialWeapon.BANDOS_GODSWORD;
		}
		if (animation == AnimationID.HUMAN_ELDER_MAUL_SPEC
			&& (weaponItem == ItemID.ELDER_MAUL || weaponItem == ItemID.ELDER_MAUL_ORNAMENT))
		{
			return SpecialWeapon.ELDER_MAUL;
		}
		if (animation == AnimationID.DRAGON_WARHAMMER_SA_PLAYER && (weaponItem == ItemID.DRAGON_WARHAMMER
			|| weaponItem == ItemID.DRAGON_WARHAMMER_ORNAMENT || weaponItem == ItemID.BH_DRAGON_WARHAMMER_CORRUPTED))
		{
			return SpecialWeapon.DRAGON_WARHAMMER;
		}
		return null;
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

	/**
	 * One player whose target is the monster fought ({@code npc}, at {@code npcIndex}), as seen on game tick {@code tick}:
	 * true when a special attack is estimated from it. The animation starts on its first frame, or when it is set again
	 * with nothing changed; the player's own and those of the party's senders are not estimated.
	 */
	boolean seen(int tick, int playerId, boolean me, int animation, int frame, int weaponItem, int npcIndex, Object npc,
		boolean tekton)
	{
		String state = animation + " " + weaponItem;
		boolean changed = !state.equals(states.put(playerId, state));
		boolean again = !changed && animation != -1 && (animationSet.contains(playerId) || frame == 0);
		boolean start = again || (changed && frame == 0);
		SpecialWeapon w = weapon(animation, weaponItem, bgs);
		if (!start || w == null || me || senders.contains(playerId))
		{
			return false;
		}
		Integer last = lastStart.get(playerId);
		if (last != null && tick - last < MIN_TICKS_BETWEEN && tick >= last)
		{
			return false;
		}
		lastStart.put(playerId, tick);
		pending.add(new Pending(playerId, npcIndex, npc, w, tick + delay(w), tekton));
		return true;
	}

	/** Ticks from the special attack animation to its hitsplat. */
	static int delay(SpecialWeapon w)
	{
		switch (w)
		{
			case ELDER_MAUL:
				return ELDER_MAUL_DELAY;
			case BANDOS_GODSWORD:
				return BGS_DELAY;
			default:
				return DWH_DELAY;
		}
	}

	/**
	 * Whether the Bandos godsword is estimated too (the plugin always turns it on with "Estimate specs"); turned off, the
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
	 * GameTick) and that lower something: a hit, or a miss on Tekton. The others are dropped. A Bandos godsword counts only
	 * with exactly one hitsplat that is not the player's own on that tick, which is then its damage.
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
			if (senders.contains(p.playerId) || p.hits.isEmpty())
			{
				continue;
			}
			if (p.weapon == SpecialWeapon.BANDOS_GODSWORD)
			{
				int damage = p.hits.get(0);
				if (p.hits.size() == 1 && (damage > 0 || p.tekton))
				{
					out.add(new Landed(p.playerId, p.npcIndex, p.npc, p.weapon, damage, p.tekton, p.hitTick + 1));
				}
				continue;
			}
			boolean allHit = p.hits.stream().allMatch(h -> h > 0);
			boolean allMissed = p.hits.stream().allMatch(h -> h == 0);
			if (allHit || (allMissed && p.tekton))
			{
				out.add(new Landed(p.playerId, p.npcIndex, p.npc, p.weapon, allHit ? 1 : 0, p.tekton, p.hitTick + 1));
			}
		}
		return out;
	}

	/** A Special Attack Counter message from the player at {@code playerId}: theirs come exactly from now on. */
	void partySpec(int playerId)
	{
		senders.add(playerId);
		pending.removeIf(p -> p.playerId == playerId);
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
