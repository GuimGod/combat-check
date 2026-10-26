/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ShownForTest
{
	@Test
	public void itIsShownForItsTicksAndThenKeepsQuiet()
	{
		ShownFor s = new ShownFor();
		for (int i = 0; i < 9; i++)
		{
			assertTrue("tick " + i, s.visible(true, 9));
		}
		assertFalse(s.visible(true, 9));
		assertFalse(s.visible(true, 9));
	}

	@Test
	public void goingAwayAndComingBackByItselfDoesNotShowItAgain()
	{
		ShownFor s = new ShownFor();
		for (int i = 0; i < 9; i++)
		{
			s.visible(true, 9);
		}
		// the monster died and another was attacked: the mistake is the same one
		assertFalse(s.visible(false, 9));
		assertFalse(s.visible(true, 9));
	}

	@Test
	public void theTicksItWasShownForAreKeptAcrossAGap()
	{
		ShownFor s = new ShownFor();
		assertTrue(s.visible(true, 3));
		assertTrue(s.visible(true, 3));
		assertFalse(s.visible(false, 3));
		assertTrue(s.visible(true, 3));
		assertFalse(s.visible(true, 3));
	}

	@Test
	public void armingItShowsItAgain()
	{
		ShownFor s = new ShownFor();
		for (int i = 0; i < 4; i++)
		{
			s.visible(true, 3);
		}
		assertFalse(s.visible(true, 3));
		s.arm();
		assertTrue(s.visible(true, 3));
	}

	@Test
	public void withNoTicksItStaysForAsLongAsTheMistakeStands()
	{
		ShownFor s = new ShownFor();
		for (int i = 0; i < 500; i++)
		{
			assertTrue(s.visible(true, 0));
		}
		assertFalse(s.visible(false, 0));
	}

	@Test
	public void fiveSecondsAreNineTicks()
	{
		assertEquals(9, ShownFor.ticks(5));
		assertEquals(2, ShownFor.ticks(1));
		assertEquals(0, ShownFor.ticks(0));
	}

	@Test
	public void theWeaponAndTheAmmunitionAreNotArmourAndAShieldIs()
	{
		int[] before = new int[14];
		before[0] = 10828;
		before[ShownFor.WEAPON_SLOT] = 4151;
		before[5] = 12954;
		before[ShownFor.AMMO_SLOT] = 9243;
		int[] weapon = before.clone();
		weapon[ShownFor.WEAPON_SLOT] = 11802;
		assertFalse(ShownFor.armourChanged(before, weapon));
		int[] ammo = before.clone();
		ammo[ShownFor.AMMO_SLOT] = 9242;
		assertFalse(ShownFor.armourChanged(before, ammo));
		int[] shield = before.clone();
		shield[5] = -1;
		assertTrue(ShownFor.armourChanged(before, shield));
		int[] helm = before.clone();
		helm[0] = 11865;
		assertTrue(ShownFor.armourChanged(before, helm));
		// the first look has nothing to compare with
		assertFalse(ShownFor.armourChanged(null, before));
		// an empty slot is an empty slot, whichever way it is written
		int[] empty = before.clone();
		empty[1] = 0;
		int[] alsoEmpty = before.clone();
		alsoEmpty[1] = -1;
		assertFalse(ShownFor.armourChanged(empty, alsoEmpty));
	}

	/**
	 * The Guardians of a Challenge Mode raid (2026-10-17): the Crystal pickaxe wielded in the lobby with the ranged
	 * armour on and "Only while fighting" off. The pieces' 5 seconds ran out before the first Guardian was attacked, and
	 * the whole room showed nothing.
	 */
	@Test
	public void timeShownOutOfAFightDoesNotUseUpTheFights()
	{
		ShownFor fight = new ShownFor();
		ShownFor idle = new ShownFor();
		int ticks = ShownFor.ticks(5);
		// walking to the room with the pickaxe in hand: shown its 9 ticks, then quiet
		for (int i = 0; i < ticks; i++)
		{
			assertTrue(ShownFor.visible(fight, idle, false, true, ticks));
		}
		assertFalse(ShownFor.visible(fight, idle, false, true, ticks));
		// the first Guardian attacked: shown again, its 9 ticks, with the figure
		for (int i = 0; i < ticks; i++)
		{
			assertTrue(ShownFor.visible(fight, idle, true, true, ticks));
		}
		// then quiet for the rest of the room, the next Guardian included, and out of the fight too
		assertFalse(ShownFor.visible(fight, idle, true, true, ticks));
		assertFalse(ShownFor.visible(fight, idle, false, true, ticks));
		// a piece of armour changed arms both
		fight.arm();
		idle.arm();
		assertTrue(ShownFor.visible(fight, idle, true, true, ticks));
		assertTrue(ShownFor.visible(fight, idle, false, true, ticks));
	}
}
