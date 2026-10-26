/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The Fractured Archive (Raids 4): the bosses' rows, which the calculator's data does not have yet
 * (tools/extra_monsters.json, archive_sets.tsv from tools/gen_archive.py), the drains across the Archive Guardian's
 * forms, "Def ?" where the Defence is not known (Ocellus's next phase, Ezekiel after a special attack, Dreadnoth's
 * Jellies, Bork stunned), Bork's stun, the hitpoints from the boss health bar; and the line of the best weapon in the
 * bank (hard cases 1 to 6 and 10 to 13).
 */
public class FracturedArchiveTest
{
	private static final String ARCHIVE = "archive_sets.tsv";

	private static Map<String, SetCalcTest.Case> on(int npc) throws IOException
	{
		Map<String, SetCalcTest.Case> cases = new HashMap<>();
		for (SetCalcTest.Case k : SetCalcTest.cases(ARCHIVE))
		{
			if (k.npc == npc)
			{
				cases.put(k.set, k);
			}
		}
		assertTrue("cases on " + npc, !cases.isEmpty());
		return cases;
	}

	/** {@code better} deals more than {@code worse} by the calculator and by the plugin. */
	private static void first(Map<String, SetCalcTest.Case> cases, String better, String worse)
	{
		SetCalcTest.Case a = cases.get(better);
		SetCalcTest.Case b = cases.get(worse);
		assertNotNull(better, a);
		assertNotNull(worse, b);
		assertTrue("calculator: " + better + " over " + worse + " on " + a.npc, a.expected > b.expected);
		assertTrue("plugin: " + better + " over " + worse + " on " + a.npc, a.got > b.got);
	}

	/**
	 * The rows: the wiki's numbers checked with in-game examines. The Archive Guardian with the
	 * opening's Defence 260 and Magic 250 in both forms (the ground's own defence bonuses and fire weakness 40%), Ezekiel
	 * as first examined (Defence 140), Bork with the wiki's Defence 300 and the examine's flat armour 8, the Smorks with
	 * flat armour 2, Dreadnoth a demon 25% more vulnerable to demonbane (the examine), Ocellus in his three phases.
	 */
	@Test
	public void theBossesRowsAreTheConfirmedNumbers() throws IOException
	{
		StyleData data = new StyleData();
		StyleData.Monster flying = data.monster(16659);
		StyleData.Monster ground = data.monster(16660);
		for (StyleData.Monster m : new StyleData.Monster[]{flying, ground})
		{
			assertEquals("Archive Guardian", m.name);
			assertEquals(260, m.level);
			assertEquals(250, m.magicLevel);
			assertTrue(m.fracturedArchive);
			assertEquals("fire", m.weakness);
		}
		assertArrayEquals(new int[]{120, 110, 20, 130, 75, 120, 50, 80},
			new int[]{flying.stab, flying.slash, flying.crush, flying.magic, flying.light, flying.standard, flying.heavy,
				flying.weaknessSeverity});
		assertArrayEquals(new int[]{120, 90, 20, 130, 50, 115, 50, 40},
			new int[]{ground.stab, ground.slash, ground.crush, ground.magic, ground.light, ground.standard, ground.heavy,
				ground.weaknessSeverity});
		assertEquals("fa", flying.immune);
		assertEquals("a", ground.immune);
		StyleData.Monster ezekiel = data.monster(16652);
		assertEquals(140, ezekiel.level);
		assertEquals(200, ezekiel.magicLevel);
		StyleData.Monster bork = data.monster(16640);
		assertEquals(300, bork.level);
		assertEquals(8, bork.flatArmour);
		assertEquals(220, data.monster(16641).level);
		assertEquals(2, data.monster(16641).flatArmour);
		StyleData.Monster dreadnoth = data.monster(16665);
		assertEquals(125, dreadnoth.level);
		assertTrue(dreadnoth.demon);
		assertEquals(125, dreadnoth.demonbane);
		assertTrue(data.monster(16684).demon);
		assertEquals(195, data.monster(16667).level);
		assertNull("Moia's weakness is not confirmed", data.monster(16667).weakness);
		for (int id : new int[]{16673, 16674})
		{
			assertEquals(215, data.monster(id).level);
			assertTrue(data.monster(id).demon);
		}
		assertEquals(250, data.monster(16675).level);
		assertFalse(data.monster(16675).demon);
		assertEquals(250, data.monster(16677).level);
		assertTrue(data.monster(16677).demon);
		// the Dagon'hai have no numbers: no row
		for (int id : new int[]{16643, 16644, 16645, 16646, 16647, 16648})
		{
			assertNull(data.monster(id));
		}
		// a monster outside the raid is not one of it
		assertFalse(data.monster(8061).fracturedArchive);
	}

	/**
	 * Hard case 3: Dreadnoth's Jellies (16666) are not taken by the name for the Slayer Jelly; they have no row
	 * at all, so no weapon is pointed at on them, and the plugin shows "Def ?" (unknownDefenceText).
	 */
	@Test
	public void dreadnothsJellyIsNotTheSlayerJelly() throws IOException
	{
		StyleData data = new StyleData();
		assertNull(data.monster(16666, "Jelly"));
		assertTrue(data.unmodelled(16666));
		// another id of a Jelly is still the Slayer one, by the name
		StyleData.Monster slayer = data.monster(-12345, "Jelly");
		assertNotNull(slayer);
		assertEquals(120, slayer.level);
		assertEquals("Def ?", CombatCheckPlugin.unknownDefenceText(CombatCheckConfig.Language.ENGLISH, 0));
	}

	/** Every case within 0.1% of the calculator run on the same numbers, and the order of the weapons. */
	@Test
	public void theFiguresAreTheCalculators() throws IOException
	{
		assertEquals(95, SetCalcTest.figuresOf(ARCHIVE).size());
		Map<String, SetCalcTest.Case> inFlight = on(16659);
		first(inFlight, "tbow", "blowpipe");
		first(inFlight, "harmonised-fire-surge", "harmonised-earth-surge");
		first(inFlight, "noxious-halberd", "scythe");
		assertEquals(0, inFlight.get("scythe").got, 0);
		Map<String, SetCalcTest.Case> landed = on(16660);
		first(landed, "scythe", "fang");
		first(landed, "inquisitors-mace-in-inquisitor", "inquisitors-mace");
		Map<String, SetCalcTest.Case> ezekiel = on(16652);
		first(ezekiel, "harmonised-earth-surge", "harmonised-fire-surge");
		Map<String, SetCalcTest.Case> bork = on(16640);
		first(bork, "inquisitors-mace", "scythe");
		// Dreadnoth: demonbane by the examine, the Emberlight first (12.8 per second)
		Map<String, SetCalcTest.Case> dreadnoth = on(16665);
		first(dreadnoth, "emberlight", "scythe");
		assertEquals(12.83, dreadnoth.get("emberlight").got / 0.6, 0.01);
		first(on(16684), "emberlight", "scythe");
		first(on(16677), "emberlight", "scythe");
	}

	/**
	 * Hard case 1: the Archive Guardian's forms keep the drains (the same Defence on both rows; measured, the
	 * Defence does not come back when it lands). Two special attacks of the Tonalztics, three hits landed (Magic 250 / 8 = 31 each) take 260 to 167, the
	 * examine of 2026-10-20 08:37, in flight and on the ground alike.
	 */
	@Test
	public void theArchiveGuardiansDrainsGoOnAcrossItsForms() throws IOException
	{
		StyleData data = new StyleData();
		StyleData.Monster flying = data.monster(16659);
		StyleData.Monster ground = data.monster(16660);
		assertEquals(CombatCheckPlugin.FormChange.KEEP, CombatCheckPlugin.formChange(flying, ground, 16659, 16660));
		assertEquals(CombatCheckPlugin.FormChange.KEEP, CombatCheckPlugin.formChange(ground, flying, 16660, 16659));
		DrainTracker drains = new DrainTracker();
		Object npc = new Object();
		drains.add(7, npc, SpecialWeapon.TONALZTICS_OF_RALOS, 2, false, 100);
		drains.add(7, npc, SpecialWeapon.TONALZTICS_OF_RALOS, 1, false, 106);
		DefenceDrains.Levels inFlight = DefenceDrains.left(flying, 16659, MonsterLevels.scaled(flying, null, null, null),
			drains.of(7, npc), 0);
		assertEquals(167, inFlight.defence);
		// it lands: the same NPC, its drains kept
		DefenceDrains.Levels onGround = DefenceDrains.left(ground, 16660, MonsterLevels.scaled(ground, null, null, null),
			drains.of(7, npc), 0);
		assertEquals(167, onGround.defence);
		// a change into a form of another Defence still drops them (Ocellus aside)
		assertEquals(CombatCheckPlugin.FormChange.FORGET,
			CombatCheckPlugin.formChange(flying, data.monster(16640), 16659, 16640));
		assertEquals(CombatCheckPlugin.FormChange.FORGET, CombatCheckPlugin.formChange(flying, null, 16659, 16658));
	}

	/**
	 * Hard case 2: Ocellus's next phase keeps the drains, but how is not known ("Defence reduction works on all
	 * phases", OSRS Wiki): after a drain in the first phase, the second and third show "Def ?"; the special attacks of the
	 * new phase lower the figures from its full Defence. Within the first phase (16673, 16674) the drains go on.
	 */
	@Test
	public void ocellusNextPhaseShowsTheDefenceAsNotKnown() throws IOException
	{
		StyleData data = new StyleData();
		assertEquals(CombatCheckPlugin.FormChange.UNKNOWN,
			CombatCheckPlugin.formChange(data.monster(16674), data.monster(16675), 16674, 16675));
		assertEquals(CombatCheckPlugin.FormChange.UNKNOWN,
			CombatCheckPlugin.formChange(data.monster(16675), data.monster(16677), 16675, 16677));
		assertEquals(CombatCheckPlugin.FormChange.UNKNOWN,
			CombatCheckPlugin.formChange(data.monster(16674), data.monster(16677), 16674, 16677));
		assertEquals(CombatCheckPlugin.FormChange.KEEP,
			CombatCheckPlugin.formChange(data.monster(16673), data.monster(16674), 16673, 16674));
		DrainTracker drains = new DrainTracker();
		Object npc = new Object();
		drains.add(3, npc, SpecialWeapon.DRAGON_WARHAMMER, 40, false, 10);
		drains.add(3, npc, SpecialWeapon.DRAGON_WARHAMMER, 45, false, 12);
		assertFalse(drains.unknown(3, npc));
		drains.carryUnknown(3, npc);
		assertTrue(drains.unknown(3, npc));
		assertEquals(2, drains.counted(3, npc));
		assertNull("the figures no longer count the first phase's drains", drains.of(3, npc));
		assertTrue(CombatCheckPlugin.defenceUnknown(drains.unknown(3, npc), false, false, 2));
		assertEquals("Def ? (2 specs)", CombatCheckPlugin.unknownDefenceText(CombatCheckConfig.Language.ENGLISH, 2));
		// a special attack in the third phase: counted from its full Defence, the line still "?"
		drains.add(3, npc, SpecialWeapon.DRAGON_WARHAMMER, 30, false, 200);
		StyleData.Monster third = data.monster(16677);
		assertEquals(175, DefenceDrains.left(third, 16677, MonsterLevels.scaled(third, null, null, null), drains.of(3, npc), 0)
			.defence);
		assertEquals(3, drains.counted(3, npc));
		assertTrue(drains.unknown(3, npc));
		// no drain before the change: nothing is unknown
		DrainTracker none = new DrainTracker();
		none.carryUnknown(4, npc);
		assertFalse(none.unknown(4, npc));
	}

	/**
	 * Hard case 4: no drain is confirmed on Ezekiel (an examine read Defence 159 after two Dragon warhammers):
	 * the figures keep his full Defence and, once a special attack is counted on him, the line says "Def ?".
	 */
	@Test
	public void ezekielShowsTheDefenceAsNotKnownAfterASpecialAttack() throws IOException
	{
		StyleData data = new StyleData();
		StyleData.Monster ezekiel = data.monster(16652);
		assertTrue(DefenceDrains.immune(16652));
		assertTrue(DefenceDrains.notConfirmed(16652));
		DefenceDrains drains = new DefenceDrains();
		drains.add(SpecialWeapon.DRAGON_WARHAMMER, 40, false, 0);
		assertEquals(140, DefenceDrains.left(ezekiel, 16652, MonsterLevels.scaled(ezekiel, null, null, null), drains, 0).defence);
		assertTrue(CombatCheckPlugin.defenceUnknown(false, DefenceDrains.notConfirmed(16652), false, 1));
		assertFalse(CombatCheckPlugin.defenceUnknown(false, DefenceDrains.notConfirmed(16652), false, 0));
		assertFalse(DefenceDrains.notConfirmed(16640));
		assertEquals("Def ? (1 spec)", CombatCheckPlugin.unknownDefenceText(CombatCheckConfig.Language.ENGLISH, 1));
	}

	/**
	 * Hard case 5: Bork's stun starts with its animation (14545) or his "Oof." and ends with the stomps' (14549)
	 * or "Bork very angry!!", and never lasts more than the 14 ticks measured; while it lasts his flat armour is 0 (wiki)
	 * and his Defence is "Def ?". Outside it his flat armour is the examine's 8.
	 */
	@Test
	public void borksStunIsReadFromTheGame() throws IOException
	{
		BorkStun stun = new BorkStun();
		Object bork = new Object();
		assertFalse(stun.stunned(bork, 100));
		stun.animation(bork, BorkStun.BORK, BorkStun.STUNNED, 100);
		assertTrue(stun.stunned(bork, 100));
		assertTrue(stun.stunned(bork, 114));
		assertFalse("never longer than measured", stun.stunned(bork, 115));
		stun.says(bork, BorkStun.BORK, "Oof.", 200);
		assertTrue(stun.stunned(bork, 205));
		stun.animation(bork, BorkStun.BORK, BorkStun.STOMP, 213);
		assertFalse(stun.stunned(bork, 213));
		stun.says(bork, BorkStun.BORK, "Oof.", 300);
		stun.says(bork, BorkStun.BORK, "Bork very angry!!", 313);
		assertFalse(stun.stunned(bork, 313));
		// another monster's animation of the same number, or the Smorks' words, change nothing
		Object smorks = new Object();
		stun.animation(smorks, 16641, BorkStun.STUNNED, 400);
		stun.says(smorks, 16641, "Oof.", 400);
		assertFalse(stun.stunned(smorks, 401));
		assertTrue(CombatCheckPlugin.defenceUnknown(false, false, true, 0));

		// the flat armour: 8 off every hit that lands outside the stun, nothing in it
		StyleData data = new StyleData();
		SetCalc.Target out = data.monster(16640).target();
		assertEquals(8, out.flatArmour);
		SetCalc.Target in = data.monster(16640).target();
		in.flatArmour = 0;
		SetCalc.Loadout blowpipe = new SetCalc.Loadout();
		blowpipe.weaponName = "Toxic blowpipe";
		blowpipe.category = "Thrown";
		blowpipe.speed = 2;
		blowpipe.ranged = 175;
		blowpipe.rangedStrength = 75;
		blowpipe.dartKnown = true;
		blowpipe.styles = data.styles(12926);
		SetCalc.Player p = new SetCalc.Player();
		p.ranged = 112;
		p.prayerRanged = 120;
		p.prayerRangedStrength = 123;
		assertTrue(SetCalc.perTick(blowpipe, p, in, false) > SetCalc.perTick(blowpipe, p, out, false) * 1.2);
	}

	/**
	 * Hard case 6: the hitpoints of a boss of the raid are the most its boss health bar shows, which the game
	 * scales for the party (Bork in a trio: 2180); with no bar of its own (the "null" bar of another NPC is not read: the
	 * plugin reads it only when it names the monster fought), the row's are left and the time to kill and the bolts have
	 * no figure. A monster outside the raid never takes them from the bar.
	 */
	@Test
	public void theRaidsHitpointsComeFromTheBossBar() throws IOException
	{
		StyleData data = new StyleData();
		StyleData.Monster bork = data.monster(16640);
		DefenceDrains.Levels full = MonsterLevels.scaled(bork, null, null, null);
		assertEquals(1500, full.hitpoints);
		assertEquals(2180, MonsterLevels.fromBar(bork, full, 2180).hitpoints);
		assertEquals(1500, MonsterLevels.fromBar(bork, full, 0).hitpoints);
		StyleData.Monster vorkath = data.monster(8061);
		DefenceDrains.Levels v = MonsterLevels.scaled(vorkath, null, null, null);
		assertEquals(v.hitpoints, MonsterLevels.fromBar(vorkath, v, 9999).hitpoints);
		// the levels are left as they are
		assertEquals(300, MonsterLevels.fromBar(bork, full, 2180).defence);
	}

	/** Hard case 12: the line of the bank's weapon is never shown inside a raid, or on a raid's monster. */
	@Test
	public void theBankLineIsNotShownInARaid() throws IOException
	{
		StyleData data = new StyleData();
		StyleData.Monster vorkath = data.monster(8061);
		assertTrue(BankHint.allowed(false, 0, false, 0, vorkath));
		assertTrue("in a party of the Theatre, outside it", BankHint.allowed(false, 1, false, 0, vorkath));
		assertFalse("the Chambers", BankHint.allowed(true, 0, false, 0, vorkath));
		assertFalse("the Theatre", BankHint.allowed(false, 2, false, 0, vorkath));
		assertFalse("the Tombs", BankHint.allowed(false, 0, true, 0, vorkath));
		assertFalse("the Fractured Archive", BankHint.allowed(false, 0, false, 3, vorkath));
		assertFalse("a boss of the Archive", BankHint.allowed(false, 0, false, 0, data.monster(16640)));
		for (StyleData.Monster m : data.monsters())
		{
			if (m.coxScaling != 0 || m.tobScaling != 0 || m.toa || m.fracturedArchive)
			{
				assertFalse(m.name + " " + m.id, BankHint.allowed(false, 0, false, 0, m));
			}
		}
	}

	private static SwitchSet.Piece piece(int id, String name, int slot, boolean twoHanded)
	{
		SwitchSet.Piece p = new SwitchSet.Piece();
		p.itemId = id;
		p.name = name;
		p.slot = slot;
		p.twoHanded = twoHanded;
		return p;
	}

	/**
	 * Hard case 10: a weapon of two hands in the bank is counted without the shield worn; one of one hand keeps
	 * it; nothing else worn changes.
	 */
	@Test
	public void aTwoHandedWeaponOfTheBankTakesTheShieldOff()
	{
		SwitchSet.Piece[] worn = new SwitchSet.Piece[SwitchSet.SLOTS];
		worn[SwitchSet.WEAPON] = piece(24417, "Inquisitor's mace", SwitchSet.WEAPON, false);
		worn[SwitchSet.SHIELD] = piece(22322, "Avernic defender", SwitchSet.SHIELD, false);
		worn[SwitchSet.AMMO] = piece(11212, "Dragon arrow", SwitchSet.AMMO, false);
		SwitchSet.Piece[] scythe = BankHint.withWeapon(worn, piece(22325, "Scythe of vitur", SwitchSet.WEAPON, true));
		assertEquals(22325, scythe[SwitchSet.WEAPON].itemId);
		assertNull(scythe[SwitchSet.SHIELD]);
		assertSame(worn[SwitchSet.AMMO], scythe[SwitchSet.AMMO]);
		assertNotNull("what is worn is left as it is", worn[SwitchSet.SHIELD]);
		SwitchSet.Piece[] fang = BankHint.withWeapon(worn, piece(26219, "Osmumten's fang", SwitchSet.WEAPON, false));
		assertSame(worn[SwitchSet.SHIELD], fang[SwitchSet.SHIELD]);
	}

	/**
	 * Hard case 11: a weapon without its charges is left out (the Scythe of vitur too), a bow without arrows it fires
	 * on is left out, and another dress of a weapon is the weapon itself.
	 */
	@Test
	public void theBankSkipsWeaponsThatCannotAttackAsTheyAre()
	{
		assertTrue(BankHint.usable("Scythe of vitur"));
		assertFalse(BankHint.usable("Scythe of vitur (uncharged)"));
		assertFalse(BankHint.usable("Tumeken's shadow (uncharged)"));
		assertFalse(BankHint.usable(null));
		assertTrue("a melee weapon needs no ammunition", BankHint.firesWhatIsOn(22325, "Scythe", "Scythe of vitur", null));
		assertFalse("no arrows on", BankHint.firesWhatIsOn(20997, "Bow", "Twisted bow", null));
		assertTrue(BankHint.firesWhatIsOn(20997, "Bow", "Twisted bow", piece(11212, "Dragon arrow", SwitchSet.AMMO, false)));
		assertFalse("bolts in a bow", BankHint.firesWhatIsOn(20997, "Bow", "Twisted bow",
			piece(21905, "Dragon bolts", SwitchSet.AMMO, false)));
		// the Armadyl godsword (or) is the Armadyl godsword
		assertEquals(11802, Variants.base(20368));
	}

	private static SetCalc.Loadout melee(StyleData data, int id, String name, String category, int speed, int stab, int slash,
		int crush, int strength)
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		l.weaponName = name;
		l.category = category;
		l.speed = speed;
		l.styles = data.styles(id);
		l.stab = stab;
		l.slash = slash;
		l.crush = crush;
		l.strength = strength;
		return l;
	}

	/**
	 * Hard case 13 and the rule of the line: the best weapon of the bank over what is worn, when it deals at
	 * least the minimum more; worked out again only when the monster, what is worn or the bank changes, and quick with 60
	 * weapons in the bank.
	 */
	@Test
	public void theBankLineNamesTheBestWeaponOverTheMinimumAndIsQuick() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Target vorkath = data.monster(8061).target();
		SetCalc.Player p = new SetCalc.Player();
		p.attack = 118;
		p.strength = 118;
		p.prayerAttack = 120;
		p.prayerStrength = 123;
		SetCalc.Loadout whip = melee(data, 4151, "Abyssal whip", "Whip", 4, 0, 82, 0, 82);
		double current = SetCalc.perTick(whip, p, vorkath, false);
		assertTrue(current > 0);
		List<BankHint.Candidate> candidates = new ArrayList<>();
		candidates.add(new BankHint.Candidate(22325, "Scythe of vitur", melee(data, 22325, "Scythe of vitur", "Scythe", 5,
			70, 125, 30, 75)));
		candidates.add(new BankHint.Candidate(1305, "Dragon longsword", melee(data, 1305, "Dragon longsword", "Slash Sword", 5,
			58, 69, -2, 71)));
		BankHint.Result best = BankHint.best(current, candidates, p, vorkath, BankHint.DEFAULT_MINIMUM);
		assertNotNull(best);
		assertEquals(22325, best.itemId);
		assertTrue(best.gain >= 10);
		// above what the best deals: nothing
		assertNull(BankHint.best(current, candidates, p, vorkath, best.gain + 1));
		// nothing worn to count: nothing
		assertNull(BankHint.best(-1, candidates, p, vorkath, 10));

		// 60 weapons: worked out in well under a game tick
		List<BankHint.Candidate> many = new ArrayList<>();
		for (int i = 0; i < 60; i++)
		{
			many.add(new BankHint.Candidate(22325, "Scythe of vitur", melee(data, 22325, "Scythe of vitur", "Scythe", 5,
				70, 125, 30, 75)));
		}
		long start = System.nanoTime();
		for (int i = 0; i < 10; i++)
		{
			BankHint.best(current, many, p, vorkath, 10);
		}
		long perRun = (System.nanoTime() - start) / 10;
		assertTrue("60 weapons in " + perRun / 1000 + " us", perRun < 100_000_000L);

		// the memory: the line is worked out again only on a change of the monster, what is worn or the bank
		BankHint.Memory memory = new BankHint.Memory();
		assertNull(memory.weapons());
		memory.bank(Arrays.asList(22325, 22325, 1305, 0));
		assertEquals(Arrays.asList(22325, 1305), memory.weapons());
		int version = memory.version();
		memory.bank(Arrays.asList(22325, 1305));
		assertEquals("the same bank is not a change", version, memory.version());
		List<Object> key = Arrays.asList(8061, 5, Arrays.asList(4151), memory.version(), 10.0, false);
		assertTrue(memory.changed(key));
		memory.remember(key, best);
		assertFalse(memory.changed(Arrays.asList(8061, 5, Arrays.asList(4151), memory.version(), 10.0, false)));
		assertSame(best, memory.last());
		assertTrue("another weapon in hand", memory.changed(Arrays.asList(8061, 5, Arrays.asList(22325), memory.version(), 10.0,
			false)));
		memory.bank(Arrays.asList(22325));
		assertTrue("another bank", memory.changed(Arrays.asList(8061, 5, Arrays.asList(4151), memory.version(), 10.0, false)));
	}

	/** The line's words: "Bank: <weapon> +X%". */
	@Test
	public void theBankLineReads()
	{
		assertEquals("Bank: Scythe of vitur +35%", String.format(Words.of(CombatCheckConfig.Language.ENGLISH, Words.Key.BANK),
			"Scythe of vitur", CombatCheckPlugin.gainText(35)));
	}

	/** The Defence of a row after every drain {@code drains} counts, from its full levels. */
	private static int defenceAfter(StyleData data, int id, DefenceDrains drains)
	{
		StyleData.Monster m = data.monster(id);
		return DefenceDrains.left(m, id, MonsterLevels.scaled(m, null, null, null), drains, 0).defence;
	}

	/** A Bandos godsword hit of 1000 and four Elder mauls: far more than any Defence of the raid. */
	private static DefenceDrains everything()
	{
		DefenceDrains d = new DefenceDrains();
		d.add(SpecialWeapon.BANDOS_GODSWORD, 1000, false, 0);
		for (int i = 0; i < 4; i++)
		{
			d.add(SpecialWeapon.ELDER_MAUL, 1, false, 0);
		}
		return d;
	}

	/**
	 * The official hotfix of 20 October 2026 ("The Fractured Archive - Out Today"): the Archive Guardian, Bork, Ezekiel,
	 * Moia, Xarth and Ocellus keep 60% of their base Defence whatever lands; a drain that would go past it stops there.
	 */
	@Test
	public void theCappedBossesKeepSixtyPercentOfTheirDefence() throws IOException
	{
		StyleData data = new StyleData();
		int[][] floors = {{16659, 260, 156}, {16660, 260, 156}, {16640, 300, 180}, {16667, 195, 117}, {16684, 170, 102},
			{16673, 215, 129}, {16674, 215, 129}, {16677, 250, 150}};
		for (int[] f : floors)
		{
			assertEquals("base of " + f[0], f[1], data.monster(f[0]).level);
			assertEquals("floor of " + f[0], f[2], DefenceDrains.floor(f[0], f[1]));
			assertEquals("drained to the floor: " + f[0], f[2], defenceAfter(data, f[0], everything()));
		}
		// a drain above the floor is left as it is: one Dragon warhammer on Bork, 300 to 210 (the examine of 2026-10-20)
		DefenceDrains dwh = new DefenceDrains();
		dwh.add(SpecialWeapon.DRAGON_WARHAMMER, 14, false, 0);
		assertEquals(210, defenceAfter(data, 16640, dwh));
		// a second one would take it to 147, under the floor: 180
		dwh.add(SpecialWeapon.DRAGON_WARHAMMER, 20, false, 0);
		assertEquals(180, defenceAfter(data, 16640, dwh));
		// the Archive Guardian on the ground: the old examine's 139 was before the cap; now no lower than 156
		assertEquals(156, defenceAfter(data, 16660, everything()));
		// Ezekiel has the cap too (60% of the examine's 140), though his drains are still not counted ("Def ?")
		assertEquals(84, DefenceDrains.floor(16652, 140));
		assertEquals(140, defenceAfter(data, 16652, everything()));
	}

	/** The hotfix's bosses with "no cap": Dreadnoth, the Smorks, the Dagon'hai and The Blade can be taken to 0. */
	@Test
	public void theUncappedBossesGoDownToNothing() throws IOException
	{
		StyleData data = new StyleData();
		assertEquals(0, defenceAfter(data, 16665, everything()));
		assertEquals(0, defenceAfter(data, 16641, everything()));
		// The Blade (Ocellus's second phase) and the Dagon'hai have no numbers and no row: no floor either
		for (int id : new int[]{16676, 16643, 16644, 16645, 16646, 16647, 16648})
		{
			assertEquals(0, DefenceDrains.floor(id, 200));
		}
		assertNull(data.monster(16676));
	}

	/**
	 * The Tonalztics of Ralos: its special attack hits twice and each hit that lands lowers the Defence by an eighth
	 * of the Magic level (OSRS Wiki, Tonalztics of Ralos; the calculator): on the Smorks (Magic 185), two hits that
	 * land are two drains of 23, one hit one, and a third counted for one attack is not.
	 */
	@Test
	public void eachRalosHitThatLandsIsADrain() throws IOException
	{
		StyleData data = new StyleData();
		DefenceDrains two = new DefenceDrains();
		two.add(SpecialWeapon.TONALZTICS_OF_RALOS, 2, false, 0);
		assertEquals(220 - 2 * 23, defenceAfter(data, 16641, two));
		DefenceDrains one = new DefenceDrains();
		one.add(SpecialWeapon.TONALZTICS_OF_RALOS, 1, false, 0);
		assertEquals(220 - 23, defenceAfter(data, 16641, one));
		DefenceDrains three = new DefenceDrains();
		three.add(SpecialWeapon.TONALZTICS_OF_RALOS, 3, false, 0);
		assertEquals(220 - 2 * 23, defenceAfter(data, 16641, three));
		DefenceDrains none = new DefenceDrains();
		none.add(SpecialWeapon.TONALZTICS_OF_RALOS, 0, false, 0);
		assertEquals(220, defenceAfter(data, 16641, none));
	}

	/**
	 * Ocellus's third phase keeps his Defence lowered from one quadrant to the next (the hotfix: "Ocellus will no longer
	 * restore his Defence when moving to a new quadrant during P3"): the NPC stays 16677, so nothing is dropped, and the
	 * drains stop at the 60% cap (150).
	 */
	@Test
	public void ocellusThirdPhaseKeepsItsDrainsAcrossQuadrants() throws IOException
	{
		StyleData data = new StyleData();
		StyleData.Monster third = data.monster(16677);
		assertEquals(CombatCheckPlugin.FormChange.KEEP, CombatCheckPlugin.formChange(third, third, 16677, 16677));
		DrainTracker drains = new DrainTracker();
		Object npc = new Object();
		drains.add(1, npc, SpecialWeapon.DRAGON_WARHAMMER, 30, false, 10);
		assertEquals(175, defenceAfter(data, 16677, drains.of(1, npc)));
		// the next quadrant: still 175; another two Dragon warhammers stop at 150
		drains.add(1, npc, SpecialWeapon.DRAGON_WARHAMMER, 30, false, 300);
		drains.add(1, npc, SpecialWeapon.DRAGON_WARHAMMER, 30, false, 301);
		assertEquals(150, defenceAfter(data, 16677, drains.of(1, npc)));
		assertFalse(drains.unknown(1, npc));
	}

	/** Nex's crush defence, +60 to +35 in the same update (official post "The Fractured Archive - Out Today"). */
	@Test
	public void nexsCrushDefenceIsThirtyFive() throws IOException
	{
		StyleData data = new StyleData();
		for (int id : new int[]{11278, 11279, 11280, 11281, 11282})
		{
			assertEquals(35, data.monster(id).crush);
			assertEquals(260, data.monster(id).level);
		}
	}

	/**
	 * Bork's armour by the Smorks' health (OSRS Wiki, Bork, 2026-10-24): 8 flat armour and 0 magic resistance with the
	 * Smorks at full health, 2 more of each every 25% they lose, up to 14 and 6; stunned, the flat armour 0. The quarters
	 * come from the shouts at 75%, 50% and 25% (the recorder: on the tick the mark is passed), the boss health display
	 * exactly, and the health bar only once its rounding (1 + (scale - 1) x health / max) leaves no doubt; never fewer in
	 * the fight, and a new Bork starts again.
	 */
	@Test
	public void borksArmourGrowsWithTheSmorksHealthLost() throws IOException
	{
		assertEquals(1, BorkArmour.said("Smorks go faster!"));
		assertEquals(1, BorkArmour.said("Bork get into it!"));
		assertEquals(2, BorkArmour.said("Smorks feeling drum beat now!"));
		assertEquals(2, BorkArmour.said("Bork make fast drum!"));
		assertEquals(3, BorkArmour.said("Smorks go fastest!"));
		assertEquals(3, BorkArmour.said("Bork maximum crush!"));
		assertEquals(0, BorkArmour.said("Big bomb! Bork watch out!"));
		assertEquals(0, BorkArmour.said(null));

		// the bar of 40: 30 can still be above 75% (74.4% to 76.9%), 29 cannot
		assertEquals(0, BorkArmour.fromBar(40, 40));
		assertEquals(0, BorkArmour.fromBar(30, 40));
		assertEquals(1, BorkArmour.fromBar(29, 40));
		assertEquals(1, BorkArmour.fromBar(20, 40));
		assertEquals(2, BorkArmour.fromBar(19, 40));
		assertEquals(2, BorkArmour.fromBar(10, 40));
		assertEquals(3, BorkArmour.fromBar(9, 40));
		assertEquals(3, BorkArmour.fromBar(0, 40));
		assertEquals("not shown", 0, BorkArmour.fromBar(-1, -1));
		// the display, exactly: the Smorks of a trio, 870
		assertEquals(0, BorkArmour.fromHealth(870, 870));
		assertEquals(0, BorkArmour.fromHealth(653, 870));
		assertEquals(1, BorkArmour.fromHealth(652, 870));
		assertEquals(2, BorkArmour.fromHealth(435, 870));
		assertEquals(3, BorkArmour.fromHealth(217, 870));
		assertEquals(3, BorkArmour.fromHealth(0, 870));
		assertEquals(0, BorkArmour.fromHealth(10, 0));

		BorkArmour armour = new BorkArmour();
		armour.says(BorkArmour.SMORKS, "Smorks go faster!");
		assertEquals(1, armour.quarters());
		armour.bar(30, 40);
		assertEquals("never fewer", 1, armour.quarters());
		armour.says(BorkStun.BORK, "Bork make fast drum!");
		assertEquals(2, armour.quarters());
		armour.says(16659, "Smorks go fastest!");
		assertEquals("another NPC's words", 2, armour.quarters());
		armour.bar(9, 40);
		assertEquals(3, armour.quarters());
		armour.display(0, 870);
		assertEquals(3, armour.quarters());
		armour.bork(BorkArmour.SMORKS);
		assertEquals("the Smorks gone, Bork keeps it", 3, armour.quarters());
		armour.bork(BorkStun.BORK);
		assertEquals("a new Bork", 0, armour.quarters());

		int[][] byQuarter = {{8, 0}, {10, 2}, {12, 4}, {14, 6}};
		StyleData data = new StyleData();
		SetCalc.Target t = data.monster(16640).target();
		for (int q = 0; q <= 3; q++)
		{
			BorkArmour.apply(t, 8, q, false);
			assertEquals(byQuarter[q][0], t.flatArmour);
			assertEquals(byQuarter[q][1], t.magicResistance);
			BorkArmour.apply(t, 8, q, true);
			assertEquals("stunned", 0, t.flatArmour);
			assertEquals(byQuarter[q][1], t.magicResistance);
		}

		// what it changes: the blowpipe deals less on 14 than on 8; Tumeken's shadow less with 6 off each cast, never more
		// than 6 a cast less, and the flat armour alone does not touch it
		SetCalc.Loadout blowpipe = new SetCalc.Loadout();
		blowpipe.weaponName = "Toxic blowpipe";
		blowpipe.category = "Thrown";
		blowpipe.speed = 2;
		blowpipe.ranged = 175;
		blowpipe.rangedStrength = 75;
		blowpipe.dartKnown = true;
		blowpipe.styles = data.styles(12926);
		SetCalc.Player p = new SetCalc.Player();
		p.ranged = 112;
		p.prayerRanged = 120;
		p.prayerRangedStrength = 123;
		p.magic = 112;
		p.prayerMagic = 125;
		p.prayerMagicDamage = 4;
		SetCalc.Target full = data.monster(16640).target();
		SetCalc.Target last = data.monster(16640).target();
		BorkArmour.apply(full, 8, 0, false);
		BorkArmour.apply(last, 8, 3, false);
		assertTrue(SetCalc.perTick(blowpipe, p, last, false) < SetCalc.perTick(blowpipe, p, full, false));
		SetCalc.Loadout shadow = new SetCalc.Loadout();
		shadow.weaponName = "Tumeken's shadow";
		shadow.category = "Powered Staff";
		shadow.speed = 5;
		shadow.styles = data.styles(27275);
		shadow.magic = 162;
		shadow.magicDamage = 25;
		double atFull = SetCalc.perTick(shadow, p, full, false);
		double atLast = SetCalc.perTick(shadow, p, last, false);
		assertTrue(atFull > 0);
		assertTrue(atLast < atFull);
		assertTrue(atFull - atLast <= 6.0 / 5 + 1e-9);
		SetCalc.Target armourOnly = data.monster(16640).target();
		armourOnly.flatArmour = 14;
		assertEquals(atFull, SetCalc.perTick(shadow, p, armourOnly, false), 1e-12);
	}

	/**
	 * The hitpoints by the size of the team (the OSRS Wiki's tables, each boss's "Hitpoints", 2026-10-26), for when the
	 * boss health bar does not show the monster fought: the bar wins where it does. A solo raid as a duo, the base;
	 * more than 8 as 8; no team, no table (Moia, The Blade) or a monster outside the raid: nothing. Every number is the
	 * rule measured in recorded fights: the base x 1.45 for 3 and 0.42 more for each player after, to the nearest ten,
	 * the two the wiki leaves empty (Dreadnoth with 7, Ezekiel with 8) included.
	 */
	@Test
	public void theRaidsHitpointsComeFromTheTeamsTable() throws IOException
	{
		assertEquals(1500, ArchiveScaling.hitpoints(16640, 1));
		assertEquals(1500, ArchiveScaling.hitpoints(16640, 2));
		assertEquals(2180, ArchiveScaling.hitpoints(16640, 3));
		assertEquals(4700, ArchiveScaling.hitpoints(16640, 7));
		assertEquals(5330, ArchiveScaling.hitpoints(16640, 9));
		assertEquals(0, ArchiveScaling.hitpoints(16640, 0));
		assertEquals(870, ArchiveScaling.hitpoints(16641, 3));
		assertEquals(800, ArchiveScaling.hitpoints(16659, 3));
		assertEquals(1600, ArchiveScaling.hitpoints(16660, 3));
		assertEquals(4350, ArchiveScaling.hitpoints(16652, 3));
		assertEquals(6570, ArchiveScaling.hitpoints(16665, 7));
		assertEquals(10650, ArchiveScaling.hitpoints(16652, 8));
		assertEquals(4860, ArchiveScaling.hitpoints(16684, 4));
		assertEquals(4680, ArchiveScaling.hitpoints(16674, 4));
		assertEquals(16430, ArchiveScaling.hitpoints(16677, 7));
		for (int id : new int[]{16667, 16675, 16676, 16643, 8061})
		{
			assertEquals("no table for " + id, 0, ArchiveScaling.hitpoints(id, 4));
		}

		StyleData data = new StyleData();
		// the base is the row's (the Archive Guardian on the ground aside: its row has the shield's 550, the wiki 1100)
		int[][] bases = {{16659, 550}, {16660, 1100}, {16640, 1500}, {16641, 600}, {16652, 3000}, {16665, 2100}, {16684, 2600},
			{16673, 2500}, {16674, 2500}, {16677, 5250}};
		for (int[] b : bases)
		{
			int id = b[0];
			if (id != 16660)
			{
				assertEquals("row of " + id, b[1], data.monster(id).hitpoints);
			}
			for (int team = 1; team <= 8; team++)
			{
				int percent = team <= 2 ? 100 : 145 + 42 * (team - 3);
				int rule = ((b[1] * percent + 500) / 1000) * 10;
				assertEquals(id + " with " + team, rule, ArchiveScaling.hitpoints(id, team));
			}
		}

		// the bar wins; without it the table; with neither, nothing (the time to kill and the bolts then have no figure)
		assertEquals(2166, CombatCheckPlugin.raidHitpoints(2166, 16640, 3));
		assertEquals(3440, CombatCheckPlugin.raidHitpoints(0, 16640, 5));
		assertEquals(0, CombatCheckPlugin.raidHitpoints(0, 16640, 0));
		assertEquals(0, CombatCheckPlugin.raidHitpoints(0, 16667, 4));
		StyleData.Monster bork = data.monster(16640);
		DefenceDrains.Levels full = MonsterLevels.scaled(bork, null, null, null);
		assertEquals(3440, MonsterLevels.fromBar(bork, full, CombatCheckPlugin.raidHitpoints(0, 16640, 5)).hitpoints);
		// the team is the most seen since the raid began: a player who dies or leaves does not lower it
		int team = CombatCheckPlugin.lockedParty(0, true, 7);
		team = CombatCheckPlugin.lockedParty(team, true, 6);
		assertEquals(7, team);
		assertEquals(8140, ArchiveScaling.hitpoints(16684, team));
		assertEquals("out of the raid", 0, CombatCheckPlugin.lockedParty(team, false, 0));
	}
}
