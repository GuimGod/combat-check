/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

/**
 * The Nylocas Ischyros (grey, melee) of the waves of the Theatre of Blood's Nylocas room, where one melee weapon is
 * held for small and big alike, with no tick lost on a switch between them; only the grey ones, where the Swift blade
 * and the Dual macuahuitl are close, while the blowpipe and the Eye of Ayak win either way on the other two. A weapon
 * is compared by the time it takes to kill one of them, the small and the big in the share they come in: the OSRS
 * Wiki's chart of the 31 waves (Theatre of Blood/Strategies/Nylocas) has 25 small and 16 big Ischyros; Hard Mode has
 * the same waves (and a Nylocas Prinkipas on the 10th, 20th and 30th, a boss of its own), and Entry Mode's 30 waves
 * have no chart, so they are taken in the same share. The small ones a big one leaves when killed ("two smaller nylocas
 * of any type") are not counted: which colour they are is not known.
 */
final class NyloWaves
{
	/** The small and the big Ischyros, in normal, Entry and Hard Mode (the plugin's table). */
	private static final int[][] ROWS = {{8342, 8345}, {10774, 10777}, {10791, 10794}};
	/** The other ids the game gives them: the ones that go for the team (normal 8348 and 8351, Entry, Hard). */
	private static final int[][] AGGRESSIVE = {{8348, 8351}, {10780, 10783}, {10797, 10800}};
	/** Of the 31 waves (the wiki's chart), the small and the big Ischyros. */
	static final int SMALL = 25;
	static final int BIG = 16;

	private NyloWaves()
	{
	}

	/** The small and the big row of the wave Ischyros of this NPC id (its mode), or null for any other. */
	static int[] rows(int npcId)
	{
		for (int i = 0; i < ROWS.length; i++)
		{
			for (int size = 0; size < 2; size++)
			{
				if (ROWS[i][size] == npcId || AGGRESSIVE[i][size] == npcId)
				{
					return ROWS[i].clone();
				}
			}
		}
		return null;
	}

	/** The share of the big ones among the wave Ischyros: 16 of 41. */
	static double bigShare()
	{
		return BIG / (double) (SMALL + BIG);
	}

	/** The ticks to kill one wave Ischyros, the small and the big in their share: the time a weapon holds them. */
	static double combined(double smallTicks, double bigTicks)
	{
		return (1 - bigShare()) * smallTicks + bigShare() * bigTicks;
	}
}
