/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class WordsTest
{
	@Test
	public void everyKeyHasEveryLanguage()
	{
		for (Words.Key key : Words.Key.values())
		{
			for (CombatCheckConfig.Language language : CombatCheckConfig.Language.values())
			{
				String text = Words.of(language, key);
				org.junit.Assert.assertNotNull(text);
				org.junit.Assert.assertFalse(key + " in " + language, text.isEmpty());
			}
		}
	}

	@Test
	public void theNoticesAreWordedInTheLanguage()
	{
		assertEquals("Wrong prayer: Rigour", Words.prayerNotice(CombatCheckConfig.Language.ENGLISH, "Wrong prayer: Rigour"));
		assertEquals("Prayer errada: Rigour", Words.prayerNotice(CombatCheckConfig.Language.PORTUGUESE, "Wrong prayer: Rigour"));
		assertEquals("Piety desligado", Words.prayerNotice(CombatCheckConfig.Language.PORTUGUESE, "Piety off"));
		assertEquals("Nahkampf-Gebet aus", Words.prayerNotice(CombatCheckConfig.Language.GERMAN, "Melee prayer off"));
		// names of items are not translated
		assertEquals("Diamond +3% (quiver)", Words.bolts(CombatCheckConfig.Language.PORTUGUESE, "Diamond +3% (quiver)"));
		org.junit.Assert.assertTrue(Words.of(CombatCheckConfig.Language.GERMAN, Words.Key.CHECK_BLOWPIPE).contains("lowpipe"));
		assertEquals("Ruby +15%", Words.bolts(CombatCheckConfig.Language.ENGLISH, "Ruby +15%"));
		assertEquals("faltam 2", String.format(Words.of(CombatCheckConfig.Language.PORTUGUESE, Words.Key.PIECES_LEFT), 2));
	}
}
