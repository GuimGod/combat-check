/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.HashSet;
import java.util.Set;

/**
 * Xarth (the Fractured Archive) out of melee's reach until his first charge. In the first phase a shield takes melee until the first
 * charge goes off: 16 of 16 crush hits on him before it dealt 0 (examined in the raid), and in recorded fights of
 * 2026-10-20 to 11-01 no melee hit landed before it, 2,024 after it. The charge is an animation everyone sees
 * (14992, and 14989 three ticks later, the same as the explosions of his second phase); in 167 fights it came with him
 * at 84% to 96% of his health.
 *
 * <p>What is read is what is on the screen: an animation of his seen since he came into view, and his health bar. Out of
 * reach while no charge has been seen on him and his bar shows 84% or more (or none: not hit yet); a charge seen, or his
 * bar under 84% (a charge the plugin did not see: turned on, or come back into view, after it), and melee is counted as
 * before. Nothing tells when the charge will come.
 */
final class XarthShield
{
	static final int XARTH = 16684;
	/** The charge of the first phase (and its explosion, the same animation as the second phase's explosions). */
	static final int CHARGE = 14992;
	static final int EXPLOSION = 14989;
	/** The least health, in percent, he was seen with before his first charge (84 to 96% in 167 fights). */
	static final int LEAST_HEALTH_BEFORE_CHARGE = 84;

	/** The NPC indices of the Xarths a charge was seen on. */
	private final Set<Integer> charged = new HashSet<>();

	/** An animation of an NPC: a charge or an explosion of Xarth's ends the shield. */
	void animation(int npcIndex, int npcId, int animation)
	{
		if (npcId == XARTH && (animation == CHARGE || animation == EXPLOSION))
		{
			charged.add(npcIndex);
		}
	}

	/** Xarth died: his index may be another Xarth's in the next raid. */
	void died(int npcIndex)
	{
		charged.remove(npcIndex);
	}

	void clear()
	{
		charged.clear();
	}

	/**
	 * True while this Xarth's shield takes melee: no charge seen on him, and his health bar at 84% or more, or not
	 * shown ({@code healthPercent} below 0).
	 */
	boolean shielded(int npcIndex, int npcId, int healthPercent)
	{
		return npcId == XARTH && !charged.contains(npcIndex)
			&& (healthPercent < 0 || healthPercent >= LEAST_HEALTH_BEFORE_CHARGE);
	}

	/** The health bar as a percent, -1 when it is not shown. */
	static int healthPercent(int ratio, int scale)
	{
		return ratio < 0 || scale <= 0 ? -1 : ratio * 100 / scale;
	}
}
