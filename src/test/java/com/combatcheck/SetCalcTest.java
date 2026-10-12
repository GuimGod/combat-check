/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Whole sets of gear against a few monsters, compared with the OSRS Wiki calculator.
 * What matters to the plugin is the ratio between two sets, so the test
 * checks each figure and then the gain of every pair of sets on the same monster.
 */
public class SetCalcTest
{
	/** How far a figure may be from the calculator's, as a fraction of it. */
	private static final double FIGURE = 0.05;
	/** How far the gain of one set over another may be from the calculator's, in percentage points. */
	private static final double GAIN_POINTS = 6;

	private static final class Case
	{
		String set;
		int npc;
		double expected;
		double got;
	}

	private List<Case> cases() throws IOException
	{
		StyleData data = new StyleData();
		List<Case> out = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
			SetCalcTest.class.getResourceAsStream("sets.tsv"), StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = r.readLine()) != null)
			{
				if (line.isEmpty() || line.startsWith("#"))
				{
					continue;
				}
				String[] c = line.split("\t", -1);
				SetCalc.Loadout l = new SetCalc.Loadout();
				int weaponId = Integer.parseInt(c[2]);
				l.weaponName = c[3];
				l.category = c[4];
				l.speed = Integer.parseInt(c[5]);
				l.styles = data.styles(weaponId);
				l.stab = Integer.parseInt(c[8]);
				l.slash = Integer.parseInt(c[9]);
				l.crush = Integer.parseInt(c[10]);
				l.magic = Integer.parseInt(c[11]);
				l.ranged = Integer.parseInt(c[12]);
				l.strength = Integer.parseInt(c[13]);
				l.rangedStrength = Integer.parseInt(c[14]);
				// the blowpipe cases carry the strength of the darts inside
				l.dartKnown = c[3].toLowerCase(java.util.Locale.ROOT).contains("blowpipe");
				l.magicDamage = Double.parseDouble(c[15]);
				Case k = new Case();
				k.set = c[0];
				k.npc = Integer.parseInt(c[6]);
				k.expected = Double.parseDouble(c[16]);
				SetCalc.Target target = data.monster(k.npc).target();
				if (c.length > 19)
				{
					l.mask = Integer.parseInt(c[17]);
					l.salve = Integer.parseInt(c[18]);
					target.onTask = "1".equals(c[19]);
				}
				if (c.length > 23)
				{
					l.crystal = Integer.parseInt(c[20]);
					l.voidMelee = Integer.parseInt(c[21]);
					l.voidRanged = Integer.parseInt(c[22]);
					target.inWilderness = "1".equals(c[23]);
				}
				if (c.length > 25)
				{
					l.spell = Integer.parseInt(c[24]);
					l.tome = c[25].isEmpty() ? null : c[25];
				}
				k.got = SetCalc.perTick(l, player(c[1]), target, false);
				out.add(k);
			}
		}
		return out;
	}

	/** The presets the cases were generated with: Piety and a super combat, Rigour and a ranging potion, Augury and a heart. */
	private static SetCalc.Player player(String cls)
	{
		SetCalc.Player p = new SetCalc.Player();
		p.attack = 99;
		p.strength = 99;
		p.ranged = 99;
		p.magic = 99;
		if ("melee".equals(cls))
		{
			p.attack = 118;
			p.strength = 118;
			p.prayerAttack = 120;
			p.prayerStrength = 123;
		}
		else if ("ranged".equals(cls))
		{
			p.ranged = 112;
			p.prayerRanged = 120;
			p.prayerRangedStrength = 123;
		}
		else
		{
			p.magic = 112;
			p.prayerMagic = 125;
			p.prayerMagicDamage = 4;
		}
		return p;
	}

	@Test
	public void figuresAreCloseToTheCalculator() throws IOException
	{
		List<String> off = new ArrayList<>();
		for (Case k : cases())
		{
			if (Math.abs(k.got - k.expected) > FIGURE * k.expected)
			{
				off.add(String.format("%s on %d: %.3f, calculator %.3f", k.set, k.npc, k.got, k.expected));
			}
		}
		assertTrue(off.size() + " off: " + off, off.isEmpty());
	}

	@Test
	public void gainsBetweenSetsAreCloseToTheCalculator() throws IOException
	{
		Map<Integer, List<Case>> byNpc = new HashMap<>();
		for (Case k : cases())
		{
			byNpc.computeIfAbsent(k.npc, n -> new ArrayList<>()).add(k);
		}
		List<String> off = new ArrayList<>();
		for (List<Case> list : byNpc.values())
		{
			for (Case a : list)
			{
				for (Case b : list)
				{
					if (a == b || a.expected <= 0 || b.expected < a.expected)
					{
						continue;
					}
					double expected = (b.expected / a.expected - 1) * 100;
					double got = SetCalc.gain(a.got, b.got);
					// a big gain is judged in proportion: +300% against +320% is the same advice
					if (Math.abs(got - expected) > Math.max(GAIN_POINTS, expected * 0.08))
					{
						off.add(String.format("%s to %s on %d: %+.0f%%, calculator %+.0f%%", a.set, b.set, a.npc, got, expected));
					}
				}
			}
		}
		assertTrue(off.size() + " off: " + off, off.isEmpty());
	}

	/** Dharok's whole set against an Abyssal demon, with Piety and a super combat: the calculator's figures at 99, 50 and 10 hitpoints. */
	@Test
	public void dharoksSetGrowsWithTheHitpointsLost() throws IOException
	{
		StyleData data = new StyleData();
		double[][] cases = {{99, 3.12754}, {50, 4.58398}, {10, 5.82363}};
		for (double[] k : cases)
		{
			SetCalc.Loadout l = new SetCalc.Loadout();
			l.weaponName = "Dharok's greataxe";
			l.category = "Axe";
			l.speed = 7;
			l.styles = data.styles(4718);
			l.stab = 43;
			l.slash = 150;
			l.crush = 142;
			l.strength = 156;
			l.dharokSet = true;
			SetCalc.Player p = player("melee");
			p.hitpoints = (int) k[0];
			double got = SetCalc.perTick(l, p, data.monster(415).target(), false);
			assertTrue("at " + k[0] + " hitpoints: " + got + ", calculator " + k[1], Math.abs(got - k[1]) <= FIGURE * k[1]);
		}
		// without the whole set it is an ordinary axe
		SetCalc.Loadout plain = new SetCalc.Loadout();
		plain.weaponName = "Dharok's greataxe";
		plain.category = "Axe";
		assertTrue(SetCalc.plain(plain));
	}

	@Test
	public void anIdTheTableDoesNotListIsFoundByName() throws IOException
	{
		StyleData data = new StyleData();
		// the Hellhound of the Catacombs of Kourend
		org.junit.Assert.assertNull(data.monster(7256));
		org.junit.Assert.assertNotNull(data.monster(7256, "Hellhound"));
		org.junit.Assert.assertEquals("Hellhound", data.monster(7256, "Hellhound").name);
		org.junit.Assert.assertNull(data.monster(7256, "No such monster"));
		org.junit.Assert.assertSame(data.monster(415), data.monster(415, "Something else"));
	}

	@Test
	public void anotherClassIsComparedWithItsOwnBestPrayer()
	{
		// Rigour on, a bow in hand: the melee weapon in the inventory is counted with Piety
		SetCalc.Player rigour = player("ranged");
		SetCalc.Player melee = SetCalc.withPrayerOf(rigour, StyleCalc.MELEE);
		assertEquals(120, melee.prayerAttack);
		assertEquals(123, melee.prayerStrength);
		// the class already prayed for is left as it is
		assertSame(rigour, SetCalc.withPrayerOf(rigour, StyleCalc.RANGED));
		// no attack prayer on: none is added to the other class either
		SetCalc.Player none = new SetCalc.Player();
		assertSame(none, SetCalc.withPrayerOf(none, StyleCalc.MELEE));
		// without the scrolls and at a lower level, the best the player has
		SetCalc.Player low = player("melee");
		low.prayerLevel = 50;
		low.rigour = false;
		low.deadeye = false;
		assertEquals(115, SetCalc.withPrayerOf(low, StyleCalc.RANGED).prayerRanged);
		low.augury = false;
		low.vigour = false;
		assertEquals(115, SetCalc.withPrayerOf(low, StyleCalc.MAGIC).prayerMagic);
	}

	/** The Ancient Magicks on autocast with a Kodai wand, against an Abyssal demon: the calculator's figures. */
	@Test
	public void ancientSpellsAreCloseToTheCalculator() throws IOException
	{
		StyleData data = new StyleData();
		double[][] cases = {{31, 1.79288}, {34, 2.18717}, {36, 2.48325}, {37, 2.97714}, {39, 3.17481}, {42, 3.66914},
			{43, 3.76804}, {44, 3.96584}, {45, 4.06475}, {46, 4.16366}};
		for (double[] k : cases)
		{
			SetCalc.Loadout l = new SetCalc.Loadout();
			l.weaponName = "Kodai wand";
			l.category = "Staff";
			l.magic = 157;
			l.magicDamage = 39;
			l.spell = (int) k[0];
			assertTrue("the spell has a figure", SetCalc.plain(l));
			double got = SetCalc.perTick(l, player("magic"), data.monster(415).target(), false);
			assertTrue("spell " + k[0] + ": " + got + ", calculator " + k[1], Math.abs(got - k[1]) <= FIGURE * k[1]);
		}
		assertTrue(SetCalc.harmlessBolts("Sapphire dragon bolts (e)"));
		assertFalse(SetCalc.harmlessBolts("Onyx bolts (e)"));
		assertFalse(SetCalc.harmlessBolts("Runite bolts"));
	}

	@Test
	public void weaponsWithTheirOwnRulesGetNoFigure()
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		l.category = "Polearm";
		l.weaponName = "Dragon hunter lance";
		assertTrue("the lance's rule is known", SetCalc.plain(l));
		l.category = "Axe";
		l.weaponName = "Soulreaper axe";
		assertFalse(SetCalc.plain(l));
		l.category = "Staff";
		l.weaponName = "Kodai wand";
		assertFalse(SetCalc.plain(l));
		l.category = "Crossbow";
		l.weaponName = "Zaryte crossbow";
		l.boltEffect = true;
		assertFalse(SetCalc.plain(l));
		l.boltEffect = false;
		assertTrue(SetCalc.plain(l));
		l.category = "Powered Staff";
		l.weaponName = "Tumeken's shadow";
		assertTrue(SetCalc.plain(l));
	}

	@Test
	public void gainIsUnknownWithoutBothFigures()
	{
		assertTrue(Double.isNaN(SetCalc.gain(-1, 5)));
		assertTrue(Double.isNaN(SetCalc.gain(5, -1)));
		assertEquals(50, SetCalc.gain(4, 6), 1e-9);
	}

	/** Ruby and diamond bolts (e) at several healths of a few monsters, against the calculator (bolts.tsv). */
	@Test
	public void rubyAndDiamondBoltsFollowTheMonstersHealth() throws IOException
	{
		StyleData data = new StyleData();
		List<String> off = new ArrayList<>();
		int n = 0;
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
			SetCalcTest.class.getResourceAsStream("bolts.tsv"), StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = r.readLine()) != null)
			{
				if (line.isEmpty() || line.startsWith("#"))
				{
					continue;
				}
				String[] c = line.split("\t", -1);
				SetCalc.Loadout l = new SetCalc.Loadout();
				l.weaponName = c[2];
				l.category = "Crossbow";
				l.bolt = SetCalc.boltKind(c[1]);
				l.ranged = Integer.parseInt(c[7]);
				l.rangedStrength = Integer.parseInt(c[8]);
				l.speed = Integer.parseInt(c[9]);
				SetCalc.Player p = player("ranged");
				p.kandarinDiary = "1".equals(c[3]);
				SetCalc.Target t = data.monster(Integer.parseInt(c[4])).target();
				t.hitpointsNow = Integer.parseInt(c[6]);
				double expected = Double.parseDouble(c[10]);
				double got = SetCalc.perTick(l, p, t, false);
				n++;
				if (Math.abs(got - expected) > FIGURE * expected)
				{
					off.add(String.format("%s, %s, %s at %s: %.3f, calculator %.3f", c[1], c[2], c[5], c[6], got, expected));
				}
			}
		}
		assertTrue(n > 500);
		assertTrue(off.size() + " of " + n + " off: " + off, off.isEmpty());
	}

	@Test
	public void theRubyEffectIsCappedLowerOnTheGemstoneCrab()
	{
		SetCalc.Player p = new SetCalc.Player();
		p.hitpoints = 99;
		SetCalc.Target t = new SetCalc.Target();
		t.hitpoints = 50000;
		assertEquals(0.06 * 100, SetCalc.withBolt(SetCalc.RUBY, false, p, t, 40, 0.0), 1e-9);
		t.name = "Gemstone Crab";
		assertEquals(0.06 * 60, SetCalc.withBolt(SetCalc.RUBY, false, p, t, 40, 0.0), 1e-9);
		assertEquals(0.06 * 66, SetCalc.withBolt(SetCalc.RUBY, true, p, t, 40, 0.0), 1e-9);
	}
}
