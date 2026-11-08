/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;

/**
 * The "Defence left" line: the monster's Defence before the special attacks and now ("Def 240 -> 180"), and under it one
 * row per weapon that lowered it, with the weapon's icon ("Ralos 1 spec (2hits)", "Maul x1"). Before, one count of all the special attacks ("(3 specs)") said neither which weapon nor, for the Tonalztics
 * of Ralos, whether a count was a special attack or one of its two hits.
 */
final class DefenceLine
{
	/** One weapon's row: its icon and its words. */
	static final class Row
	{
		final int itemId;
		final String text;

		Row(int itemId, String text)
		{
			this.itemId = itemId;
			this.text = text;
		}
	}

	final String head;
	final List<Row> rows;

	DefenceLine(String head, List<Row> rows)
	{
		this.head = head;
		this.rows = rows == null ? Collections.emptyList() : Collections.unmodifiableList(rows);
	}

	/** A line of words alone ("Def ?" with nothing counted). */
	static DefenceLine of(String head)
	{
		return head == null ? null : new DefenceLine(head, null);
	}

	/** The rows of these weapons, in their order; "~" before a count with an estimate in it when {@code markEstimate}. */
	static List<Row> rows(CombatCheckConfig.Language language, List<DrainTracker.WeaponCount> counts, boolean markEstimate)
	{
		List<Row> out = new ArrayList<>();
		for (DrainTracker.WeaponCount c : counts)
		{
			out.add(new Row(icon(c.weapon), rowText(language, c, markEstimate && c.estimated)));
		}
		return out;
	}

	/**
	 * One weapon's words: its short name, then for a weapon that hits more than once its special attacks and the hits that
	 * landed ("Ralos 1 spec (2hits)", "Ralos 2 specs (3hits)"), for the others how many ("Maul x1").
	 */
	static String rowText(CombatCheckConfig.Language language, DrainTracker.WeaponCount c, boolean estimated)
	{
		String mark = estimated ? "~" : "";
		if (DrainTracker.multiHit(c.weapon))
		{
			String specs = c.specs == 1 ? Words.of(language, Words.Key.ONE_SPEC)
				: String.format(Words.of(language, Words.Key.SPECS), c.specs);
			String hits = c.hits == 1 ? Words.of(language, Words.Key.ONE_HIT)
				: String.format(Words.of(language, Words.Key.HITS), c.hits);
			return shortName(c.weapon) + " " + mark + specs + " " + hits;
		}
		return shortName(c.weapon) + " " + mark + "x" + c.specs;
	}

	/** The name players use for the weapon (the game's own names, shortened; never translated). */
	static String shortName(SpecialWeapon weapon)
	{
		switch (weapon)
		{
			case TONALZTICS_OF_RALOS:
				return "Ralos";
			case ELDER_MAUL:
				return "Maul";
			case DRAGON_WARHAMMER:
				return "DWH";
			case BANDOS_GODSWORD:
				return "BGS";
			case ARCLIGHT:
				return "Arclight";
			case DARKLIGHT:
				return "Darklight";
			case EMBERLIGHT:
				return "Emberlight";
			case SEERCULL:
				return "Seercull";
			case EYE_OF_AYAK:
				return "Ayak";
			case ACCURSED_SCEPTRE:
				return "Accursed";
			case BONE_DAGGER:
				return "Bone dagger";
			case DORGESHUUN_CROSSBOW:
				return "Dorgeshuun cbow";
			case BARRELCHEST_ANCHOR:
				return "Anchor";
			default:
				return weapon.getName();
		}
	}

	/** The item whose icon the row shows: the weapon's first item in the Special Attack Counter's list. */
	static int icon(SpecialWeapon weapon)
	{
		int[] ids = weapon.getItemID();
		return ids == null || ids.length == 0 ? -1 : ids[0];
	}
}
