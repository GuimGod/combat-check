/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

/**
 * The pieces get their seconds in a fight again on every new fight, without a change of armour, but not on the next
 * monster of the same room or task ({@link CombatCheckPlugin#newFight}).
 */
public class NewFightTest
{
	private static final int SHOWN = ShownFor.ticks(5);

	/** The pieces' time in a fight used up, as after the first fight of a raid with the armour unchanged. */
	private static ShownFor usedUp()
	{
		ShownFor fight = new ShownFor();
		for (int i = 0; i < SHOWN; i++)
		{
			assertTrue(fight.visible(true, SHOWN));
		}
		assertFalse(fight.visible(true, SHOWN));
		return fight;
	}

	/** A fight starting after none was remembered: armed when it is a new one, as the plugin does. */
	private static boolean shownWhenItStarts(ShownFor fight, int lastTick, WorldPoint lastAt, int tick, WorldPoint here)
	{
		if (CombatCheckPlugin.newFight(lastTick, lastAt, tick, here))
		{
			fight.arm();
		}
		return fight.visible(true, SHOWN);
	}

	@Test
	public void theNextRoomOfARaidIsANewFight()
	{
		// the Guardians fought at one end of the room, the next room's monster 40 tiles on, half a minute later
		ShownFor fight = usedUp();
		assertTrue(shownWhenItStarts(fight, 1000, new WorldPoint(3312, 5313, 1), 1050, new WorldPoint(3312, 5353, 1)));
		// and the floor below, right under: another plane
		ShownFor below = usedUp();
		assertTrue(shownWhenItStarts(below, 1000, new WorldPoint(3312, 5313, 1), 1010, new WorldPoint(3312, 5313, 0)));
	}

	@Test
	public void theOtherGuardianOfTheSameRoomIsNot()
	{
		// the first Guardian forgotten a second after it died; the second one attacked 6 ticks later, two tiles over
		ShownFor fight = usedUp();
		assertFalse(shownWhenItStarts(fight, 1000, new WorldPoint(3312, 5313, 1), 1006, new WorldPoint(3314, 5313, 1)));
	}

	@Test
	public void theNextMonsterOfATaskIsNot()
	{
		// a dead monster of the task stays remembered (by default until let go the other ways; with the option at 10
		// seconds, 17 ticks): the next one attacked within them is not a fight starting after none, so nothing is armed
		int remembered = CombatCheckPlugin.afterDeathTicks(true, 10);
		assertTrue(remembered == 17);
		// and one attacked just after that memory ran out, nearby, is not a new fight either
		assertFalse(CombatCheckPlugin.newFight(1000, new WorldPoint(1600, 9990, 0),
			1000 + remembered + 5, new WorldPoint(1608, 9995, 0)));
	}

	@Test
	public void backToTheFightAfterAMinuteIsANewFight()
	{
		// the monster's minute of memory after the last attack ran out where the fight was: a new fight
		ShownFor fight = usedUp();
		assertTrue(shownWhenItStarts(fight, 1000, new WorldPoint(3000, 3000, 0),
			1000 + CombatCheckPlugin.REMEMBER_TARGET_TICKS + 1, new WorldPoint(3001, 3000, 0)));
		// within the minute, not
		assertFalse(CombatCheckPlugin.newFight(1000, new WorldPoint(3000, 3000, 0),
			1000 + CombatCheckPlugin.REMEMBER_TARGET_TICKS - 1, new WorldPoint(3001, 3000, 0)));
	}

	@Test
	public void theFirstFightIsNew()
	{
		assertTrue(CombatCheckPlugin.newFight(-1, null, 50, new WorldPoint(3000, 3000, 0)));
	}
}
