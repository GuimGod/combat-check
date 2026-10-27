/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/**
 * The Tonalztics of Ralos and the Elder maul of two Fractured Archive raids ({@code spec_estimate_archive.txt}: the local
 * "SPEC diagnostic" lines of 2026-10-27, players as numbers), estimated as if neither player sent through the party,
 * against their own party messages of the same ticks.
 */
public class SpecEstimateArchiveTest
{
	private static SpecEstimateTest.Message messageOf(SpecEstimateTest.Replay r, SpecEstimate.Landed l)
	{
		for (SpecEstimateTest.Message m : r.messages)
		{
			// the message comes on the tick after the hit, the Elder maul's on the tick of it or the one after
			if (!m.me && m.player == l.playerId && m.weapon == l.weapon && m.npcIndex == l.npcIndex
				&& m.tick >= l.tick - 1 && m.tick <= l.tick)
			{
				return m;
			}
		}
		return null;
	}

	/**
	 * Every special attack counted agrees with its message: never more hits than it landed. Of the 8 Tonalztics
	 * special attacks seen start on the monster fought, 5 are counted, each with as many hits as its message (2, 1, 1, 2,
	 * 2); the 3 left out are 2 that missed with both hits (messages 0: nothing to lower) and 1 whose monster had no hitsplat
	 * of others on the tick (the player had turned to the next one). The Elder maul: 3 of 4 counted as hits, all hits; the
	 * fourth fell on a 0 and a 69 on one tick.
	 */
	@Test
	public void theRaidsRalosAndMaulsAgainstTheAnswerKey() throws IOException
	{
		SpecEstimateTest.Replay r = SpecEstimateTest.replay("spec_estimate_archive.txt", false, false);
		List<Integer> ralosHits = new ArrayList<>();
		int mauls = 0;
		for (SpecEstimate.Landed l : r.landed)
		{
			SpecEstimateTest.Message m = messageOf(r, l);
			assertNotNull("player " + l.playerId + " " + l.weapon + " at " + l.tick + ": no message", m);
			assertTrue("never more hits than the message: " + l.hit + " against " + m.hit, l.hit <= m.hit);
			if (l.weapon == SpecialWeapon.TONALZTICS_OF_RALOS)
			{
				assertEquals("as many hits as its message", m.hit, l.hit);
				ralosHits.add(l.hit);
			}
			else
			{
				assertEquals(SpecialWeapon.ELDER_MAUL, l.weapon);
				mauls++;
			}
		}
		assertEquals(java.util.Arrays.asList(2, 1, 1, 2, 2), ralosHits);
		assertEquals(3, mauls);
		assertEquals(8, r.started.stream().filter(s -> s.endsWith("TONALZTICS_OF_RALOS")).count());
		assertEquals(4, r.started.stream().filter(s -> s.endsWith("ELDER_MAUL")).count());
	}

	/**
	 * With the party's messages, as the plugin runs in that raid: only the first Tonalztics special attack of player 2 is
	 * estimated, decided before that player's first message came; the message takes it out and counts it once, and from
	 * then on nothing of theirs is estimated.
	 */
	@Test
	public void withThePartyEachIsCountedOnce() throws IOException
	{
		SpecEstimateTest.Replay r = SpecEstimateTest.replay("spec_estimate_archive.txt", true, false);
		assertEquals(1, r.landed.size());
		assertEquals(32392, r.landed.get(0).tick);
		Object npc = r.npc(52674);
		assertTrue(!r.drains.estimated(52674, npc));
		// the Elder maul's message and the Tonalztics' on that monster
		assertEquals(2, r.drains.counted(52674, npc));
	}
}
