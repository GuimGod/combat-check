/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * The damage engine stands on its own: none of its classes imports or calls the plugin, so that it can be copied
 * as it is into another plugin (the CA Planner). Read from the source files.
 */
public class EngineBoundaryTest
{
	private static final String[] ENGINE = {"SetCalc", "SetEffects", "StyleCalc", "StyleData", "SwitchSet", "CoxScaling",
		"DefenceDrains", "TobScaling", "ToaScaling", "MonsterLevels", "Weapons", "RemoteData"};

	@Test
	public void noEngineClassNamesThePlugin() throws IOException
	{
		Path dir = Paths.get("src", "main", "java", "com", "combatcheck");
		for (String name : ENGINE)
		{
			Path file = dir.resolve(name + ".java");
			assertTrue(file + " is missing", Files.isRegularFile(file));
			String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
			assertFalse(name + ".java names CombatCheckPlugin", source.contains("CombatCheckPlugin"));
		}
	}
}
