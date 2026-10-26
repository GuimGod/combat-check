/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.io.IOException;
import net.runelite.client.game.ItemEquipmentStats;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The Fractured Archive's weapons in the plugin's figures (hard cases 7 to 9 and 14): the Obligator, Zeal, the
 * Ascension crossbows and their bolts, Zorya's tome.
 *
 * <p>The parity cases are the simulator's numbers, the flat armour taken off once, with the same gear: full
 * Inquisitor's, Infernal max cape, Amulet of rancour, Ferocious gloves, Avernic treads (max), Ultor ring, Rada's
 * blessing 4 (no strength), a super combat potion (118 Attack and Strength), on the Aggressive stance the plugin picks
 * itself. Within 0.5%.
 *
 * <p>The premise these figures stand on, which the game's posts do not state: the Obligator's Thrust makes <b>one</b>
 * roll of accuracy for its two to four damage rolls (if each damage roll had its own, the Thrust would deal 9% to 62%
 * more). {@link #theThrustRollsAccuracyOnce} pins it, so that a change of it is seen.
 */
public class ArchiveWeaponsTest
{
	/** The tolerance against the simulator: 0.5%. */
	private static final double TOLERANCE = 0.005;

	/** The melee gear of the simulator's "Inquisitor" set, without the weapon (RuneLite's item data, 2026-10-22). */
	private static SetCalc.Loadout inquisitor(StyleData data, int weaponId, String name, String category, int stab,
		int slash, int crush, int strength, int speed)
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		// helm, hauberk, plateskirt, Infernal max cape, Rancour, Ferocious gloves, Avernic treads (max), Ultor ring
		l.stab = 4 + 25 + 16 + 5 + stab;
		l.slash = 4 + 25 + 16 + 5 + slash;
		l.crush = 10 + 16 + 12 + 4 + 25 + 16 + 5 + crush;
		l.strength = 6 + 4 + 2 + 8 + 12 + 14 + 6 + 12 + strength;
		l.inquisitor = 5;
		l.weaponName = name;
		l.category = category;
		l.speed = speed;
		l.twoHanded = true;
		l.styles = data.styles(weaponId);
		return l;
	}

	private static SetCalc.Loadout obligator(StyleData data)
	{
		ItemEquipmentStats e = ArchiveItems.equipment(ArchiveItems.OBLIGATOR);
		return inquisitor(data, ArchiveItems.OBLIGATOR, "Obligator", data.category(ArchiveItems.OBLIGATOR), e.getAstab(),
			e.getAslash(), e.getAcrush(), e.getStr(), e.getAspeed());
	}

	/** 99 Attack and Strength with a super combat potion, and Zeal or Piety. */
	private static SetCalc.Player melee(boolean zeal)
	{
		SetCalc.Player p = new SetCalc.Player();
		p.attack = 118;
		p.strength = 118;
		p.ranged = 99;
		p.magic = 99;
		p.prayerAttack = zeal ? ArchiveItems.ZEAL_ATTACK : 120;
		p.prayerStrength = zeal ? ArchiveItems.ZEAL_STRENGTH : 123;
		p.zeal = zeal;
		return p;
	}

	private static double perSecond(SetCalc.Loadout l, SetCalc.Player p, SetCalc.Target t)
	{
		return SetCalc.perTick(l, p, t, false) / 0.6;
	}

	private static void near(String what, double expected, double got)
	{
		assertTrue(what + ": " + got + " against the simulator's " + expected,
			Math.abs(got / expected - 1) <= TOLERANCE);
	}

	/** The items' bonuses are the OSRS Wiki's (read 2026-10-22), used while RuneLite's item data has none for them. */
	@Test
	public void theNewItemsHaveTheWikisBonuses()
	{
		ItemEquipmentStats o = ArchiveItems.equipment(ArchiveItems.OBLIGATOR);
		assertEquals(22, o.getAstab());
		assertEquals(-8, o.getAslash());
		assertEquals(120, o.getAcrush());
		assertEquals(40, o.getStr());
		assertEquals(5, o.getAspeed());
		assertTrue(o.isTwoHanded());
		assertEquals(o, ArchiveItems.equipment(ArchiveItems.OBLIGATOR_UNCHARGED));
		ItemEquipmentStats x = ArchiveItems.equipment(ArchiveItems.ASCENSION_CROSSBOWS);
		assertEquals(34, x.getArange());
		assertEquals(3, x.getAspeed());
		assertTrue(x.isTwoHanded());
		assertEquals(53, ArchiveItems.equipment(ArchiveItems.ASCENSION_BOLTS).getRstr());
		assertEquals(58, ArchiveItems.equipment(ArchiveItems.DIAMOND_ASCENSION_BOLTS_E).getRstr());
		assertEquals(58, ArchiveItems.equipment(ArchiveItems.ONYX_ASCENSION_BOLTS_E).getRstr());
		ItemEquipmentStats tome = ArchiveItems.equipment(ArchiveItems.ZORYAS_TOME);
		assertEquals(45, tome.getAmagic());
		// +15% magic damage, in percent as RuneLite gives it (the Occult necklace 5.0), not 1.5 (finding A5)
		assertEquals(15f, tome.getMdmg(), 0);
		assertEquals(3, tome.getAspeed());
		assertNull(ArchiveItems.equipment(4151));
		for (int id : ArchiveItems.items())
		{
			int slot = ArchiveItems.equipment(id).getSlot();
			assertTrue(id + " is a weapon or ammunition", slot == SwitchSet.WEAPON || slot == SwitchSet.AMMO);
		}
	}

	@Test
	public void theNewWeaponsAreInTheTable() throws IOException
	{
		StyleData data = new StyleData();
		assertEquals("Extending Chain Mace", data.category(ArchiveItems.OBLIGATOR));
		assertEquals("Extending Chain Mace", data.category(ArchiveItems.OBLIGATOR_UNCHARGED));
		assertEquals("Crossbow", data.category(ArchiveItems.ASCENSION_CROSSBOWS));
		assertEquals("Powered Staff", data.category(ArchiveItems.ZORYAS_TOME));
		// Module:CombatStyles: Thrust crush Accurate, Thrust crush Aggressive, Smash crush Aggressive
		java.util.List<StyleData.Style> styles = data.styles(ArchiveItems.OBLIGATOR);
		assertEquals(3, styles.size());
		assertEquals("Thrust", styles.get(0).name);
		assertEquals("Accurate", styles.get(0).stance);
		assertEquals("Thrust", styles.get(1).name);
		assertEquals("Aggressive", styles.get(1).stance);
		assertEquals("Smash", styles.get(2).name);
		for (StyleData.Style s : styles)
		{
			assertEquals(StyleCalc.CRUSH, s.type);
		}
	}

	/** Parity: the Obligator's Thrust on Bork (Defence 300, flat armour 8, 4x4), Zeal 10.12 and Piety 9.43. */
	@Test
	public void obligatorOnBorkIsTheSimulators() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Target bork = data.monster(16640).target();
		assertEquals(8, bork.flatArmour);
		assertEquals(4, bork.size);
		near("Obligator, Zeal, Bork", 10.12, perSecond(obligator(data), melee(true), bork));
		near("Obligator, Piety, Bork", 9.43, perSecond(obligator(data), melee(false), bork));
	}

	/** Parity: on Ocellus's third phase (Defence 250, crush 30, 3x3), Zeal 11.80 and Piety 11.08. */
	@Test
	public void obligatorOnOcellusIsTheSimulators() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Target ocellus = data.monster(16677).target();
		near("Obligator, Zeal, Ocellus 3", 11.80, perSecond(obligator(data), melee(true), ocellus));
		near("Obligator, Piety, Ocellus 3", 11.08, perSecond(obligator(data), melee(false), ocellus));
	}

	/** Parity: the Scythe's jab with full Inquisitor's on Ocellus's third phase, Piety 8.17 and Zeal 8.81. */
	@Test
	public void zealOverPietyWithTheScytheIsTheSimulators() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Target ocellus = data.monster(16677).target();
		SetCalc.Loadout scythe = inquisitor(data, 22325, "Scythe of vitur", data.category(22325), 70, 125, 30, 75, 5);
		double piety = perSecond(scythe, melee(false), ocellus);
		double zeal = perSecond(scythe, melee(true), ocellus);
		near("Scythe, Piety, Ocellus 3", 8.17, piety);
		near("Scythe, Zeal, Ocellus 3", 8.81, zeal);
		assertTrue(zeal > piety);
	}

	/**
	 * Parity on every boss of the raid with a row of its own, no special attack: the Obligator with Zeal and with
	 * Piety, the Ascension crossbows with Diamond ascension bolts (e). The Archive Guardian on the ground is left out:
	 * the simulator's row is the examine's Defence 240 of its first descent, the plugin's the opening's 260 in both
	 * forms.
	 */
	@Test
	public void everyBossOfTheRaidIsTheSimulators() throws IOException
	{
		StyleData data = new StyleData();
		// npc, Obligator Zeal, Obligator Piety, Ascension diamond (e); NaN where the simulator has no row
		double[][] cases = {
			{16640, 10.12, 9.43, 6.28}, // Bork
			{16641, 6.64, 6.08, 7.60}, // the Smorks
			{16652, 13.62, 12.89, 5.40}, // Ezekiel
			{16667, 13.36, 12.64, 7.09}, // Moia
			{16665, 14.67, 13.93, 10.94}, // Dreadnoth
			{16684, 13.57, 12.84, 7.48}, // Xarth
			{16673, 11.15, 10.44, 9.86}, // Ocellus, first phase
			{16677, 11.80, 11.08, 4.07}, // Ocellus, third phase
			{16659, Double.NaN, Double.NaN, 8.96}, // the Archive Guardian flying: melee only with a polearm
		};
		for (double[] c : cases)
		{
			int npc = (int) c[0];
			SetCalc.Target t = data.monster(npc).target();
			if (Double.isNaN(c[1]))
			{
				assertEquals(0, perSecond(obligator(data), melee(true), t), 0);
			}
			else
			{
				near("Obligator, Zeal, " + npc, c[1], perSecond(obligator(data), melee(true), t));
				near("Obligator, Piety, " + npc, c[2], perSecond(obligator(data), melee(false), t));
			}
			near("Ascension, diamond (e), " + npc, c[3], perSecond(ascension("Diamond ascension bolts (e)", 58), rigour(), t));
		}
	}

	/**
	 * Hard case 7: two rolls by default, one more for each of stab and slash higher than crush (a defence equal to crush
	 * is not higher); 20% more on 2x2 and 40% on 3x3 and larger.
	 */
	@Test
	public void theThrustRollsByTheMonstersDefences()
	{
		assertEquals(4, ArchiveItems.obligatorRolls(140, 130, 10));
		assertEquals(3, ArchiveItems.obligatorRolls(140, 5, 10));
		assertEquals(3, ArchiveItems.obligatorRolls(5, 140, 10));
		assertEquals(2, ArchiveItems.obligatorRolls(5, 5, 10));
		assertEquals(2, ArchiveItems.obligatorRolls(10, 10, 10));
		assertEquals(3, ArchiveItems.obligatorRolls(11, 10, 10));
		assertEquals(1, ArchiveItems.obligatorSize(1)[0] / ArchiveItems.obligatorSize(1)[1]);
		assertEquals(12, ArchiveItems.obligatorSize(2)[0]);
		assertEquals(14, ArchiveItems.obligatorSize(3)[0]);
		assertEquals(14, ArchiveItems.obligatorSize(5)[0]);
		assertEquals(10, ArchiveItems.obligatorSize(5)[1]);
		// the best of four rolls from 0 to 1 is 1 unless all four are 0
		assertEquals(1 - Math.pow(0.5, 4), SetCalc.bestRollChance(1, 1, 4), 1e-12);
		double sum = 0;
		for (int k = 0; k <= 44; k++)
		{
			sum += SetCalc.bestRollChance(k, 44, 3);
		}
		assertEquals(1, sum, 1e-12);
	}

	/** A plain monster for the rules alone: Defence 100, the given bonuses and size, no flat armour. */
	private static SetCalc.Target plain(int stab, int slash, int crush, int size, int flatArmour)
	{
		SetCalc.Target t = new SetCalc.Target();
		t.name = "Test";
		t.defenceLevel = 100;
		t.stab = stab;
		t.slash = slash;
		t.crush = crush;
		t.size = size;
		t.flatArmour = flatArmour;
		return t;
	}

	/**
	 * Hard case 7: the flat armour comes off the Thrust's final hit once, not off each roll and not twice (the
	 * simulator first took it off the max hit and then off the hit).
	 */
	@Test
	public void theFlatArmourComesOffOnce()
	{
		SetCalc.Target t = plain(140, 130, 10, 4, 8);
		long max = 44;
		double once = 0;
		for (long k = 0; k <= max; k++)
		{
			once += SetCalc.bestRollChance(k, max, 4) * Math.max(0, Math.max(1, k * 14 / 10) - 8);
		}
		SetCalc.Scale sized = SetCalc.NO_SCALE.first(14, 10);
		assertEquals(once, SetCalc.bestOfRolls(max, 4, t, sized, 0, false), 1e-9);
		// twice would be the same with 8 more off (16), from a max hit already 8 lower
		double twice = 0;
		for (long k = 0; k <= max - 8; k++)
		{
			twice += SetCalc.bestRollChance(k, max - 8, 4) * Math.max(0, Math.max(1, k * 14 / 10) - 8);
		}
		assertTrue(once > twice * 1.2);
	}

	/**
	 * The premise: one roll of accuracy, then the best of the damage rolls. The figure is the chance to land times the
	 * mean of the best roll, not the mean of the best of four whole attacks (which would count the misses among the rolls).
	 */
	@Test
	public void theThrustRollsAccuracyOnce() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Loadout l = obligator(data);
		l.styles = java.util.Collections.singletonList(data.styles(ArchiveItems.OBLIGATOR).get(1));
		SetCalc.Player p = melee(true);
		SetCalc.Target t = data.monster(16677).target();
		StyleCalc.Input in = new StyleCalc.Input();
		in.attackLevel = p.attack;
		in.strengthLevel = p.strength;
		in.prayerAttack = p.prayerAttack;
		in.prayerStrength = p.prayerStrength;
		in.crush = l.crush;
		in.strength = l.strength;
		in.inquisitor = l.inquisitor;
		in.defenceLevel = t.defenceLevel;
		in.defenceCrush = t.crush;
		double[] cm = StyleCalc.chanceAndMax(in, StyleCalc.CRUSH, "Aggressive");
		assertEquals(44, (long) cm[1]);
		double once = cm[0] * SetCalc.bestOfRolls(44, 4, t, SetCalc.NO_SCALE.first(14, 10), 0, false) / 5;
		assertEquals(once, SetCalc.perTick(l, p, t, false), 1e-9);
		// the same worked out outside the plugin (Python, the simulator's expected damage with a hit of 0 raised to 1):
		// chance 0.71829 x the mean of floor(1.4 k) for the best k of 4 rolls from 0 to 44, over 3 seconds
		assertEquals(11.797341609668399, SetCalc.perTick(l, p, t, false) / 0.6, 1e-9);
		// Bork, crush 10 (4x4, flat armour 8 once): 10.117652569472346 the same way
		assertEquals(10.117652569472346, SetCalc.perTick(l, p, data.monster(16640).target(), false) / 0.6, 1e-9);
		// and its damage as chances gives the same figure, for the time to kill
		SetCalc.Attack a = SetCalc.attack(l, p, t);
		assertNotNull(a);
		assertEquals(5, a.speed, 0);
	}

	/**
	 * Hard case 7: the Smash attacks every 3 ticks, one roll, no size bonus; the Thrust every 5. On a large monster weakest
	 * to crush the Thrust deals more; on a small one strongest to crush the Smash does, and the plugin takes the best.
	 */
	@Test
	public void thrustAgainstSmash() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Player p = melee(true);
		SetCalc.Loadout thrust = obligator(data);
		thrust.styles = java.util.Collections.singletonList(data.styles(ArchiveItems.OBLIGATOR).get(1));
		SetCalc.Loadout smash = obligator(data);
		smash.styles = java.util.Collections.singletonList(data.styles(ArchiveItems.OBLIGATOR).get(2));
		SetCalc.Loadout both = obligator(data);

		SetCalc.Target big = plain(140, 130, 10, 4, 0);
		double t = SetCalc.perTick(thrust, p, big, false);
		double s = SetCalc.perTick(smash, p, big, false);
		assertTrue(t > s);
		assertEquals(t, SetCalc.perTick(both, p, big, false), 1e-12);

		SetCalc.Target small = plain(10, 10, 140, 1, 0);
		t = SetCalc.perTick(thrust, p, small, false);
		s = SetCalc.perTick(smash, p, small, false);
		assertTrue(s > t);
		assertEquals(s, SetCalc.perTick(both, p, small, false), 1e-12);

		// the Smash is a plain crush attack of 3 ticks: no rolls, no size
		StyleCalc.Input in = new StyleCalc.Input();
		in.attackLevel = p.attack;
		in.strengthLevel = p.strength;
		in.prayerAttack = p.prayerAttack;
		in.prayerStrength = p.prayerStrength;
		in.crush = smash.crush;
		in.strength = smash.strength;
		in.inquisitor = smash.inquisitor;
		in.defenceLevel = big.defenceLevel;
		in.defenceCrush = big.crush;
		assertEquals(StyleCalc.expectedHit(in, StyleCalc.CRUSH, "Aggressive") / 3, SetCalc.perTick(smash, p, big, false), 1e-9);
	}

	/**
	 * The style notice compares the Obligator's styles with the figures' own rules ({@link SetCalc#styleFigure}): per
	 * tick, with the Thrust's rolls and size, on every monster's rules. On the Corporeal Beast (5x5, crush the weakest:
	 * 4 rolls, 40% more, half of every hit) the Thrust deals more, which a score without the rolls would turn around.
	 */
	@Test
	public void theStyleNoticeComparesTheObligatorsStylesPerTick() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Player p = melee(true);
		SetCalc.Loadout l = obligator(data);
		java.util.List<StyleData.Style> styles = l.styles;
		for (int npc : new int[]{16640, 16677, 319})
		{
			SetCalc.Target t = data.monster(npc).target();
			double thrust = SetCalc.styleFigure(l, p, t, styles.get(1));
			double smash = SetCalc.styleFigure(l, p, t, styles.get(2));
			assertTrue("Thrust over Smash on " + npc, thrust > smash);
			assertEquals(SetCalc.perTick(l, p, t, false), Math.max(thrust, Math.max(smash,
				SetCalc.styleFigure(l, p, t, styles.get(0)))), 1e-12);
			// the loadout's styles are left as they were
			assertEquals(3, l.styles.size());
		}
		SetCalc.Target small = plain(10, 10, 140, 1, 0);
		assertTrue(SetCalc.styleFigure(l, p, small, styles.get(2)) > SetCalc.styleFigure(l, p, small, styles.get(1)));
	}

	/**
	 * Hard case 8: Zeal is melee's only (25% Attack, 28% Strength over Piety's 20% and 23%), takes Piety's place as the
	 * best melee prayer once unlocked, and does nothing to ranged or magic.
	 */
	@Test
	public void zealIsMeleesAndReplacesPiety() throws IOException
	{
		SetCalc.Player p = new SetCalc.Player();
		p.prayerLevel = 99;
		p.piety = true;
		p.zeal = true;
		SetCalc.Player m = SetCalc.withBestPrayer(p, StyleCalc.MELEE);
		assertEquals(125, m.prayerAttack);
		assertEquals(128, m.prayerStrength);
		SetCalc.Player r = SetCalc.withBestPrayer(p, StyleCalc.RANGED);
		assertEquals(120, r.prayerRanged);
		assertEquals(100, r.prayerAttack);
		SetCalc.Player g = SetCalc.withBestPrayer(p, StyleCalc.MAGIC);
		assertEquals(125, g.prayerMagic);
		assertEquals(100, g.prayerStrength);
		p.zeal = false;
		assertEquals(120, SetCalc.withBestPrayer(p, StyleCalc.MELEE).prayerAttack);
		assertEquals(123, SetCalc.withBestPrayer(p, StyleCalc.MELEE).prayerStrength);

		// with Zeal on and a bow in hand, the bow's figure is the one with no ranged prayer
		StyleData data = new StyleData();
		SetCalc.Loadout bow = new SetCalc.Loadout();
		bow.weaponName = "Twisted bow";
		bow.category = "Bow";
		bow.speed = 5;
		bow.ranged = 70;
		bow.rangedStrength = 20;
		bow.ammo = "Dragon arrow";
		SetCalc.Target t = data.monster(16677).target();
		SetCalc.Player none = new SetCalc.Player();
		none.ranged = 99;
		assertEquals(SetCalc.perTick(bow, none, t, false), SetCalc.perTick(bow, melee(true), t, false), 1e-12);
	}

	/** The simulator's ranged gear: Masori (f), Dizana's max cape, Necklace of rupture, Zaryte, treads (max), Venator. */
	private static SetCalc.Loadout ascension(String bolts, int boltStrength)
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		ItemEquipmentStats x = ArchiveItems.equipment(ArchiveItems.ASCENSION_CROSSBOWS);
		l.ranged = 12 + 18 + 20 + 43 + 27 + 18 + 15 + 10 + x.getArange();
		l.rangedStrength = 2 + 3 + 8 + 4 + 2 + 2 + 3 + 2 + boltStrength;
		l.weaponName = "Ascension crossbows";
		l.category = "Crossbow";
		l.speed = x.getAspeed();
		l.twoHanded = true;
		l.ammo = bolts;
		l.ammoWorn = bolts;
		l.dizanaQuiver = true;
		l.bolt = SetCalc.boltKind(bolts);
		l.boltEffect = l.bolt == 0 && bolts.endsWith("(e)") && !SetCalc.harmlessBolts(bolts);
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

	/**
	 * Parity and hard case 9: the Ascension crossbows with Diamond ascension bolts (e) on Ocellus's first phase, 9.86 in
	 * the simulator (Rapid, 2 ticks; the diamond's effect 5.5% of the shots: 10%, a tenth more with the Kandarin diary,
	 * halved). The effect at its full chance would be more.
	 */
	@Test
	public void ascensionCrossbowsAreTheSimulators() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Target ocellus = data.monster(16673).target();
		SetCalc.Loadout l = ascension("Diamond ascension bolts (e)", 58);
		assertEquals(SetCalc.DIAMOND, l.bolt);
		assertTrue(SetCalc.plain(l));
		double got = perSecond(l, rigour(), ocellus);
		near("Ascension, diamond (e), Ocellus 1", 9.86, got);

		SetCalc.Player p = rigour();
		double normal = 0.5;
		double halved = SetCalc.withBolt(SetCalc.DIAMOND, false, true, p, ocellus, 34, normal, SetCalc.NO_SCALE,
			SetCalc.NO_SCALE, 0);
		double full = SetCalc.withBolt(SetCalc.DIAMOND, false, false, p, ocellus, 34, normal, SetCalc.NO_SCALE,
			SetCalc.NO_SCALE, 0);
		double effect = SetCalc.landed(0, 34 * 115 / 100, ocellus);
		assertEquals(0.055 * effect + 0.945 * normal, halved, 1e-9);
		assertEquals(0.11 * effect + 0.89 * normal, full, 1e-9);

		// plain Ascension bolts: no effect; the onyx's effect is not modelled (no figure), as with any other onyx bolts
		SetCalc.Loadout plainBolts = ascension("Ascension bolts", 53);
		assertEquals(0, plainBolts.bolt);
		assertTrue(SetCalc.plain(plainBolts));
		assertTrue(perSecond(plainBolts, rigour(), ocellus) < got);
		assertFalse(SetCalc.plain(ascension("Onyx ascension bolts (e)", 58)));
		assertTrue(SetCalc.plain(ascension("Onyx ascension bolts", 58)));
	}

	/** Hard case 9: the Ascension crossbows fire the Ascension bolts only, and no other crossbow fires them. */
	@Test
	public void onlyTheAscensionCrossbowsFireAscensionBolts()
	{
		int crossbows = ArchiveItems.ASCENSION_CROSSBOWS;
		for (int id : new int[]{ArchiveItems.ASCENSION_BOLTS, ArchiveItems.DIAMOND_ASCENSION_BOLTS_E,
			ArchiveItems.ONYX_ASCENSION_BOLTS_E, ArchiveItems.DIAMOND_ASCENSION_BOLTS, ArchiveItems.ONYX_ASCENSION_BOLTS})
		{
			assertTrue(Weapons.canFire(crossbows, "Crossbow", "Ascension crossbows", id, "Diamond ascension bolts (e)"));
			// the Zaryte crossbow (listed) and a crossbow the calculator does not list
			assertFalse(Weapons.canFire(26374, "Crossbow", "Zaryte crossbow", id, "Diamond ascension bolts (e)"));
			assertFalse(Weapons.canFire(99999, "Crossbow", "Some crossbow", id, "Diamond ascension bolts (e)"));
		}
		// ruby and diamond dragon bolts (e), plain bolts: not in the Ascension crossbows
		assertFalse(Weapons.canFire(crossbows, "Crossbow", "Ascension crossbows", 21944, "Ruby dragon bolts (e)"));
		assertFalse(Weapons.canFire(crossbows, "Crossbow", "Ascension crossbows", 9243, "Diamond bolts (e)"));
		assertFalse(Weapons.canFire(crossbows, "Crossbow", "Ascension crossbows", 9144, "Runite bolts"));
		assertTrue(Weapons.canFire(26374, "Crossbow", "Zaryte crossbow", 21944, "Ruby dragon bolts (e)"));
		assertEquals(SetCalc.DIAMOND, SetCalc.boltKind("Diamond ascension bolts (e)"));
		assertEquals(0, SetCalc.boltKind("Onyx ascension bolts (e)"));
		assertEquals(0, SetCalc.boltKind("Diamond ascension bolts"));
	}

	/**
	 * Zorya's tome: no figure anywhere, the lenient one included (its max hit by the Magic level is not published), none uncharged (it needs its charges to
	 * cast), and it is held for its special attack. The Obligator uncharged: no figure (whether it attacks is not known).
	 */
	@Test
	public void theTomeAndTheUnchargedObligatorGetNoFigure() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Loadout tome = new SetCalc.Loadout();
		tome.weaponName = "Zorya's tome";
		tome.category = data.category(ArchiveItems.ZORYAS_TOME);
		tome.speed = 3;
		tome.magic = 45;
		tome.magicDamage = 15;
		tome.twoHanded = true;
		assertEquals(StyleCalc.MAGIC, SetCalc.combatClass(tome));
		assertFalse(SetCalc.plain(tome));
		assertTrue(Weapons.cannotAttackUncharged("Zorya's tome (uncharged)"));
		assertFalse(Weapons.cannotAttackUncharged("Zorya's tome"));
		assertTrue(SetEffects.isSpecialAttackWeapon("Zorya's tome"));

		SetCalc.Loadout uncharged = obligator(data);
		uncharged.weaponName = "Obligator (uncharged)";
		assertFalse(SetCalc.plain(uncharged));
		// nor by the lenient figure the pieces' and the prayer's gain use, nor by the attack style notice
		SetCalc.Target bork = data.monster(16640).target();
		SetCalc.Player p = melee(true);
		assertEquals(-1, SetCalc.perTick(uncharged, p, bork, true), 0);
		assertEquals(-1, SetCalc.styleFigure(uncharged, p, bork, uncharged.styles.get(1)), 0);
		assertTrue(Double.isNaN(SetCalc.prayerGain(uncharged, p, bork)));
		assertEquals(-1, SetCalc.perTick(tome, p, bork, true), 0);
		assertTrue(Double.isNaN(SetCalc.prayerGain(tome, p, bork)));
		assertTrue(SetCalc.plain(obligator(data)));
		assertFalse(SetEffects.isSpecialAttackWeapon("Obligator"));
	}
}
