/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The "Defence left" line: the monster's Defence before the special attacks counted on it and the one the figures use
 * now, with how many were counted; not where the Defence cannot be lowered, nor with none counted.
 */
public class DefenceLineTest
{
	@Test
	public void theLine()
	{
		CombatCheckConfig.Language en = CombatCheckConfig.Language.ENGLISH;
		assertEquals("Def 246 -> 158 (3 specs)", CombatCheckPlugin.defenceText(en, 246, 158, 3, false));
		assertEquals("Def 246 -> ~158 (3 specs)", CombatCheckPlugin.defenceText(en, 246, 158, 3, true));
		assertEquals("Def 250 -> 162 (1 spec)", CombatCheckPlugin.defenceText(en, 250, 162, 1, false));
		// every one missed: the Defence is whole, and the line says how many there were
		assertEquals("Def 200 -> 200 (2 specs)", CombatCheckPlugin.defenceText(en, 200, 200, 2, false));
		assertNull(CombatCheckPlugin.defenceText(en, 250, 250, 0, false));
		assertEquals("Def 246 -> 158 (3 specs)",
			CombatCheckPlugin.defenceText(CombatCheckConfig.Language.PORTUGUESE, 246, 158, 3, false));
		assertEquals("Vert 246 -> 158 (3 Specs)",
			CombatCheckPlugin.defenceText(CombatCheckConfig.Language.GERMAN, 246, 158, 3, false));
	}

	@Test
	public void whenItIsShown()
	{
		// Xarpus (8340) after a maul: shown
		assertTrue(CombatCheckPlugin.showsDefence(false, DefenceDrains.floor(8340, 250), 250, 1));
		// none counted yet
		assertFalse(CombatCheckPlugin.showsDefence(false, DefenceDrains.floor(8340, 250), 250, 0));
		// Verzik's second phase (8372) and Vardorvis (12223): their Defence cannot be lowered, whatever is counted
		assertFalse(CombatCheckPlugin.showsDefence(false, DefenceDrains.floor(8372, 200), 200, 2));
		assertFalse(CombatCheckPlugin.showsDefence(false, DefenceDrains.floor(12223, 215), 215, 2));
		// a fight immune to drains
		assertFalse(CombatCheckPlugin.showsDefence(true, 0, 200, 2));
	}

	/** The specs lower levels, not bonuses: melee and ranged roll against Defence, magic against Magic (or Defence: "D"). */
	@Test
	public void theLevelOfTheWeaponInHand()
	{
		CombatCheckConfig.Language en = CombatCheckConfig.Language.ENGLISH;
		DefenceDrains.Levels full = new DefenceDrains.Levels();
		full.defence = 250;
		full.magic = 220;
		DefenceDrains.Levels mauled = full.copy();
		mauled.defence = 163;
		// Xarpus after a maul: the Defence for melee and ranged; nothing for a staff, his Magic is whole
		assertEquals("Def 250 -> 163 (1 spec)",
			CombatCheckPlugin.defenceTextFor(en, StyleCalc.MELEE, false, full, mauled, 1, false));
		assertEquals("Def 250 -> 163 (1 spec)",
			CombatCheckPlugin.defenceTextFor(en, StyleCalc.RANGED, false, full, mauled, 1, false));
		assertNull(CombatCheckPlugin.defenceTextFor(en, StyleCalc.MAGIC, false, full, mauled, 1, false));
		// a monster whose magic defence is rolled with Defence (the Ice demon): the Defence line for magic too
		assertEquals("Def 250 -> 163 (1 spec)",
			CombatCheckPlugin.defenceTextFor(en, StyleCalc.MAGIC, true, full, mauled, 1, false));
		// a godsword whose damage went past the Defence down to Magic: the Magic level for a staff
		DefenceDrains.Levels godsword = mauled.copy();
		godsword.defence = 0;
		godsword.magic = 200;
		assertEquals("Magic 220 -> ~200 (2 specs)",
			CombatCheckPlugin.defenceTextFor(en, StyleCalc.MAGIC, false, full, godsword, 2, true));
		// the table's rule "D" is what SetCalc rolls magic defence with: the Ice demon (7584) has it, Xarpus (8340) not
		StyleData data;
		try
		{
			data = new StyleData();
		}
		catch (java.io.IOException e)
		{
			throw new AssertionError(e);
		}
		assertTrue(data.monster(7584).magicDefenceFromDefence);
		assertFalse(data.monster(8340).magicDefenceFromDefence);
	}
}
