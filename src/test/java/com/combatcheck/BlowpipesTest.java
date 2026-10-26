/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.Test;

/**
 * The blowpipes of Sailing (Camphor, Ironwood, Rosewood) as the OSRS Wiki DPS calculator counts them, the darts inside
 * them read from their Check message like the Toxic blowpipe's, each blowpipe's darts kept apart (tools/gen_blowpipes.py,
 * blowpipe_sets.tsv).
 */
public class BlowpipesTest
{
	/** Each case of blowpipe_sets.tsv within 0.1% of the calculator: every blowpipe with the darts it can shoot. */
	@Test
	public void figuresAreTheCalculators() throws IOException
	{
		List<SetCalcTest.Case> cases = SetCalcTest.cases("blowpipe_sets.tsv");
		assertEquals(40, cases.size());
		for (SetCalcTest.Case k : cases)
		{
			assertTrue(k.set, k.got > 0);
			assertEquals(k.set + " on " + k.npc, k.expected, k.got, k.expected * 0.001);
		}
	}

	/** Without its darts known a blowpipe has no figure, as before: they are most of its damage. */
	@Test
	public void noFigureWithoutTheDarts() throws IOException
	{
		for (SetCalcTest.Case k : SetCalcTest.cases("blowpipe_sets.tsv"))
		{
			k.loadout.dartKnown = false;
			assertFalse(k.set, SetCalc.plain(k.loadout));
		}
	}

	/** Which blowpipe each name is, and which ones are read (not the empty ones, not the Drygore blowpipe). */
	@Test
	public void theBlowpipesRead()
	{
		assertEquals("", CombatCheckPlugin.blowpipeKind("Toxic blowpipe"));
		assertEquals("", CombatCheckPlugin.blowpipeKind("Blazing blowpipe"));
		assertEquals("camphor", CombatCheckPlugin.blowpipeKind("Camphor blowpipe"));
		assertEquals("ironwood", CombatCheckPlugin.blowpipeKind("Ironwood blowpipe (empty)"));
		assertEquals("rosewood", CombatCheckPlugin.blowpipeKind("Rosewood blowpipe"));
		assertNull(CombatCheckPlugin.blowpipeKind("Drygore blowpipe"));
		assertTrue(CombatCheckPlugin.readsDarts("Camphor blowpipe"));
		assertTrue(CombatCheckPlugin.readsDarts("Ironwood blowpipe"));
		assertTrue(CombatCheckPlugin.readsDarts("Rosewood blowpipe"));
		assertFalse(CombatCheckPlugin.readsDarts("Rosewood blowpipe (empty)"));
		assertFalse(CombatCheckPlugin.readsDarts("Drygore blowpipe"));
		assertFalse(CombatCheckPlugin.readsDarts(null));
	}

	/**
	 * The Check message: the Toxic blowpipe's names its scales and is the Toxic's; one without scales (the blowpipes of
	 * Sailing take none: its exact words not seen yet) is the blowpipe of Sailing last checked, and with none checked it is
	 * not read.
	 */
	@Test
	public void theCheckMessage()
	{
		assertEquals(810, CombatCheckPlugin.dartFromCheck("Darts: Adamant dart x 500."));
		assertEquals(811, CombatCheckPlugin.dartFromCheck("Darts: Rune dart x 2,000."));
		assertEquals(0, CombatCheckPlugin.dartFromCheck("Darts: None."));
		assertEquals(11230, CombatCheckPlugin.dartFromCheck("Darts: Dragon dart x 1,234. Scales: 16,383 (100.0%)."));
		assertEquals("", CombatCheckPlugin.checkedKind("Darts: Dragon dart x 1,234. Scales: 16,383 (100.0%).", "camphor"));
		assertEquals("camphor", CombatCheckPlugin.checkedKind("Darts: Adamant dart x 500.", "camphor"));
		assertNull(CombatCheckPlugin.checkedKind("Darts: Adamant dart x 500.", null));
		assertNull(CombatCheckPlugin.checkedKind("Darts: Adamant dart x 500.", ""));
	}
}
