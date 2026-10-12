/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class IndicesTest
{
	@Test
	public void fourStylesKeepTheirOrder()
	{
		assertArrayEquals(new int[]{0, 1, 2, 3},
			CombatCheckPlugin.indices(new String[]{"Accurate", "Aggressive", "Aggressive", "Defensive"}, 4));
	}

	@Test
	public void aWhipsThirdStyleIsTheFourthButton()
	{
		assertArrayEquals(new int[]{0, 1, 3},
			CombatCheckPlugin.indices(new String[]{"Accurate", "Controlled", "Other", "Defensive"}, 3));
	}

	@Test
	public void aStaffsMeleeStylesSkipTheGap()
	{
		assertArrayEquals(new int[]{0, 1, 3, 4, 5},
			CombatCheckPlugin.indices(new String[]{"Accurate", "Aggressive", "Other", "Defensive", "Casting", "Defensive"}, 5));
	}

	@Test
	public void withoutTheClientsListTheOrderIsKept()
	{
		assertArrayEquals(new int[]{0, 1, 2}, CombatCheckPlugin.indices(null, 3));
		assertArrayEquals(new int[]{0, 1, 2, 3}, CombatCheckPlugin.indices(new String[]{"Accurate", "Other"}, 4));
	}

	@Test
	public void setAndWeaponNames()
	{
		assertEquals(1, CombatCheckPlugin.inquisitorWeight("Inquisitor's great helm"));
		assertEquals(2, CombatCheckPlugin.inquisitorWeight("Inquisitor's hauberk"));
		assertEquals(0, CombatCheckPlugin.inquisitorWeight("Inquisitor's mace"));
		assertTrue(CombatCheckPlugin.isCorpbane("Zamorakian spear"));
		assertTrue(CombatCheckPlugin.isCorpbane("Osmumten's fang (or)"));
		assertFalse(CombatCheckPlugin.isCorpbane("Blue moon spear"));
		assertFalse(CombatCheckPlugin.isCorpbane("Scythe of vitur"));
	}

	@Test
	public void theTablesLoad() throws Exception
	{
		StyleData data = new StyleData();
		assertEquals("Jab", data.styles(22325).get(2).name); // Scythe of vitur
		assertEquals("crush", data.styles(22325).get(2).type);
		assertEquals(3, data.styles(4151).size()); // Abyssal whip
		assertTrue(data.styles(-1).size() >= 3); // unarmed
		assertTrue(data.monster(8058) != null); // Vorkath
	}
}
