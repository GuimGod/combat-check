/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Ocellus in his first phase out of melee's reach while he flies from corner to corner, as anyone sees him do. Every melee attack lands 0 then, and ranged and
 * magic as usual (OSRS Wiki, The Fractured Archive/Strategies: "Protect from Melee while he moves to the corner").
 *
 * <p>Only what is seen on the screen, the tile he stands on changing: on the tick he moves and the next ({@link
 * #AFTER_WALK}, for the tick he sometimes stands still in the middle of a walk, 24 times in 3,720 steps). Measured in
 * recorded fights of 2026-10-20 to 29 (a melee attack's animation on a tick and its hitsplat the tick after): 43 of 44
 * melee hits were 0 there, elsewhere about a third, the misses of a fight. The game keeps him out of reach 3 ticks more
 * after he stops, and from the 3rd to the 18th tick after "Death... awaits..." in the middle (62 of 62 zeros), but that is
 * a time no one sees: the plugin does not tell when a boss's mechanic starts or ends (Jagex's third-party client guidelines). The jump into
 * the middle (six tiles in one tick) is no walk: he is hit as usual right after it.
 */
final class OcellusReach
{
	/** Ocellus's first phase (OSRS Wiki, Ocellus: 16673 and 16674). */
	static final int FIRST = 16673;
	static final int FIRST_ANOTHER = 16674;
	/** Out of reach up to this many ticks after a step from tile to tile. */
	static final int AFTER_WALK = 1;
	/** A monster not seen for this long is dropped: it died or went away. */
	private static final int FORGET_TICKS = 100;

	private static final class Seen
	{
		int x;
		int y;
		int seenAt;
		int walkedAt = Integer.MIN_VALUE / 2;
	}

	private final Map<Object, Seen> seen = new HashMap<>();

	static boolean firstPhase(int npcId)
	{
		return npcId == FIRST || npcId == FIRST_ANOTHER;
	}

	/** Where this Ocellus stands (his south-west tile) on game tick {@code tick}. */
	void at(Object npc, int npcId, int x, int y, int tick)
	{
		if (!firstPhase(npcId))
		{
			return;
		}
		for (Iterator<Seen> i = seen.values().iterator(); i.hasNext(); )
		{
			if (tick - i.next().seenAt > FORGET_TICKS)
			{
				i.remove();
			}
		}
		Seen s = seen.get(npc);
		if (s == null)
		{
			s = new Seen();
			seen.put(npc, s);
		}
		else if (s.seenAt < tick && Math.max(Math.abs(x - s.x), Math.abs(y - s.y)) == 1)
		{
			// one tile at a time is walking; the jump into the middle is not
			s.walkedAt = tick;
		}
		s.x = x;
		s.y = y;
		s.seenAt = tick;
	}

	/** True while melee does not reach this Ocellus on game tick {@code tick}. */
	boolean outOfReach(Object npc, int tick)
	{
		Seen s = seen.get(npc);
		return s != null && tick - s.walkedAt <= AFTER_WALK;
	}

	void clear()
	{
		seen.clear();
	}
}
