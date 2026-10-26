/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.HashMap;
import java.util.Map;

/**
 * Bork of the Fractured Archive stunned by the Smorks' heavy cannonball, read from what the game shows. While stunned his
 * flat armour is 0 (OSRS Wiki, Bork); nothing else of the stun is known. Only the figures change: nothing is drawn for
 * the stun, and the Defence line stays as it was.
 *
 * <p>The stun as recorded in two measured fights: the cannonball falls on him, he says "Oof." and plays the stun's
 * animation ({@link #STUNNED}); 14 ticks on he says "Bork very angry!!" and starts his stomps ({@link #STOMP}), which
 * ends it. Either sign starts or ends it, whichever comes first, and it never lasts more than the {@link #TICKS}
 * measured.
 */
final class BorkStun
{
	/** Bork's NPC id. */
	static final int BORK = 16640;
	/** The animation of the stun, and of the stomps that end it (the recorder, 2026-10-20). */
	static final int STUNNED = 14545;
	static final int STOMP = 14549;
	/** The stun's length: 14 ticks from "Oof." to "Bork very angry!!" (16 fights in a trio, 2026-10-20). */
	static final int TICKS = 14;

	/** The game tick each Bork's stun began on, while it lasts; by the NPC itself. */
	private final Map<Object, Integer> since = new HashMap<>();

	/** Bork playing an animation on game tick {@code tick}: the stun's starts it, the stomps' ends it. */
	void animation(Object npc, int npcId, int animation, int tick)
	{
		if (npcId != BORK)
		{
			return;
		}
		if (animation == STUNNED)
		{
			since.putIfAbsent(npc, tick);
		}
		else if (animation == STOMP)
		{
			since.remove(npc);
		}
	}

	/** What Bork says on game tick {@code tick}: "Oof." starts the stun, "Bork very angry!!" ends it. */
	void says(Object npc, int npcId, String text, int tick)
	{
		if (npcId != BORK || text == null)
		{
			return;
		}
		if (text.startsWith("Oof"))
		{
			since.putIfAbsent(npc, tick);
		}
		else if (text.startsWith("Bork very angry"))
		{
			since.remove(npc);
		}
	}

	/** True while this Bork is stunned on game tick {@code tick}. */
	boolean stunned(Object npc, int tick)
	{
		Integer start = since.get(npc);
		if (start != null && tick - start > TICKS)
		{
			since.remove(npc);
			return false;
		}
		return start != null;
	}

	/** Drops this NPC: it died or went away. */
	void forget(Object npc)
	{
		since.remove(npc);
	}

	void clear()
	{
		since.clear();
	}
}
