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
 * The defence drain settings: one "Mark estimated specs" option for the Elder maul, Dragon warhammer and Bandos godsword,
 * off by default and on when either old option was on; the drains of the player, the party and the estimates counted with
 * no option at all; the raids always scaled, with no option.
 */
public class ConfigMigrationTest
{
	/** "Mark estimated specs" is on when either old option was on; what is saved for it already holds otherwise. */
	@Test
	public void eitherOldOptionOnTurnsItOn()
	{
		// a profile with estimateSpecs=true, estimateBgs=false: stays on, nothing to write
		assertNull(CombatCheckPlugin.mergedEstimate("true", "false"));
		// never saved: the default, off
		assertNull(CombatCheckPlugin.mergedEstimate(null, null));
		assertNull(CombatCheckPlugin.mergedEstimate(null, "true"));
		assertNull(CombatCheckPlugin.mergedEstimate("true", "true"));
		// the maul and hammer off, the godsword on: on
		assertEquals(Boolean.TRUE, CombatCheckPlugin.mergedEstimate("false", "true"));
		// both off: stays off
		assertNull(CombatCheckPlugin.mergedEstimate("false", "false"));
		assertNull(CombatCheckPlugin.mergedEstimate("false", null));
	}

	/**
	 * One option to mark the estimates, off by default; none for the BGS alone, none for counting the drains, none for the
	 * raids' scaling. "Show the Defence left" off by default.
	 */
	@Test
	public void theOptions()
	{
		CombatCheckConfig config = new CombatCheckConfig()
		{
		};
		assertTrue(!config.estimateSpecs());
		assertTrue(!config.showDefence());
		boolean estimate = false;
		for (Method m : CombatCheckConfig.class.getDeclaredMethods())
		{
			ConfigItem item = m.getAnnotation(ConfigItem.class);
			if (item == null)
			{
				continue;
			}
			assertTrue(item.keyName(), !item.keyName().equals("countDrains") && !item.keyName().equals("estimateBgs")
				&& !item.keyName().endsWith("Scaling"));
			assertTrue(item.name(), !item.name().contains("outside the party"));
			estimate |= item.keyName().equals("estimateSpecs") && item.name().equals("Mark estimated specs");
		}
		assertTrue(estimate);
	}
	/**
	 * "Weapon: minimum gain" is a decimal number: 1% by default, a whole number saved when it was an int reads the
	 * same, half a percent can be set, and 0, which points at any gain that shows as +0.1%; kept from 0 to 200.
	 */
	@Test
	public void theMinimumGainTakesHalfAPercent() throws Exception
	{
		CombatCheckConfig config = new CombatCheckConfig()
		{
		};
		assertEquals(1.0, config.weaponMinimumGain(), 0);
		Method m = CombatCheckConfig.class.getDeclaredMethod("weaponMinimumGain");
		assertEquals(double.class, m.getReturnType());
		assertEquals("weaponMinimumGain", m.getAnnotation(ConfigItem.class).keyName());
		// RuneLite reads a double setting with Double.parseDouble: what an int setting saved ("1", "3") reads the same
		assertEquals(1.0, CombatCheckPlugin.minimumGain(Double.parseDouble("1")), 0);
		assertEquals(3.0, CombatCheckPlugin.minimumGain(Double.parseDouble("3")), 0);
		assertEquals(0.5, CombatCheckPlugin.minimumGain(Double.parseDouble("0.5")), 0);
		assertEquals(0, CombatCheckPlugin.minimumGain(0), 0);
		assertEquals(0, CombatCheckPlugin.minimumGain(-2), 0);
		// with 0: +0.1% is pointed at, a gain that would read +0.0% is not, nor none at all
		assertTrue(CombatCheckPlugin.pointsAt(0.09, 0));
		assertEquals("+0.1%", CombatCheckPlugin.gainText(0.09));
		assertTrue(!CombatCheckPlugin.pointsAt(0.04, 0));
		assertTrue(!CombatCheckPlugin.pointsAt(0, 0));
		assertTrue(!CombatCheckPlugin.pointsAt(Double.NaN, 0));
		assertEquals(200, CombatCheckPlugin.minimumGain(500), 0);
		// a weapon 0.6% better is pointed at with 0.5, not with 1; "(low HP)" follows the same minimum
		assertTrue(CombatCheckPlugin.pointsAt(0.6, CombatCheckPlugin.minimumGain(0.5)));
		assertTrue(!CombatCheckPlugin.pointsAt(0.6, CombatCheckPlugin.minimumGain(1)));
		assertTrue(CombatCheckPlugin.killsSooner(0.3, 0.6, 0.5));
		assertTrue(!CombatCheckPlugin.killsSooner(0.3, 0.6, 1));
	}
}
