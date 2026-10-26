/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * What the attack prayer the reminder names would add to the gear worn ({@link SetCalc#prayerGain}), against the
 * OSRS Wiki calculator: the sets of prayer_sets.tsv (tools/gen_prayer.py) with Piety, Rigour or Augury and with no
 * prayer, on monsters of low and high defence. Augury on Tumeken's shadow adds under 2% against an Abyssal demon
 * and a quarter against Dagannoth Prime: the figure has to tell the two apart.
 */
public class PrayerGainTest
{
	private static final String FILE = "prayer_sets.tsv";
	/** How far the plugin's gain may be from the calculator's, in percentage points. */
	private static final double POINTS = 0.15;

	/** The cases with and without prayer, by set and monster. */
	private static Map<String, SetCalcTest.Case> byKey() throws IOException
	{
		Map<String, SetCalcTest.Case> out = new HashMap<>();
		for (SetCalcTest.Case k : SetCalcTest.figuresOf(FILE))
		{
			out.put(k.set + "@" + k.npc, k);
		}
		return out;
	}

	private static double calculatorGain(Map<String, SetCalcTest.Case> all, String set, int npc)
	{
		SetCalcTest.Case on = all.get(set + "@" + npc);
		SetCalcTest.Case off = all.get(set + "-no-prayer@" + npc);
		return (on.expected / off.expected - 1) * 100;
	}

	@Test
	public void figuresWithAndWithoutPrayerAreTheCalculators() throws IOException
	{
		assertTrue(SetCalcTest.figuresOf(FILE).size() >= 100);
	}

	@Test
	public void gainOfTheBestPrayerIsTheCalculators() throws IOException
	{
		Map<String, SetCalcTest.Case> all = byKey();
		List<String> off = new ArrayList<>();
		int n = 0;
		for (SetCalcTest.Case k : all.values())
		{
			if (!k.set.endsWith("-no-prayer") || k.expected <= 0)
			{
				continue; // a monster the set cannot hit (Kree'arra and Zulrah with a melee weapon) has no figure
			}
			String set = k.set.substring(0, k.set.length() - "-no-prayer".length());
			double want = calculatorGain(all, set, k.npc);
			double got = SetCalc.prayerGain(k.loadout, k.player, k.target);
			n++;
			if (Math.abs(got - want) > POINTS)
			{
				off.add(String.format("%s on %d: %.2f%%, calculator %.2f%%", set, k.npc, got, want));
			}
		}
		assertTrue(n >= 50);
		assertTrue(off.size() + " of " + n + " off: " + off, off.isEmpty());
	}

	@Test
	public void auguryIsWorthLittleAgainstALowMagicDefenceAndMuchAgainstAHighOne() throws IOException
	{
		Map<String, SetCalcTest.Case> all = byKey();
		// Tumeken's shadow with the magic switch and a heart (calculator: +1.6% and +24.7%)
		double demon = gain(all, "shadow", 415);
		double prime = gain(all, "shadow", 2266);
		assertEquals(1.63, demon, POINTS);
		assertEquals(24.65, prime, POINTS);
		assertEquals("+1.6%", CombatCheckPlugin.differenceText(demon));
		assertEquals("+25%", CombatCheckPlugin.differenceText(prime));
		// the Sanguinesti staff, whose damage the +4% counts for more of: +4.4% and +27.9%
		assertEquals(4.38, gain(all, "sang", 415), POINTS);
		assertEquals(27.86, gain(all, "sang", 2266), POINTS);
	}

	@Test
	public void pietyAndRigourAgainstTheCalculator() throws IOException
	{
		Map<String, SetCalcTest.Case> all = byKey();
		// Piety on the Scythe: +20% on a Hill Giant, +41% on General Graardor
		assertEquals(calculatorGain(all, "scythe", 7261), gain(all, "scythe", 7261), POINTS);
		assertEquals(calculatorGain(all, "scythe", 2215), gain(all, "scythe", 2215), POINTS);
		assertTrue(gain(all, "scythe", 2215) > 40);
		// Rigour on the Twisted bow: +19% on Zulrah, +41% on General Graardor
		assertEquals(calculatorGain(all, "tbow", 2042), gain(all, "tbow", 2042), POINTS);
		assertEquals(calculatorGain(all, "tbow", 2215), gain(all, "tbow", 2215), POINTS);
	}

	@Test
	public void wrongPrayerOnCountsAsNoneForTheWeaponsClass() throws IOException
	{
		Map<String, SetCalcTest.Case> all = byKey();
		SetCalcTest.Case k = all.get("shadow-no-prayer@2266");
		double none = SetCalc.prayerGain(k.loadout, k.player, k.target);
		// Rigour on with a staff in hand: the magic figure has no prayer in it, so Augury adds the same
		SetCalc.Player rigour = copy(k.player);
		rigour.prayerRanged = 120;
		rigour.prayerRangedStrength = 123;
		assertEquals(none, SetCalc.prayerGain(k.loadout, rigour, k.target), 1e-9);
		// Augury already on: nothing to add
		SetCalc.Player augury = copy(k.player);
		augury.prayerMagic = 125;
		augury.prayerMagicDamage = 4;
		assertEquals(0, SetCalc.prayerGain(k.loadout, augury, k.target), 1e-9);
		assertEquals("0%", CombatCheckPlugin.differenceText(0));
	}

	@Test
	public void theBestPrayerIsTheOneThePlayerHas() throws IOException
	{
		Map<String, SetCalcTest.Case> all = byKey();
		SetCalcTest.Case k = all.get("shadow-no-prayer@2266");
		// Prayer 50 and no Augury: Mystic might (+15% accuracy, +2% damage) adds less than Augury
		SetCalc.Player low = copy(k.player);
		low.prayerLevel = 50;
		low.augury = false;
		low.vigour = false;
		double might = SetCalc.prayerGain(k.loadout, low, k.target);
		assertTrue(might > 5 && might < SetCalc.prayerGain(k.loadout, k.player, k.target));
		assertEquals(115, SetCalc.withBestPrayer(low, StyleCalc.MAGIC).prayerMagic);
		assertEquals(2, SetCalc.withBestPrayer(low, StyleCalc.MAGIC).prayerMagicDamage, 1e-9);
	}

	/** The plugin's gain of the best prayer over none for a set on a monster. */
	private static double gain(Map<String, SetCalcTest.Case> all, String set, int npc)
	{
		SetCalcTest.Case k = all.get(set + "-no-prayer@" + npc);
		return SetCalc.prayerGain(k.loadout, k.player, k.target);
	}

	private static SetCalc.Player copy(SetCalc.Player p)
	{
		SetCalc.Player q = SetCalc.withBestPrayer(p, StyleCalc.MELEE);
		q.prayerAttack = p.prayerAttack;
		q.prayerStrength = p.prayerStrength;
		return q;
	}
}
