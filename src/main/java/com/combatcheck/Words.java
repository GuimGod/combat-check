/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

/**
 * The few words the plugin writes on screen, in the languages of its "Language" setting. Names of items,
 * weapons, prayers and attack styles are the game's own and are never translated.
 */
final class Words
{
	enum Key
	{
		WRONG_PRAYER, PRAYER_OFF, PRAYER_STILL_ON, CHECK_BLOWPIPE, PIECES_LEFT, PIECES_OFF,
		MELEE_PRAYER, RANGED_PRAYER, MAGIC_PRAYER, RUBY, DIAMOND, QUIVER, SPEC_ESTIMATED
	}

	/** One row per key, in the order of {@link CombatCheckConfig.Language}: English, Portuguese, Spanish, German, French. */
	private static final String[][] TEXT = {
		{"Wrong prayer", "Prayer errada", "Rezo equivocado", "Falsches Gebet", "Mauvaise pri\u00e8re"},
		{"%s off", "%s desligado", "%s apagado", "%s aus", "%s d\u00e9sactiv\u00e9"},
		{"Prayer still on", "Prayer ainda ligada", "Rezo a\u00fan activo", "Gebet noch an", "Pri\u00e8re encore active"},
		{"Check your blowpipe", "D\u00ea Check na blowpipe", "Usa Check en la blowpipe", "Blowpipe: Check benutzen", "Fais Check sur la blowpipe"},
		{"%d left", "faltam %d", "faltan %d", "%d fehlen", "%d restants"},
		{"%d off", "tire %d", "quita %d", "%d ablegen", "%d \u00e0 retirer"},
		{"Melee prayer", "Prayer de melee", "Rezo de melee", "Nahkampf-Gebet", "Pri\u00e8re de m\u00eal\u00e9e"},
		{"Ranged prayer", "Prayer de ranged", "Rezo a distancia", "Fernkampf-Gebet", "Pri\u00e8re \u00e0 distance"},
		{"Magic prayer", "Prayer de magia", "Rezo de magia", "Magie-Gebet", "Pri\u00e8re de magie"},
		// names of items stay as the game has them, in every language
		{"Ruby", "Ruby", "Ruby", "Ruby", "Ruby"},
		{"Diamond", "Diamond", "Diamond", "Diamond", "Diamond"},
		{"quiver", "quiver", "quiver", "quiver", "quiver"},
		// the note after the figures worked out with a special attack estimated ("spec" as players say it everywhere)
		{"spec estimated", "spec estimada", "spec estimado", "Spec geschätzt", "spec estimé"},
	};

	private Words()
	{
	}

	static String of(CombatCheckConfig.Language language, Key key)
	{
		int column = language == null ? 0 : language.ordinal();
		String[] row = TEXT[key.ordinal()];
		return column < row.length ? row[column] : row[0];
	}

	/** The attack prayer reminder as the plugin words it in English ("Wrong prayer: Rigour", "Piety off"), in the language. */
	static String prayerNotice(CombatCheckConfig.Language language, String english)
	{
		String wrong = "Wrong prayer: ";
		if (english.startsWith(wrong))
		{
			return of(language, Key.WRONG_PRAYER) + ": " + english.substring(wrong.length());
		}
		if (english.endsWith(" off"))
		{
			String name = english.substring(0, english.length() - 4);
			Key general = name.equals("Melee prayer") ? Key.MELEE_PRAYER : name.equals("Ranged prayer") ? Key.RANGED_PRAYER
				: name.equals("Magic prayer") ? Key.MAGIC_PRAYER : null;
			return String.format(of(language, Key.PRAYER_OFF), general == null ? name : of(language, general));
		}
		return english;
	}

	/** The bolts reminder ("Diamond +3% (quiver)") in the language. */
	static String bolts(CombatCheckConfig.Language language, String english)
	{
		if (english == null || language == null || language == CombatCheckConfig.Language.ENGLISH)
		{
			return english;
		}
		return english.replace("Diamond", of(language, Key.DIAMOND)).replace("Ruby", of(language, Key.RUBY))
			.replace("(quiver)", "(" + of(language, Key.QUIVER) + ")");
	}
}
