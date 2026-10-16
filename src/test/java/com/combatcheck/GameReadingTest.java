/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * What the plugin reads from the game, with the state the client would give built here: a fight with a player, the
 * boss of a boss task, the master of the task, the Defence Rigour and Augury need, the quiver worn.
 */
public class GameReadingTest
{
	private static Set<String> set(String... s)
	{
		return new HashSet<>(Arrays.asList(s));
	}

	private static final Set<String> KNOWN = set("hellhound", "abyssal demon", "revenant", "bosse", "spider", "bear");

	/**
	 * CC-12: an alt or a friend following the player, or traded with, is no fight with a player: the game's "interacting"
	 * is also following and trading (RuneLite API, Actor.getInteracting). Only where players can attack each other (the
	 * Wilderness, a PvP area) does a player targeting the player, or targeted, silence the plugin.
	 */
	@Test
	public void anAltFollowingIsNoFightWithAPlayer()
	{
		// at the Corporeal Beast, the Royal Titans, in a raid: the alt follows, the player follows the alt
		assertFalse(CombatCheckPlugin.playerFightNow(false, false, true));
		assertFalse(CombatCheckPlugin.playerFightNow(false, true, false));
		assertFalse(CombatCheckPlugin.playerFightNow(false, true, true));
		// in the Wilderness or a PvP area: attacking a player, or attacked by one
		assertTrue(CombatCheckPlugin.playerFightNow(true, true, false));
		assertTrue(CombatCheckPlugin.playerFightNow(true, false, true));
		assertFalse(CombatCheckPlugin.playerFightNow(true, false, false));
		// the minute after a fight with a player still silences it, as before
		assertTrue(CombatCheckPlugin.stillFightingPlayer(150, 100));
		assertFalse(CombatCheckPlugin.stillFightingPlayer(201, 100));
		assertFalse(CombatCheckPlugin.stillFightingPlayer(201, -1));
	}

	/**
	 * CC-37: a boss task is of the boss the game names (SLAYER_TARGET 98 and the boss read from its table of boss tasks,
	 * as RuneLite's Slayer plugin reads it), no longer "not known": the Slayer helmet counts on Cerberus, not on the
	 * Abyssal Sire. The names are those of RuneLite's Slayer plugin (Task.java).
	 */
	@Test
	public void aBossTaskIsOfTheBossTheGameNames()
	{
		assertEquals(Boolean.TRUE, CombatCheckPlugin.onTask(20, true, 5, false, "Cerberus", "Cerberus", set("bosse", "hellhound"),
			false, KNOWN));
		assertEquals(Boolean.FALSE, CombatCheckPlugin.onTask(20, true, 5, false, "Cerberus", "Abyssal Sire",
			set("bosse", "abyssal demon"), false, KNOWN));
		// before, a boss task was "not known" and the helmet's bonus was left out of every figure
		assertNull(SetEffects.onTask("Bosses", set("bosse", "hellhound"), KNOWN));
		// the task names RuneLite's Slayer plugin knows, with "The" and a task named for several bosses
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("The Abyssal Sire", "Abyssal Sire", false, false));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("The Abyssal Sire", "Respiratory system", false, false));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("The Cave Kraken Boss", "Kraken", false, false));
		assertEquals(Boolean.FALSE, SetEffects.onBossTask("The Cave Kraken Boss", "Cave kraken", false, false));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("Dagannoth Kings", "Dagannoth Prime", false, false));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("Barrows Brothers", "Karil the Tainted", false, false));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("The Grotesque Guardians", "Dusk", false, false));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("The Leviathan", "The Leviathan", false, false));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("Kree'arra", "Kree'arra", false, false));
		assertEquals(Boolean.FALSE, SetEffects.onBossTask("Kree'arra", "Wingman Skree", false, false));
		// the bosses the wiki lets kill instead (Boss slayer tasks): Artio, Spindel, Calvar'ion, the Deranged archaeologist
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("Callisto", "Artio", false, false));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("Venenatis", "Spindel", false, false));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("Vet'ion", "Calvar'ion", false, false));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("Crazy Archaeologists", "Deranged archaeologist", false, false));
		// but not on Konar's task (the wiki: "this does not apply to her tasks")
		assertEquals(Boolean.FALSE, SetEffects.onBossTask("Crazy Archaeologists", "Deranged archaeologist", false, true));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("Crazy Archaeologists", "Crazy archaeologist", false, true));
		// Slayer equipment does nothing in the awakened fights of Desert Treasure II's bosses (OSRS Wiki, Slayer helmet)
		assertEquals(Boolean.FALSE, SetEffects.onBossTask("Vardorvis", "Vardorvis", true, false));
		assertEquals(Boolean.TRUE, SetEffects.onBossTask("Vardorvis", "Vardorvis", false, false));
		// a boss task the game's table gives no name for stays not known
		assertNull(CombatCheckPlugin.onTask(20, true, 5, false, null, "Cerberus", set("bosse", "hellhound"), false, KNOWN));
		// no task left: not on task
		assertEquals(Boolean.FALSE, CombatCheckPlugin.onTask(0, true, 5, false, "Cerberus", "Cerberus", set("bosse"), false, KNOWN));
	}

	/** CC-37: with the boss of the task known, SetEffects counts the Slayer helmet on it and keeps it on. */
	@Test
	public void theHelmetCountsOnTheBossOfTheTask()
	{
		SetEffects.Context c = new SetEffects.Context();
		c.combatClass = StyleCalc.MELEE;
		c.weaponName = "Scythe of vitur";
		c.ownedNames = Collections.emptyList();
		c.onTask = CombatCheckPlugin.onTask(20, true, 5, false, "Cerberus", "Cerberus", set("bosse", "hellhound"), false, KNOWN);
		assertEquals(SetEffects.TASK_PIECE, SetEffects.scoreBonus("Slayer helmet (i)", c), 0);
		assertTrue(SetEffects.keep("Slayer helmet (i)", c));
		c.onTask = CombatCheckPlugin.onTask(20, true, 5, false, "Cerberus", "General Graardor", set("bosse"), false, KNOWN);
		assertEquals(0, SetEffects.scoreBonus("Slayer helmet (i)", c), 0);
		assertFalse(SetEffects.keep("Slayer helmet (i)", c));
	}

	/**
	 * CC-38: a task of Krystilia's (SLAYER_MASTER 7) counts only inside the Wilderness (OSRS Wiki, Slayer helmet): the
	 * revenants of her task outside it are not on task. Another master's task counts anywhere. Konar's location is not
	 * read (nothing tells where its area is): her task counts by its category, as before.
	 */
	@Test
	public void krystiliasTaskCountsOnlyInTheWilderness()
	{
		Set<String> revenant = set("revenant");
		assertEquals(Boolean.FALSE, CombatCheckPlugin.onTask(30, false, CombatCheckPlugin.KRYSTILIA, false, "Revenants",
			"Revenant knight", revenant, false, KNOWN));
		assertEquals(Boolean.TRUE, CombatCheckPlugin.onTask(30, false, CombatCheckPlugin.KRYSTILIA, true, "Revenants",
			"Revenant knight", revenant, false, KNOWN));
		assertEquals(Boolean.TRUE, CombatCheckPlugin.onTask(30, false, 5, false, "Abyssal demons", "Abyssal demon",
			set("abyssal demon"), false, KNOWN));
		assertEquals(Boolean.TRUE, CombatCheckPlugin.onTask(30, false, CombatCheckPlugin.KONAR, false, "Abyssal demons",
			"Abyssal demon", set("abyssal demon"), false, KNOWN));
		assertEquals(Boolean.FALSE, CombatCheckPlugin.onTask(30, true, CombatCheckPlugin.KRYSTILIA, false, "Callisto",
			"Callisto", set("bosse", "bear"), false, KNOWN));
	}

	/**
	 * CC-39: Rigour and Augury need Defence 70 besides their scroll, which is read with no requirement (OSRS Wiki): below
	 * it the ranged class is named and counted with Deadeye, the magic one with Mystic vigour.
	 */
	@Test
	public void rigourAndAuguryNeedDefenceSeventy()
	{
		assertFalse(CombatCheckPlugin.unlockedByScroll(true, 69));
		assertTrue(CombatCheckPlugin.unlockedByScroll(true, 70));
		assertFalse(CombatCheckPlugin.unlockedByScroll(false, 99));
		boolean rigour = CombatCheckPlugin.unlockedByScroll(true, 45);
		assertEquals("Deadeye", CombatCheckPlugin.bestPrayer("ranged", 99, false, false, rigour, true, rigour, true));
		assertEquals("Mystic vigour", CombatCheckPlugin.bestPrayer("magic", 99, false, false, rigour, true, rigour, true));
		SetCalc.Player p = new SetCalc.Player();
		p.prayerLevel = 99;
		// Piety on, a bow in the inventory: it is counted with the best ranged prayer the player can use
		p.prayerAttack = 120;
		p.prayerStrength = 123;
		p.rigour = rigour;
		p.deadeye = true;
		SetCalc.Player ranged = SetCalc.withPrayerOf(p, StyleCalc.RANGED);
		assertEquals(118, ranged.prayerRanged);
	}

	private static SwitchSet.Piece piece(int id, String name, int slot)
	{
		SwitchSet.Piece p = new SwitchSet.Piece();
		p.itemId = id;
		p.name = name;
		p.slot = slot;
		return p;
	}

	/**
	 * CC-41: the ammunition kept in Dizana's quiver is shot only with the quiver worn (as RuneLite's Ammo plugin reads
	 * it): a Twisted bow with no arrows on and the quiver in the bank shoots nothing, and the dragon arrows of the quiver
	 * are no longer counted for it. With the quiver worn (or put on by the switch) they are.
	 */
	@Test
	public void theQuiversAmmunitionNeedsTheQuiverWorn()
	{
		SwitchSet.Piece[] on = new SwitchSet.Piece[SwitchSet.SLOTS];
		on[SwitchSet.WEAPON] = piece(20997, "Twisted bow", SwitchSet.WEAPON);
		SwitchSet.Piece dragonArrow = piece(11212, "Dragon arrow", SwitchSet.AMMO);
		assertNull(CombatCheckPlugin.shotAmmo(on, "Bow", false, 500, dragonArrow));
		assertSame(dragonArrow, CombatCheckPlugin.shotAmmo(on, "Bow", true, 500, dragonArrow));
		assertNull(CombatCheckPlugin.shotAmmo(on, "Bow", true, 0, dragonArrow));
		// the slot is shot first, the quiver's when the slot holds none of the kind (bolts on, arrows in the quiver)
		SwitchSet.Piece bolts = piece(21944, "Ruby dragon bolts (e)", SwitchSet.AMMO);
		on[SwitchSet.AMMO] = bolts;
		assertSame(dragonArrow, CombatCheckPlugin.shotAmmo(on, "Bow", true, 500, dragonArrow));
		assertSame(bolts, CombatCheckPlugin.shotAmmo(on, "Bow", false, 500, dragonArrow));
		assertFalse(CombatCheckPlugin.quiverInUse(false, 500));
		assertTrue(CombatCheckPlugin.quiverInUse(true, 1));
	}
}
