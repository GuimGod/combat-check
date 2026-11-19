/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.HashMap;
import java.util.Map;

/**
 * The hitpoints of the Fractured Archive's bosses by the size of the team, for when the boss health bar does not show
 * the monster fought (the bar, read by the plugin, wins where it does). Only the hitpoints scale (the in-game examines
 * in a trio: the Defence and the other levels as the wiki's).
 *
 * <p>The tables of the OSRS Wiki, each boss's "Hitpoints" (read 2026-10-26): Archive Guardian (in flight and on the
 * ground), Bork, the Smorks, Ezekiel, Dreadnoth the Jellied, Xarth, Ocellus (his first phase and the rest). A solo raid is
 * scaled as a duo ("the minimum scaling being set to two players"): both have the same hitpoints. Two places the wiki
 * leaves empty, Dreadnoth with 7 and Ezekiel with 8, are the rule all the others follow, the base x 1.45, 1.87, 2.29, 2.71,
 * 3.13, 3.55 for 3 to 8, to the nearest ten (measured in recorded fights): 6570 (also measured, three fights)
 * and 10650; the wiki's "8.130" for Ezekiel with 6 is 8,130. Moia has no table on the wiki, and its 785 is not her
 * base: in recorded fights the hits on her up to her death came to 1,900 in each of 6 duos and 2,760 in each of 4
 * trios (1,900 x 1.45), so 1,900 is her base and the rule the rest. The Dagon'hai (the shields take the hits first)
 * and The Blade have no table: no hitpoints.
 *
 * <p>Since the official post "Quest Month - Week Two & TFA Changes" (2026-11-11): Bork's base 150 more (1,650), the
 * Smorks' 50 more (650) and Xarth's 150 less (2,450), and "the base HP for solos/duos" 10% more for every boss. A trio
 * and up is the new base by the same rule as before: measured in a recorded trio of 2026-11-11 (the hitsplats added
 * up), the Guardian 2,400 as before, Bork 2,390, the Smorks 940 and Xarth 3,550 (1,650, 650 and 2,450 x 1.45 to the
 * nearest ten). A solo or a duo is the base x 1.10, worked out and not measured yet: how the game rounds it is not known,
 * so the exact figure (every base here is a multiple of ten, so it is a whole number).
 */
final class ArchiveScaling
{
	/** The largest team of the raid. */
	static final int MOST = 8;

	/** By NPC id, the hitpoints with a team of 1 to 8. */
	private static final Map<Integer, int[]> HITPOINTS = new HashMap<>();

	static
	{
		// a solo or a duo: the base x 1.10 (2026-11-11, worked out); a trio and up: the base x 1.45, 1.87, 2.29, 2.71,
		// 3.13, 3.55 to the nearest ten
		HITPOINTS.put(16659, new int[]{605, 605, 800, 1030, 1260, 1490, 1720, 1950}); // Archive Guardian, in flight
		HITPOINTS.put(16660, new int[]{1210, 1210, 1600, 2060, 2520, 2980, 3440, 3910}); // and on the ground
		HITPOINTS.put(16640, new int[]{1815, 1815, 2390, 3090, 3780, 4470, 5160, 5860}); // Bork (base 1,650 since 2026-11-11)
		HITPOINTS.put(16641, new int[]{715, 715, 940, 1220, 1490, 1760, 2030, 2310}); // the Smorks (base 650 since 2026-11-11)
		HITPOINTS.put(16652, new int[]{3300, 3300, 4350, 5610, 6870, 8130, 9390, 10650}); // Ezekiel
		HITPOINTS.put(16667, new int[]{2090, 2090, 2760, 3550, 4350, 5150, 5950, 6750}); // Moia (her base measured)
		HITPOINTS.put(16665, new int[]{2310, 2310, 3050, 3930, 4810, 5690, 6570, 7460}); // Dreadnoth the Jellied
		HITPOINTS.put(16684, new int[]{2695, 2695, 3550, 4580, 5610, 6640, 7670, 8700}); // Xarth (base 2,450 since 2026-11-11)
		int[] ocellusFirst = {2750, 2750, 3630, 4680, 5730, 6780, 7830, 8880};
		HITPOINTS.put(16673, ocellusFirst); // Ocellus, his first phase
		HITPOINTS.put(16674, ocellusFirst);
		HITPOINTS.put(16677, new int[]{5775, 5775, 7610, 9820, 12020, 14230, 16430, 18640}); // and the rest
	}

	private ArchiveScaling()
	{
	}

	/** The hitpoints of this NPC with a team of {@code team}; 0 for a team not known (0) or an NPC without a table. */
	static int hitpoints(int npcId, int team)
	{
		int[] byTeam = HITPOINTS.get(npcId);
		return byTeam == null || team <= 0 ? 0 : byTeam[Math.min(team, MOST) - 1];
	}
}
