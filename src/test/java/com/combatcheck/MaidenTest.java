/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * A Maiden of 2026-10-19 (tools/gen_maiden.py, maiden_sets.tsv): the Theatre of Blood in normal mode, a party of 3,
 * Defence 200 down to 19 (two Elder maul specials and a Bandos godsword's 66), the Scythe of vitur with the whole melee
 * switch against the Twisted bow with the Elite void, Dizana's max cape and Seeking dragon arrows. The plugin pointed
 * at the bow (+1.1%) where the calculator, run with a full super combat, put the Scythe 1% ahead: the player's Attack
 * and Strength were 117 then, not 118 (the Combat Logger), and at 117 the calculator puts the bow ahead too. Seeking
 * arrows' least hit of 3 was missing from the plugin (the bow 0.13% short).
 */
public class MaidenTest
{
	private static Map<String, SetCalcTest.Case> cases() throws IOException
	{
		List<SetCalcTest.Case> all = SetCalcTest.figuresOf("maiden_sets.tsv");
		Map<String, SetCalcTest.Case> by = new HashMap<>();
		for (SetCalcTest.Case k : all)
		{
			by.put(k.set + "@" + k.npc, k);
		}
		return by;
	}

	@Test
	public void everyFigureIsTheCalculators() throws IOException
	{
		assertEquals(23, SetCalcTest.figuresOf("maiden_sets.tsv").size());
	}

	@Test
	public void theDrainedDefenceIs19() throws IOException
	{
		assertEquals(19, cases().get("maiden-tbow-void@8360").target.defenceLevel);
	}

	/** At 118 the Scythe is the better one, at 117 the bow, the gain each way within 0.1 of the calculator's. */
	@Test
	public void theBowIsBetterOnlyWithTheSuperCombatDown() throws IOException
	{
		Map<String, SetCalcTest.Case> c = cases();
		SetCalcTest.Case bow = c.get("maiden-tbow-void@8360");
		SetCalcTest.Case full = c.get("maiden-scythe-melee@8360");
		SetCalcTest.Case down = c.get("maiden-scythe-melee-117@8360");
		double atFull = SetCalc.gain(full.got, bow.got);
		double atDown = SetCalc.gain(down.got, bow.got);
		assertEquals((bow.expected / full.expected - 1) * 100, atFull, 0.1);
		assertEquals((bow.expected / down.expected - 1) * 100, atDown, 0.1);
		assertTrue("the Scythe ahead at 118: " + atFull, atFull < -0.5);
		assertTrue("the bow ahead at 117: " + atDown, atDown > 1);
		// and so the time it takes to kill, which the notice uses
		double ticksBow = SetCalc.ticksToKill(bow.loadout, bow.player, bow.target);
		assertTrue(CombatCheckPlugin.weaponGain(atFull, SetCalc.ticksToKill(full.loadout, full.player, full.target), ticksBow) < 0);
		assertTrue(CombatCheckPlugin.weaponGain(atDown, SetCalc.ticksToKill(down.loadout, down.player, down.target), ticksBow) >= 1);
	}

	/**
	 * Defence 91 (an Elder maul special and a Bandos godsword's 39): per second the bow is 0.09% ahead (13.39 against
	 * 13.37, the calculator), but the notice compares the time to kill, and there the Scythe is 0.09% sooner: the
	 * calculator's own mean attacks to kill, 66.009 Scythe attacks against 66.068 bow attacks, 330.05 against 330.34
	 * ticks. With a minimum gain of 0 the bow in hand points at the Scythe, "+0.1%", and the Scythe in hand at nothing;
	 * with the default 1%, or 0.5, nothing either way.
	 */
	@Test
	public void atDefence91TheScytheKillsATenthSooner() throws IOException
	{
		Map<String, SetCalcTest.Case> c = cases();
		SetCalcTest.Case bow = c.get("maiden-tbow-void-def91@8360");
		SetCalcTest.Case scythe = c.get("maiden-scythe-melee-def91@8360");
		assertEquals(91, bow.target.defenceLevel);
		double perTick = SetCalc.gain(scythe.got, bow.got);
		assertEquals((bow.expected / scythe.expected - 1) * 100, perTick, 0.1);
		assertTrue("the bow ahead per tick: " + perTick, perTick > 0);
		double ticksScythe = SetCalc.ticksToKill(scythe.loadout, scythe.player, scythe.target);
		double ticksBow = SetCalc.ticksToKill(bow.loadout, bow.player, bow.target);
		// the calculator's: 330.05 and 330.34 ticks
		assertEquals(330.05, ticksScythe, 0.33);
		assertEquals(330.34, ticksBow, 0.33);
		double scytheInHand = CombatCheckPlugin.weaponGain(perTick, ticksScythe, ticksBow);
		double bowInHand = CombatCheckPlugin.weaponGain(-perTick, ticksBow, ticksScythe);
		assertTrue("Scythe in hand: " + scytheInHand, !CombatCheckPlugin.pointsAt(scytheInHand, CombatCheckPlugin.minimumGain(0)));
		assertTrue("bow in hand: " + bowInHand, CombatCheckPlugin.pointsAt(bowInHand, CombatCheckPlugin.minimumGain(0)));
		assertEquals("+0.1%", CombatCheckPlugin.gainText(bowInHand));
		assertTrue(!CombatCheckPlugin.pointsAt(bowInHand, CombatCheckPlugin.minimumGain(0.5)));
		assertTrue(!CombatCheckPlugin.pointsAt(bowInHand, CombatCheckPlugin.minimumGain(1)));
	}

	/**
	 * The same Defence 91 with the default "Compare weapons by", the damage per second: the bow with the Elite void
	 * deals 13.39 a second, the Scythe 13.37 (the calculator), so with the Scythe in hand and a minimum gain of 0 the
	 * notice reads "+0.1%" for the bow, and with the bow in hand nothing; with the default 1% nothing either way.
	 */
	@Test
	public void atDefence91TheBowDealsATenthMorePerSecond() throws IOException
	{
		Map<String, SetCalcTest.Case> c = cases();
		SetCalcTest.Case bow = c.get("maiden-tbow-void-def91@8360");
		SetCalcTest.Case scythe = c.get("maiden-scythe-melee-def91@8360");
		CombatCheckConfig.WeaponComparison by = new CombatCheckConfig()
		{
		}.weaponComparison();
		assertEquals(CombatCheckConfig.WeaponComparison.DAMAGE_PER_SECOND, by);
		double perTick = SetCalc.gain(scythe.got, bow.got);
		double ticksScythe = SetCalc.ticksToKill(scythe.loadout, scythe.player, scythe.target);
		double ticksBow = SetCalc.ticksToKill(bow.loadout, bow.player, bow.target);
		double scytheInHand = CombatCheckPlugin.comparedGain(by, perTick, ticksScythe, ticksBow);
		double bowInHand = CombatCheckPlugin.comparedGain(by, -perTick, ticksBow, ticksScythe);
		assertTrue("Scythe in hand: " + scytheInHand, CombatCheckPlugin.pointsAt(scytheInHand, CombatCheckPlugin.minimumGain(0)));
		assertEquals("+0.1%", CombatCheckPlugin.gainText(scytheInHand));
		assertFalse("bow in hand: " + bowInHand, CombatCheckPlugin.pointsAt(bowInHand, CombatCheckPlugin.minimumGain(0)));
		assertFalse(CombatCheckPlugin.pointsAt(scytheInHand, CombatCheckPlugin.minimumGain(1)));
		// the time to kill, the setting's other choice: the Scythe kills a tenth sooner
		CombatCheckConfig.WeaponComparison ttk = CombatCheckConfig.WeaponComparison.TIME_TO_KILL;
		assertFalse(CombatCheckPlugin.pointsAt(CombatCheckPlugin.comparedGain(ttk, perTick, ticksScythe, ticksBow), 0));
		assertTrue(CombatCheckPlugin.pointsAt(CombatCheckPlugin.comparedGain(ttk, -perTick, ticksBow, ticksScythe), 0));
	}

	/** Seeking arrows deal more than the same arrows without the least hit of 3, as much more as the calculator says. */
	@Test
	public void seekingArrowsHitAtLeastThree() throws IOException
	{
		Map<String, SetCalcTest.Case> c = cases();
		int compared = 0;
		for (SetCalcTest.Case k : c.values())
		{
			if (!k.set.startsWith("seeking-") || !k.set.contains("-seeking-"))
			{
				continue;
			}
			SetCalcTest.Case plain = c.get(k.set.replace("-seeking-", "-") + "@" + k.npc);
			assertTrue(k.set + " on " + k.npc, k.got > plain.got);
			assertEquals(k.set + " on " + k.npc, (k.expected / plain.expected - 1) * 100, SetCalc.gain(plain.got, k.got), 0.1);
			compared++;
		}
		assertEquals(8, compared);
		SetCalc.Loadout l = new SetCalc.Loadout();
		l.ammo = "Seeking broad arrows";
		assertTrue(SetCalc.seekingArrows(l));
		l.ammo = "Dragon arrow";
		assertFalse(SetCalc.seekingArrows(l));
	}
}
