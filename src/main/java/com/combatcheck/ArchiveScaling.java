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
 * scaled as a duo ("the minimum scaling being set to two players"): both have the base hitpoints. Two places the wiki
 * leaves empty, Dreadnoth with 7 and Ezekiel with 8, are the rule all the others follow, the base x 1.45, 1.87, 2.29, 2.71,
 * 3.13, 3.55 for 3 to 8, to the nearest ten (measured in recorded fights): 6570 (also measured, three fights)
 * and 10650; the wiki's "8.130" for Ezekiel with 6 is 8,130. Moia, the Dagon'hai and The Blade have no table: no
 * hitpoints.
 */
final class ArchiveScaling
{
	/** The largest team of the raid. */
	static final int MOST = 8;

	/** By NPC id, the hitpoints with a team of 1 to 8. */
	private static final Map<Integer, int[]> HITPOINTS = new HashMap<>();

	static
	{
		HITPOINTS.put(16659, new int[]{550, 550, 800, 1030, 1260, 1490, 1720, 1950}); // Archive Guardian, in flight
		HITPOINTS.put(16660, new int[]{1100, 1100, 1600, 2060, 2520, 2980, 3440, 3910}); // and on the ground
		HITPOINTS.put(16640, new int[]{1500, 1500, 2180, 2810, 3440, 4070, 4700, 5330}); // Bork
		HITPOINTS.put(16641, new int[]{600, 600, 870, 1120, 1370, 1630, 1880, 2130}); // the Smorks
		HITPOINTS.put(16652, new int[]{3000, 3000, 4350, 5610, 6870, 8130, 9390, 10650}); // Ezekiel
		HITPOINTS.put(16665, new int[]{2100, 2100, 3050, 3930, 4810, 5690, 6570, 7460}); // Dreadnoth the Jellied
		HITPOINTS.put(16684, new int[]{2600, 2600, 3770, 4860, 5950, 7050, 8140, 9230}); // Xarth
		int[] ocellusFirst = {2500, 2500, 3630, 4680, 5730, 6780, 7830, 8880};
		HITPOINTS.put(16673, ocellusFirst); // Ocellus, his first phase
		HITPOINTS.put(16674, ocellusFirst);
		HITPOINTS.put(16677, new int[]{5250, 5250, 7610, 9820, 12020, 14230, 16430, 18640}); // and the rest
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
