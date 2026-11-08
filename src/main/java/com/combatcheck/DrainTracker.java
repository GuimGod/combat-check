/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;

/**
 * The Defence drains of every monster around, by its NPC index. The index is reused by the game for another
 * monster once one goes away, so each entry also keeps the monster itself: drains are only ever read back
 * for that same monster.
 *
 * <p>Each special attack is kept with the tick it was counted on, in that order; one estimated from what the client shows
 * ({@link SpecEstimate}) goes in by its tick among the others, and can be taken out again when the player's own message
 * comes after all. The drains are worked out from that list, one after the other ({@link DefenceDrains}).
 */
final class DrainTracker
{
	/** No player: a special attack counted exactly (the player's own, or the party's message). */
	private static final int EXACT = -1;

	private static final class Record
	{
		final SpecialWeapon weapon;
		final int hit;
		final boolean tekton;
		final int tick;
		/** The player's index for an estimated one, {@link #EXACT} otherwise. */
		final int estimatedBy;

		Record(SpecialWeapon weapon, int hit, boolean tekton, int tick, int estimatedBy)
		{
			this.weapon = weapon;
			this.hit = hit;
			this.tekton = tekton;
			this.tick = tick;
			this.estimatedBy = estimatedBy;
		}
	}

	private static final class Entry
	{
		final Object npc;
		final List<Record> records = new ArrayList<>();
		DefenceDrains drains = new DefenceDrains();
		/** The game tick of the last special attack counted. */
		int lastTick;

		Entry(Object npc)
		{
			this.npc = npc;
		}

		void rebuild()
		{
			drains = new DefenceDrains();
			for (Record r : records)
			{
				drains.add(r.weapon, r.hit, r.tekton, r.tick);
			}
		}
	}

	private final Map<Integer, Entry> byIndex = new HashMap<>();

	private Entry entry(int index, Object npc)
	{
		Entry e = byIndex.get(index);
		if (e == null || e.npc != npc)
		{
			e = new Entry(npc);
			byIndex.put(index, e);
		}
		return e;
	}

	/** Counts a special attack that landed on the monster at {@code index}, with no tick to time its drain from. */
	void add(int index, Object npc, SpecialWeapon weapon, int hit, boolean tekton)
	{
		Entry e = entry(index, npc);
		int last = e.records.isEmpty() ? 0 : e.records.get(e.records.size() - 1).tick;
		e.records.add(new Record(weapon, hit, tekton, last, EXACT));
		e.drains.add(weapon, hit, tekton, last);
		e.lastTick = 0;
	}

	/** Counts a special attack that landed on the monster at {@code index} on game tick {@code tick}. */
	void add(int index, Object npc, SpecialWeapon weapon, int hit, boolean tekton, int tick)
	{
		Entry e = entry(index, npc);
		insert(e, new Record(weapon, hit, tekton, tick, EXACT));
		e.lastTick = Math.max(e.lastTick, tick);
	}

	/**
	 * Counts a special attack of the player at {@code playerId} estimated from what the client shows, on {@code tick}
	 * (the tick the party's message of it would have come), among the others by that tick.
	 */
	void addEstimated(int index, Object npc, SpecialWeapon weapon, int hit, boolean tekton, int tick, int playerId)
	{
		Entry e = entry(index, npc);
		insert(e, new Record(weapon, hit, tekton, tick, playerId));
		e.lastTick = Math.max(e.lastTick, tick);
	}

	/** Puts the record after every one counted on the same tick or before; a later tick (an estimate) goes in its place. */
	private static void insert(Entry e, Record r)
	{
		int at = e.records.size();
		while (at > 0 && e.records.get(at - 1).tick > r.tick)
		{
			at--;
		}
		e.records.add(at, r);
		if (at == e.records.size() - 1)
		{
			e.drains.add(r.weapon, r.hit, r.tekton, r.tick);
		}
		else
		{
			e.rebuild();
		}
	}

	/**
	 * Takes out the special attacks with this weapon estimated for the player at {@code playerId} on this monster from
	 * tick {@code fromTick} on: the player's own message of it has come, which counts it exactly. True when one was.
	 */
	boolean dropEstimated(int index, Object npc, int playerId, SpecialWeapon weapon, int fromTick)
	{
		Entry e = byIndex.get(index);
		if (e == null || e.npc != npc
			|| !e.records.removeIf(r -> r.estimatedBy == playerId && r.weapon == weapon && r.tick >= fromTick))
		{
			return false;
		}
		e.rebuild();
		return true;
	}

	/** True when the drains on this monster count a special attack estimated from what the client shows. */
	boolean estimated(int index, Object npc)
	{
		Entry e = byIndex.get(index);
		return e != null && e.npc == npc && e.records.stream().anyMatch(r -> r.estimatedBy != EXACT);
	}

	/**
	 * How many special attacks are counted on this monster, the ones that lowered nothing too.
	 */
	int counted(int index, Object npc)
	{
		Entry e = byIndex.get(index);
		return e == null || e.npc != npc ? 0 : e.records.size();
	}

	/** The special attacks of one weapon counted on a monster: how many, and the hits of theirs that landed. */
	static final class WeaponCount
	{
		final SpecialWeapon weapon;
		int specs;
		/** The hits that landed, for the weapons that hit more than once (the Tonalztics of Ralos); 0 for the others. */
		int hits;
		/** True when one of them was estimated from what the client shows. */
		boolean estimated;

		WeaponCount(SpecialWeapon weapon)
		{
			this.weapon = weapon;
		}
	}

	/**
	 * The special attacks counted on this monster by weapon, in the order each weapon was first counted; a weapon that
	 * lowers nothing (Dinh's bulwark) is left out (one line per weapon that lowered the Defence, "Ralos 1 spec (2hits)",
	 * "Maul x1").
	 */
	List<WeaponCount> byWeapon(int index, Object npc)
	{
		List<WeaponCount> out = new ArrayList<>();
		Entry e = byIndex.get(index);
		if (e == null || e.npc != npc)
		{
			return out;
		}
		for (Record r : e.records)
		{
			if (r.weapon == null || r.weapon == SpecialWeapon.BULWARK)
			{
				continue;
			}
			WeaponCount c = null;
			for (WeaponCount w : out)
			{
				if (w.weapon == r.weapon)
				{
					c = w;
				}
			}
			if (c == null)
			{
				c = new WeaponCount(r.weapon);
				out.add(c);
			}
			c.specs++;
			if (multiHit(r.weapon))
			{
				c.hits += Math.max(0, Math.min(r.hit, 2));
			}
			c.estimated |= r.estimatedBy != EXACT;
		}
		return out;
	}

	/** The weapons whose special attack hits more than once, each hit lowering on its own (OSRS Wiki, Tonalztics of Ralos). */
	static boolean multiHit(SpecialWeapon weapon)
	{
		return weapon == SpecialWeapon.TONALZTICS_OF_RALOS;
	}

	/** How many ticks ago, at tick {@code now}, the last special attack was counted on this monster; 0 for none. */
	int ticksSince(int index, Object npc, int now)
	{
		Entry e = byIndex.get(index);
		return e == null || e.npc != npc ? 0 : Math.max(0, now - e.lastTick);
	}

	/** The drains on this monster, or null when none was counted. */
	DefenceDrains of(int index, Object npc)
	{
		Entry e = byIndex.get(index);
		return e == null || e.npc != npc || e.drains.none() ? null : e.drains;
	}

	/** Drops what was counted on this monster: it died, went away or changed into another. */
	void forget(int index, Object npc)
	{
		Entry e = byIndex.get(index);
		if (e != null && e.npc == npc)
		{
			byIndex.remove(index);
		}
	}

	void clear()
	{
		byIndex.clear();
	}
}
