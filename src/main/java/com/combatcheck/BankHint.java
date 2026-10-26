/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The weapon of the bank that would deal the most to the monster fought: the weapon only, never a set or a shield,
 * against the set worn now. One weapon at a time goes in the place of the one in hand, everything else worn kept as it
 * is; a weapon of two hands takes the shield off. It is counted the way the weapon notice counts a carried weapon
 * (damage per second, each class with its own attack prayer) and shown only when it deals at least the minimum more,
 * outside the raids.
 *
 * <p>The bank is the one last seen open: until it is, there is no line. A weapon with no charges, or one that needs
 * ammunition the player does not have on (worn or in Dizana's quiver), is left out; another dress of a weapon (an
 * ornament kit, a recolour) is the weapon itself ({@link Variants}, the plugin's {@code itemOf}).
 */
final class BankHint
{
	/** The minimum gain by default, in percent. */
	static final double DEFAULT_MINIMUM = 10;

	private BankHint()
	{
	}

	/**
	 * True outside the raids, where the line is shown: not inside the Chambers of Xeric, not in the Theatre of Blood
	 * (its party status 2 and up: inside or watching), not in a room of the Tombs of Amascut, not in a party of the
	 * Fractured Archive (its places that hold someone), and never on a monster of any of the four raids, wherever it is
	 * read.
	 */
	static boolean allowed(boolean inChambers, int theatreStatus, boolean inTombs, int archiveMembers, StyleData.Monster monster)
	{
		if (inChambers || theatreStatus >= 2 || inTombs || archiveMembers > 0)
		{
			return false;
		}
		return monster == null || !(monster.fracturedArchive || monster.coxScaling != 0 || monster.tobScaling != 0
			|| monster.toa);
	}

	/** True for a weapon of the bank that may be counted: not one without its charges. */
	static boolean usable(String weaponName)
	{
		return weaponName != null && !Weapons.uncharged(weaponName) && !Weapons.cannotAttackUncharged(weaponName);
	}

	/**
	 * True when the weapon can attack with what is on: one that shoots no ammunition, or a bow or a crossbow with
	 * ammunition it fires in the slot or in Dizana's quiver ({@code ammo}, the plugin's shotAmmo).
	 */
	static boolean firesWhatIsOn(int itemId, String category, String weaponName, SwitchSet.Piece ammo)
	{
		if (Weapons.ammoKind(category, weaponName) == null)
		{
			return true;
		}
		return ammo != null && Weapons.canFire(itemId, category, weaponName, ammo.itemId, ammo.name);
	}

	/**
	 * What is worn with the bank's weapon in hand: the same pieces, the weapon changed; a weapon of two hands takes the
	 * shield off.
	 */
	static SwitchSet.Piece[] withWeapon(SwitchSet.Piece[] worn, SwitchSet.Piece weapon)
	{
		SwitchSet.Piece[] with = worn.clone();
		with[SwitchSet.WEAPON] = weapon;
		if (weapon != null && weapon.twoHanded && with.length > SwitchSet.SHIELD)
		{
			with[SwitchSet.SHIELD] = null;
		}
		return with;
	}

	/** One weapon of the bank as it would be counted: its item, its set and the figure per tick (-1 for none). */
	static final class Candidate
	{
		final int itemId;
		final String name;
		final SetCalc.Loadout loadout;
		double perTick = -1;

		Candidate(int itemId, String name, SetCalc.Loadout loadout)
		{
			this.itemId = itemId;
			this.name = name;
			this.loadout = loadout;
		}
	}

	/** The weapon the line names and how much more it deals, in percent. */
	static final class Result
	{
		final int itemId;
		final String name;
		final double gain;

		Result(int itemId, String name, double gain)
		{
			this.itemId = itemId;
			this.name = name;
			this.gain = gain;
		}
	}

	/**
	 * The best of the candidates over {@code current} (the figure per tick of what is worn now), when it deals at least
	 * {@code minimum} percent more; null otherwise. Each candidate is counted with the attack prayer of its own class
	 * ({@link SetCalc#withPrayerOf}), as the weapon notice counts a carried weapon; one that has no figure is left out.
	 */
	static Result best(double current, List<Candidate> candidates, SetCalc.Player stats, SetCalc.Target target,
		double minimum)
	{
		if (current < 0)
		{
			return null;
		}
		Result best = null;
		for (Candidate c : candidates)
		{
			if (!SetCalc.plain(c.loadout))
			{
				continue;
			}
			String cls = SetCalc.combatClass(c.loadout);
			c.perTick = SetCalc.perTick(c.loadout, SetCalc.withPrayerOf(stats, cls), target, false);
			if (c.perTick < 0)
			{
				continue;
			}
			double gain = current == 0 ? (c.perTick > 0 ? Double.POSITIVE_INFINITY : Double.NaN) : SetCalc.gain(current, c.perTick);
			if (CombatCheckPlugin.pointsAt(gain, minimum) && (best == null || gain > best.gain))
			{
				best = new Result(c.itemId, c.name, gain);
			}
		}
		return best;
	}

	/**
	 * The weapons of the bank last seen open, by item (each dress as its weapon, once), and a count of the times it was
	 * read: what the line is worked out from changes only with it, the monster and what is worn.
	 */
	static final class Memory
	{
		private List<Integer> weapons;
		private int version;
		private Object key;
		private Result last;

		/** The bank was seen open with these weapons in it. */
		void bank(List<Integer> weaponIds)
		{
			List<Integer> distinct = new ArrayList<>();
			for (Integer id : weaponIds)
			{
				if (id != null && id > 0 && !distinct.contains(id))
				{
					distinct.add(id);
				}
			}
			if (!distinct.equals(weapons))
			{
				weapons = Collections.unmodifiableList(distinct);
				version++;
			}
		}

		/** The weapons of the bank last seen, or null when it was never seen open. */
		List<Integer> weapons()
		{
			return weapons;
		}

		int version()
		{
			return version;
		}

		/**
		 * True when the line must be worked out again for this key (the monster, what is worn, the bank's version, the
		 * setting): it is kept as it was otherwise.
		 */
		boolean changed(Object newKey)
		{
			return !Objects.equals(key, newKey);
		}

		/** The line worked out for this key. */
		void remember(Object newKey, Result result)
		{
			key = newKey;
			last = result;
		}

		Result last()
		{
			return last;
		}

		void forgetLine()
		{
			key = null;
			last = null;
		}

		/** Logged out: the next account's bank is not this one's, so nothing is named until it is seen open. */
		void clear()
		{
			weapons = null;
			version++;
			forgetLine();
		}
	}
}
