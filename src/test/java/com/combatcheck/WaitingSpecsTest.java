/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/** The party's special attacks on a monster not in the scene yet, counted when it comes in ({@link WaitingSpecs}). */
public class WaitingSpecsTest
{
	/**
	 * Two players' special attacks while the player is outside the room: when the monster comes in at that index they are
	 * counted on it, in their order, by the tick each landed; an Elder maul's message comes 2 ticks before its hit.
	 */
	@Test
	public void countedWhenTheMonsterComesIn()
	{
		WaitingSpecs w = new WaitingSpecs();
		w.add(300, SpecialWeapon.ELDER_MAUL, 1, 1000, 7);
		w.add(300, SpecialWeapon.TONALZTICS_OF_RALOS, 2, 1006, 8);
		w.add(301, SpecialWeapon.DRAGON_WARHAMMER, 40, 1008, 9);
		w.expire(1030);
		List<WaitingSpecs.Spec> in = w.arrived(300, 1040);
		assertEquals(2, in.size());
		assertEquals(SpecialWeapon.ELDER_MAUL, in.get(0).weapon);
		assertEquals(SpecialWeapon.TONALZTICS_OF_RALOS, in.get(1).weapon);
		// counted as the plugin counts them: the Elder maul 2 ticks on, the Tonalztics on the tick its message came
		Object npc = new Object();
		DrainTracker drains = new DrainTracker();
		for (WaitingSpecs.Spec s : in)
		{
			drains.add(300, npc, s.weapon, s.hit, false, CombatCheckPlugin.partyHitTick(s.weapon, s.received));
		}
		assertEquals(2, drains.counted(300, npc));
		// Defence 200, Magic 160: the maul takes 35% (130), then the two hits an eighth of the Magic each, 20 and 20 (90)
		assertEquals(90, drains.of(300, npc).apply(200, 160, 0, false, 0).defence);
		// the other index still waits
		assertTrue(!w.none());
	}

	/**
	 * The index is the game's, given to another monster once one goes away: only the first monster to come in at it takes
	 * the messages, and none older than a minute.
	 */
	@Test
	public void theIndexOfAnotherMonster()
	{
		WaitingSpecs w = new WaitingSpecs();
		w.add(300, SpecialWeapon.ELDER_MAUL, 1, 1000, 7);
		assertEquals(1, w.arrived(300, 1010).size());
		// a second monster at that index later takes nothing
		assertTrue(w.arrived(300, 1020).isEmpty());
		// older than WAIT_TICKS: dropped when the monster comes in, or by the tick's expiry
		w.add(300, SpecialWeapon.ELDER_MAUL, 1, 2000, 7);
		assertTrue(w.arrived(300, 2000 + WaitingSpecs.WAIT_TICKS + 1).isEmpty());
		w.add(301, SpecialWeapon.ELDER_MAUL, 1, 3000, 7);
		w.expire(3000 + WaitingSpecs.WAIT_TICKS);
		assertTrue(!w.none());
		w.expire(3000 + WaitingSpecs.WAIT_TICKS + 1);
		assertTrue(w.none());
		// another world or login: nothing waits
		w.add(302, SpecialWeapon.ELDER_MAUL, 1, 4000, 7);
		w.clear();
		assertTrue(w.none());
	}
}
