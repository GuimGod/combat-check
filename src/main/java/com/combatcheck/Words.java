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
		MELEE_PRAYER, RANGED_PRAYER, MAGIC_PRAYER, RUBY, DIAMOND, QUIVER, SPEC_ESTIMATED, DEFENCE_LEFT, SPECS, ONE_SPEC, MAGIC_LEFT, LOW_HP,
		BASH_ONE, BASH_MANY, DEFENCE_UNKNOWN, BANK
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
		// the monster's Defence before the special attacks and now ("Def 246 -> 158"); ASCII for the game's font
		{"Def %d -> %s", "Def %d -> %s", "Def %d -> %s", "Vert %d -> %s", "Déf %d -> %s"},
		{"%d specs", "%d specs", "%d specs", "%d Specs", "%d specs"},
		{"1 spec", "1 spec", "1 spec", "1 Spec", "1 spec"},
		// the same for the Magic level, which a magic weapon rolls against on most monsters
		{"Magic %d -> %s", "Magic %d -> %s", "Magia %d -> %s", "Magie %d -> %s", "Magie %d -> %s"},
		// after the weapon notice's figure when it is the time to kill that points at the weapon ("+117% (low HP)")
		{"(low HP)", "(pouca vida)", "(poca vida)", "(wenig LP)", "(peu de PV)"},
		// Dinh's bulwark's special attack: on the monster fought, mean and max of its two hits ("Spec: 41.2 (max 96)")
		{"Spec: %s (max %d)", "Spec: %s (m\u00e1x %d)", "Spec: %s (m\u00e1x %d)", "Spec: %s (max %d)", "Spec: %s (max %d)"},
		// and on the others in reach, one hit each ("Spec 4 targets: 41.2 (max 96), others 20.6 (max 48)")
		{"Spec %d targets: %s (max %d), others %s (max %d)", "Spec %d alvos: %s (m\u00e1x %d), outros %s (m\u00e1x %d)",
			"Spec %d objetivos: %s (m\u00e1x %d), otros %s (m\u00e1x %d)", "Spec %d Ziele: %s (max %d), andere %s (max %d)",
			"Spec %d cibles : %s (max %d), autres %s (max %d)"},
		// the monster's Defence when it is not known
		{"Def ?", "Def ?", "Def ?", "Vert ?", "Déf ?"},
		// the best weapon in the bank and how much more it would deal ("Bank: Scythe of vitur +35%")
		{"Bank: %s %s", "Banco: %s %s", "Banco: %s %s", "Bank: %s %s", "Banque : %s %s"},
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

	/** The weapon notice ("+117% (low HP)") in the language. */
	static String weapon(CombatCheckConfig.Language language, String english)
	{
		if (english == null || language == null || language == CombatCheckConfig.Language.ENGLISH)
		{
			return english;
		}
		return english.replace(CombatCheckPlugin.LOW_HP, of(language, Key.LOW_HP));
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
