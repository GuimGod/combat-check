/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/**
 * How the plugin reads a special attack from the game before counting its drain: the weapon in hand, the hits that
 * landed, and the damage rolled where the Alchemical Hydra's damage reduction cut the hitsplat.
 */
public class SpecialAttackReadingTest
{
	/** The calculator's Defence left in a case of drains.tsv (tools/gen_drains.py). */
	private static int calculator(String label) throws IOException
	{
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
			SpecialAttackReadingTest.class.getResourceAsStream("drains.tsv"), StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = in.readLine()) != null)
			{
				String[] c = line.split("\t", -1);
				if (c[0].equals(label))
				{
					return Integer.parseInt(c[16]);
				}
			}
		}
		throw new IllegalArgumentException(label);
	}

	/**
	 * CC-32: on the Alchemical Hydra with its damage reduction up, a Bandos godsword whose hitsplat shows 17 rolled about 68
	 * (OSRS Wiki, Alchemical Hydra/Strategies: "the Hydra reduces incoming damage by 75%"; equipment: "BGS spec applies
	 * before Hydra's 75% damage reduction"), and lowers its Defence of 100 to the calculator's 32 for a godsword hit of 68
	 * (hydra-bgs-68), not to 83. The Barrelchest anchor's drain goes by the damage rolled too (Barrelchest anchor, Mod Ash:
	 * "It's 10% of the damage *rolled*"); the other special attacks keep the hitsplat.
	 */
	@Test
	public void theHydrasReductionCutsTheHitsplatNotTheDrain() throws IOException
	{
		assertEquals(68, HydraShield.rolled(SpecialWeapon.BANDOS_GODSWORD, 17, true));
		assertEquals(17, HydraShield.rolled(SpecialWeapon.BANDOS_GODSWORD, 17, false));
		assertEquals(40, HydraShield.rolled(SpecialWeapon.BARRELCHEST_ANCHOR, 10, true));
		assertEquals(0, HydraShield.rolled(SpecialWeapon.BANDOS_GODSWORD, 0, true));
		assertEquals(1, HydraShield.rolled(SpecialWeapon.DRAGON_WARHAMMER, 1, true));
		assertEquals(25, HydraShield.rolled(SpecialWeapon.EYE_OF_AYAK, 25, true));
		DefenceDrains d = new DefenceDrains();
		d.add(SpecialWeapon.BANDOS_GODSWORD, HydraShield.rolled(SpecialWeapon.BANDOS_GODSWORD, 17, true), false);
		assertEquals(calculator("hydra-bgs-68"), d.apply(100, 260, 150, false, DefenceDrains.floor(8615, 100)).defence);
	}

	/**
	 * CC-32: when the reduction is up (OSRS Wiki, Alchemical Hydra: "At the start of each phase, it has a 75% damage
	 * reduction [...] Barring the last phase, the damage reduction is reapplied"; ids from its infobox: Serpentine 8615,
	 * Electric 8619, Fire 8620, Extinguished 8621, the changes of colour 8616, 8617, 8618), and the roar that takes it off
	 * (Alchemical Hydra/Strategies: "The Hydra will {{overhead|Roaaaaaaaaaaar!}} when hit by the correct vent"; "its damage
	 * reduction ability does not reset" in the last phase).
	 */
	@Test
	public void theHydrasReductionFromWhatTheGameShows()
	{
		HydraShield shield = new HydraShield();
		Object hydra = new Object();
		Object other = new Object();
		shield.form(hydra, 8615, 100);
		assertTrue("the fight starts with it", shield.shielded(hydra));
		assertFalse(shield.shielded(other));
		shield.says(hydra, 8615, "Roaaaaaaaaaaar!", 100);
		assertTrue("a roar on the tick the phase began is the change of colour's", shield.shielded(hydra));
		shield.says(hydra, 8615, "Something else", 104);
		assertTrue(shield.shielded(hydra));
		shield.says(hydra, 8615, "Roaaaaaaaaaaar!", 105);
		assertFalse("the right vent sprayed it", shield.shielded(hydra));
		shield.form(hydra, 8616, 200);
		assertFalse("the change of colour keeps what was", shield.shielded(hydra));
		shield.form(hydra, 8619, 202);
		assertTrue("the next phase starts with it again", shield.shielded(hydra));
		shield.says(hydra, 8617, "Roaaaaaaaaaaar!", 230);
		assertTrue("a roar while changing colour", shield.shielded(hydra));
		shield.form(hydra, 8617, 300);
		assertTrue(shield.shielded(hydra));
		shield.form(hydra, 8620, 302);
		assertTrue(shield.shielded(hydra));
		shield.form(hydra, 8621, 400);
		assertFalse("none in the last phase", shield.shielded(hydra));
		shield.form(hydra, 8615, 500);
		shield.forget(hydra);
		assertFalse("gone", shield.shielded(hydra));
		shield.form(other, 2215, 600);
		assertFalse("any other monster", shield.shielded(other));
		assertTrue(HydraShield.roar("Roaaaaaaaaaaar!"));
		assertFalse(HydraShield.roar(null));
		assertFalse(HydraShield.roar("Roar of the vent"));
	}

	/**
	 * CC-50: two weapons the Special Attack Counter does not list are read as those whose special attack they have (OSRS
	 * Wiki): the Arclight without charges (30305) as the Darklight, which it "functions identically to" (Arclight,
	 * Charging), and the Tonalztics of Ralos without charges (28919) as the charged one, hitting once ("Uncharged, it will
	 * only hit once"; Gallery: "A player performs Division with the uncharged weapon").
	 */
	@Test
	public void theUnchargedArclightAndTonalztics() throws IOException
	{
		assertEquals(SpecialWeapon.DARKLIGHT, CombatCheckPlugin.specialWeapon(30305));
		assertEquals(SpecialWeapon.TONALZTICS_OF_RALOS, CombatCheckPlugin.specialWeapon(28919));
		assertEquals(SpecialWeapon.ARCLIGHT, CombatCheckPlugin.specialWeapon(19675));
		assertEquals(SpecialWeapon.TONALZTICS_OF_RALOS, CombatCheckPlugin.specialWeapon(28922));
		assertEquals(SpecialWeapon.DARKLIGHT, CombatCheckPlugin.specialWeapon(6746));
		assertNull(CombatCheckPlugin.specialWeapon(4151));
		assertNull(CombatCheckPlugin.specialWeapon(-1));
		assertEquals(2, CombatCheckPlugin.specialHits(28922));
		assertEquals(1, CombatCheckPlugin.specialHits(28919));
		assertEquals(1, CombatCheckPlugin.specialHits(30305));
		// the hits that landed, of the weapon's last hitsplats (a thrall's may come before them on the same tick)
		assertEquals(Integer.valueOf(1), CombatCheckPlugin.landedHits(Arrays.asList(3, 12, 0), 2));
		assertEquals(Integer.valueOf(2), CombatCheckPlugin.landedHits(Arrays.asList(9, 14), 2));
		assertEquals(Integer.valueOf(0), CombatCheckPlugin.landedHits(Arrays.asList(12, 0), 1));
		assertEquals(Integer.valueOf(1), CombatCheckPlugin.landedHits(Collections.singletonList(31), 1));
		assertNull(CombatCheckPlugin.landedHits(Collections.singletonList(31), 2));
		assertNull(CombatCheckPlugin.landedHits(Collections.emptyList(), 1));
		// two special attacks of the Arclight without charges on K'ril Tsutsaroth, a demon: the calculator's two Arclight
		DefenceDrains arclight = new DefenceDrains();
		arclight.add(CombatCheckPlugin.specialWeapon(30305), 1, false);
		arclight.add(CombatCheckPlugin.specialWeapon(30305), 1, false);
		assertEquals(calculator("kril-arclight-2"), arclight.apply(270, 200, 0, true, 0).defence);
		// one hit of the uncharged Tonalztics on Tekton: an eighth of his Magic, as each of the calculator's hits
		DefenceDrains one = new DefenceDrains();
		one.add(CombatCheckPlugin.specialWeapon(28919), 1, false);
		assertEquals(205 - 205 / 8, one.apply(205, 205, 0, false, 0).defence);
		one.add(CombatCheckPlugin.specialWeapon(28919), 1, false);
		assertEquals(calculator("tekton-tonalztic-2"), one.apply(205, 205, 0, false, 0).defence);
	}
}
