/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;

/**
 * The defence drain settings: one "Estimate specs" option for the Elder maul, Dragon warhammer and Bandos godsword, on by
 * default and on when either old option was on; the drains of the player and the party counted with no option at all.
 */
public class ConfigMigrationTest
{
	/** "Estimate specs" is on when either old option was on; what is saved for it already holds otherwise. */
	@Test
	public void eitherOldOptionOnTurnsItOn()
	{
		// a profile with estimateSpecs=true, estimateBgs=false: stays on, nothing to write
		assertNull(CombatCheckPlugin.mergedEstimate("true", "false"));
		// never saved: the default, on
		assertNull(CombatCheckPlugin.mergedEstimate(null, null));
		assertNull(CombatCheckPlugin.mergedEstimate(null, "true"));
		assertNull(CombatCheckPlugin.mergedEstimate("true", "true"));
		// the maul and hammer off, the godsword on: on
		assertEquals(Boolean.TRUE, CombatCheckPlugin.mergedEstimate("false", "true"));
		// both off: stays off
		assertNull(CombatCheckPlugin.mergedEstimate("false", "false"));
		assertNull(CombatCheckPlugin.mergedEstimate("false", null));
	}

	/** One option for the estimate, on by default; none for the BGS alone, none for counting the drains. */
	@Test
	public void theOptions()
	{
		CombatCheckConfig config = new CombatCheckConfig()
		{
		};
		assertTrue(config.estimateSpecs());
		boolean estimate = false;
		for (Method m : CombatCheckConfig.class.getDeclaredMethods())
		{
			ConfigItem item = m.getAnnotation(ConfigItem.class);
			if (item == null)
			{
				continue;
			}
			assertTrue(item.keyName(), !item.keyName().equals("countDrains") && !item.keyName().equals("estimateBgs"));
			assertTrue(item.name(), !item.name().contains("outside the party"));
			estimate |= item.keyName().equals("estimateSpecs") && item.name().equals("Estimate specs");
		}
		assertTrue(estimate);
	}
}
