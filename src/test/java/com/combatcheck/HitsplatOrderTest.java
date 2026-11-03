/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/**
 * Which of the player's hitsplats on a tick is the special attack's, and the tick a party member's special attack is
 * counted on, against the raids of 2026-11-02 (recordings 19:15, 19:52 and 23:06, and the party's messages in the
 * client's log).
 */
public class HitsplatOrderTest
{
	private static final Object BLADE = new Object();
	private static final int INDEX = 33435;

	/** The Blade's Defence 250 (Magic 220, floor 0) after what was counted. */
	private static int blade(DrainTracker drains)
	{
		DefenceDrains d = drains.of(INDEX, BLADE);
		return d == null ? 250 : d.apply(250, 220, 0, false, 0).defence;
	}

	/**
	 * The player's own Tonalztics of Ralos with a thrall on the same tick: the Ralos is a delayed hit, so its two hitsplats
	 * come first and the thrall's after them ("[delayed hits, thrall hit, veng, instant hits]", the Special Attack Counter,
	 * which reads the first two). 19:15, line 9300: [20, 14, 0] is 2 hits (the last two read 1); 23:06, line 29314: [0, 0,
	 * 1] is none (the last two read 1); the Blade of 19:52, line 34497: [0, 14, 2] is 1 (the last two read 2).
	 */
	@Test
	public void theRalosHitsplatsComeBeforeTheThralls()
	{
		assertEquals(Integer.valueOf(2), CombatCheckPlugin.landedHits(Arrays.asList(20, 14, 0), 2));
		assertEquals(Integer.valueOf(0), CombatCheckPlugin.landedHits(Arrays.asList(0, 0, 1), 2));
		assertEquals(Integer.valueOf(1), CombatCheckPlugin.landedHits(Arrays.asList(0, 14, 2), 2));
		// the uncharged weapon's one hitsplat: the first
		assertEquals(Integer.valueOf(1), CombatCheckPlugin.landedHits(Arrays.asList(12, 0), 1));
		assertNull(CombatCheckPlugin.landedHits(Collections.singletonList(20), 2));
	}

	/**
	 * Any other weapon's hitsplat as the Special Attack Counter picks it: a delayed hit's (hit delay over a tick: the
	 * Accursed sceptre, the Dorgeshuun crossbow) is the first, before a thrall's; an instant one's (the Dragon warhammer, the
	 * Bandos godsword) the last, after a thrall's and vengeance's.
	 */
	@Test
	public void aDelayedHitFirstAnInstantOneLast()
	{
		assertEquals(Integer.valueOf(35), CombatCheckPlugin.weaponHitsplat(SpecialWeapon.ACCURSED_SCEPTRE,
			Arrays.asList(35, 3)));
		assertEquals(Integer.valueOf(0), CombatCheckPlugin.weaponHitsplat(SpecialWeapon.DORGESHUUN_CROSSBOW,
			Arrays.asList(0, 4)));
		assertEquals(Integer.valueOf(48), CombatCheckPlugin.weaponHitsplat(SpecialWeapon.DRAGON_WARHAMMER,
			Arrays.asList(3, 48)));
		assertEquals(Integer.valueOf(0), CombatCheckPlugin.weaponHitsplat(SpecialWeapon.BANDOS_GODSWORD,
			Arrays.asList(4, 12, 0)));
		assertEquals(Integer.valueOf(40), CombatCheckPlugin.weaponHitsplat(SpecialWeapon.BANDOS_GODSWORD,
			Collections.singletonList(40)));
		assertNull(CombatCheckPlugin.weaponHitsplat(SpecialWeapon.DRAGON_WARHAMMER, Collections.emptyList()));
	}

	/**
	 * A party member's Elder maul is counted on the tick its message comes, which is the tick its hit lands (77 of 77
	 * messages 2 ticks after the animation); a Tonalztics of Ralos', or any other weapon's, the tick before its message (116
	 * of 118 Ralos messages 3 ticks after the animation, the hit 2 after). The Blade, recorded: a party member's maul (animation
	 * 13368, message and hit 13370) then the player's own Ralos (animation 13369, hit 13371, [0, 14, 2], 1 hit): Defence 136
	 * as in the game; with the maul counted 2 ticks after its message it went after the Ralos, 145.
	 */
	@Test
	public void thePartysElderMaulOnTheTickItsMessageComes()
	{
		assertEquals(13370, CombatCheckPlugin.partyHitTick(SpecialWeapon.ELDER_MAUL, 13370));
		assertEquals(13371, CombatCheckPlugin.partyHitTick(SpecialWeapon.TONALZTICS_OF_RALOS, 13372));
		assertEquals(100, CombatCheckPlugin.partyHitTick(SpecialWeapon.BANDOS_GODSWORD, 101));

		DrainTracker drains = new DrainTracker();
		drains.add(INDEX, BLADE, SpecialWeapon.ELDER_MAUL, 1, false,
			CombatCheckPlugin.partyHitTick(SpecialWeapon.ELDER_MAUL, 13370));
		// the player's own, counted when it lands, on the tick of its hit
		drains.add(INDEX, BLADE, SpecialWeapon.TONALZTICS_OF_RALOS,
			CombatCheckPlugin.landedHits(Arrays.asList(0, 14, 2), 2), false, 13371);
		assertEquals(136, blade(drains));

		// the order that gave 145: the Ralos first
		DrainTracker wrong = new DrainTracker();
		wrong.add(INDEX, BLADE, SpecialWeapon.TONALZTICS_OF_RALOS, 1, false, 13371);
		wrong.add(INDEX, BLADE, SpecialWeapon.ELDER_MAUL, 1, false, 13372);
		assertEquals(145, blade(wrong));

		// two party members: a Ralos landing on 13369 (message 13370) before a maul landing on 13370 (message 13370), in
		// whichever order the messages came
		DrainTracker party = new DrainTracker();
		party.add(INDEX, BLADE, SpecialWeapon.ELDER_MAUL, 1, false,
			CombatCheckPlugin.partyHitTick(SpecialWeapon.ELDER_MAUL, 13370));
		party.add(INDEX, BLADE, SpecialWeapon.TONALZTICS_OF_RALOS, 1, false,
			CombatCheckPlugin.partyHitTick(SpecialWeapon.TONALZTICS_OF_RALOS, 13370));
		assertEquals(145, blade(party));
	}

	/**
	 * An estimated special attack (a player outside the party) is counted by the tick its hit landed too, among the party's
	 * and the player's own: an Elder maul seen on 100 lands on 102.
	 */
	@Test
	public void anEstimateByTheTickItsHitLanded()
	{
		SpecEstimate e = new SpecEstimate();
		Object npc = new Object();
		e.seen(100, 9, false, 11124, 0, net.runelite.api.gameval.ItemID.ELDER_MAUL, 5, npc, false);
		e.endTick(Collections.singleton(9));
		e.hitsplat(102, npc, 40, net.runelite.api.HitsplatID.DAMAGE_OTHER, false);
		SpecEstimate.Landed l = e.resolve(102).get(0);
		assertEquals(102, l.hitTick);
		assertEquals("the tick its party message has come by", 103, l.tick);
	}
}
