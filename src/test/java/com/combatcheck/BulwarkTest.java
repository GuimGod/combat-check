/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Dinh's bulwark (OSRS Wiki, Dinh's bulwark): its strength in the Pummel style from the defence bonuses worn, against the
 * calculator (bulwark_sets.tsv, tools/gen_bulwark.py); its special attack, Shield Bash, which the calculator has no figure
 * for, against the wiki's rule worked out here by hand; and the bulwark in hand never pointed away from.
 */
public class BulwarkTest
{
	/** Every case within 0.1% of the calculator, in armour under the threshold and well over it, the blazing one too. */
	@Test
	public void theStrengthFromTheDefenceWornIsTheCalculators() throws IOException
	{
		List<SetCalcTest.Case> all = SetCalcTest.figuresOf("bulwark_sets.tsv");
		assertEquals(42, all.size());
		for (SetCalcTest.Case k : all)
		{
			assertTrue(k.set, SetCalc.plain(k.loadout));
		}
	}

	/** The wiki's formula: max(0, (stab + slash + crush + ranged defence - 800) / 12 - 38), for the bulwark only. */
	@Test
	public void theStrengthFollowsTheWikisFormula()
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		l.weaponName = "Dinh's bulwark";
		l.defence = 472;
		assertEquals(0, SetCalc.bulwarkStrength(l));
		// an average defence of 314 is the threshold: 4 * 314 = 1256 adds nothing, 1268 adds 1
		l.defence = 1256;
		assertEquals(0, SetCalc.bulwarkStrength(l));
		l.defence = 1268;
		assertEquals(1, SetCalc.bulwarkStrength(l));
		// Justiciar with the bulwark, Guardian boots, Barrows gloves, an Amulet of fury: (1908 - 800) / 12 - 38 = 54
		l.defence = 1908;
		assertEquals(54, SetCalc.bulwarkStrength(l));
		l.weaponName = "Dinh's blazing bulwark";
		assertEquals(54, SetCalc.bulwarkStrength(l));
		l.weaponName = "Abyssal whip";
		assertEquals(0, SetCalc.bulwarkStrength(l));
	}

	/**
	 * Shield Bash (OSRS Wiki, Dinh's bulwark, "Special attack"): the Pummel style's attack, crush, with 20% more
	 * accuracy, at the usual max hit. Worked out here from the wiki's accuracy and max hit formulas: Piety on 118
	 * Attack and Strength, the Pummel style accurate (+3 Attack), a hit that lands deals 0 to the max (a 0 counts 1, as
	 * every figure of the plugin and the calculator).
	 */
	@Test
	public void theSpecialAttackFollowsTheWiki() throws IOException
	{
		SetCalcTest.Case k = SetCalcTest.cases("bulwark_sets.tsv").stream()
			.filter(c -> c.set.equals("bulwark-justiciar") && c.npc == 7261).findFirst().orElse(null);
		assertNotNull(k);
		SetCalc.Loadout l = k.loadout;
		SetCalc.Target t = k.target;
		long attack = 118 * 120 / 100 + 3 + 8;
		long roll = attack * (l.crush + 64) * 6 / 5;
		long defence = (long) (t.defenceLevel + 9) * (t.crush + 64);
		double chance = roll > defence ? 1 - (defence + 2) / (2.0 * (roll + 1)) : roll / (2.0 * (defence + 1));
		long strength = 118 * 123 / 100 + 8;
		long max = (strength * (l.strength + SetCalc.bulwarkStrength(l) + 64) + 320) / 640;
		double sum = 0;
		for (long h = 0; h <= max; h++)
		{
			sum += Math.max(1, h);
		}
		double mean = chance * sum / (max + 1);
		double[] spec = SetCalc.bulwarkSpec(l, k.player, t);
		assertNotNull(spec);
		assertEquals(mean, spec[0], 1e-9);
		assertEquals(max, spec[1], 1e-9);
		// more accurate than an ordinary attack of the same style, the same max
		double ordinary = SetCalc.perTick(l, k.player, t, false) * l.speed;
		assertTrue(spec[0] > ordinary);
	}

	/** The line: the monster fought takes two hits, each other monster in reach one. */
	@Test
	public void theLineGivesTheTargetsTwoHitsAndTheOthersOne()
	{
		double[] main = {10.25, 40};
		assertEquals("Spec: 20.5 (max 80)", CombatCheckPlugin.bulwarkSpecText(CombatCheckConfig.Language.ENGLISH, main,
			Collections.emptyList()));
		List<double[]> others = Arrays.asList(new double[]{8, 40}, new double[]{6, 36});
		assertEquals("Spec 3 targets: 20.5 (max 80), others 7.0 (max 40)",
			CombatCheckPlugin.bulwarkSpecText(CombatCheckConfig.Language.ENGLISH, main, others));
		assertEquals("Spec 3 alvos: 20.5 (máx 80), outros 7.0 (máx 40)",
			CombatCheckPlugin.bulwarkSpecText(CombatCheckConfig.Language.PORTUGUESE, main, others));
		// up to 10 monsters within 5 tiles, for half the special attack energy
		assertEquals(10, SetCalc.BULWARK_SPEC_TARGETS);
		assertEquals(5, SetCalc.BULWARK_SPEC_REACH);
		assertEquals(500, SetCalc.BULWARK_SPEC_COST);
	}

	/** No figure of Shield Bash for another weapon. */
	@Test
	public void anotherWeaponHasNoShieldBash() throws IOException
	{
		SetCalcTest.Case k = SetCalcTest.cases("bulwark_sets.tsv").get(0);
		SetCalc.Loadout whip = new SetCalc.Loadout();
		whip.weaponName = "Abyssal whip";
		whip.category = "Whip";
		whip.styles = k.loadout.styles;
		assertNull(SetCalc.bulwarkSpec(whip, k.player, k.target));
	}

	/**
	 * With the bulwark in hand (the Maiden, the team stacking her) no other weapon is pointed at: it is held for what it
	 * does, as the weapons held for their special attack. Its Block style does not attack, so it has no figure and no
	 * style to point at: only the Pummel style counts.
	 */
	@Test
	public void theBulwarkInHandIsNeverPointedAwayFrom() throws IOException
	{
		assertTrue(SetEffects.pointsAtNoOtherWeapon("Dinh's bulwark", 0));
		assertTrue(SetEffects.pointsAtNoOtherWeapon("Dinh's blazing bulwark", 0));
		SetCalcTest.Case k = SetCalcTest.cases("bulwark_sets.tsv").get(0);
		int attacking = 0;
		for (StyleData.Style s : k.loadout.styles)
		{
			if (StyleCalc.melee(s.type))
			{
				attacking++;
				assertEquals(StyleCalc.CRUSH, s.type);
			}
		}
		assertEquals(1, attacking);
		assertFalse(k.loadout.styles.isEmpty());
	}
}
