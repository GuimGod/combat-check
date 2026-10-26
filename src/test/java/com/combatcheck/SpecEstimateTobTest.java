/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * The Elder maul and Dragon warhammer special attacks of the Theatre of Blood of 2026-10-17 (three raids, normal mode,
 * 3 players; spec_estimate_tob.txt), as the plugin counts them: the party's Special Attack Counter messages when there
 * is a RuneLite party, the estimate from the animation and the hitsplats when there is not. The player's own are
 * counted when they land and are not in the file's replay.
 */
public class SpecEstimateTobTest
{
	private static final String FILE = "spec_estimate_tob.txt";
	/** The monsters by their index in the file. */
	private static final int MAIDEN_1 = 52082;
	private static final int SOTETSEG_1 = 36561;
	private static final int XARPUS_1 = 41928;
	private static final int MAIDEN_2 = 36597;
	private static final int SOTETSEG_2 = 55118;
	private static final int XARPUS_2 = 60926;
	private static final int MAIDEN_3 = 51800;

	/**
	 * The second raid, with a party of the three: every special attack has its message. Taking everybody as outside the
	 * party, every estimate decided agrees with the message (8 of 8 hits); the maul that missed Xarpus is not counted (a
	 * miss lowers nothing there), one hit on Xarpus fell on a tick that could not be decided, and the two on Sotetseg
	 * while the player attacked a Nylocas are counted only by their message.
	 */
	@Test
	public void everyEstimateOfTheSecondRaidAgreesWithItsMessage() throws IOException
	{
		SpecEstimateTest.Replay r = SpecEstimateTest.replay(FILE, false, false);
		int checked = 0;
		List<String> wrong = new ArrayList<>();
		for (SpecEstimate.Landed l : r.landed)
		{
			if (l.tick < 6464 || l.tick > 7960)
			{
				continue;
			}
			SpecEstimateTest.Message mine = null;
			for (SpecEstimateTest.Message m : r.messages)
			{
				if (!m.me && m.player == l.playerId && m.weapon == l.weapon && m.npcIndex == l.npcIndex
					&& m.tick >= l.tick - 1 && m.tick <= l.tick)
				{
					mine = m;
				}
			}
			if (mine == null || mine.hit == 0)
			{
				wrong.add(l.tick + " " + l.playerId + " " + l.weapon);
			}
			checked++;
		}
		assertEquals(8, checked);
		assertTrue(wrong.toString(), wrong.isEmpty());
	}

	/** Each room as the plugin counted the team's (not the player's own) special attacks, raid by raid. */
	@Test
	public void whatWasCountedInEachRoom() throws IOException
	{
		SpecEstimateTest.Replay r = SpecEstimateTest.replay(FILE, true, false);
		// first raid, no party: all estimated. The Maiden one maul; Sotetseg two mauls (one Dragon warhammer on a tick
		// with a 1 and a 0, not decided; the other missed); Xarpus two mauls and two hammers
		assertCounted(r, MAIDEN_1, 1, true);
		assertCounted(r, SOTETSEG_1, 2, true);
		assertCounted(r, XARPUS_1, 4, true);
		// second raid, the party of three: every one by its message, nothing estimated
		assertCounted(r, MAIDEN_2, 4, false);
		assertCounted(r, SOTETSEG_2, 4, false);
		assertCounted(r, XARPUS_2, 3, false);
		// third raid, another team: one player's two mauls by message, another's maul (no message) estimated
		assertCounted(r, MAIDEN_3, 3, true);
	}

	private static void assertCounted(SpecEstimateTest.Replay r, int npc, int counted, boolean estimated)
	{
		assertEquals("counted on " + npc, counted, r.drains.counted(npc, r.npc(npc)));
		if (estimated)
		{
			assertTrue("estimated on " + npc, r.drains.estimated(npc, r.npc(npc)));
		}
		else
		{
			assertFalse("estimated on " + npc, r.drains.estimated(npc, r.npc(npc)));
		}
	}
}
