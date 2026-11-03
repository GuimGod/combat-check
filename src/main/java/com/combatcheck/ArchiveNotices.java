/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

/**
 * Three reminders of the Fractured Archive, from a report of the raid (2026-11-01), each about
 * what the player himself does, told after he does it:
 * <ul>
 * <li>Dreadnoth's Jellies (no row: never examined): a chinchompa on Short fuse, where the Medium fuse is the one (a
 * simulator and recorded fights, 25.5 against 15.5 a second, +32.5%), and the Ancient Magicks cast on them, where a
 * chinchompa is the one (the rooms with chinchompas on 80% or more of the Jellies about 7% faster; who cast Barrage and
 * throws instead gains 6 to 9 seconds a wave).</li>
 * <li>Ocellus's last stage: a damage special attack used during its six explosions, where he takes half the damage (OSRS
 * Wiki). The explosions are the graphic everyone sees on the floor (4339,
 * every 2 ticks, six times: 447 of 490 runs in the recordings of 2026-10-20 to 11-01).</li>
 * <li>The special attack bar left unused: half of it or more for 50 ticks with a melee weapon in hand against a monster
 * of the raid melee reaches, and the Crimson kisten (50%) carried (the bar left at the end of the rooms, +0.22% of the
 * raid).</li>
 * </ul>
 */
final class ArchiveNotices
{
	/** Dreadnoth's Jelly. */
	static final int JELLY = 16666;
	/** Ocellus's third phase, where the last stage's explosions come. */
	static final int OCELLUS_LAST = 16677;
	/** The graphic of an explosion of Ocellus's last stage. */
	static final int EXPLOSION_GRAPHIC = 4339;
	/** An explosion seen this tick or up to 2 before: the explosions come every 2 ticks. */
	static final int EXPLOSION_TICKS = 2;
	/** How long a reminder told after the fact stays (a cast on a Jelly, a spec in the explosions): 8 ticks, about 5 s. */
	static final int NOTICE_TICKS = 8;
	/** A Jelly attacked this tick or up to this many before: the game shows no target between two attacks in the raid. */
	static final int JELLY_TICKS = 5;
	/**
	 * The casting animations: 10092 in the Fractured Archive for the Ancient Magicks (recorded fights of 2026-10-20 to
	 * 11-01: 283 casts of it on a Jelly followed by Ice or Blood Barrage's graphic on it), 1978 and 1979 the older ones.
	 */
	private static final int[] CAST_ANIMATIONS = {1978, 1979, 10092};
	/** The fuse to throw with on the Jellies. */
	static final String MEDIUM_FUSE = "Medium fuse";
	/** Half the bar, the Crimson kisten's special attack, in the game's tenths of a percent. */
	static final int KISTEN_COST = 500;
	/** The ticks the bar is left at half or more before the reminder: the 30 seconds that give back 10% of it. */
	static final int UNUSED_TICKS = 50;
	/** The item the reminder of the bar names. */
	static final String KISTEN = "Crimson kisten";

	/** A reminder: the item it shows, 0 for none, and its words. */
	static final class Notice
	{
		final int itemId;
		final String text;

		Notice(int itemId, String text)
		{
			this.itemId = itemId;
			this.text = text;
		}
	}

	private ArchiveNotices()
	{
	}

	/** True for a chinchompa on Short fuse thrown at a Jelly. */
	static boolean shortFuseOnJelly(boolean onJelly, String category, String styleName)
	{
		return onJelly && "Chinchompas".equals(category) && "Short fuse".equals(styleName);
	}

	/** True for one of the casting animations. */
	static boolean castAnimation(int animation)
	{
		for (int a : CAST_ANIMATIONS)
		{
			if (a == animation)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * True for the Ancient Magicks on a Jelly: a cast at it on the Ancient spellbook, or a burst or a barrage on autocast
	 * while attacking it.
	 */
	static boolean ancientsOnJelly(boolean onJelly, boolean ancientSpellbook, int animation, int autocastSpell)
	{
		return onJelly && (ancientSpellbook && castAnimation(animation) || SetEffects.isManyTargetSpell(autocastSpell));
	}

	/** True while an explosion of the last stage was seen this tick or up to 2 before. */
	static boolean inExplosions(int lastExplosionTick, int tick)
	{
		return lastExplosionTick >= 0 && tick >= lastExplosionTick && tick - lastExplosionTick <= EXPLOSION_TICKS;
	}

	/** True for a damage special attack used on Ocellus's last stage during its explosions. */
	static boolean specIntoExplosions(int npcId, String weaponName, int lastExplosionTick, int tick)
	{
		return npcId == OCELLUS_LAST && SetEffects.isDamageSpecialAttackWeapon(weaponName) && inExplosions(lastExplosionTick, tick);
	}

	/**
	 * True when the bar is left unused: {@code energy} (tenths of a percent) at half or more since {@code since} for 50
	 * ticks, with a melee weapon in hand against a monster of the raid melee reaches, and the Crimson kisten carried.
	 */
	static boolean specUnused(boolean archiveMonster, boolean meleeReaches, boolean meleeInHand, boolean kistenCarried,
		int energy, int since, int tick)
	{
		return archiveMonster && meleeReaches && meleeInHand && kistenCarried && energy >= KISTEN_COST && since >= 0
			&& tick - since >= UNUSED_TICKS;
	}

	/**
	 * The tick the bar has been at half or more since: kept while it stays there without being used, the tick of now
	 * when it gets there or is used and stays there, -1 under half.
	 */
	static int unusedSince(int since, int energyBefore, int energy, int tick)
	{
		if (energy < KISTEN_COST)
		{
			return -1;
		}
		return since < 0 || energyBefore >= 0 && energy < energyBefore ? tick : since;
	}
}
