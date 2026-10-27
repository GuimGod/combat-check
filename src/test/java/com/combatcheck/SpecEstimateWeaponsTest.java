/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import net.runelite.api.HitsplatID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/**
 * The special attacks that lower the monster's levels estimated besides the hammers and the godsword: the Tonalztics of
 * Ralos, the Arclight, Darklight and Emberlight, the Accursed sceptre, the Eye of Ayak and the Barrelchest anchor; and how
 * many of the hits of one landed when other hitsplats share its tick.
 */
public class SpecEstimateWeaponsTest
{
	/** Each by its own special attack animation (the game's names), with the weapon seen in hand; never a normal attack. */
	@Test
	public void eachWeaponByItsSpecialAttackAnimation()
	{
		assertEquals(SpecialWeapon.TONALZTICS_OF_RALOS, SpecEstimate.weapon(10914, ItemID.TONALZTICS_OF_RALOS_CHARGED, false));
		assertEquals(SpecialWeapon.TONALZTICS_OF_RALOS,
			SpecEstimate.weapon(10916, ItemID.TONALZTICS_OF_RALOS_UNCHARGED, false));
		// its normal throws, charged and not
		assertNull(SpecEstimate.weapon(10923, ItemID.TONALZTICS_OF_RALOS_CHARGED, false));
		assertNull(SpecEstimate.weapon(10922, ItemID.TONALZTICS_OF_RALOS_UNCHARGED, false));
		assertEquals(SpecialWeapon.ARCLIGHT, SpecEstimate.weapon(2890, ItemID.ARCLIGHT, false));
		assertEquals(SpecialWeapon.DARKLIGHT, SpecEstimate.weapon(2890, ItemID.DARKLIGHT, false));
		// the Arclight without charges has Darklight's special attack
		assertEquals(SpecialWeapon.DARKLIGHT, SpecEstimate.weapon(2890, ItemID.ARCLIGHT_INACTIVE, false));
		assertEquals(SpecialWeapon.EMBERLIGHT, SpecEstimate.weapon(11138, ItemID.EMBERLIGHT, false));
		assertEquals(SpecialWeapon.ACCURSED_SCEPTRE, SpecEstimate.weapon(9961, ItemID.WILD_CAVE_ACCURSED_CHARGED, false));
		assertEquals(SpecialWeapon.EYE_OF_AYAK, SpecEstimate.weapon(12394, ItemID.EYE_OF_AYAK, false));
		assertEquals(SpecialWeapon.BARRELCHEST_ANCHOR, SpecEstimate.weapon(5870, ItemID.BRAIN_ANCHOR, false));
		assertEquals(SpecialWeapon.BARRELCHEST_ANCHOR, SpecEstimate.weapon(10398, ItemID.BRAIN_ANCHOR, false));
		// an animation with another weapon seen: nothing
		assertNull(SpecEstimate.weapon(10914, ItemID.TWISTED_BOW, false));
		assertNull(SpecEstimate.weapon(2890, ItemID.EMBERLIGHT, false));
		assertNull(SpecEstimate.weapon(12394, ItemID.TONALZTICS_OF_RALOS_CHARGED, false));
		// the Eye of Ayak's normal attack
		assertNull(SpecEstimate.weapon(12397, ItemID.EYE_OF_AYAK, false));
		// two hitsplats for the charged Tonalztics, one for the uncharged and every other
		assertEquals(2, SpecEstimate.splats(10914));
		assertEquals(1, SpecEstimate.splats(10916));
		assertEquals(1, SpecEstimate.splats(11124));
	}

	/**
	 * The hit delays are the Special Attack Counter's: the Tonalztics' 2 at any distance (measured: its message 3 ticks
	 * after the animation at 1 to 7 tiles), the Accursed sceptre's by the distance, the melee ones 1, the Elder maul 2.
	 */
	@Test
	public void theHitDelays()
	{
		assertEquals(2, SpecEstimate.delay(SpecialWeapon.TONALZTICS_OF_RALOS, 1));
		assertEquals(2, SpecEstimate.delay(SpecialWeapon.TONALZTICS_OF_RALOS, 7));
		assertEquals(2, SpecEstimate.delay(SpecialWeapon.ELDER_MAUL));
		assertEquals(1, SpecEstimate.delay(SpecialWeapon.DRAGON_WARHAMMER));
		assertEquals(1, SpecEstimate.delay(SpecialWeapon.ARCLIGHT));
		assertEquals(1, SpecEstimate.delay(SpecialWeapon.EMBERLIGHT));
		assertEquals(2, SpecEstimate.delay(SpecialWeapon.ACCURSED_SCEPTRE, 1));
		assertEquals(4, SpecEstimate.delay(SpecialWeapon.ACCURSED_SCEPTRE, 5));
		// a player standing on the monster's tiles is taken as next to it
		assertEquals(2, SpecEstimate.delay(SpecialWeapon.ACCURSED_SCEPTRE, 0));
	}

	/**
	 * The charged Tonalztics' two hitsplats are among the others' on the tick: with one 0 among them, one of its hits
	 * landed at least; with two, maybe none; with fewer hitsplats than its own two, nothing is known.
	 */
	@Test
	public void howManyOfTheTonalzticsHitsLanded()
	{
		Object npc = new Object();
		SpecEstimate e = new SpecEstimate();
		// all above 0: both landed (the raid's tick 32391: 1, 22 and 2; its message: 2)
		assertEquals(2, landed(e, npc, 100, 1, 22, 2).hit);
		// one 0 and two above: one at least (the raid's 33712: 0 and 9; its message: 1)
		assertEquals(1, landed(e, npc, 110, 0, 9).hit);
		// two 0s: maybe both missed
		assertNull(landedOrNull(e, npc, 120, 0, 0, 30));
		// one hitsplat: not its two
		assertNull(landedOrNull(e, npc, 130, 25));
		// the uncharged one hits once: one hitsplat above 0 is its hit
		assertTrue(e.seen(140, 9, false, 10916, 0, ItemID.TONALZTICS_OF_RALOS_UNCHARGED, 5, npc, false));
		e.hitsplat(142, npc, 25, HitsplatID.DAMAGE_OTHER, false);
		List<SpecEstimate.Landed> l = e.resolve(142);
		assertEquals(1, l.size());
		assertEquals(1, l.get(0).hit);
		// counted as it lowers: an eighth of the Magic left off Defence for each hit (DefenceDrains)
		DrainTracker drains = new DrainTracker();
		drains.addEstimated(5, npc, SpecialWeapon.TONALZTICS_OF_RALOS, 1, false, 143, 9);
		assertEquals(200 - 160 / 8, drains.of(5, npc).apply(200, 160, 0, false, 0).defence);
	}

	private static int player = 1;

	private static SpecEstimate.Landed landed(SpecEstimate e, Object npc, int tick, int... hitsplats)
	{
		SpecEstimate.Landed l = landedOrNull(e, npc, tick, hitsplats);
		assertTrue(l != null);
		return l;
	}

	private static SpecEstimate.Landed landedOrNull(SpecEstimate e, Object npc, int tick, int... hitsplats)
	{
		int p = player++;
		assertTrue(e.seen(tick, p, false, 10914, 0, ItemID.TONALZTICS_OF_RALOS_CHARGED, 5, npc, false, 4));
		e.endTick(Collections.singleton(p));
		for (int h : hitsplats)
		{
			e.hitsplat(tick + 2, npc, h, h == 0 ? HitsplatID.BLOCK_OTHER : HitsplatID.DAMAGE_OTHER, false);
		}
		// the player's own hitsplat is never one of them
		e.hitsplat(tick + 2, npc, 0, HitsplatID.BLOCK_ME, true);
		assertTrue(e.resolve(tick + 1).isEmpty());
		List<SpecEstimate.Landed> l = e.resolve(tick + 2);
		assertTrue(l.size() <= 1);
		return l.isEmpty() ? null : l.get(0);
	}

	/**
	 * Arclight, Emberlight and the Accursed sceptre lower by a hit that landed: counted with every hitsplat of others above
	 * 0, never with a 0 among them, and their miss lowers nothing, not even on Tekton.
	 */
	@Test
	public void theOnesThatLowerByAHit()
	{
		Object npc = new Object();
		SpecEstimate e = new SpecEstimate();
		assertTrue(e.seen(100, 1, false, 2890, 0, ItemID.ARCLIGHT, 5, npc, false));
		e.hitsplat(101, npc, 30, HitsplatID.DAMAGE_OTHER, false);
		e.hitsplat(101, npc, 12, HitsplatID.DAMAGE_OTHER, false);
		List<SpecEstimate.Landed> l = e.resolve(101);
		assertEquals(1, l.size());
		assertEquals(SpecialWeapon.ARCLIGHT, l.get(0).weapon);
		assertEquals(1, l.get(0).hit);
		// a 0 among them
		assertTrue(e.seen(110, 2, false, 11138, 0, ItemID.EMBERLIGHT, 5, npc, false));
		e.hitsplat(111, npc, 0, HitsplatID.BLOCK_OTHER, false);
		e.hitsplat(111, npc, 40, HitsplatID.DAMAGE_OTHER, false);
		assertTrue(e.resolve(111).isEmpty());
		// the Accursed sceptre 5 tiles off: its hit 4 ticks after the animation
		assertTrue(e.seen(120, 3, false, 9961, 0, ItemID.WILD_CAVE_ACCURSED_CHARGED, 5, npc, false, 5));
		e.hitsplat(122, npc, 33, HitsplatID.DAMAGE_OTHER, false);
		assertTrue(e.resolve(122).isEmpty());
		e.hitsplat(124, npc, 33, HitsplatID.DAMAGE_OTHER, false);
		l = e.resolve(124);
		assertEquals(1, l.size());
		assertEquals(SpecialWeapon.ACCURSED_SCEPTRE, l.get(0).weapon);
		// a miss on Tekton: only the hammers' lowers him
		Object tekton = new Object();
		assertTrue(e.seen(130, 4, false, 2890, 0, ItemID.ARCLIGHT, 4, tekton, true));
		e.hitsplat(131, tekton, 0, HitsplatID.BLOCK_OTHER, false);
		assertTrue(e.resolve(131).isEmpty());
	}

	/**
	 * The Eye of Ayak and the Barrelchest anchor lower by their damage, read as the Bandos godsword's: one hitsplat of
	 * others is theirs; with more, all above 0, the smallest, the least they dealt; with a 0 among them, nothing.
	 */
	@Test
	public void theOnesThatLowerByTheirDamage()
	{
		Object npc = new Object();
		SpecEstimate e = new SpecEstimate();
		// the Eye of Ayak: 5 ticks to its hit
		assertTrue(e.seen(100, 1, false, 12394, 0, ItemID.EYE_OF_AYAK, 5, npc, false, 3));
		e.hitsplat(105, npc, 37, HitsplatID.DAMAGE_OTHER, false);
		List<SpecEstimate.Landed> l = e.resolve(105);
		assertEquals(1, l.size());
		assertEquals(37, l.get(0).hit);
		// the anchor: 44 and 20 on the tick, all above 0, so 20 at least
		assertTrue(e.seen(110, 2, false, 5870, 0, ItemID.BRAIN_ANCHOR, 5, npc, false));
		e.hitsplat(111, npc, 44, HitsplatID.DAMAGE_OTHER, false);
		e.hitsplat(111, npc, 20, HitsplatID.DAMAGE_OTHER, false);
		l = e.resolve(111);
		assertEquals(1, l.size());
		assertEquals(20, l.get(0).hit);
		// with a 0 among them it may have missed
		assertTrue(e.seen(120, 3, false, 5870, 0, ItemID.BRAIN_ANCHOR, 5, npc, false));
		e.hitsplat(121, npc, 0, HitsplatID.BLOCK_OTHER, false);
		e.hitsplat(121, npc, 44, HitsplatID.DAMAGE_OTHER, false);
		assertTrue(e.resolve(121).isEmpty());
	}

	/**
	 * A party member whose messages come is still estimated with a weapon the Special Attack Counter does not report (the
	 * uncharged Tonalztics, the Arclight without charges): no message of it will come. With the charged one, never.
	 */
	@Test
	public void aSenderWithAWeaponTheCounterDoesNotReport()
	{
		assertTrue(SpecEstimate.reported(ItemID.TONALZTICS_OF_RALOS_CHARGED));
		assertFalse(SpecEstimate.reported(ItemID.TONALZTICS_OF_RALOS_UNCHARGED));
		assertFalse(SpecEstimate.reported(ItemID.ARCLIGHT_INACTIVE));
		assertTrue(SpecEstimate.reported(ItemID.ELDER_MAUL_ORNAMENT));
		Object npc = new Object();
		SpecEstimate e = new SpecEstimate();
		e.partySpec(7);
		assertFalse(e.seen(100, 7, false, 10914, 0, ItemID.TONALZTICS_OF_RALOS_CHARGED, 5, npc, false));
		assertTrue(e.seen(110, 7, false, 10916, 0, ItemID.TONALZTICS_OF_RALOS_UNCHARGED, 5, npc, false));
		// a message of another weapon of theirs on the way does not take this one out
		e.partySpec(7);
		e.hitsplat(112, npc, 19, HitsplatID.DAMAGE_OTHER, false);
		List<SpecEstimate.Landed> l = e.resolve(112);
		assertEquals(1, l.size());
		assertEquals(1, l.get(0).hit);
	}
}
