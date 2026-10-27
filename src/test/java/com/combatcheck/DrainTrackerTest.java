/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/** The drains kept for each monster in a fight, and the tick of the last special attack counted on it. */
public class DrainTrackerTest
{
	/** Each monster keeps its own drains, and a monster that dies or goes away loses them. */
	@Test
	public void perMonster()
	{
		DrainTracker t = new DrainTracker();
		Object tekton = new Object();
		Object other = new Object();
		t.add(5, tekton, SpecialWeapon.ELDER_MAUL, 1, true);
		t.add(5, tekton, SpecialWeapon.ELDER_MAUL, 1, true);
		t.add(9, other, SpecialWeapon.DRAGON_WARHAMMER, 30, false);
		// Tekton (205) after two Elder mauls: 88; the other monster after a Dragon warhammer: 70% of 200
		assertEquals(88, t.of(5, tekton).apply(205, 205, 0, false, 0).defence);
		assertEquals(140, t.of(9, other).apply(200, 100, 0, false, 0).defence);
		// the same index taken by another monster: nothing of the old one is carried over
		Object next = new Object();
		assertTrue(t.of(5, next) == null);
		t.add(5, next, SpecialWeapon.DRAGON_WARHAMMER, 30, false);
		assertEquals(140, t.of(5, next).apply(200, 100, 0, false, 0).defence);
		t.forget(5, next);
		assertTrue(t.of(5, next) == null);
		assertEquals(140, t.of(9, other).apply(200, 100, 0, false, 0).defence);
		t.clear();
		assertTrue(t.of(9, other) == null);
	}

	/** The tick of the last special attack counted on a monster: the Nightmares win their Defence back from it. */
	@Test
	public void theTickOfTheLastSpecialAttack()
	{
		DrainTracker tracker = new DrainTracker();
		Object phosani = new Object();
		tracker.add(3, phosani, SpecialWeapon.DRAGON_WARHAMMER, 40, false, 1000);
		assertEquals(50, tracker.ticksSince(3, phosani, 1050));
		assertEquals(0, tracker.ticksSince(3, new Object(), 1050));
		tracker.add(3, phosani, SpecialWeapon.DRAGON_WARHAMMER, 40, false, 1040);
		assertEquals(10, tracker.ticksSince(3, phosani, 1050));
	}

	/**
	 * An estimated special attack goes in by the tick it landed, among the exact ones: an Elder maul estimated for tick 104
	 * comes before a Bandos godsword counted at 105, whatever came first. Taken out, the drains are the exact ones alone.
	 */
	@Test
	public void anEstimateTakesItsPlaceByTheTick()
	{
		Object xarpus = new Object();
		DrainTracker tracker = new DrainTracker();
		tracker.add(2, xarpus, SpecialWeapon.BANDOS_GODSWORD, 50, false, 105);
		tracker.addEstimated(2, xarpus, SpecialWeapon.ELDER_MAUL, 1, false, 104, 9);
		// maul then godsword 50 on 250: 163, less 50 = 113 (the simulator's order case); godsword first would be 130
		assertEquals(113, tracker.of(2, xarpus).apply(250, 250, 0, false, 0).defence);
		assertTrue(tracker.estimated(2, xarpus));
		assertEquals(2, tracker.counted(2, xarpus));
		// another player's, or another weapon's, message takes nothing out
		assertTrue(!tracker.dropEstimated(2, xarpus, 8, SpecialWeapon.ELDER_MAUL, 0));
		assertTrue(!tracker.dropEstimated(2, xarpus, 9, SpecialWeapon.DRAGON_WARHAMMER, 0));
		assertTrue(tracker.dropEstimated(2, xarpus, 9, SpecialWeapon.ELDER_MAUL, 95));
		assertTrue(!tracker.estimated(2, xarpus));
		assertEquals(200, tracker.of(2, xarpus).apply(250, 250, 0, false, 0).defence);
		// an estimate is always counted: "Mark estimated specs" off, the Defence is the same, only the "~" goes
		tracker.addEstimated(2, xarpus, SpecialWeapon.DRAGON_WARHAMMER, 1, false, 106, 4);
		assertEquals(2, tracker.counted(2, xarpus));
		int left = tracker.of(2, xarpus).apply(250, 250, 0, false, 0).defence;
		assertEquals(140, left);
		boolean estimated = tracker.estimated(2, xarpus);
		CombatCheckConfig.Language en = CombatCheckConfig.Language.ENGLISH;
		assertEquals("Def 250 -> 140 (2 specs)",
			CombatCheckPlugin.defenceText(en, 250, left, 2, CombatCheckPlugin.marksEstimate(estimated, false)));
		assertEquals("Def 250 -> ~140 (2 specs)",
			CombatCheckPlugin.defenceText(en, 250, left, 2, CombatCheckPlugin.marksEstimate(estimated, true)));
		// with no estimate in the Defence, nothing is marked whatever the option
		assertTrue(!CombatCheckPlugin.marksEstimate(false, true));
	}
}
