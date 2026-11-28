/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/**
 * The Defence drawn above each monster whose Defence was lowered ("Defence above monsters"): its words, and
 * which monsters get it.
 */
public class DefenceAboveTest
{
	private static final CombatCheckConfig.Language EN = CombatCheckConfig.Language.ENGLISH;

	private static List<DefenceLine.Row> rows(DrainTracker drains, int index, Object npc, boolean markEstimate)
	{
		return DefenceLine.rows(EN, drains.byWeapon(index, npc), markEstimate);
	}

	/** How much more the Defence can be lowered: its Defence now less its floor ("Def 0" at the floor). */
	@Test
	public void theDefenceLine()
	{
		// no floor: the Defence now
		assertEquals("Def 154", CombatCheckPlugin.aboveHead(EN, false, 154, 0, false));
		// Defence 250 that goes no lower than 150, lowered to 150
		assertEquals("Def 0", CombatCheckPlugin.aboveHead(EN, false, 150, 150, false));
		assertEquals("Def 30", CombatCheckPlugin.aboveHead(EN, false, 180, 150, false));
		// a Fractured Archive boss with Defence 250: floored at 60% of it, 150
		assertEquals("Def 25", CombatCheckPlugin.aboveHead(EN, false, 175, DefenceDrains.floor(16659, 250), false));
		// never below 0
		assertEquals("Def 0", CombatCheckPlugin.aboveHead(EN, false, 140, 150, false));
		// an estimate among them, with "Mark estimated specs" on
		assertEquals("Def ~54", CombatCheckPlugin.aboveHead(EN, false, 204, 150, true));
		// Ezekiel after "Saradomin, give me strength."
		assertEquals("Def ?", CombatCheckPlugin.aboveHead(EN, true, 0, 120, false));
		// the languages
		assertEquals("Vert 54", CombatCheckPlugin.aboveHead(CombatCheckConfig.Language.GERMAN, false, 54, 0, false));
		assertEquals("Déf 54", CombatCheckPlugin.aboveHead(CombatCheckConfig.Language.FRENCH, false, 54, 0, false));
	}

	/** Under it, a row per weapon with its icon, the same rows as the line of the monster fought. */
	@Test
	public void theWeaponRows()
	{
		DrainTracker drains = new DrainTracker();
		Object npc = new Object();
		drains.add(1, npc, SpecialWeapon.BANDOS_GODSWORD, 40, false, 10);
		drains.add(1, npc, SpecialWeapon.DRAGON_WARHAMMER, 30, false, 12);
		List<DefenceLine.Row> rows = rows(drains, 1, npc, true);
		assertEquals(2, rows.size());
		assertEquals("BGS x1", rows.get(0).text);
		assertEquals("DWH x1", rows.get(1).text);
		assertEquals(DefenceLine.icon(SpecialWeapon.BANDOS_GODSWORD), rows.get(0).itemId);
		// a miss and the Ralos's hits
		DrainTracker more = new DrainTracker();
		Object other = new Object();
		more.add(2, other, SpecialWeapon.ELDER_MAUL, 0, false, 5);
		more.add(2, other, SpecialWeapon.ELDER_MAUL, 0, false, 10);
		more.add(2, other, SpecialWeapon.TONALZTICS_OF_RALOS, 2, false, 15);
		List<DefenceLine.Row> two = rows(more, 2, other, true);
		assertEquals("Maul 2 specs (0hits)", two.get(0).text);
		assertEquals("Ralos 1 spec (2hits)", two.get(1).text);
	}

	@Test
	public void whichMonsters()
	{
		// Xarpus (8340) after a maul: drawn, attacked by the player or not
		assertTrue(CombatCheckPlugin.showsAbove(false, false, false, DefenceDrains.floor(8340, 250), 250, 1));
		// nothing counted on it
		assertFalse(CombatCheckPlugin.showsAbove(false, false, false, DefenceDrains.floor(8340, 250), 250, 0));
		// dying: it goes
		assertFalse(CombatCheckPlugin.showsAbove(true, false, false, DefenceDrains.floor(8340, 250), 250, 1));
		// Verzik's second phase (8372) and Vardorvis (12223): their Defence cannot be lowered
		assertFalse(CombatCheckPlugin.showsAbove(false, false, false, DefenceDrains.floor(8372, 200), 200, 2));
		assertFalse(CombatCheckPlugin.showsAbove(false, false, false, DefenceDrains.floor(12223, 215), 215, 2));
		// a fight immune to drains
		assertFalse(CombatCheckPlugin.showsAbove(false, false, true, 0, 200, 2));
		// a Defence not known (Ezekiel after his call): "Def ?"
		assertTrue(CombatCheckPlugin.showsAbove(false, true, false, 120, 200, 1));
	}

	/** Each monster keeps its own drains: the one hit gets the line, the one next to it nothing; gone, nothing. */
	@Test
	public void eachMonsterItsOwn()
	{
		DrainTracker drains = new DrainTracker();
		Object hit = new Object();
		Object next = new Object();
		drains.add(1, hit, SpecialWeapon.DRAGON_WARHAMMER, 30, false, 10);
		assertEquals(1, drains.counted(1, hit));
		assertEquals(0, drains.counted(2, next));
		// another monster on the same index later (the game reuses them): nothing of the first
		assertEquals(0, drains.counted(1, next));
		drains.forget(1, hit);
		assertEquals(0, drains.counted(1, hit));
	}

	/**
	 * Each "Defence position" keeps every line off the health bar (the bar's pixels and a second bar's over it), on a big
	 * boss, on a small monster and on one so far off that its feet are by its bar, with 0 to 4 weapon rows.
	 */
	@Test
	public void noPositionTouchesTheBar()
	{
		// {bar's point, ground's point} on screen
		int[][] monsters = {{100, 420}, {300, 330}, {300, 304}};
		for (CombatCheckConfig.DefencePosition position : CombatCheckConfig.DefencePosition.values())
		{
			for (int[] m : monsters)
			{
				for (int ascent = 9; ascent <= 13; ascent++)
				{
					for (int rows = 0; rows <= 4; rows++)
					{
						int first = DefenceAboveOverlay.firstBaseline(position, m[0], m[1], ascent, 3, rows);
						for (int i = 0; i <= rows; i++)
						{
							int baseline = first + i * DefenceAboveOverlay.LINE_H;
							// as the line is drawn: the words (their shadow 1 px under) and the icon beside them
							int icon = DefenceAboveOverlay.iconTop(baseline, ascent);
							int top = Math.min(baseline - ascent, icon);
							int bottom = Math.max(baseline + 3 + 1, icon + DefenceAboveOverlay.ICON_H);
							String where = position + " bar " + m[0] + " feet " + m[1] + " ascent " + ascent + " line " + i;
							assertTrue(where, bottom < m[0] - DefenceAboveOverlay.BAR_UP || top > m[0] + DefenceAboveOverlay.BAR_DOWN);
							if (position == CombatCheckConfig.DefencePosition.ABOVE_BAR)
							{
								assertTrue(where, bottom < m[0] - DefenceAboveOverlay.BAR_UP);
							}
							else
							{
								assertTrue(where, top > m[0] + DefenceAboveOverlay.BAR_DOWN);
							}
						}
					}
				}
			}
		}
		// at the feet: the first line starts at the near edge of its tiles when there is room
		int first = DefenceAboveOverlay.firstBaseline(CombatCheckConfig.DefencePosition.FEET, 100, 420, 11, 3, 2);
		assertEquals(420, first - DefenceAboveOverlay.lineUp(11));
		// under the bar: the first line starts 3 px under it
		first = DefenceAboveOverlay.firstBaseline(CombatCheckConfig.DefencePosition.UNDER_BAR, 100, 420, 11, 3, 2);
		assertEquals(100 + DefenceAboveOverlay.BAR_DOWN + DefenceAboveOverlay.CLEAR,
			first - DefenceAboveOverlay.lineUp(11));
	}

	@Test
	public void underTheBarByDefault()
	{
		assertEquals(CombatCheckConfig.DefencePosition.UNDER_BAR, new CombatCheckConfig()
		{
		}.defencePosition());
		assertEquals("Above the health bar", CombatCheckConfig.DefencePosition.ABOVE_BAR.toString());
		assertEquals("Under the health bar", CombatCheckConfig.DefencePosition.UNDER_BAR.toString());
		assertEquals("At its feet", CombatCheckConfig.DefencePosition.FEET.toString());
	}

	@Test
	public void onByDefault()
	{
		assertTrue(new CombatCheckConfig()
		{
		}.showDefenceAbove());
	}
}
