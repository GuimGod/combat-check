/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/**
 * An item in another dress counts as the item: the radiant oathplate and the sanguine torva have the same bonuses as
 * the plain pieces under their own ids (RuneLite's item stats, read by id), no set effect (OSRS Wiki, Oathplate armour:
 * "does not have a passive effect"), and every rule this plugin keeps by name reads a dress, before the name or after
 * it, as the item.
 */
public class OrnamentsTest
{
	@Test
	public void armourInAnotherDressHasNoRuleOfItsOwn()
	{
		for (String[] pair : new String[][]{{"Oathplate chest", "Radiant oathplate chest"}, {"Oathplate helm", "Radiant oathplate helm"},
			{"Oathplate legs", "Radiant oathplate legs"}, {"Torva full helm", "Sanguine torva full helm"},
			{"Torva platebody", "Sanguine torva platebody"}, {"Torva platelegs", "Sanguine torva platelegs"}})
		{
			for (String name : pair)
			{
				assertFalse(name, SetEffects.keep(name));
				assertFalse(name, SetEffects.carriedForUse(name));
				assertEquals(name, 0, SetEffects.maskKind(name));
				assertEquals(name, 0, SetEffects.salveKind(name));
				assertEquals(name, 0, SetEffects.crystalWeight(name));
				assertEquals(name, 0, Weapons.inquisitorWeight(name));
			}
		}
	}

	@Test
	public void setsInAnotherDressAreTheSets()
	{
		// the Elite Void in its ornament kit
		assertEquals(2, SetEffects.voidKind(Arrays.asList("Void ranger helm (or)", "Elite void top (or)", "Elite void robe (or)",
			"Void knight gloves (or)"), StyleCalc.RANGED));
		assertEquals(SetEffects.MASK_IMBUED, SetEffects.maskKind("Twisted slayer helmet (i)"));
		assertTrue(Weapons.isFang("Osmumten's fang (or)"));
	}

	/** The Holy and the Sanguine Scythe of vitur deal what the Scythe does: three hits on a large monster. */
	@Test
	public void theScytheInAnotherDress() throws IOException
	{
		SetCalcTest.Case scythe = null;
		for (SetCalcTest.Case k : SetCalcTest.cases())
		{
			if (k.set.equals("scythe") && k.target.size >= 3)
			{
				scythe = k;
				break;
			}
		}
		assertNotNull(scythe);
		double plain = SetCalc.perTick(scythe.loadout, scythe.player, scythe.target, false);
		for (String dress : new String[]{"Holy scythe of vitur", "Sanguine scythe of vitur"})
		{
			scythe.loadout.weaponName = dress;
			assertEquals(dress, plain, SetCalc.perTick(scythe.loadout, scythe.player, scythe.target, false), 1e-12);
		}
	}

	/** The weapons in another dress are in the table, and their special attacks are the plain ones'. */
	@Test
	public void weaponsInAnotherDress() throws IOException
	{
		StyleData data = new StyleData();
		for (int id : new int[]{ItemID.SCYTHE_OF_VITUR_OR, ItemID.SCYTHE_OF_VITUR_BL, ItemID.GHRAZI_RAPIER_OR,
			ItemID.SANGUINESTI_STAFF_OR, ItemID.OSMUMTENS_FANG_ORNAMENT, ItemID.ELDER_MAUL_ORNAMENT,
			ItemID.BH_DRAGON_CLAWS_CORRUPTED, ItemID.TOXIC_BLOWPIPE_LOADED_ORNAMENT})
		{
			assertNotNull(id + " in the table", data.category(id));
		}
		assertEquals(SpecialWeapon.ELDER_MAUL, CombatCheckPlugin.specialWeapon(ItemID.ELDER_MAUL_ORNAMENT));
		assertEquals(SpecialWeapon.ELDER_MAUL, SpecEstimate.weapon(11124, ItemID.ELDER_MAUL_ORNAMENT));
	}

	/**
	 * Every dress of the table (Variants) has its item's special attack, in the hand and seen on another player.
	 */
	@Test
	public void everyDressHasItsItemsSpecialAttack()
	{
		java.util.List<String> wrong = new java.util.ArrayList<>();
		for (java.util.Map.Entry<Integer, Integer> e : Variants.all().entrySet())
		{
			SpecialWeapon dress = CombatCheckPlugin.specialWeapon(e.getKey());
			SpecialWeapon item = CombatCheckPlugin.specialWeapon(e.getValue());
			if (dress != item)
			{
				wrong.add(Variants.name(e.getKey()) + " " + e.getKey() + ": " + dress + ", " + Variants.name(e.getValue()) + " "
					+ e.getValue() + ": " + item);
			}
		}
		assertTrue(String.join("\n", wrong), wrong.isEmpty());
		assertEquals(SpecialWeapon.DRAGON_WARHAMMER, CombatCheckPlugin.specialWeapon(ItemID.BH_DRAGON_WARHAMMER_CORRUPTED));
		assertEquals(SpecialWeapon.BANDOS_GODSWORD, CombatCheckPlugin.specialWeapon(20370)); // Bandos godsword (or)
		// seen on another player: the Dragon warhammer, the Elder maul and the Bandos godsword in another dress
		assertEquals(SpecialWeapon.ELDER_MAUL, SpecEstimate.weapon(11124, 27100));
	}
}
