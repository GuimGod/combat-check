/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.io.IOException;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * What the OSRS Wiki DPS calculator changed between the commit the tables were made with (89c3e25, 2026-09-02) and
 * 3da927f (2026-11-18), checked on 2026-11-19, where the
 * generated cases do not show it on their own. The figures of the rest are the calculator's, in the regenerated cases.
 */
public class CalcUpdatesTest
{
	/**
	 * The game's names changed by case alone ("Blood Moon helm", "Amulet of the Damned"; the calculator's 7e1dae8): the
	 * Blood Moon set is seen by either name, and its pieces and the amulet are kept.
	 */
	@Test
	public void theGamesNewNamesAreTheSameItems()
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		l.weaponName = "Dual macuahuitl";
		l.category = "Spiked";
		SetEffects.wornSets(l, Arrays.asList("Dual macuahuitl", "Blood Moon helm", "Blood Moon chestplate",
			"Blood Moon tassets"));
		assertTrue("the set by the game's name", l.bloodMoonSet);
		assertFalse("no figure with the set", SetCalc.plain(l));
		SetEffects.wornSets(l, Arrays.asList("Dual macuahuitl", "Blood moon helm", "Blood moon chestplate",
			"Blood moon tassets"));
		assertTrue("the old name too", l.bloodMoonSet);
		for (String name : new String[]{"Blood Moon tassets", "Blue Moon helm", "Eclipse Moon chestplate",
			"Amulet of the Damned", "Amulet of the Damned (full)"})
		{
			assertTrue(name, SetEffects.keep(name));
		}
	}

	/**
	 * A demonbane weapon's bonus in whole numbers, as the calculator adds it (x + x * percent / 100, rounded down), its
	 * percentage first scaled by the demon's share: a max hit of 125 with the Emberlight on Yama (120%: 84%) is 230, which
	 * the multiplier in floating point made 229; on Dreadnoth (25%) the Scorching bow's 30% is 7%.
	 */
	@Test
	public void theDemonbaneBonusIsInWholeNumbers()
	{
		SetCalc.Target yama = new SetCalc.Target();
		yama.demon = true;
		yama.demonbane = 120;
		assertEquals(84, SetCalc.demonbane(yama, 70));
		assertEquals(230, SetCalc.withDemonbane(125, yama, 70));
		assertEquals("the floating point way", 229, (long) (125 * (1 + 84 / 100.0)));
		SetCalc.Target dreadnoth = new SetCalc.Target();
		dreadnoth.demon = true;
		dreadnoth.demonbane = 25;
		assertEquals(7, SetCalc.demonbane(dreadnoth, 30));
		assertEquals(17, SetCalc.demonbane(dreadnoth, 70));
		SetCalc.Target notADemon = new SetCalc.Target();
		notADemon.demonbane = 25;
		assertEquals(100, SetCalc.withDemonbane(100, notADemon, 70));
	}

	/**
	 * The onyx bolts (e), counted since 2026-11-19 as the calculator counts them: 11% of the
	 * shots that land roll from 0 to 120% of the max hit instead (132% with a Zaryte crossbow), a tenth more often with the
	 * Kandarin diary, half as often on the Ascension crossbows, never on the undead.
	 */
	@Test
	public void theOnyxBoltsEffect()
	{
		assertEquals(SetCalc.ONYX, SetCalc.boltKind("Onyx bolts (e)"));
		assertEquals(SetCalc.ONYX, SetCalc.boltKind("Onyx dragon bolts (e)"));
		assertEquals(SetCalc.ONYX, SetCalc.boltKind("Onyx ascension bolts (e)"));
		assertEquals(0, SetCalc.boltKind("Onyx bolts"));
		assertEquals("Onyx", SetCalc.boltName(SetCalc.ONYX));
		SetCalc.Player p = new SetCalc.Player();
		SetCalc.Target t = new SetCalc.Target();
		// max 40, every shot lands: the plain shot's mean 20; the effect's 0 to 48, a 0 raised to 1: (1 + 1 + ... + 48) / 49
		double effect = (1 + 48.0 * 49 / 2) / 49;
		double shot = SetCalc.withBolt(SetCalc.ONYX, false, false, p, t, 40, 20, SetCalc.NO_SCALE, SetCalc.NO_SCALE, 0, 1);
		assertEquals(0.89 * 20 + 0.11 * effect, shot, 1e-9);
		// half the shots land: the effect on half as many
		double half = SetCalc.withBolt(SetCalc.ONYX, false, false, p, t, 40, 10, SetCalc.NO_SCALE, SetCalc.NO_SCALE, 0, 0.5);
		assertEquals(0.89 * 10 + 0.5 * 0.11 * effect, half, 1e-9);
		double ascension = SetCalc.withBolt(SetCalc.ONYX, false, true, p, t, 40, 20, SetCalc.NO_SCALE, SetCalc.NO_SCALE, 0, 1);
		assertEquals(0.945 * 20 + 0.055 * effect, ascension, 1e-9);
		t.undead = true;
		assertEquals(20, SetCalc.withBolt(SetCalc.ONYX, false, false, p, t, 40, 20, SetCalc.NO_SCALE, SetCalc.NO_SCALE, 0, 1),
			0);
		// with no bolts on, the gain shown is over the next best kind carried, not over the worst
		boolean[] three = {false, true, true, true};
		double[] top = CombatCheckPlugin.bestTwoKinds(new double[]{0, 5.0, 4.0, 5.5}, three);
		assertEquals(SetCalc.ONYX, (int) top[0]);
		assertEquals(5.5, top[1], 0);
		assertEquals("the ruby's, not the diamond's", 5.0, top[2], 0);
		double[] one = CombatCheckPlugin.bestTwoKinds(new double[]{0, 0, 4.0, 0}, new boolean[]{false, false, true, false});
		assertEquals(2, (int) one[0]);
		assertEquals("one kind: no next", -1, one[2], 0);
		assertEquals(null, CombatCheckPlugin.bestTwoKinds(new double[4], new boolean[4]));
		// with no monster yet: ruby, then diamond, then onyx, then any other bolts
		assertEquals(2, CombatCheckPlugin.preferredBolts(new String[]{"Rune bolts", "Onyx bolts (e)", "Diamond bolts (e)"}));
		assertEquals(1, CombatCheckPlugin.preferredBolts(new String[]{"Rune bolts", "Onyx bolts (e)"}));
	}

	/** The Tanglefoots: only Magic secateurs in melee hurt one, and they get no figure (their max hit is by Farming). */
	@Test
	public void onlyMagicSecateursHurtATanglefoot() throws IOException
	{
		StyleData data = new StyleData();
		for (int id : new int[]{5848, 5853, 5854, 6291, 6353})
		{
			SetCalc.Target t = data.monster(id).target();
			assertTrue(String.valueOf(id), t.immune.contains("s"));
			SetCalc.Loadout whip = new SetCalc.Loadout();
			whip.weaponName = "Abyssal whip";
			whip.category = "Whip";
			whip.speed = 4;
			whip.styles = data.styles(4151);
			whip.slash = 82;
			whip.strength = 82;
			assertEquals(0, SetCalc.perTick(whip, new SetCalc.Player(), t, false), 0);
			SetCalc.Loadout secateurs = new SetCalc.Loadout();
			secateurs.weaponName = "Magic secateurs";
			secateurs.category = "Slash Sword";
			secateurs.speed = 4;
			secateurs.styles = data.styles(7409);
			assertEquals(-1, SetCalc.perTick(secateurs, new SetCalc.Player(), t, false), 0);
		}
	}

	/**
	 * Rows the calculator's data has had since: Verzik's Nylocas (Defence 0, 25 hitpoints; before, the big Nylocas's
	 * Defence 20 by the name), the Feral Vampyres of Meiyerditch and Burgh de Rott (Defence 30), the Skeleton Hellhounds
	 * of Calvar'ion (size 2), the Nightmare's totems (defence -100), the quest's Phantom Muspah by its name.
	 */
	@Test
	public void theCalculatorsNewerData() throws IOException
	{
		StyleData data = new StyleData();
		for (int id : new int[]{8381, 8382, 8383})
		{
			assertEquals(0, data.monster(id).level);
			assertEquals(25, data.monster(id).hitpoints);
			assertEquals(2, data.monster(id).size);
		}
		for (int id : new int[]{10841, 10842, 10843})
		{
			// the same in Entry Mode: 3 hitpoints
			assertEquals(0, data.monster(id).level);
			assertEquals(3, data.monster(id).hitpoints);
		}
		for (int id : new int[]{3707, 3708, 4431})
		{
			assertEquals(String.valueOf(id), 30, data.monster(id).level);
		}
		assertEquals(2, data.monster(12107).size);
		assertEquals(2, data.monster(12108).size);
		for (int id = 9434; id <= 9445; id++)
		{
			assertEquals(String.valueOf(id), -100, data.monster(id).stab);
			assertEquals(String.valueOf(id), -100, data.monster(id).magic);
		}
		assertEquals("Phantom Muspah", data.monster(12074).name);
		assertTrue("Ba-Ba's baboon in the Tombs", data.monster(11781).toa && data.monster(11781).toaRoom);
	}

	/**
	 * Bork stunned as the calculator counts him: every melee attack lands, ranged
	 * still rolls against his Defence level.
	 */
	@Test
	public void borkStunnedTakesEveryMeleeAttack() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Target t = data.monster(16640).target();
		BorkArmour.apply(t, 8, 0, true);
		BorkStun.stun(t);
		SetCalc.Loadout mace = new SetCalc.Loadout();
		mace.weaponName = "Inquisitor's mace";
		mace.category = "Spiked";
		mace.speed = 4;
		mace.styles = data.styles(24417);
		mace.crush = 95;
		mace.strength = 89;
		SetCalc.Player p = new SetCalc.Player();
		p.attack = 99;
		p.strength = 99;
		double stunned = SetCalc.perTick(mace, p, t, false);
		t.meleeLands = false;
		assertTrue("every melee attack lands", stunned > SetCalc.perTick(mace, p, t, false));
	}
}
