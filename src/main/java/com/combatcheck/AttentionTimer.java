/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

/**
 * Tells how long a mistake has been left standing, so a reminder can wait for a configured
 * number of game ticks before it is shown. A mistake fixed within the wait is never shown.
 */
final class AttentionTimer
{
	private int startedAt = -1;

	/**
	 * @param tick    the current game tick
	 * @param mistake whether there is something to remind the player of right now
	 * @param delay   ticks the mistake must stand before it is shown; 0 shows it at once
	 * @return true once the mistake has stood for {@code delay} ticks
	 */
	boolean due(int tick, boolean mistake, int delay)
	{
		if (!mistake)
		{
			startedAt = -1;
			return false;
		}
		if (startedAt == -1)
		{
			startedAt = tick;
		}
		return tick - startedAt >= Math.max(0, delay);
	}

	void reset()
	{
		startedAt = -1;
	}
}
