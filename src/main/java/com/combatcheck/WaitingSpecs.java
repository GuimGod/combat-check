/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;

/**
 * The party's special attacks on a monster this client has not loaded yet (the player is still outside the room): the
 * message gives the monster's index only, so it waits for a monster to come into the scene at that index, and is counted
 * on it then, by the tick it landed. RuneLite's Special Attack Counter counts such a message by the index too, with no
 * monster loaded.
 *
 * <p>The game gives an index to another monster once one goes away, and the message does not say which monster it was:
 * a message waits {@link #WAIT_TICKS} ticks at most, and only the first monster to come in at its index takes it.
 */
final class WaitingSpecs
{
	/** How long a message waits for its monster: a minute. */
	static final int WAIT_TICKS = 100;

	/** A special attack of the party waiting for its monster. */
	static final class Spec
	{
		final int npcIndex;
		final SpecialWeapon weapon;
		final int hit;
		/** The tick its message came. */
		final int received;
		final int playerId;

		Spec(int npcIndex, SpecialWeapon weapon, int hit, int received, int playerId)
		{
			this.npcIndex = npcIndex;
			this.weapon = weapon;
			this.hit = hit;
			this.received = received;
			this.playerId = playerId;
		}
	}

	private final List<Spec> waiting = new ArrayList<>();

	/** A party message on the monster at {@code npcIndex}, which is not in the scene, on tick {@code tick}. */
	void add(int npcIndex, SpecialWeapon weapon, int hit, int tick, int playerId)
	{
		waiting.add(new Spec(npcIndex, weapon, hit, tick, playerId));
	}

	/**
	 * A monster came into the scene at {@code npcIndex} on tick {@code now}: the messages that waited for that index, in the
	 * order they came, the ones older than {@link #WAIT_TICKS} left out. None waits for that index any more.
	 */
	List<Spec> arrived(int npcIndex, int now)
	{
		List<Spec> out = new ArrayList<>();
		for (Iterator<Spec> i = waiting.iterator(); i.hasNext(); )
		{
			Spec s = i.next();
			if (s.npcIndex != npcIndex)
			{
				continue;
			}
			i.remove();
			if (now - s.received <= WAIT_TICKS && now >= s.received)
			{
				out.add(s);
			}
		}
		return out;
	}

	/** Drops the messages older than {@link #WAIT_TICKS} on tick {@code now}. */
	void expire(int now)
	{
		waiting.removeIf(s -> now - s.received > WAIT_TICKS || now < s.received);
	}

	/** True when no message waits. */
	boolean none()
	{
		return waiting.isEmpty();
	}

	/** Another world, another login. */
	void clear()
	{
		waiting.clear();
	}
}
