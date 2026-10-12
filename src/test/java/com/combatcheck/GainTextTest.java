/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GainTextTest
{
	@Test
	public void smallGainsKeepOneDecimal()
	{
		assertEquals("+0.1%", CombatCheckPlugin.gainText(0.13));
		assertEquals("+4.5%", CombatCheckPlugin.gainText(4.46));
	}

	@Test
	public void largeGainsAreWholeNumbers()
	{
		assertEquals("+10%", CombatCheckPlugin.gainText(9.96));
		assertEquals("+28%", CombatCheckPlugin.gainText(28.3));
	}
}
