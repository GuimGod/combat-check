/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

/**
 * Moia out of melee's reach but in the middle of her arena. She starts outside the arena, where "melee and halberds cannot reach her" (OSRS Wiki, The Fractured
 * Archive/Strategies), and below a third of her health she goes to the middle and stays there, as anyone sees.
 *
 * <p>Measured in recorded fights of 2026-10-20 to 31 (13 fights, her south-west tile on the template map): outside on
 * one of four tiles, 4055,5662, 4062,5669, 4069,5662 and 4062,5655, seven tiles from the middle each, and at the end on
 * 4062,5662, the middle; 63 Obligator and 3 Inquisitor's mace hits landed on her there (up to 60), none outside.
 * Read by the tile at her centre (south-west plus one), which a rotated instance does not move.
 */
final class MoiaReach
{
	/** Moia (OSRS Wiki, Moia). */
	static final int MOIA = 16667;
	/** The tile at her centre in the middle of the arena, on the template map. */
	static final int MIDDLE_X = 4063;
	static final int MIDDLE_Y = 5663;
	/** Up to this many tiles from the middle is in it: outside she is seven away. */
	static final int NEAR = 2;

	private MoiaReach()
	{
	}

	/** True while melee does not reach a Moia whose centre is on this tile of the template map. */
	static boolean outOfReach(int centreX, int centreY)
	{
		return Math.max(Math.abs(centreX - MIDDLE_X), Math.abs(centreY - MIDDLE_Y)) > NEAR;
	}
}
