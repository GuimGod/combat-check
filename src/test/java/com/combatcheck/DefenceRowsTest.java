/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/**
 * The rows under the "Defence left" line, one per weapon that lowered it ("(N specs)" said neither which weapon nor
 * whether a Tonalztics of Ralos count was a special attack or one of its hits): "Ralos 1 spec (2hits)", "Maul x1", each
 * with the weapon's icon.
 */
public class DefenceRowsTest
{
	private static final CombatCheckConfig.Language EN = CombatCheckConfig.Language.ENGLISH;

	private static List<String> texts(List<DefenceLine.Row> rows)
	{
		List<String> out = new ArrayList<>();
		for (DefenceLine.Row r : rows)
		{
			out.add(r.text);
		}
		return out;
	}

	/** One charged Ralos special attack whose two hits landed is one special attack with 2 hits; a maul after it, x1. */
	@Test
	public void oneRalosSpecIsOneWithItsHits()
	{
		DrainTracker d = new DrainTracker();
		Object npc = new Object();
		d.add(7, npc, SpecialWeapon.TONALZTICS_OF_RALOS, 2, false, 100);
		d.add(7, npc, SpecialWeapon.ELDER_MAUL, 1, false, 106);
		List<DefenceLine.Row> rows = DefenceLine.rows(EN, d.byWeapon(7, npc), false);
		assertEquals(java.util.Arrays.asList("Ralos 1 spec (2hits)", "Maul x1"), texts(rows));
		assertEquals(ItemID.TONALZTICS_OF_RALOS_CHARGED, rows.get(0).itemId);
		assertEquals(ItemID.ELDER_MAUL, rows.get(1).itemId);
		// the count of special attacks is the same: two, not three
		assertEquals(2, d.counted(7, npc));
	}

	/**
	 * Two players' Ralos special attacks on the same tick (his own, counted from his hitsplats, and a party member's message)
	 * are two special attacks, their hits added: 2 and 1.
	 */
	@Test
	public void twoRalosSpecsOnOneTickAreTwo()
	{
		DrainTracker d = new DrainTracker();
		Object npc = new Object();
		d.add(7, npc, SpecialWeapon.TONALZTICS_OF_RALOS, 2, false, 200);
		d.add(7, npc, SpecialWeapon.TONALZTICS_OF_RALOS, 1, false, 200);
		assertEquals(java.util.Arrays.asList("Ralos 2 specs (3hits)"), texts(DefenceLine.rows(EN, d.byWeapon(7, npc), false)));
		assertEquals(2, d.counted(7, npc));
	}

	/** One hit, none, a miss of another weapon, an estimate marked, Dinh's bulwark left out, another language. */
	@Test
	public void theOtherCases()
	{
		DrainTracker d = new DrainTracker();
		Object npc = new Object();
		d.add(7, npc, SpecialWeapon.TONALZTICS_OF_RALOS, 1, false, 10);
		assertEquals(java.util.Arrays.asList("Ralos 1 spec (1hit)"), texts(DefenceLine.rows(EN, d.byWeapon(7, npc), false)));
		d.add(7, npc, SpecialWeapon.TONALZTICS_OF_RALOS, 0, false, 20);
		d.add(7, npc, SpecialWeapon.DRAGON_WARHAMMER, 0, false, 30);
		d.add(7, npc, SpecialWeapon.BULWARK, 40, false, 40);
		d.addEstimated(7, npc, SpecialWeapon.DRAGON_WARHAMMER, 1, false, 50, 3);
		assertEquals(java.util.Arrays.asList("Ralos 2 specs (1hit)", "DWH ~x2"), texts(DefenceLine.rows(EN, d.byWeapon(7, npc), true)));
		assertEquals(java.util.Arrays.asList("Ralos 2 specs (1hit)", "DWH x2"), texts(DefenceLine.rows(EN, d.byWeapon(7, npc), false)));
		assertEquals(java.util.Arrays.asList("Ralos 2 specs (1hit)", "DWH x2"),
			texts(DefenceLine.rows(CombatCheckConfig.Language.PORTUGUESE, d.byWeapon(7, npc), false)));
		// the uncharged one's single hit too
		DrainTracker u = new DrainTracker();
		u.add(1, npc, SpecialWeapon.TONALZTICS_OF_RALOS, 1, false, 5);
		u.add(1, npc, SpecialWeapon.TONALZTICS_OF_RALOS, 1, false, 15);
		assertEquals(java.util.Arrays.asList("Ralos 2 specs (2hits)"), texts(DefenceLine.rows(EN, u.byWeapon(1, npc), false)));
	}

	/** A change that gives the Defence back (Ocellus's next phase, the Bloat's stomp) drops the rows with the drains. */
	@Test
	public void aResetDropsTheRows()
	{
		DrainTracker d = new DrainTracker();
		Object npc = new Object();
		d.add(7, npc, SpecialWeapon.TONALZTICS_OF_RALOS, 2, false, 10);
		d.forget(7, npc);
		assertTrue(d.byWeapon(7, npc).isEmpty());
		d.add(7, npc, SpecialWeapon.ELDER_MAUL, 1, false, 20);
		assertEquals(java.util.Arrays.asList("Maul x1"), texts(DefenceLine.rows(EN, d.byWeapon(7, npc), false)));
	}
}
