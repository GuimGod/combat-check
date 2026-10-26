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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * The time to kill, against the OSRS Wiki DPS calculator's getHtk (ttk_sets.tsv, tools/gen_ttk.py): sets on the wave
 * Nylocas of the Theatre of Blood (the three colours, small and big, in the three modes, a party of 3), on monsters of
 * a few hitpoints out of the Theatre (the Inferno, the Colosseum) and on three of many.
 */
public class TimeToKillTest
{
	private static final String FILE = "ttk_sets.tsv";
	/** How far the plugin's time to kill may be from the calculator's, as a fraction of it. */
	private static final double TICKS = 0.005;

	@Test
	public void everyFigureIsTheCalculators() throws IOException
	{
		assertEquals(83, SetCalcTest.figuresOf(FILE).size());
	}

	@Test
	public void everyTimeToKillIsTheCalculators() throws IOException
	{
		List<String> off = new ArrayList<>();
		for (SetCalcTest.Case k : SetCalcTest.cases(FILE))
		{
			double want = k.htk * k.calculatorSpeed;
			double got = SetCalc.ticksToKill(k.loadout, k.player, k.target);
			if (Double.isNaN(got) || Math.abs(got - want) > TICKS * want)
			{
				off.add(String.format("%s on %d (%s): %.4f ticks, calculator %.4f", k.set, k.npc, k.target.name, got, want));
			}
		}
		assertTrue(off.size() + " off: " + off, off.isEmpty());
	}

	/** The ticks to kill of each set on a monster of the file, by the set's label. */
	private static Map<String, Double> ticks(List<SetCalcTest.Case> all, int npc)
	{
		Map<String, Double> out = new LinkedHashMap<>();
		for (SetCalcTest.Case k : all)
		{
			if (k.npc == npc)
			{
				out.put(k.set, SetCalc.ticksToKill(k.loadout, k.player, k.target));
			}
		}
		return out;
	}

	private static Map<String, Double> perTick(List<SetCalcTest.Case> all, int npc)
	{
		Map<String, Double> out = new LinkedHashMap<>();
		for (SetCalcTest.Case k : all)
		{
			if (k.npc == npc)
			{
				out.put(k.set, k.got);
			}
		}
		return out;
	}

	private static String fastest(Map<String, Double> ticks)
	{
		String best = null;
		for (Map.Entry<String, Double> e : ticks.entrySet())
		{
			if (best == null || e.getValue() < ticks.get(best))
			{
				best = e.getKey();
			}
		}
		return best;
	}

	private static String most(Map<String, Double> perTick)
	{
		String best = null;
		for (Map.Entry<String, Double> e : perTick.entrySet())
		{
			if (best == null || e.getValue() > perTick.get(best))
			{
				best = e.getKey();
			}
		}
		return best;
	}

	/**
	 * The grey Ischyros, small and big in their share in the waves (25 and 16): the Swift blade over the Dual macuahuitl in
	 * every mode (by the figure per tick the plugin pointed at the Dual macuahuitl). The Toxobolos and the Hagios one by one,
	 * as any monster: the blowpipe and the Eye of Ayak, small and big.
	 */
	@Test
	public void theWavesWeapons() throws IOException
	{
		List<SetCalcTest.Case> all = SetCalcTest.cases(FILE);
		// the Ischyros small and big in normal, Entry and Hard Mode
		int[][] ischyros = {{8342, 8345}, {10774, 10777}, {10791, 10794}};
		for (int[] mode : ischyros)
		{
			Map<String, Double> s = ticks(all, mode[0]);
			Map<String, Double> b = ticks(all, mode[1]);
			Map<String, Double> combined = new LinkedHashMap<>();
			for (String set : s.keySet())
			{
				combined.put(set, NyloWaves.combined(s.get(set), b.get(set)));
			}
			assertEquals(mode[0] + " " + combined, "ttk-swift-blade", fastest(combined));
		}
		// normal mode, a trio: the Swift blade kills one 6.3% sooner than the Dual macuahuitl (the calculator's: 6.3%)
		Map<String, Double> s = ticks(all, 8342);
		Map<String, Double> b = ticks(all, 8345);
		double swift = NyloWaves.combined(s.get("ttk-swift-blade"), b.get("ttk-swift-blade"));
		double dual = NyloWaves.combined(s.get("ttk-dual-macuahuitl"), b.get("ttk-dual-macuahuitl"));
		assertEquals(6.3, (dual / swift - 1) * 100, 0.2);
		assertEquals("ttk-dual-macuahuitl", most(perTick(all, 8342)));
		// the Toxobolos and the Hagios, small and big, each by itself
		for (int npc : new int[]{8343, 8346, 10775, 10778, 10792, 10795})
		{
			assertEquals(npc + "", "ttk-blowpipe", fastest(ticks(all, npc)));
		}
		for (int npc : new int[]{8344, 8347, 10776, 10779, 10793, 10796})
		{
			assertEquals(npc + "", "ttk-eye", fastest(ticks(all, npc)));
		}
		assertEquals("ttk-tbow", most(perTick(all, 8343)));
	}

	/**
	 * The notice: the Twisted bow in hand on a small Toxobolos points at the blowpipe and says why; the blowpipe in hand
	 * points at nothing. The gain is how much sooner it kills, against the figure per tick, which would point at the bow.
	 */
	@Test
	public void theNoticeOnTheToxobolos() throws IOException
	{
		List<SetCalcTest.Case> all = SetCalcTest.cases(FILE);
		Map<String, Double> s = ticks(all, 8343);
		Map<String, Double> per = perTick(all, 8343);
		double bow = s.get("ttk-tbow");
		double pipe = s.get("ttk-blowpipe");
		double perTickGain = SetCalc.gain(per.get("ttk-tbow"), per.get("ttk-blowpipe"));
		assertTrue(perTickGain < 0);
		double gain = CombatCheckPlugin.weaponGain(perTickGain, bow, pipe);
		assertTrue("blowpipe over the bow: " + gain, gain > 90);
		assertTrue(CombatCheckPlugin.killsSooner(perTickGain, gain, 1));
		// the blowpipe in hand: the bow kills slower, nothing to point at
		assertTrue(CombatCheckPlugin.weaponGain(-perTickGain, pipe, bow) < 0);
		assertEquals("+117% (low HP)", CombatCheckPlugin.gainText(117) + " " + CombatCheckPlugin.LOW_HP);
		assertEquals("+117% (pouca vida)", Words.weapon(CombatCheckConfig.Language.PORTUGUESE, "+117% (low HP)"));
	}

	/**
	 * The rule is general: on monsters of a few hitpoints out of the Theatre the faster weapon can win too. On those of
	 * many hitpoints the time to kill picks the weapon the figure per tick picks.
	 */
	@Test
	public void outOfTheTheatre() throws IOException
	{
		List<SetCalcTest.Case> all = SetCalcTest.cases(FILE);
		// the Inferno's Jal-MejRah (25 hitpoints): the blowpipe kills sooner than the bow
		Map<String, Double> bat = ticks(all, 7692);
		assertTrue(bat.toString(), bat.get("ttk-blowpipe") < bat.get("ttk-tbow"));
		// Vorkath, Xarpus, Verzik's third phase: the same weapon by either, in each class
		for (int npc : new int[]{8059, 8340, 8374})
		{
			Map<String, Double> t = ticks(all, npc);
			Map<String, Double> p = perTick(all, npc);
			for (String[] cls : new String[][]{{"ttk-scythe", "ttk-swift-blade", "ttk-dual-macuahuitl"}, {"ttk-blowpipe", "ttk-tbow"}})
			{
				Map<String, Double> tc = new LinkedHashMap<>();
				Map<String, Double> pc = new LinkedHashMap<>();
				for (String set : cls)
				{
					tc.put(set, t.get(set));
					pc.put(set, p.get(set));
				}
				assertEquals(npc + " " + tc + " " + pc, most(pc), fastest(tc));
			}
		}
	}

	@Test
	public void theWaves()
	{
		assertEquals(8342, NyloWaves.rows(8348)[0]);
		assertEquals(8345, NyloWaves.rows(8351)[1]);
		assertEquals(10794, NyloWaves.rows(10800)[1]);
		assertEquals(10777, NyloWaves.rows(10783)[1]);
		// only the grey ones; Verzik's Nylocas are not the waves'
		assertNull(NyloWaves.rows(8343));
		assertNull(NyloWaves.rows(8347));
		assertNull(NyloWaves.rows(8381));
		// 16 big of 41 in the wiki's chart of the 31 waves
		assertEquals(16 / 41.0, NyloWaves.bigShare(), 1e-12);
		assertEquals(25 / 41.0 * 4 + 16 / 41.0 * 8, NyloWaves.combined(4, 8), 1e-9);
	}

	@Test
	public void hitsToKillTheCalculatorsWay()
	{
		// a hit of 10 for sure on 10 hitpoints: one attack; half the attacks land: two
		assertEquals(1, KillTime.hitsToKill(KillTime.certain(10), 10), 1e-12);
		assertEquals(2, KillTime.hitsToKill(KillTime.withChance(0.5, KillTime.certain(10)), 10), 1e-12);
		// 0 to 2 evenly on 2 hitpoints: htk[1] = 1 / (2/3) = 1.5; htk[2] = (1 + 1/3 * 1.5) / (2/3) = 2.25
		double[] even = {1 / 3.0, 1 / 3.0, 1 / 3.0};
		assertEquals(2.25, KillTime.hitsToKill(even, 2), 1e-12);
		assertTrue(Double.isInfinite(KillTime.hitsToKill(KillTime.certain(0), 5)));
		assertEquals(0, KillTime.hitsToKill(even, 0), 0);
	}

	/** The Dual macuahuitl has a figure now, but not with the Blood moon set, whose attacks can come sooner. */
	@Test
	public void theDualMacuahuitl()
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		l.weaponName = "Dual macuahuitl";
		l.category = "Spiked";
		SetEffects.wornSets(l, java.util.Arrays.asList("Dual macuahuitl", "Torva full helm"));
		assertTrue(SetCalc.plain(l));
		SetEffects.wornSets(l, java.util.Arrays.asList("Dual macuahuitl", "Blood moon helm", "Blood moon chestplate",
			"Blood moon tassets"));
		assertFalse(SetCalc.plain(l));
	}

	/** A set whose damage is not put as chances (a bolt's effect) gets no time to kill: the figure per tick decides. */
	@Test
	public void notPutAsChances() throws IOException
	{
		SetCalcTest.Case k = SetCalcTest.cases(FILE).get(0);
		assertNotNull(SetCalc.attack(k.loadout, k.player, k.target));
		double now = 1.0;
		assertEquals(5.0, CombatCheckPlugin.weaponGain(5.0, Double.NaN, now), 0);
		assertEquals(5.0, CombatCheckPlugin.weaponGain(5.0, now, Double.NaN), 0);
		assertTrue(Double.isInfinite(CombatCheckPlugin.weaponGain(Double.POSITIVE_INFINITY, 3, 2)));
	}

	/**
	 * By default the weapons are compared by the damage per second: on the grey Ischyros the Dual macuahuitl, with the
	 * Swift blade in hand, is pointed at; on the small Toxobolos the Twisted bow in hand points at nothing and the
	 * blowpipe in hand at the bow, never with "(low HP)". The time to kill is the setting's other choice.
	 */
	@Test
	public void byDefaultTheDamagePerSecondDecides() throws IOException
	{
		CombatCheckConfig config = new CombatCheckConfig()
		{
		};
		CombatCheckConfig.WeaponComparison by = config.weaponComparison();
		assertEquals(CombatCheckConfig.WeaponComparison.DAMAGE_PER_SECOND, by);
		List<SetCalcTest.Case> all = SetCalcTest.cases(FILE);
		double minimum = CombatCheckPlugin.minimumGain(0);
		// the grey Ischyros in a normal trio: the Dual macuahuitl deals the most, though the Swift blade kills sooner
		Map<String, Double> per = perTick(all, 8342);
		Map<String, Double> s = ticks(all, 8342);
		Map<String, Double> b = ticks(all, 8345);
		double swift = NyloWaves.combined(s.get("ttk-swift-blade"), b.get("ttk-swift-blade"));
		double dual = NyloWaves.combined(s.get("ttk-dual-macuahuitl"), b.get("ttk-dual-macuahuitl"));
		double toDual = SetCalc.gain(per.get("ttk-swift-blade"), per.get("ttk-dual-macuahuitl"));
		assertTrue(CombatCheckPlugin.pointsAt(CombatCheckPlugin.comparedGain(by, toDual, swift, dual), minimum));
		assertFalse(CombatCheckPlugin.pointsAt(CombatCheckPlugin.comparedGain(by, -toDual, dual, swift), minimum));
		// by the time to kill, the other way round
		CombatCheckConfig.WeaponComparison ttk = CombatCheckConfig.WeaponComparison.TIME_TO_KILL;
		assertFalse(CombatCheckPlugin.pointsAt(CombatCheckPlugin.comparedGain(ttk, toDual, swift, dual), minimum));
		assertTrue(CombatCheckPlugin.pointsAt(CombatCheckPlugin.comparedGain(ttk, -toDual, dual, swift), minimum));
		// the small Toxobolos: the bow deals more per second, the blowpipe kills sooner
		Map<String, Double> tox = perTick(all, 8343);
		Map<String, Double> toxTicks = ticks(all, 8343);
		double toPipe = SetCalc.gain(tox.get("ttk-tbow"), tox.get("ttk-blowpipe"));
		double bowInHand = CombatCheckPlugin.comparedGain(by, toPipe, toxTicks.get("ttk-tbow"), toxTicks.get("ttk-blowpipe"));
		double pipeInHand = CombatCheckPlugin.comparedGain(by, -toPipe, toxTicks.get("ttk-blowpipe"), toxTicks.get("ttk-tbow"));
		assertEquals(toPipe, bowInHand, 0);
		assertFalse(CombatCheckPlugin.pointsAt(bowInHand, minimum));
		assertTrue(CombatCheckPlugin.pointsAt(pipeInHand, minimum));
		assertFalse(CombatCheckPlugin.killsSooner(-toPipe, pipeInHand, minimum));
		// each one by the damage per second, as fought (the waves' share is for the time to kill only), in the three modes:
		// the small Ischyros the Dual macuahuitl, the big the Scythe (three hits on a big one); the Toxobolos the Twisted
		// bow; the small Hagios the Eye of Ayak (6.78 a tick, the Shadow 0.1% under), the big the Shadow
		for (int npc : new int[]{8342, 10774, 10791})
		{
			assertEquals(npc + "", "ttk-dual-macuahuitl", most(perTick(all, npc)));
		}
		for (int npc : new int[]{8345, 10777, 10794})
		{
			assertEquals(npc + "", "ttk-scythe", most(perTick(all, npc)));
		}
		for (int npc : new int[]{8343, 8346, 10775, 10778, 10792, 10795})
		{
			assertEquals(npc + "", "ttk-tbow", most(perTick(all, npc)));
		}
		assertEquals("ttk-eye", most(perTick(all, 8344)));
		assertEquals("ttk-shadow", most(perTick(all, 8347)));
	}
}
