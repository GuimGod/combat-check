/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

/**
 * A reminder that is shown for a while and then keeps quiet: it is shown for a number of game ticks,
 * all told, and after that not again until something the player does arms it once more (a piece of
 * armour changed, an attack prayer turned on). The reminder going away and coming back by itself (a
 * monster dying, the next one attacked) arms nothing: the ticks it was shown for are kept.
 */
final class ShownFor
{
	/** The slots of the worn equipment that are not armour: the weapon and the ammunition. */
	static final int WEAPON_SLOT = 3;
	static final int AMMO_SLOT = 13;

	private int shown;

	/**
	 * @param showing whether there is something to remind the player of right now
	 * @param ticks   how many ticks it is shown for, all told; 0 or less shows it for as long as it stands
	 * @return whether the reminder is on screen this tick
	 */
	boolean visible(boolean showing, int ticks)
	{
		if (!showing)
		{
			return false;
		}
		if (ticks <= 0)
		{
			return true;
		}
		if (shown >= ticks)
		{
			return false;
		}
		shown++;
		return true;
	}

	/** The player did what makes the reminder worth showing again. */
	void arm()
	{
		shown = 0;
	}

	/** Seconds as game ticks of 0.6 seconds, rounded up: 5 seconds are 9 ticks. */
	static int ticks(int seconds)
	{
		return seconds <= 0 ? 0 : (seconds * 10 + 5) / 6;
	}

	/**
	 * Whether the armour worn changed: any slot but the weapon and the ammunition. A shield is armour.
	 *
	 * @param before the item ids by slot the last time, or null for the first look
	 * @param now    the item ids by slot now
	 */
	static boolean armourChanged(int[] before, int[] now)
	{
		if (before == null || now == null)
		{
			return false;
		}
		for (int slot = 0; slot < Math.max(before.length, now.length); slot++)
		{
			if (slot == WEAPON_SLOT || slot == AMMO_SLOT)
			{
				continue;
			}
			int was = slot < before.length ? before[slot] : -1;
			int is = slot < now.length ? now[slot] : -1;
			if (Math.max(was, 0) != Math.max(is, 0))
			{
				return true;
			}
		}
		return false;
	}
}
