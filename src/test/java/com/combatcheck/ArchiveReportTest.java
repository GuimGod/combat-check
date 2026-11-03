/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The cases of a report of the Fractured Archive (2026-11-01). The figures are a simulator's of 2026-10-29 (built on
 * the calculator of 2026-10-26), with its gear: ranged
 * with seven pieces (Masori (f), Dizana's max cape, Necklace of rupture, Zaryte vambraces, Avernic treads (max), Ultor
 * ring), Rigour and 112 Ranged; magic with Ancestral, the Imbued Zamorak max cape, Confliction gauntlets, the same treads,
 * Augury and 112 Magic (the Saturated heart): Tumeken's shadow with the Occult necklace and the Magus ring (its eight
 * pieces), the Harmonised nightmare staff on Fire Surge with the Tome of fire, the Elemental amulet and the Ultor ring.
 * The simulator counts for the Harmonised staff the raid's fire fragment (+2 to the max hit) and the searing pages; the
 * plugin counts them once it knows of them (ElementalMagicTest, with the simulator's figures), and here they are left out:
 * the staff deals less, and where the two weapons change places is later. The Elemental amulet's 2 more on the base of an
 * elemental spell, which the calculator has and the plugin had not, came with this list.
 */
public class ArchiveReportTest
{
	private static SetCalc.Player rigour()
	{
		SetCalc.Player p = new SetCalc.Player();
		p.ranged = 112;
		p.prayerRanged = 120;
		p.prayerRangedStrength = 123;
		p.kandarinDiary = true;
		return p;
	}

	private static SetCalc.Player augury()
	{
		SetCalc.Player p = new SetCalc.Player();
		p.magic = 112;
		p.prayerMagic = 125;
		p.prayerMagicDamage = 4;
		return p;
	}

	private static SetCalc.Loadout ranged(StyleData data, int weaponId, String weapon, String category, int ranged,
		int rangedStrength, int speed, String ammo)
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		l.weaponName = weapon;
		l.category = category;
		l.styles = data.styles(weaponId);
		l.speed = speed;
		l.twoHanded = true;
		l.stab = -3;
		l.slash = -3;
		l.crush = -3;
		l.magic = 4;
		l.ranged = ranged;
		l.rangedStrength = rangedStrength;
		l.strength = 18;
		l.ammo = ammo;
		l.ammoWorn = ammo;
		l.dizanaQuiver = true;
		l.bolt = SetCalc.boltKind(ammo);
		return l;
	}

	/** The Twisted bow with Seeking dragon arrows: ranged 243, ranged strength 104 (the simulator's sum). */
	private static SetCalc.Loadout tbow(StyleData data)
	{
		return ranged(data, 20997, "Twisted bow", "Bow", 243, 104, 6, "Seeking dragon arrow");
	}

	/** The Ascension crossbows with Diamond ascension bolts (e): ranged 187, ranged strength 82. */
	private static SetCalc.Loadout ascension(StyleData data)
	{
		return ranged(data, ArchiveItems.ASCENSION_CROSSBOWS, "Ascension crossbows", "Crossbow", 187, 82, 3,
			"Diamond ascension bolts (e)");
	}

	/** Tumeken's shadow with its eight pieces: magic 177, magic damage 27%. */
	private static SetCalc.Loadout shadow(StyleData data)
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		l.weaponName = "Tumeken's shadow";
		l.category = "Powered Staff";
		l.styles = data.styles(27275);
		l.speed = 5;
		l.twoHanded = true;
		l.stab = 5;
		l.slash = 5;
		l.crush = 5;
		l.magic = 177;
		l.ranged = -6;
		l.strength = 6;
		l.rangedStrength = 3;
		l.magicDamage = 27;
		l.conflictionGauntlets = true;
		return l;
	}

	/** The Harmonised nightmare staff on Fire Surge with the Tome of fire: magic 149, magic damage 35%. */
	private static SetCalc.Loadout harmonised(StyleData data)
	{
		SetCalc.Loadout l = new SetCalc.Loadout();
		l.weaponName = "Harmonised nightmare staff";
		l.category = "Staff";
		l.styles = data.styles(24423);
		l.speed = 5;
		l.stab = 5;
		l.slash = 5;
		l.crush = 5;
		l.magic = 149;
		l.ranged = -6;
		l.strength = 18;
		l.rangedStrength = 3;
		l.magicDamage = 35;
		l.spell = 51; // Fire Surge
		l.tome = "fire";
		l.conflictionGauntlets = true;
		l.elementalAmulet = "all";
		return l;
	}

	private static double perSecond(SetCalc.Loadout l, SetCalc.Player p, SetCalc.Target t)
	{
		return SetCalc.perTick(l, p, t, false) / 0.6;
	}

	private static SetCalc.Target at(StyleData data, int npcId, int defence)
	{
		SetCalc.Target t = data.monster(npcId).target();
		t.defenceLevel = defence;
		return t;
	}

	private static void near(String what, double expected, double got, double tolerance)
	{
		assertTrue(what + ": " + got + " against the simulator's " + expected, Math.abs(got / expected - 1) <= tolerance);
	}

	/**
	 * 1. Dreadnoth's Jellies: a chinchompa on Short fuse is shown the Medium fuse; the Ancient Magicks cast on them (a
	 * cast on the Ancient spellbook, or a burst or a barrage on autocast) are reminded of the chinchompa. Only on a Jelly.
	 */
	@Test
	public void theJelliesAreForAChinchompaOnMediumFuse() throws IOException
	{
		assertTrue(ArchiveNotices.shortFuseOnJelly(true, "Chinchompas", "Short fuse"));
		assertFalse(ArchiveNotices.shortFuseOnJelly(true, "Chinchompas", ArchiveNotices.MEDIUM_FUSE));
		assertFalse(ArchiveNotices.shortFuseOnJelly(true, "Chinchompas", "Long fuse"));
		assertFalse("not on Dreadnoth", ArchiveNotices.shortFuseOnJelly(false, "Chinchompas", "Short fuse"));
		assertFalse(ArchiveNotices.shortFuseOnJelly(true, "Bow", "Short fuse"));

		// the cast of the Fractured Archive (10092) and the older ones, on the Ancient spellbook
		assertTrue(ArchiveNotices.ancientsOnJelly(true, true, 10092, 0));
		assertTrue(ArchiveNotices.ancientsOnJelly(true, true, 1979, 0));
		assertFalse("Fire Surge from the Harmonised staff casts with 10092 too", ArchiveNotices.ancientsOnJelly(true, false, 10092, 0));
		assertFalse(ArchiveNotices.ancientsOnJelly(true, true, 7618, 0));
		// Ice Barrage (45) and Ice Burst (37) on autocast; Ice Blitz (41) hits one
		assertTrue(ArchiveNotices.ancientsOnJelly(true, false, -1, 45));
		assertTrue(ArchiveNotices.ancientsOnJelly(true, false, -1, 37));
		assertFalse(ArchiveNotices.ancientsOnJelly(true, false, -1, 41));
		assertFalse("not on Dreadnoth", ArchiveNotices.ancientsOnJelly(false, true, 10092, 45));

		// the Jellies have no numbers: the style notice cannot work them out, so the rule is written for them
		assertTrue(new StyleData().unmodelled(ArchiveNotices.JELLY));
	}

	/**
	 * 2. Dreadnoth's first phase: with his Defence full (125) the Harmonised staff deals the most (the simulator: 13.04
	 * against the Ascension crossbows' 10.48); with it at 0 (the Tonalztics of Ralos landed) the Ascension crossbows
	 * (13.64). The plugin's staff without the fragment and the pages: the same order at both ends, by more than the
	 * minimum gain of 1%.
	 */
	@Test
	public void dreadnothIsTheHarmonisedStaffsUntilHisDefenceIsDown() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Target full = at(data, 16665, 125);
		SetCalc.Target none = at(data, 16665, 0);
		double staffFull = perSecond(harmonised(data), augury(), full);
		double staffNone = perSecond(harmonised(data), augury(), none);
		double crossbowsFull = perSecond(ascension(data), rigour(), full);
		double crossbowsNone = perSecond(ascension(data), rigour(), none);
		near("Ascension, Def 125", 10.48, crossbowsFull, 0.01);
		near("Ascension, Def 0", 13.64, crossbowsNone, 0.01);
		assertTrue(staffFull + " over " + crossbowsFull, SetCalc.gain(crossbowsFull, staffFull) >= 1);
		assertTrue(crossbowsNone + " over " + staffNone, SetCalc.gain(staffNone, crossbowsNone) >= 1);
		// the Defence does not change the staff's figure (a spell rolls against his Magic)
		assertEquals(staffFull, staffNone, 1e-9);
		// the Elemental amulet's 2 more on the spell's base (the calculator's hasMatchingElementalAmulet) is what puts the
		// staff first: without it the crossbows would be pointed at with his Defence full
		SetCalc.Loadout bare = harmonised(data);
		bare.elementalAmulet = null;
		assertTrue(perSecond(bare, augury(), full) < crossbowsFull);
		SetCalc.Loadout fireAmulet = harmonised(data);
		fireAmulet.elementalAmulet = "fire";
		assertEquals(staffFull, perSecond(fireAmulet, augury(), full), 1e-9);
		SetCalc.Loadout waterAmulet = harmonised(data);
		waterAmulet.elementalAmulet = "water";
		assertEquals(perSecond(bare, augury(), full), perSecond(waterAmulet, augury(), full), 1e-9);
		assertEquals("all", SetEffects.elementalAmulet(Arrays.asList("Ancestral hat", "Elemental amulet")));
		assertEquals("fire", SetEffects.elementalAmulet(Arrays.asList("Amulet of fire")));
		assertEquals(null, SetEffects.elementalAmulet(Arrays.asList("Occult necklace")));
		// Dreadnoth does not change form: the weapon notice may point at a weapon of the other class
		assertFalse(CombatCheckPlugin.changesForm("Dreadnoth the Jellied"));
	}

	/**
	 * 3. The Blade (16676, Ocellus's second phase) has a row, the wiki's "Phase 2": with its Defence full Tumeken's
	 * shadow with eight pieces deals the most (the simulator: 9.23 against the Twisted bow's 7.99, +16%); the bow passes it
	 * under a Defence of about 180; at 0 the Ascension crossbows deal the most (13.64). The Blade does not change form,
	 * so the weapon notice may point at the Shadow with a bow in hand; never at a melee weapon.
	 */
	@Test
	public void theBladeIsTheShadowsWithItsDefenceFull() throws IOException
	{
		StyleData data = new StyleData();
		StyleData.Monster blade = data.monster(CombatCheckPlugin.THE_BLADE);
		assertNotNull(blade);
		assertEquals("The Blade", blade.name);
		assertEquals(250, blade.level);
		assertEquals(220, blade.magicLevel);
		assertTrue(blade.fracturedArchive);
		assertFalse(CombatCheckPlugin.changesForm(blade.name));
		assertTrue("Ocellus himself still does", CombatCheckPlugin.changesForm("Ocellus"));
		assertEquals("no floor", 0, DefenceDrains.floor(CombatCheckPlugin.THE_BLADE, 250));

		double shadow = perSecond(shadow(data), augury(), at(data, CombatCheckPlugin.THE_BLADE, 250));
		double bow = perSecond(tbow(data), rigour(), at(data, CombatCheckPlugin.THE_BLADE, 250));
		double crossbows = perSecond(ascension(data), rigour(), at(data, CombatCheckPlugin.THE_BLADE, 250));
		near("Shadow (8), Def 250", 9.23, shadow, 0.01);
		near("Twisted bow, Def 250", 7.99, bow, 0.01);
		near("Ascension, Def 250", 7.32, crossbows, 0.01);
		assertTrue(SetCalc.gain(bow, shadow) >= 1);
		// the bow under about 180
		assertTrue(perSecond(tbow(data), rigour(), at(data, CombatCheckPlugin.THE_BLADE, 190)) < shadow);
		assertTrue(perSecond(tbow(data), rigour(), at(data, CombatCheckPlugin.THE_BLADE, 170)) > shadow);
		// at 0, the crossbows
		double bowNone = perSecond(tbow(data), rigour(), at(data, CombatCheckPlugin.THE_BLADE, 0));
		double crossbowsNone = perSecond(ascension(data), rigour(), at(data, CombatCheckPlugin.THE_BLADE, 0));
		near("Ascension, Def 0", 13.64, crossbowsNone, 0.01);
		near("Twisted bow, Def 0", 12.53, bowNone, 0.01);
		assertTrue(SetCalc.gain(bowNone, crossbowsNone) >= 1);

		// ranged and magic may be pointed at on it, melee not; anywhere else melee as before
		assertFalse(CombatCheckPlugin.classAllowed(CombatCheckPlugin.THE_BLADE, StyleCalc.MELEE));
		assertTrue(CombatCheckPlugin.classAllowed(CombatCheckPlugin.THE_BLADE, StyleCalc.MAGIC));
		assertTrue(CombatCheckPlugin.classAllowed(CombatCheckPlugin.THE_BLADE, StyleCalc.RANGED));
		assertTrue(CombatCheckPlugin.classAllowed(16677, StyleCalc.MELEE));
	}

	/**
	 * 4. Xarth's shield (XarthShield): melee lands nothing until his first charge is seen (14992, or the explosion 14989),
	 * or while his bar shows less than 84% (a charge not seen). Another Xarth, or one whose charge was seen, as before.
	 */
	@Test
	public void xarthTakesNoMeleeUntilHisFirstCharge() throws IOException
	{
		XarthShield shield = new XarthShield();
		int index = 60954;
		assertTrue("not hit yet: no bar", shield.shielded(index, XarthShield.XARTH, -1));
		assertTrue(shield.shielded(index, XarthShield.XARTH, XarthShield.healthPercent(92, 100)));
		assertTrue(shield.shielded(index, XarthShield.XARTH, XarthShield.healthPercent(84, 100)));
		assertFalse("under 84%: a charge the plugin did not see", shield.shielded(index, XarthShield.XARTH,
			XarthShield.healthPercent(83, 100)));
		assertFalse("not Xarth", shield.shielded(index, 16687, -1));
		// another animation of his changes nothing; the charge does
		shield.animation(index, XarthShield.XARTH, 14984);
		assertTrue(shield.shielded(index, XarthShield.XARTH, 95));
		shield.animation(index, XarthShield.XARTH, XarthShield.CHARGE);
		assertFalse(shield.shielded(index, XarthShield.XARTH, 95));
		assertTrue("another Xarth", shield.shielded(index + 1, XarthShield.XARTH, -1));
		shield.animation(index + 1, XarthShield.XARTH, XarthShield.EXPLOSION);
		assertFalse(shield.shielded(index + 1, XarthShield.XARTH, -1));
		// he died: the index may be the next raid's Xarth
		shield.died(index);
		assertTrue(shield.shielded(index, XarthShield.XARTH, -1));
		shield.clear();
		assertTrue(shield.shielded(index + 1, XarthShield.XARTH, -1));
		assertEquals(-1, XarthShield.healthPercent(-1, 30));
		assertEquals(-1, XarthShield.healthPercent(20, 0));

		// shielded, his row with "M": melee deals nothing, the Ascension crossbows as before
		StyleData data = new StyleData();
		SetCalc.Target open = data.monster(XarthShield.XARTH).target();
		SetCalc.Target shut = data.monster(XarthShield.XARTH).target();
		shut.immune = (shut.immune == null ? "" : shut.immune) + "M";
		assertTrue(CombatCheckPlugin.meleeOut(shut.immune));
		assertFalse(CombatCheckPlugin.meleeReaches(shut.immune, "Extending Chain Mace"));
		assertTrue(CombatCheckPlugin.meleeReaches(open.immune, "Extending Chain Mace"));
		assertEquals(perSecond(ascension(data), rigour(), open), perSecond(ascension(data), rigour(), shut), 0);
	}

	/**
	 * 6. The Lightbearer is never asked for in place of the Venator ring, nor the Venator ring taken off for it, whatever
	 * the weapon in hand: it has no bonuses (the plugin compares rings by them) and it is kept on when worn.
	 */
	@Test
	public void theLightbearerNeverTakesTheVenatorRingsPlace()
	{
		SwitchSet.Piece venator = new SwitchSet.Piece();
		venator.itemId = 28310;
		venator.name = "Venator ring";
		venator.slot = 12;
		venator.ranged = 10;
		venator.rangedStrength = 2;
		SwitchSet.Piece lightbearer = new SwitchSet.Piece();
		lightbearer.itemId = 25975;
		lightbearer.name = "Lightbearer";
		lightbearer.slot = 12;
		SwitchSet.Piece[] worn = new SwitchSet.Piece[SwitchSet.SLOTS];
		worn[12] = venator;
		List<SwitchSet.Piece> carried = Arrays.asList(lightbearer);
		for (String cls : new String[]{StyleCalc.RANGED, StyleCalc.MAGIC, StyleCalc.MELEE})
		{
			String type = StyleCalc.MELEE.equals(cls) ? "crush" : null;
			assertFalse(cls, SwitchSet.missing(cls, type, worn, carried).contains(25975));
			assertFalse(cls, SwitchSet.takeOff(cls, type, worn, carried).contains(28310));
		}
	}

	/**
	 * 7. Ezekiel: the Twisted bow deals more than the Ascension crossbows (the other way round from the Archive Guardian,
	 * Dreadnoth and Ocellus's first phase; the simulator of 2026-10-29):
	 * with the bow in hand the crossbows are not pointed at, and with the crossbows in hand the bow is.
	 */
	@Test
	public void ezekielIsTheTwistedBows() throws IOException
	{
		StyleData data = new StyleData();
		SetCalc.Target ezekiel = data.monster(16652).target();
		double bow = perSecond(tbow(data), rigour(), ezekiel);
		double crossbows = perSecond(ascension(data), rigour(), ezekiel);
		assertTrue(bow + " over " + crossbows, SetCalc.gain(crossbows, bow) >= 1);
		assertTrue(SetCalc.gain(bow, crossbows) < 1);
	}

	/**
	 * 8. A damage special attack on Ocellus's last stage while an explosion was seen this tick or up to 2 before; not a
	 * special attack that lowers the Defence, not on another monster, not before the first or after the last.
	 */
	@Test
	public void aDamageSpecInTheExplosionsIsToldAfterwards()
	{
		int explosion = 1000;
		assertTrue(ArchiveNotices.specIntoExplosions(16677, "Crimson kisten", explosion, 1000));
		assertTrue(ArchiveNotices.specIntoExplosions(16677, "Dragon claws", explosion, 1002));
		assertFalse("3 ticks after the last", ArchiveNotices.specIntoExplosions(16677, "Crimson kisten", explosion, 1003));
		assertFalse("before it", ArchiveNotices.specIntoExplosions(16677, "Crimson kisten", explosion, 999));
		assertFalse("none seen", ArchiveNotices.specIntoExplosions(16677, "Crimson kisten", -1, 1000));
		assertFalse("the Elder maul lowers the Defence", ArchiveNotices.specIntoExplosions(16677, "Elder maul", explosion, 1000));
		assertFalse(ArchiveNotices.specIntoExplosions(16677, "Tonalztics of ralos", explosion, 1000));
		assertFalse("Ocellus's first phase", ArchiveNotices.specIntoExplosions(16674, "Crimson kisten", explosion, 1000));
		// the damage ones are still special attack weapons for the weapon notice
		assertTrue(SetEffects.isSpecialAttackWeapon("Crimson kisten"));
		assertTrue(SetEffects.isSpecialAttackWeapon("Elder maul"));
		assertFalse(SetEffects.isDamageSpecialAttackWeapon("Elder maul"));
		assertTrue(SetEffects.isDamageSpecialAttackWeapon("Zorya's tome"));
	}

	/**
	 * 9. The bar left unused: half or more for 50 ticks, with a melee weapon in hand against a monster of the raid melee
	 * reaches and the Crimson kisten carried. Using the bar, or going under half, starts the count again.
	 */
	@Test
	public void theBarLeftUnusedIsToldAfterThirtySeconds()
	{
		int since = ArchiveNotices.unusedSince(-1, 400, 500, 100);
		assertEquals(100, since);
		assertEquals("growing, not used", 100, ArchiveNotices.unusedSince(since, 500, 600, 130));
		assertTrue(ArchiveNotices.specUnused(true, true, true, true, 600, since, 150));
		assertFalse("49 ticks", ArchiveNotices.specUnused(true, true, true, true, 600, since, 149));
		assertFalse("not the raid", ArchiveNotices.specUnused(false, true, true, true, 600, since, 150));
		assertFalse("melee does not reach", ArchiveNotices.specUnused(true, false, true, true, 600, since, 150));
		assertFalse("a bow in hand", ArchiveNotices.specUnused(true, true, false, true, 600, since, 150));
		assertFalse("no kisten", ArchiveNotices.specUnused(true, true, true, false, 600, since, 150));
		assertFalse("under half", ArchiveNotices.specUnused(true, true, true, true, 490, since, 150));
		// used and still at half or more: from now; used under half: none
		assertEquals(160, ArchiveNotices.unusedSince(since, 1000, 500, 160));
		assertEquals(-1, ArchiveNotices.unusedSince(since, 600, 100, 160));
		assertFalse(ArchiveNotices.specUnused(true, true, true, true, 500, -1, 1000));
	}

	/** The new words are in every language of the setting. */
	@Test
	public void theRemindersAreWrittenInEveryLanguage()
	{
		for (CombatCheckConfig.Language language : CombatCheckConfig.Language.values())
		{
			assertTrue(Words.of(language, Words.Key.JELLY_CHINCHOMPA).startsWith("Jellies"));
			assertTrue(Words.of(language, Words.Key.SPEC_EXPLOSIONS).startsWith("Spec"));
			assertTrue(String.format(Words.of(language, Words.Key.SPEC_UNUSED), 70).startsWith("Spec 70%"));
		}
	}
}
