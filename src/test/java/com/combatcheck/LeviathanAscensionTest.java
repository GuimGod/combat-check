/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

/**
 * A report from the Leviathan: the Ascension crossbows in hand, Ruby dragon bolts (e) in the ammunition
 * slot, Diamond ascension bolts (e) in Dizana's max cape, the Zaryte crossbow and the Twisted buckler in the inventory.
 * Three lines were wrong: "+49%" for the Zaryte crossbow, "Bank: Zaryte crossbow +43%" and "Diamond +73% (quiver)".
 * The crossbows cannot fire the rubies and the game shot the quiver's bolts (a shot every 2 ticks on Rapid, the rubies
 * on); the plugin let the rubies hold the quiver back, as anything with "bolts" in its name did, then counted the
 * crossbows with no bolts at all.
 */
public class LeviathanAscensionTest
{
	private static SwitchSet.Piece piece(int id, String name, int slot)
	{
		SwitchSet.Piece p = new SwitchSet.Piece();
		p.itemId = id;
		p.name = name;
		p.slot = slot;
		return p;
	}

	private static final int ZARYTE_CROSSBOW = 26374;
	private static final int RUBY_DRAGON_BOLTS_E = 21944;

	@Test
	public void theAscensionCrossbowsShootTheQuiversBoltsOverRubies()
	{
		SwitchSet.Piece rubies = piece(RUBY_DRAGON_BOLTS_E, "Ruby dragon bolts (e)", SwitchSet.AMMO);
		SwitchSet.Piece diamonds = piece(ArchiveItems.DIAMOND_ASCENSION_BOLTS_E, "Diamond ascension bolts (e)", SwitchSet.AMMO);
		SwitchSet.Piece[] on = new SwitchSet.Piece[SwitchSet.SLOTS];
		on[SwitchSet.WEAPON] = piece(ArchiveItems.ASCENSION_CROSSBOWS, "Ascension crossbows", SwitchSet.WEAPON);
		on[SwitchSet.AMMO] = rubies;
		assertSame(diamonds, CombatCheckPlugin.shotAmmo(on, "Crossbow", true, 33768, diamonds));
		// the quiver not worn: nothing it fires is on (the slot's rubies are what is there)
		assertSame(rubies, CombatCheckPlugin.shotAmmo(on, "Crossbow", false, 33768, diamonds));

		// the Zaryte crossbow fires the slot's rubies, not the Ascension bolts of the quiver
		on[SwitchSet.WEAPON] = piece(ZARYTE_CROSSBOW, "Zaryte crossbow", SwitchSet.WEAPON);
		assertSame(rubies, CombatCheckPlugin.shotAmmo(on, "Crossbow", true, 33768, diamonds));

		// by the names alone, as the weapons the calculator does not list are read
		assertTrue(CombatCheckPlugin.quiverShoots("Crossbow", "Ascension crossbows", "Ruby dragon bolts (e)",
			"Diamond ascension bolts (e)"));
		assertFalse(CombatCheckPlugin.quiverShoots("Crossbow", "Zaryte crossbow", "Ruby dragon bolts (e)",
			"Diamond ascension bolts (e)"));
		// the slot's bolts the crossbows fire are shot first
		assertFalse(CombatCheckPlugin.quiverShoots("Crossbow", "Ascension crossbows", "Diamond ascension bolts (e)",
			"Ascension bolts"));
		assertFalse(CombatCheckPlugin.quiverShoots("Crossbow", "Ascension crossbows", null, null));
	}

	/**
	 * Both directions of the pairing, by item and by name: the Ascension crossbows fire the ascension bolts and nothing
	 * else; no other crossbow fires them. "Diamond ascension bolts (e)" is a diamond kind for the effect (halved with the
	 * crossbows), but only the crossbows ever get it: the bolt notices and the quiver go by {@link Weapons#canFire}.
	 */
	@Test
	public void ascensionBoltsGoWithTheAscensionCrossbowsOnly()
	{
		int crossbows = ArchiveItems.ASCENSION_CROSSBOWS;
		int diamondAscension = ArchiveItems.DIAMOND_ASCENSION_BOLTS_E;
		// the crossbows: their own bolts yes; ruby and diamond dragon bolts (e), ruby and diamond bolts (e) no
		assertTrue(Weapons.canFire(crossbows, "Crossbow", "Ascension crossbows", diamondAscension, "Diamond ascension bolts (e)"));
		for (Object[] gem : new Object[][]{{21944, "Ruby dragon bolts (e)"}, {21946, "Diamond dragon bolts (e)"},
			{9242, "Ruby bolts (e)"}, {9243, "Diamond bolts (e)"}})
		{
			assertFalse("by item: " + gem[1], Weapons.canFire(crossbows, "Crossbow", "Ascension crossbows", (Integer) gem[0], (String) gem[1]));
			assertFalse("by name: " + gem[1], Weapons.canFire(0, "Crossbow", "Ascension crossbows", 0, (String) gem[1]));
			// and the Zaryte crossbow fires them
			assertTrue(Weapons.canFire(ZARYTE_CROSSBOW, "Crossbow", "Zaryte crossbow", (Integer) gem[0], (String) gem[1]));
		}
		// the Zaryte crossbow and any other crossbow: no ascension bolts, by item or by name
		assertFalse(Weapons.canFire(ZARYTE_CROSSBOW, "Crossbow", "Zaryte crossbow", diamondAscension, "Diamond ascension bolts (e)"));
		assertFalse(Weapons.canFire(0, "Crossbow", "Zaryte crossbow", 0, "Diamond ascension bolts (e)"));
		assertFalse(Weapons.canFire(0, "Crossbow", "Armadyl crossbow", 0, "Diamond ascension bolts (e)"));
		// the name reads as diamond for the effect's figure, nothing more
		assertEquals(SetCalc.DIAMOND, SetCalc.boltKind("Diamond ascension bolts (e)"));

		// the quiver, the other way round: a Zaryte crossbow with the crossbows' bolts on shoots the quiver's rubies; the
		// crossbows with their bolts on shoot those, whatever the quiver holds
		SwitchSet.Piece rubies = piece(RUBY_DRAGON_BOLTS_E, "Ruby dragon bolts (e)", SwitchSet.AMMO);
		SwitchSet.Piece diamonds = piece(diamondAscension, "Diamond ascension bolts (e)", SwitchSet.AMMO);
		SwitchSet.Piece[] on = new SwitchSet.Piece[SwitchSet.SLOTS];
		on[SwitchSet.WEAPON] = piece(ZARYTE_CROSSBOW, "Zaryte crossbow", SwitchSet.WEAPON);
		on[SwitchSet.AMMO] = diamonds;
		assertSame(rubies, CombatCheckPlugin.shotAmmo(on, "Crossbow", true, 6911, rubies));
		on[SwitchSet.WEAPON] = piece(crossbows, "Ascension crossbows", SwitchSet.WEAPON);
		assertSame(diamonds, CombatCheckPlugin.shotAmmo(on, "Crossbow", true, 6911, rubies));
	}

	/** The gear: Masori (f), Dizana's max cape, Necklace of rupture, Zaryte vambraces, Avernic treads (max), Lightbearer. */
	private static int gearRanged()
	{
		return 12 + 18 + 20 + 43 + 27 + 18 + 15;
	}

	private static int gearStrength()
	{
		return 2 + 3 + 8 + 4 + 2 + 2 + 3;
	}

	private static SetCalc.Loadout ascension()
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		l.ranged = gearRanged() + 34;
		l.rangedStrength = gearStrength() + 58;
		l.weaponName = "Ascension crossbows";
		l.category = "Crossbow";
		l.speed = 3;
		l.twoHanded = true;
		l.ammo = "Diamond ascension bolts (e)";
		l.ammoWorn = "Ruby dragon bolts (e)";
		l.dizanaQuiver = true;
		l.bolt = SetCalc.boltKind(l.ammo);
		return l;
	}

	private static SetCalc.Loadout zaryteWithRubies()
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		// the crossbow 110, the Twisted buckler 18 and 10, the rubies 122
		l.ranged = gearRanged() + 110 + 18;
		l.rangedStrength = gearStrength() + 10 + 122;
		l.weaponName = "Zaryte crossbow";
		l.category = "Crossbow";
		l.speed = 6;
		l.ammo = "Ruby dragon bolts (e)";
		l.ammoWorn = l.ammo;
		l.dizanaQuiver = true;
		l.bolt = SetCalc.boltKind(l.ammo);
		return l;
	}

	private static SetCalc.Player rigour()
	{
		SetCalc.Player p = new SetCalc.Player();
		p.ranged = 112;
		p.prayerRanged = 120;
		p.prayerRangedStrength = 123;
		p.kandarinDiary = true;
		return p;
	}

	private static void near(String what, double expected, double got)
	{
		assertTrue(what + ": " + got + " against the simulator's " + expected, Math.abs(got / expected - 1) <= 0.01);
	}

	/**
	 * The OSRS Wiki calculator with the crossbows' bonuses from the wiki and the diamond's effect halved: the crossbows
	 * 9.82 a second at any health, the
	 * Zaryte crossbow with rubies 8.43 from 900 to 500, 7.75 at 360, 6.98 at 200. The crossbows ahead everywhere: the
	 * Zaryte crossbow is never pointed at with them in hand.
	 */
	@Test
	public void atTheLeviathanTheCrossbowsBeatTheZaryteCrossbow() throws IOException
	{
		StyleData data = new StyleData();
		int[] health = {900, 500, 360, 200};
		double[] zaryte = {8.43, 8.43, 7.75, 6.98};
		for (int i = 0; i < health.length; i++)
		{
			SetCalc.Target t = data.monster(12214).target();
			t.hitpointsNow = health[i];
			double asc = SetCalc.perTick(ascension(), rigour(), t, false) / 0.6;
			double zcb = SetCalc.perTick(zaryteWithRubies(), rigour(), t, false) / 0.6;
			near("Ascension crossbows at " + health[i], 9.82, asc);
			near("Zaryte crossbow at " + health[i], zaryte[i], zcb);
			assertTrue(health[i] + ": " + asc + " over " + zcb, asc > zcb);
			assertFalse(health[i] + ": the Zaryte crossbow pointed at", CombatCheckPlugin.pointsAt(SetCalc.gain(asc, zcb), 10));
		}
	}

	/**
	 * The bank's line: a weapon in the inventory is not one of the bank, and a placeholder (a weapon taken out) is no weapon
	 * of the bank at all; RuneLite's canonicalize turns a placeholder into its item, so it had been read as the weapon.
	 */
	@Test
	public void theBankLineLeavesOutCarriedWeaponsAndPlaceholders()
	{
		assertEquals(Collections.singletonList(ZARYTE_CROSSBOW), BankHint.carriedWeapons(Arrays.asList(
			piece(ZARYTE_CROSSBOW, "Zaryte crossbow", SwitchSet.WEAPON),
			piece(21000, "Twisted buckler", SwitchSet.SHIELD),
			piece(ZARYTE_CROSSBOW, "Zaryte crossbow", SwitchSet.WEAPON),
			null)));
		assertTrue(BankHint.carriedWeapons(null).isEmpty());
		assertTrue(BankHint.placeholder(0, -1));
		assertTrue(BankHint.placeholder(1, 26374));
		assertFalse(BankHint.placeholder(1, -1));
	}
}
