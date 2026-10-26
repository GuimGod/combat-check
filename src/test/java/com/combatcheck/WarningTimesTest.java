/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Every reminder stays by default for as long as what it reminds of is true (anyone who wants it shorter lowers it in
 * the settings), and goes when it is not.
 */
public class WarningTimesTest
{
	@Test
	public void theDefaultsAreForAsLongAsItStands()
	{
		CombatCheckConfig config = new CombatCheckConfig()
		{
		};
		assertTrue(config.prayerKeep());
		assertTrue(config.switchKeep());
		assertEquals(0, config.prayerAfterAttack());
		assertEquals(0, config.boltsAfterAttack());
		assertEquals(0, config.afterKillOnTask());
		// the times still there for whoever turns the keeping off
		assertEquals(5, config.prayerShowFor());
		assertEquals(5, config.switchShowFor());
	}

	/** 0 is no limit; seconds are game ticks, as before (5 seconds the prayer's, 15 the bolts', 10 a kill on task). */
	@Test
	public void secondsAsTicks()
	{
		assertEquals(Integer.MAX_VALUE, CombatCheckPlugin.afterAttackTicks(0));
		assertEquals(9, CombatCheckPlugin.afterAttackTicks(5));
		assertEquals(25, CombatCheckPlugin.afterAttackTicks(15));
		assertEquals(17, CombatCheckPlugin.afterDeathTicks(true, 10));
		assertEquals(Integer.MAX_VALUE, CombatCheckPlugin.afterDeathTicks(true, 0));
		// off a task a dead monster is let go at once, whatever the option
		assertEquals(CombatCheckPlugin.AFTER_DEATH_TICKS, CombatCheckPlugin.afterDeathTicks(false, 0));
		assertEquals(CombatCheckPlugin.AFTER_DEATH_TICKS, CombatCheckPlugin.afterDeathTicks(null, 0));
	}

	/**
	 * Kept on screen, a reminder is there every tick its mistake stands, and gone the tick it does not: nothing is left on
	 * screen after it is put right; and it comes back with the mistake.
	 */
	@Test
	public void keptIsNeverLeftBehind()
	{
		ShownFor kept = new ShownFor();
		for (int tick = 0; tick < 500; tick++)
		{
			assertTrue(kept.visible(true, 0));
		}
		assertFalse(kept.visible(false, 0));
		assertTrue(kept.visible(true, 0));
		// with a time, it keeps quiet after it, as before
		ShownFor timed = new ShownFor();
		int shown = 0;
		for (int tick = 0; tick < 50; tick++)
		{
			shown += timed.visible(true, ShownFor.ticks(5)) ? 1 : 0;
		}
		assertEquals(9, shown);
	}

	/**
	 * With no limit after the last attack, the monster still lets every reminder go: a minute without an attack (the
	 * plugin's REMEMBER_TARGET_TICKS bounds the ticks since the last attack), and a new fight then gives them their time again.
	 */
	@Test
	public void theFightEndingLetsThemGo()
	{
		assertTrue(CombatCheckPlugin.REMEMBER_TARGET_TICKS < CombatCheckPlugin.afterAttackTicks(0));
		assertTrue(CombatCheckPlugin.newFight(1000, new net.runelite.api.coords.WorldPoint(3000, 3000, 0),
			1000 + CombatCheckPlugin.REMEMBER_TARGET_TICKS, new net.runelite.api.coords.WorldPoint(3000, 3000, 0)));
	}
}
