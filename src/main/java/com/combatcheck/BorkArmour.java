/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

/**
 * Bork of the Fractured Archive's flat armour and magic resistance, by the Smorks' health (OSRS Wiki, Bork, 2026-10-24:
 * "starting at 8 flat armour and 0 magic resistance at 100% health, and adding 2 flat armour and magic resistance every
 * 25% damage the Smorks take"): 8 and 0 with the Smorks at full health, 10 and 2 from 75% left, 12 and 4 from 50%, 14
 * and 6 from 25%. Stunned, his flat armour is 0 ({@link BorkStun}; the wiki says nothing of the magic resistance then,
 * so it is kept).
 *
 * <p>The quarters lost are read from what the game shows, the most of three: the shouts at 75%, 50% and 25% (the
 * recorder, 2026-10-20, 79 fights: they come on the very tick of the hit that takes the Smorks past the mark, from the
 * Smorks and from Bork alike), the Smorks' boss health display when it shows them (exact), and their health bar over
 * them, counted only once its rounding leaves no doubt. Never fewer for the rest of the fight: a new Bork, or Bork gone,
 * starts it again.
 */
final class BorkArmour
{
	/** The Smorks' NPC id. */
	static final int SMORKS = 16641;
	/** What each quarter of the Smorks' health lost adds to Bork's flat armour and magic resistance. */
	static final int STEP = 2;
	/** Past the third quarter nothing more is added (14 and 6). */
	static final int QUARTERS = 3;

	/** The shouts at the Smorks' 75%, 50% and 25%: one of each pair, from the Smorks and from Bork. */
	private static final String[][] SHOUTS = {
		{"Smorks go faster!", "Bork get into it!"},
		{"Smorks feeling drum beat now!", "Bork make fast drum!"},
		{"Smorks go fastest!", "Bork maximum crush!"},
	};

	/** The quarters of the Smorks' health lost in this fight, as read so far. */
	private int quarters;

	/** What Bork or the Smorks say: a shout at a quarter of the Smorks' health. */
	void says(int npcId, String text)
	{
		if (npcId == BorkStun.BORK || npcId == SMORKS)
		{
			seen(said(text));
		}
	}

	/** The quarters a shout marks, 0 for any other words. */
	static int said(String text)
	{
		if (text != null)
		{
			for (int i = 0; i < SHOUTS.length; i++)
			{
				for (String shout : SHOUTS[i])
				{
					if (text.startsWith(shout))
					{
						return i + 1;
					}
				}
			}
		}
		return 0;
	}

	/** The Smorks' health exactly, from the boss health display: {@code health} of {@code max}. */
	void display(int health, int max)
	{
		seen(fromHealth(health, max));
	}

	/** The quarters lost at {@code health} of {@code max}: a quarter from 75% left. 0 when not known. */
	static int fromHealth(int health, int max)
	{
		if (max <= 0 || health < 0)
		{
			return 0;
		}
		return (int) Math.min(QUARTERS, 4L * (max - Math.min(health, max)) / max);
	}

	/** The Smorks' health bar over them: {@code ratio} of {@code scale} (-1 while it is not shown). */
	void bar(int ratio, int scale)
	{
		seen(fromBar(ratio, scale));
	}

	/**
	 * The quarters lost for certain at a health bar of {@code ratio} of {@code scale}. The game makes the ratio
	 * 1 + (scale - 1) x health / max, rounded down, for a monster alive (RuneLite's own reading of it, in its hiscore
	 * utilities), so the health is under ratio / (scale - 1) of the max: k quarters are lost once that is no more than
	 * (4 - k) / 4. Of 40, 29 is a quarter lost and 30 is not yet sure (74.4% to 76.9%); 0 is the Smorks dead.
	 */
	static int fromBar(int ratio, int scale)
	{
		if (ratio < 0 || scale <= 1)
		{
			return 0;
		}
		for (int k = QUARTERS; k > 0; k--)
		{
			if (4L * ratio <= (long) (4 - k) * (scale - 1))
			{
				return k;
			}
		}
		return 0;
	}

	private void seen(int q)
	{
		quarters = Math.max(quarters, Math.min(QUARTERS, q));
	}

	/** The quarters of the Smorks' health lost in this fight, 0 to 3. */
	int quarters()
	{
		return quarters;
	}

	/** Bork's flat armour with {@code quarters} of the Smorks' health lost, from his row's ({@code base}, 8). */
	static int flatArmour(int base, int quarters)
	{
		return base + STEP * quarters;
	}

	/** Bork's magic resistance with {@code quarters} of the Smorks' health lost: taken off every magic hit that lands. */
	static int magicResistance(int quarters)
	{
		return STEP * quarters;
	}

	/**
	 * Bork's armour on the target {@code t}: the flat armour from his row's ({@code base}, 8) and the magic resistance for
	 * {@code quarters} of the Smorks' health lost; stunned, the flat armour 0.
	 */
	static void apply(SetCalc.Target t, int base, int quarters, boolean stunned)
	{
		t.flatArmour = stunned ? 0 : flatArmour(base, quarters);
		t.magicResistance = magicResistance(quarters);
	}

	/** A Bork that comes into the scene or leaves it: another fight. */
	void bork(int npcId)
	{
		if (npcId == BorkStun.BORK)
		{
			clear();
		}
	}

	void clear()
	{
		quarters = 0;
	}
}
