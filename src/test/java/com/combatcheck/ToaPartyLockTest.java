/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The party the Tombs of Amascut's monsters are scaled for is the one the raid began with: a player leaving does not
 * lower it; leaving the raid lets it go.
 */
public class ToaPartyLockTest
{
	@Test
	public void aPlayerLeavingDoesNotLowerIt()
	{
		int kept = 0;
		// a trio comes in: the orbs fill over the first ticks
		kept = CombatCheckPlugin.lockedParty(kept, true, 2);
		kept = CombatCheckPlugin.lockedParty(kept, true, 3);
		assertEquals(3, kept);
		// one of them leaves: the raid stays scaled for three
		kept = CombatCheckPlugin.lockedParty(kept, true, 2);
		assertEquals(3, kept);
		assertEquals(3, CombatCheckPlugin.lockedParty(kept, true, 1));
		// out of the raid: nothing kept; the next raid, a duo, is scaled for two
		kept = CombatCheckPlugin.lockedParty(kept, false, 0);
		assertEquals(0, kept);
		kept = CombatCheckPlugin.lockedParty(kept, true, 2);
		assertEquals(2, kept);
	}

	/** The hitpoints of Akkha at raid level 300 for three after one left: the trio's, not the duo's. */
	@Test
	public void akkhaKeepsTheTriosHitpoints() throws java.io.IOException
	{
		StyleData data = new StyleData();
		StyleData.Monster akkha = data.monster(11789);
		int kept = CombatCheckPlugin.lockedParty(CombatCheckPlugin.lockedParty(0, true, 3), true, 2);
		int trio = ToaScaling.hitpoints(akkha, akkha.hitpoints, new ToaScaling.Raid(300, 3, 0, 0, 0, 0));
		int duo = ToaScaling.hitpoints(akkha, akkha.hitpoints, new ToaScaling.Raid(300, 2, 0, 0, 0, 0));
		assertEquals(trio, ToaScaling.hitpoints(akkha, akkha.hitpoints, new ToaScaling.Raid(300, kept, 0, 0, 0, 0)));
		assertEquals(true, trio > duo);
	}

	/**
	 * The Theatre of Blood the same way, by its places alone: the trio of 2026-10-17 had a
	 * player leave at 12:03 and come back at 12:10 (the client's log: orbs 27,27,0 then 27,27,31); kept at three.
	 */
	@Test
	public void theTheatreToo()
	{
		int kept = 0;
		kept = CombatCheckPlugin.lockedParty(kept, true, 3);
		kept = CombatCheckPlugin.lockedParty(kept, true, 2);
		assertEquals(3, kept);
		kept = CombatCheckPlugin.lockedParty(kept, true, 3);
		assertEquals(3, kept);
		// out of the Theatre the places are empty: nothing kept
		assertEquals(0, CombatCheckPlugin.lockedParty(kept, false, 0));
		// a trio's 75%, not a duo's (the same), and a party of four that loses one keeps 87.5%
		kept = CombatCheckPlugin.lockedParty(CombatCheckPlugin.lockedParty(0, true, 4), true, 3);
		assertEquals(4, kept);
	}
}
