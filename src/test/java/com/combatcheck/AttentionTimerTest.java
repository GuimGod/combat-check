/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AttentionTimerTest
{
	@Test
	public void withNoDelayTheMistakeIsShownAtOnce()
	{
		assertTrue(new AttentionTimer().due(100, true, 0));
	}

	@Test
	public void theMistakeMustStandForTheWholeDelay()
	{
		AttentionTimer t = new AttentionTimer();
		assertFalse(t.due(100, true, 3));
		assertFalse(t.due(101, true, 3));
		assertFalse(t.due(102, true, 3));
		assertTrue(t.due(103, true, 3));
		assertTrue(t.due(104, true, 3));
	}

	@Test
	public void aMistakeFixedInTimeIsNeverShown()
	{
		AttentionTimer t = new AttentionTimer();
		assertFalse(t.due(100, true, 3));
		assertFalse(t.due(101, true, 3));
		assertFalse(t.due(102, false, 3)); // fixed
		assertFalse(t.due(103, false, 3));
	}

	@Test
	public void aNewMistakeStartsTheWaitAgain()
	{
		AttentionTimer t = new AttentionTimer();
		t.due(100, true, 2);
		t.due(101, false, 2);
		assertFalse(t.due(102, true, 2));
		assertFalse(t.due(103, true, 2));
		assertTrue(t.due(104, true, 2));
	}

	@Test
	public void nothingIsDueWithoutAMistake()
	{
		AttentionTimer t = new AttentionTimer();
		for (int tick = 0; tick < 50; tick++)
		{
			assertFalse(t.due(tick, false, 0));
		}
	}

	@Test
	public void aNegativeDelayCountsAsNone()
	{
		assertTrue(new AttentionTimer().due(5, true, -4));
	}

	@Test
	public void theStyleWaitCanBeKeptForBosses()
	{
		org.junit.Assert.assertEquals(5, CombatCheckPlugin.styleWait(5, true, 318, 200));
		org.junit.Assert.assertEquals(0, CombatCheckPlugin.styleWait(5, true, 122, 200));
		org.junit.Assert.assertEquals(5, CombatCheckPlugin.styleWait(5, false, 122, 200));
		org.junit.Assert.assertEquals(0, CombatCheckPlugin.styleWait(0, true, 318, 200));
	}

	@Test
	public void theAttackPrayerIsCheckedAgainstTheWeapon()
	{
		CombatCheckConfig.PrayerReminder both = CombatCheckConfig.PrayerReminder.BOTH;
		CombatCheckConfig.PrayerReminder noneOn = CombatCheckConfig.PrayerReminder.NONE_ON;
		CombatCheckConfig.PrayerReminder wrong = CombatCheckConfig.PrayerReminder.WRONG;
		org.junit.Assert.assertNull(CombatCheckPlugin.prayerNotice("melee", "Piety", null, null, "Piety", both));
		org.junit.Assert.assertEquals("Wrong prayer: Piety",
			CombatCheckPlugin.prayerNotice("ranged", "Piety", null, null, "Rigour", both));
		org.junit.Assert.assertEquals("Wrong prayer: Rigour",
			CombatCheckPlugin.prayerNotice("magic", null, "Rigour", null, "Augury", wrong));
		org.junit.Assert.assertNull(CombatCheckPlugin.prayerNotice("magic", null, "Rigour", null, "Augury", noneOn));
		org.junit.Assert.assertEquals("Piety off", CombatCheckPlugin.prayerNotice("melee", null, null, null, "Piety", both));
		org.junit.Assert.assertEquals("Piety off", CombatCheckPlugin.prayerNotice("melee", null, null, null, "Piety", noneOn));
		org.junit.Assert.assertNull(CombatCheckPlugin.prayerNotice("melee", null, null, null, "Piety", wrong));
		org.junit.Assert.assertNull(CombatCheckPlugin.prayerNotice("melee", null, null, null, "Piety",
			CombatCheckConfig.PrayerReminder.OFF));
		org.junit.Assert.assertNull(CombatCheckPlugin.prayerNotice(null, "Piety", null, null, "Piety", both));
		// the right one on together with another: nothing to say
		org.junit.Assert.assertNull(CombatCheckPlugin.prayerNotice("ranged", "Piety", "Rigour", null, "Rigour", both));
	}

	@Test
	public void aFightWithAPlayerSilencesThePluginForAMinute()
	{
		org.junit.Assert.assertFalse(CombatCheckPlugin.stillFightingPlayer(500, -1));
		org.junit.Assert.assertTrue(CombatCheckPlugin.stillFightingPlayer(500, 500));
		org.junit.Assert.assertTrue(CombatCheckPlugin.stillFightingPlayer(600, 500));
		org.junit.Assert.assertFalse(CombatCheckPlugin.stillFightingPlayer(601, 500));
	}

	@Test
	public void aPrayerLeftOnIsPointedOutOnlyAfterTheFight()
	{
		org.junit.Assert.assertFalse(CombatCheckPlugin.leftOn(true, 0, 17));
		org.junit.Assert.assertFalse(CombatCheckPlugin.leftOn(true, 16, 17));
		org.junit.Assert.assertTrue(CombatCheckPlugin.leftOn(true, 17, 17));
		org.junit.Assert.assertFalse(CombatCheckPlugin.leftOn(false, 500, 17));
	}

	@Test
	public void theSlayerHelmetIsOfferedOnlyWhenItDealsMore()
	{
		org.junit.Assert.assertFalse(CombatCheckPlugin.helmetIsUseless(5.0, 5.8));
		org.junit.Assert.assertTrue(CombatCheckPlugin.helmetIsUseless(5.0, 5.0));
		org.junit.Assert.assertTrue(CombatCheckPlugin.helmetIsUseless(5.0, 4.9));
		// a figure that cannot be worked out decides nothing
		org.junit.Assert.assertFalse(CombatCheckPlugin.helmetIsUseless(-1, 4.9));
		org.junit.Assert.assertFalse(CombatCheckPlugin.helmetIsUseless(5.0, -1));
	}
}
