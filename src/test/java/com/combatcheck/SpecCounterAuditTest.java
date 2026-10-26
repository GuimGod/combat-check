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
import java.util.List;
import net.runelite.api.HitsplatID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/**
 * The special attack counter audited (2026-10-19): what each Defence-lowering special attack takes by the OSRS Wiki, in
 * every order they can land in (against the calculator's own function applied one at a time, tools/gen_spec_orders.py),
 * the Tonalztics of Ralos case by case, and the party's special attacks counted once whatever way they are seen (the
 * player's own, a Special Attack Counter message, an estimate from what the client shows).
 */
public class SpecCounterAuditTest
{
	/** Adds one special attack written as in spec_orders.tsv: maul, dwh, ralos2, ralos1, bgs:n, ayak:n, seercull:n... */
	private static void add(DefenceDrains d, String spec, boolean tekton)
	{
		String[] p = spec.split(":");
		int n = p.length > 1 ? Integer.parseInt(p[1]) : 1;
		switch (p[0])
		{
			case "maul":
				d.add(SpecialWeapon.ELDER_MAUL, 1, tekton);
				break;
			case "dwh":
				d.add(SpecialWeapon.DRAGON_WARHAMMER, 1, tekton);
				break;
			case "ralos2":
				d.add(SpecialWeapon.TONALZTICS_OF_RALOS, 2, tekton);
				break;
			case "ralos1":
				d.add(SpecialWeapon.TONALZTICS_OF_RALOS, 1, tekton);
				break;
			case "bgs":
				d.add(SpecialWeapon.BANDOS_GODSWORD, n, tekton);
				break;
			case "ayak":
				d.add(SpecialWeapon.EYE_OF_AYAK, n, tekton);
				break;
			case "seercull":
				d.add(SpecialWeapon.SEERCULL, n, tekton);
				break;
			case "accursed":
				d.add(SpecialWeapon.ACCURSED_SCEPTRE, 1, tekton);
				break;
			case "arclight":
				d.add(SpecialWeapon.ARCLIGHT, 1, tekton);
				break;
			case "emberlight":
				d.add(SpecialWeapon.EMBERLIGHT, 1, tekton);
				break;
			default:
				throw new IllegalArgumentException(spec);
		}
	}

	private static DefenceDrains drains(String... specs)
	{
		DefenceDrains d = new DefenceDrains();
		for (String s : specs)
		{
			add(d, s, false);
		}
		return d;
	}

	private static DefenceDrains.Levels levels(int attack, int strength, int defence, int magic, int ranged, int mdef)
	{
		DefenceDrains.Levels l = new DefenceDrains.Levels();
		l.attack = attack;
		l.strength = strength;
		l.defence = defence;
		l.magic = magic;
		l.ranged = ranged;
		l.magicDefence = mdef;
		return l;
	}

	/** Defence left on a monster of only Defence and Magic known, no floor. */
	private static int defence(int defence, int magic, String... specs)
	{
		return drains(specs).apply(defence, magic, 0, false, 0).defence;
	}

	/**
	 * Every order of the special attacks of spec_orders.tsv (495 orders on nine monsters: Xarpus, Tekton, Sotetseg, Akkha,
	 * Nex, the Maiden, Zulrah, K'ril, the Corporeal Beast), added in that order: the Defence, Magic and magic defence left
	 * are the calculator's, its own function applied one special attack at a time in the same order (the floors included:
	 * Sotetseg 100, Akkha 70, Nex 250).
	 */
	@Test
	public void everyOrderAsTheCalculatorStepByStep() throws IOException
	{
		int rows = 0;
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
			SpecCounterAuditTest.class.getResourceAsStream("spec_orders.tsv"), StandardCharsets.UTF_8)))
		{
			for (String line = r.readLine(); line != null; line = r.readLine())
			{
				if (line.startsWith("#") || line.isEmpty())
				{
					continue;
				}
				String[] c = line.split("\t");
				int npc = Integer.parseInt(c[0]);
				boolean demon = "1".equals(c[2]);
				DefenceDrains.Levels full = levels(Integer.parseInt(c[3]), Integer.parseInt(c[4]), Integer.parseInt(c[5]),
					Integer.parseInt(c[6]), Integer.parseInt(c[7]), Integer.parseInt(c[8]));
				DefenceDrains d = new DefenceDrains();
				for (String s : c[9].split(";"))
				{
					add(d, s, false);
				}
				DefenceDrains.Levels left = d.apply(full, demon, DefenceDrains.floor(npc, full.defence));
				String what = c[1] + " " + c[9];
				assertEquals(what + ": Defence", Integer.parseInt(c[10]), left.defence);
				assertEquals(what + ": Magic", Integer.parseInt(c[11]), left.magic);
				assertEquals(what + ": magic defence", Integer.parseInt(c[12]), left.magicDefence);
				rows++;
			}
		}
		assertEquals(495, rows);
	}

	/**
	 * The examples of the OSRS Wiki's pages, each on the current level: the Dragon warhammer on 75 Defence takes 22 then 15
	 * ("leaving the enemy at 53 ... 38"), the Elder maul 26 then 17 (49, 32).
	 */
	@Test
	public void theWikisExamples()
	{
		assertEquals(53, defence(75, 1, "dwh"));
		assertEquals(38, defence(75, 1, "dwh", "dwh"));
		assertEquals(49, defence(75, 1, "maul"));
		assertEquals(32, defence(75, 1, "maul", "maul"));
		// Accursed sceptre: "a target with level 80 Defence and Magic cannot have their stats drained lower than 68"
		DefenceDrains.Levels a = drains("accursed", "accursed").apply(80, 80, 0, false, 0);
		assertEquals(68, a.defence);
		assertEquals(68, a.magic);
		// and on a Defence lowered already below 68 it takes nothing more
		assertEquals(49, defence(75, 80, "maul", "accursed"));
		// first, then an Elder maul on what it left: 75 -> 63 -> 41
		assertEquals(41, defence(75, 80, "accursed", "maul"));
		// Arclight: 5% of the base level + 1 each, adding up ("Two successful special attacks ... 10% + 2"), after a hammer
		// still a part of the base level
		assertEquals(200 - 2 * (10 + 1), defence(200, 1, "arclight", "arclight"));
		assertEquals(200 * 7 / 10 - (10 + 1), defence(200, 1, "dwh", "arclight"));
		// Emberlight on a demon: 15% + 1 (K'ril's 270: 41)
		DefenceDrains e = drains("emberlight");
		assertEquals(270 - (3 * 270 / 20 + 1), e.apply(levels(340, 300, 270, 200, 1, 80), true, 0).defence);
		assertEquals(270 - (270 / 20 + 1), e.apply(levels(340, 300, 270, 200, 1, 80), false, 0).defence);
	}

	/**
	 * The Bandos godsword: "drains the opponent's combat levels equal to the damage dealt in the following order: Defence,
	 * Strength, Prayer, Attack, Magic, Ranged" (monsters have no Prayer level: the calculator and the plugin skip it), "the
	 * current stat that is being drained must hit 0"; a floor holds its level up and the rest is lost.
	 */
	@Test
	public void theGodswordsOrderOfStats()
	{
		DefenceDrains.Levels l = drains("bgs:80").apply(levels(50, 0, 20, 30, 40, 0), false, 0);
		assertEquals(0, l.defence);
		assertEquals(0, l.strength);
		assertEquals(0, l.attack);
		assertEquals(20, l.magic);
		assertEquals(40, l.ranged);
		// Akkha (floor 70): 500 of damage leaves Defence 70 and nothing else lowered
		DefenceDrains.Levels akkha = drains("bgs:500").apply(levels(100, 140, 80, 100, 100, 10), false,
			DefenceDrains.floor(11789, 80));
		assertEquals(70, akkha.defence);
		assertEquals(140, akkha.strength);
		assertEquals(100, akkha.magic);
	}

	/**
	 * The percentage of the current level against the fixed amount: a godsword's damage before a hammer leaves the Defence
	 * higher than after it, since the hammer then takes a part of more (Xarpus 250: maul, BGS 50 = 113; BGS 50, maul = 130;
	 * maul, DWH, BGS 50 = 65; BGS 50, maul, DWH = 91). Two hammers in either order differ by the rounding only, and a
	 * fixed amount (godsword, Ralos) in either order of two of them is the same.
	 */
	@Test
	public void percentAndFixedInEveryOrder()
	{
		assertEquals(113, defence(250, 220, "maul", "bgs:50"));
		assertEquals(130, defence(250, 220, "bgs:50", "maul"));
		assertEquals(65, defence(250, 220, "maul", "dwh", "bgs:50"));
		assertEquals(64, defence(250, 220, "dwh", "maul", "bgs:50"));
		assertEquals(91, defence(250, 220, "bgs:50", "maul", "dwh"));
		assertEquals(defence(250, 220, "bgs:30", "bgs:20"), defence(250, 220, "bgs:20", "bgs:30"));
		assertEquals(defence(250, 220, "ralos2", "bgs:20"), defence(250, 220, "bgs:20", "ralos2"));
		for (int def = 50; def <= 400; def++)
		{
			int a = defence(def, 1, "maul", "dwh");
			int b = defence(def, 1, "dwh", "maul");
			assertTrue("Defence " + def, Math.abs(a - b) <= 1);
		}
	}

	/** The same special attack again: each on the level the one before left, down to the floor and never past it. */
	@Test
	public void theSameSpecialAttackRepeated()
	{
		int d = 250;
		DefenceDrains maul = new DefenceDrains();
		for (int i = 0; i < 12; i++)
		{
			maul.add(SpecialWeapon.ELDER_MAUL, 1, false);
			d -= d * 35 / 100;
			assertEquals(d, maul.apply(250, 220, 0, false, 0).defence);
		}
		// Nex (260, floor 250), Sotetseg (200, floor 100), the Nightmare (floor 120) in any number of them
		for (int n = 1; n <= 10; n++)
		{
			DefenceDrains m = new DefenceDrains();
			for (int i = 0; i < n; i++)
			{
				m.add(SpecialWeapon.ELDER_MAUL, 1, false);
				m.add(SpecialWeapon.BANDOS_GODSWORD, 70, false);
				m.add(SpecialWeapon.TONALZTICS_OF_RALOS, 2, false);
			}
			assertEquals(250, m.apply(260, 230, 300, false, DefenceDrains.floor(11278, 260)).defence);
			assertTrue(m.apply(200, 250, 30, false, DefenceDrains.floor(8388, 200)).defence >= 100);
			assertTrue(m.apply(150, 150, 120, false, DefenceDrains.floor(9425, 150)).defence >= 120);
		}
	}

	/**
	 * The Tonalztics of Ralos (OSRS Wiki, Tonalztics of Ralos, Special attack; changed on 22 July 2026 from 10% to 12.5%):
	 * each hit that deals damage takes an eighth of the target's Magic level off its Defence, rounded down, on each hit
	 * (two of floor(Magic / 8), not floor(Magic / 4)); the Magic of the moment, lowered before by the Accursed sceptre or the
	 * Seercull; no cap but the monster's floor ("The 50% Defence level reduction cap ... has been removed", 10 April 2024).
	 */
	@Test
	public void theTonalzticsOfRalos()
	{
		// the Maiden (Defence 200, Magic 350): 43 a hit (350 / 8 = 43.75), two hits 86, not 87 (350 / 4)
		assertEquals(157, defence(200, 350, "ralos1"));
		assertEquals(114, defence(200, 350, "ralos2"));
		// an eighth, not a tenth: Xarpus's Magic 220 takes 27 a hit (a tenth would be 22)
		assertEquals(250 - 27, defence(250, 220, "ralos1"));
		// the Accursed sceptre first: Magic 350 -> 297, the Ralos then 37 a hit (Defence 200 -> 170 -> 96)
		DefenceDrains.Levels a = drains("accursed", "ralos2").apply(200, 350, 0, false, 0);
		assertEquals(297, a.magic);
		assertEquals(170 - 2 * (297 / 8), a.defence);
		// the Seercull first (40 off Magic): 38 a hit
		assertEquals(200 - 2 * (310 / 8), defence(200, 350, "seercull:40", "ralos2"));
		// a godsword lowers Magic only past a Defence at 0 (or never, on a floor): it cannot change the Ralos's drain on
		// Defence (the Maiden: Ralos then 1100 of godsword, or the godsword then the Ralos, both leave Defence 0)
		assertEquals(0, defence(200, 350, "ralos2", "bgs:1100"));
		assertEquals(0, defence(200, 350, "bgs:1100", "ralos2"));
		// a fixed amount: the Ralos before a hammer leaves more Defence than after it (Maiden 75 against 44)
		assertEquals(75, drains("ralos2", "maul").apply(levels(350, 350, 200, 350, 350, 0), false, 0).defence);
		assertEquals(44, drains("maul", "ralos2").apply(levels(350, 350, 200, 350, 350, 0), false, 0).defence);
		// the floor: Sotetseg (200, Magic 250, floor 100) after three of them
		DefenceDrains s = drains("ralos2", "ralos2", "ralos2");
		assertEquals(100, s.apply(200, 250, 30, false, DefenceDrains.floor(8388, 200)).defence);
	}

	/**
	 * The Ralos's hits that count: a hit that deals no damage lowers nothing ("requires a specific check for damage to be
	 * dealt", Mod Ash, 21 July 2026), so a hitsplat of 0 is not counted; on Tekton, "immune to Range", every hit is 0 and
	 * nothing is lowered (his 5% on a miss is the Elder maul's and the Dragon warhammer's only); the charged weapon hits
	 * twice, the uncharged once ("Uncharged, it will only hit once"), and a message of more is two.
	 */
	@Test
	public void theRalossHitsThatCount()
	{
		assertEquals(Integer.valueOf(2), CombatCheckPlugin.landedHits(Arrays.asList(31, 12), 2));
		assertEquals(Integer.valueOf(1), CombatCheckPlugin.landedHits(Arrays.asList(31, 0), 2));
		assertEquals(Integer.valueOf(0), CombatCheckPlugin.landedHits(Arrays.asList(0, 0), 2));
		// a thrall's hitsplat first on the same tick: the weapon's two come last
		assertEquals(Integer.valueOf(1), CombatCheckPlugin.landedHits(Arrays.asList(4, 0, 25), 2));
		assertNull(CombatCheckPlugin.landedHits(Collections.singletonList(25), 2));
		assertEquals(2, CombatCheckPlugin.specialHits(ItemID.TONALZTICS_OF_RALOS_CHARGED));
		assertEquals(1, CombatCheckPlugin.specialHits(ItemID.TONALZTICS_OF_RALOS_UNCHARGED));
		assertEquals(SpecialWeapon.TONALZTICS_OF_RALOS, CombatCheckPlugin.specialWeapon(ItemID.TONALZTICS_OF_RALOS_UNCHARGED));
		DefenceDrains none = new DefenceDrains();
		none.add(SpecialWeapon.TONALZTICS_OF_RALOS, 0, false);
		none.add(SpecialWeapon.TONALZTICS_OF_RALOS, 0, true);
		assertTrue(none.none());
		DefenceDrains more = new DefenceDrains();
		more.add(SpecialWeapon.TONALZTICS_OF_RALOS, 5, false);
		assertEquals(defence(200, 350, "ralos2"), more.apply(200, 350, 0, false, 0).defence);
	}

	/**
	 * What each special attack takes when it misses (a 0): nothing, except on Tekton, where an Elder maul or Dragon
	 * warhammer takes 5% and a godsword 10 levels (OSRS Wiki, Tekton); the Arclight, Accursed sceptre, Seercull and Eye of
	 * Ayak nothing anywhere.
	 */
	@Test
	public void missesLowerNothingButOnTekton()
	{
		for (SpecialWeapon w : new SpecialWeapon[]{SpecialWeapon.ELDER_MAUL, SpecialWeapon.DRAGON_WARHAMMER,
			SpecialWeapon.BANDOS_GODSWORD, SpecialWeapon.ARCLIGHT, SpecialWeapon.EMBERLIGHT, SpecialWeapon.ACCURSED_SCEPTRE,
			SpecialWeapon.SEERCULL, SpecialWeapon.EYE_OF_AYAK, SpecialWeapon.TONALZTICS_OF_RALOS})
		{
			DefenceDrains d = new DefenceDrains();
			d.add(w, 0, false);
			assertTrue(w.name(), d.none());
		}
		DefenceDrains t = new DefenceDrains();
		t.add(SpecialWeapon.ELDER_MAUL, 0, true);
		assertEquals(205 - 205 * 5 / 100, t.apply(205, 205, 0, false, 0).defence);
		t.add(SpecialWeapon.BANDOS_GODSWORD, 0, true);
		assertEquals(195 - 10, t.apply(205, 205, 0, false, 0).defence);
	}

	// the party: the same special attack seen more than one way is counted once

	private static final Object NPC = new Object();
	private static final int INDEX = 5;

	/** The steps of CombatCheckPlugin.onSpecialCounterUpdate for a message received on tick {@code now}. */
	private static void message(SpecEstimate e, DrainTracker drains, int player, SpecialWeapon w, int hit, int now)
	{
		e.partySpec(player);
		drains.dropEstimated(INDEX, NPC, player, w, now - CombatCheckPlugin.ESTIMATE_REPLACED_TICKS);
		drains.add(INDEX, NPC, w, hit, false, CombatCheckPlugin.partyHitTick(w, now));
	}

	/** The steps of CombatCheckPlugin.countEstimates on tick {@code now}. */
	private static void estimates(SpecEstimate e, DrainTracker drains, int now)
	{
		for (SpecEstimate.Landed l : e.resolve(now))
		{
			drains.addEstimated(l.npcIndex, l.npc, l.weapon, l.hit, l.tekton, l.tick, l.playerId);
		}
	}

	/** An Elder maul of player {@code p} starting on tick {@code t}, seen aiming at the monster. */
	private static boolean maul(SpecEstimate e, int t, int p)
	{
		boolean seen = e.seen(t, p, false, 11124, 0, ItemID.ELDER_MAUL, INDEX, NPC, false);
		e.endTick(Collections.singleton(p));
		return seen;
	}

	/** Xarpus's Defence 250 after what was counted. */
	private static int xarpus(DrainTracker drains)
	{
		DefenceDrains d = drains.of(INDEX, NPC);
		return d == null ? 250 : d.apply(250, 220, 0, false, 0).defence;
	}

	/** A party member's Elder maul estimated, then the message: one maul. */
	@Test
	public void theMessageAfterTheEstimate()
	{
		SpecEstimate e = new SpecEstimate();
		DrainTracker drains = new DrainTracker();
		assertTrue(maul(e, 100, 7));
		e.hitsplat(102, NPC, 40, HitsplatID.DAMAGE_OTHER, false);
		estimates(e, drains, 102);
		assertEquals(163, xarpus(drains));
		assertTrue(drains.estimated(INDEX, NPC));
		message(e, drains, 7, SpecialWeapon.ELDER_MAUL, 1, 103);
		assertEquals(163, xarpus(drains));
		assertEquals(1, drains.counted(INDEX, NPC));
		assertFalse(drains.estimated(INDEX, NPC));
	}

	/**
	 * The message before the estimate is decided (the Special Attack Counter sends an Elder maul's on the tick of the
	 * attack, before its hit): nothing is estimated, one maul; and the player is never estimated again.
	 */
	@Test
	public void theMessageBeforeTheEstimate()
	{
		SpecEstimate e = new SpecEstimate();
		DrainTracker drains = new DrainTracker();
		assertTrue(maul(e, 100, 7));
		message(e, drains, 7, SpecialWeapon.ELDER_MAUL, 1, 101);
		e.hitsplat(102, NPC, 40, HitsplatID.DAMAGE_OTHER, false);
		estimates(e, drains, 102);
		assertEquals(1, drains.counted(INDEX, NPC));
		assertEquals(163, xarpus(drains));
		assertFalse(maul(e, 110, 7));
		message(e, drains, 7, SpecialWeapon.ELDER_MAUL, 1, 111);
		assertEquals(2, drains.counted(INDEX, NPC));
		assertEquals(163 - 163 * 35 / 100, xarpus(drains));
	}

	/**
	 * A party member who leaves the party (CombatCheckPlugin.onUserPart, onPartyChanged) is estimated again: their special
	 * attacks no longer come through it.
	 */
	@Test
	public void aMemberWhoLeavesThePartyIsEstimatedAgain()
	{
		SpecEstimate e = new SpecEstimate();
		DrainTracker drains = new DrainTracker();
		message(e, drains, 7, SpecialWeapon.ELDER_MAUL, 1, 90);
		assertFalse(maul(e, 100, 7));
		e.notSender(7);
		assertTrue(maul(e, 110, 7));
		e.hitsplat(112, NPC, 40, HitsplatID.DAMAGE_OTHER, false);
		estimates(e, drains, 112);
		assertEquals(2, drains.counted(INDEX, NPC));
		message(e, drains, 8, SpecialWeapon.ELDER_MAUL, 1, 120);
		e.noSenders();
		assertTrue(maul(e, 130, 8));
	}

	/** A player outside the party: each of their special attacks once, the same animation in one attack once. */
	@Test
	public void aPlayerOutsideTheParty()
	{
		SpecEstimate e = new SpecEstimate();
		DrainTracker drains = new DrainTracker();
		assertTrue(maul(e, 100, 9));
		// the animation seen again two ticks later: the same attack
		e.animationSet(9);
		assertFalse(e.seen(102, 9, false, 11124, 0, ItemID.ELDER_MAUL, INDEX, NPC, false));
		e.hitsplat(102, NPC, 51, HitsplatID.DAMAGE_OTHER, false);
		estimates(e, drains, 102);
		assertEquals(1, drains.counted(INDEX, NPC));
		// the next one, a weapon's attack later
		assertTrue(maul(e, 106, 9));
		e.hitsplat(108, NPC, 44, HitsplatID.DAMAGE_OTHER, false);
		estimates(e, drains, 108);
		assertEquals(2, drains.counted(INDEX, NPC));
		assertEquals(163 - 163 * 35 / 100, xarpus(drains));
	}

	/** A miss (every hitsplat 0) lowers nothing off Tekton; one of two hitsplats 0 cannot be told and is not counted. */
	@Test
	public void aMissAndADoubt()
	{
		SpecEstimate e = new SpecEstimate();
		DrainTracker drains = new DrainTracker();
		assertTrue(maul(e, 100, 9));
		e.hitsplat(102, NPC, 0, HitsplatID.BLOCK_OTHER, false);
		estimates(e, drains, 102);
		assertNull(drains.of(INDEX, NPC));
		assertTrue(maul(e, 110, 9));
		e.hitsplat(112, NPC, 0, HitsplatID.BLOCK_OTHER, false);
		e.hitsplat(112, NPC, 30, HitsplatID.DAMAGE_OTHER, false);
		estimates(e, drains, 112);
		assertNull(drains.of(INDEX, NPC));
		// a party member's miss: counted in the number of specs, lowering nothing
		message(e, drains, 7, SpecialWeapon.DRAGON_WARHAMMER, 0, 120);
		assertEquals(1, drains.counted(INDEX, NPC));
		assertNull(drains.of(INDEX, NPC));
	}

	/** The player's own special attack is never estimated (it is counted when it lands, as the Special Attack Counter). */
	@Test
	public void theOwnIsNeverEstimated()
	{
		SpecEstimate e = new SpecEstimate();
		assertFalse(e.seen(100, 1, true, 11124, 0, ItemID.ELDER_MAUL, INDEX, NPC, false));
		e.hitsplat(102, NPC, 40, HitsplatID.DAMAGE_OTHER, false);
		assertTrue(e.resolve(102).isEmpty());
	}

	/**
	 * In the order the hits landed: an Elder maul's message comes on the tick of the attack, 2 ticks before its hit, so a
	 * godsword landing in between is counted first (Xarpus: godsword 50 then maul = 130, not 113); one landing after it,
	 * after it. The player's own Elder maul goes by the tick of its hit too (CombatCheckPlugin.landSpecialAttack).
	 */
	@Test
	public void theOrderTheHitsLanded()
	{
		assertEquals(102, CombatCheckPlugin.partyHitTick(SpecialWeapon.ELDER_MAUL, 100));
		assertEquals(101, CombatCheckPlugin.partyHitTick(SpecialWeapon.BANDOS_GODSWORD, 101));
		SpecEstimate e = new SpecEstimate();
		DrainTracker between = new DrainTracker();
		// maul attack on 100 (message 100, hit 102), godsword hit on 101
		message(e, between, 7, SpecialWeapon.ELDER_MAUL, 1, 100);
		message(e, between, 8, SpecialWeapon.BANDOS_GODSWORD, 50, 101);
		assertEquals(130, xarpus(between));
		DrainTracker after = new DrainTracker();
		message(e, after, 7, SpecialWeapon.ELDER_MAUL, 1, 100);
		message(e, after, 8, SpecialWeapon.BANDOS_GODSWORD, 50, 103);
		assertEquals(113, xarpus(after));
		// two on the same tick: in the order they came (the game's order on a tick is the players' processing order, which
		// the client does not show); two hammers differ by the rounding only
		DrainTracker same = new DrainTracker();
		message(e, same, 7, SpecialWeapon.DRAGON_WARHAMMER, 1, 101);
		message(e, same, 8, SpecialWeapon.ELDER_MAUL, 1, 99);
		assertEquals(2, same.counted(INDEX, NPC));
		assertEquals(defence(250, 220, "dwh", "maul"), xarpus(same));
	}

	/**
	 * The levels a monster wins back with time go by the ticks the tracker counted each special attack on, whatever order
	 * they came in (StatRestoreTest): an Elder maul on tick 0 and a Dragon warhammer on 300 on General Graardor (250) leave
	 * 117 then, 118 a hundred ticks later.
	 */
	@Test
	public void theLevelsBackGoByTheTicksCounted()
	{
		DrainTracker t = new DrainTracker();
		t.add(INDEX, NPC, SpecialWeapon.DRAGON_WARHAMMER, 1, false, 300);
		t.add(INDEX, NPC, SpecialWeapon.ELDER_MAUL, 1, false, 0);
		DefenceDrains.Levels full = levels(280, 350, 250, 80, 350, 298);
		assertEquals(117, t.of(INDEX, NPC).apply(full, false, 0, 2215, t.ticksSince(INDEX, NPC, 300)).defence);
		assertEquals(118, t.of(INDEX, NPC).apply(full, false, 0, 2215, t.ticksSince(INDEX, NPC, 400)).defence);
	}

	/** What gives the Defence back: the monster's death or change (forget), Sotetseg's maze, Kephri's last phase, the Bloat's stomp. */
	@Test
	public void whatGivesTheDefenceBack()
	{
		DrainTracker drains = new DrainTracker();
		drains.add(INDEX, NPC, SpecialWeapon.ELDER_MAUL, 1, false, 100);
		assertEquals(163, xarpus(drains));
		drains.forget(INDEX, NPC);
		assertEquals(250, xarpus(drains));
		// a special attack after the reset counts from the full level
		drains.add(INDEX, NPC, SpecialWeapon.DRAGON_WARHAMMER, 1, false, 200);
		assertEquals(175, xarpus(drains));
		// Sotetseg into the maze in every mode, and only that way; the Bloat's stomp 29 ticks after it shuts down
		assertTrue(DefenceDrains.resets(8388, 8387));
		assertTrue(DefenceDrains.resets(10865, 10864));
		assertTrue(DefenceDrains.resets(10868, 10867));
		assertFalse(DefenceDrains.resets(8387, 8388));
		// Kephri's last phase (OSRS Wiki, Kephri: "the defence level is reset for the final phase"), from her shield or
		// weakened forms, not the other way
		assertTrue(DefenceDrains.resets(11719, 11721));
		assertTrue(DefenceDrains.resets(11720, 11721));
		assertFalse(DefenceDrains.resets(11719, 11720));
		assertFalse(DefenceDrains.resets(11721, 11719));
		assertTrue(DefenceDrains.bloat(8359));
		assertFalse(DefenceDrains.bloatStomps(1000, 1028));
		assertTrue(DefenceDrains.bloatStomps(1000, 1029));
	}
}
