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

	@Test
	public void offByDefault()
	{
		assertFalse(new CombatCheckConfig()
		{
		}.showDefenceAbove());
	}
}
