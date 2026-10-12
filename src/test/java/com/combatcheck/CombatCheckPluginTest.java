package com.combatcheck;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CombatCheckPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CombatCheckPlugin.class);
		RuneLite.main(args);
	}

	@org.junit.Test
	public void theQuiversAmmunitionIsShotWhenTheSlotHoldsNoneOfThatKind()
	{
		org.junit.Assert.assertTrue(CombatCheckPlugin.quiverShoots("Crossbow", "Zaryte crossbow", "Rada's blessing 4", "Ruby dragon bolts (e)"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.quiverShoots("Bow", "Twisted bow", "Diamond bolts (e)", "Dragon arrow"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.quiverShoots("Crossbow", "Armadyl crossbow", null, "Diamond bolts (e)"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.quiverShoots("Crossbow", "Zaryte crossbow", "Diamond bolts (e)", "Ruby bolts (e)"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.quiverShoots("Bow", "Twisted bow", "Dragon arrow", "Ruby bolts (e)"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.quiverShoots("Bow", "Bow of faerdhinen (c)", null, "Dragon arrow"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.quiverShoots("Slash Sword", "Emberlight", null, "Dragon arrow"));
	}

	@org.junit.Test
	public void ammunitionCountsOnlyWithAWeaponThatShootsIt()
	{
		org.junit.Assert.assertEquals("bolts", CombatCheckPlugin.ammoKind("Crossbow", "Zaryte crossbow"));
		org.junit.Assert.assertEquals("arrow", CombatCheckPlugin.ammoKind("Bow", "Twisted bow"));
		org.junit.Assert.assertNull(CombatCheckPlugin.ammoKind("Bow", "Webweaver bow"));
		org.junit.Assert.assertNull(CombatCheckPlugin.ammoKind("Bow", "Bow of faerdhinen (c)"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.shootsNoAmmo("Bow", "Webweaver bow"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.shootsNoAmmo("Bow", "Venator bow"));
		org.junit.Assert.assertEquals("arrow", CombatCheckPlugin.ammoKind("Bow", "Venator bow"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.shootsNoAmmo("Bow", "Twisted bow"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.shootsNoAmmo("Crossbow", "Zaryte crossbow"));
	}

	@org.junit.Test
	public void withNoBoltsOnRubyComesFirstThenDiamondThenAnyOthers()
	{
		org.junit.Assert.assertEquals(2, CombatCheckPlugin.preferredBolts(new String[]{null, "Diamond bolts (e)", "Ruby dragon bolts (e)"}));
		org.junit.Assert.assertEquals(1, CombatCheckPlugin.preferredBolts(new String[]{"Runite bolts", "Diamond dragon bolts (e)"}));
		org.junit.Assert.assertEquals(0, CombatCheckPlugin.preferredBolts(new String[]{"Runite bolts", "Dragon bolts (unf)"}));
		org.junit.Assert.assertEquals(-1, CombatCheckPlugin.preferredBolts(new String[]{"Dragon bolts (unf)", null}));
	}

	@org.junit.Test
	public void noOtherWeaponIsSuggestedAgainstABossThatChangesForm()
	{
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Verzik Vitur"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Nylocas Vasilias"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Dusk"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Zulrah"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("The Leviathan"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Cerberus"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Dagannoth Rex"));
		// the darts inside the blowpipe, as its Check message names them
		org.junit.Assert.assertEquals(11230, CombatCheckPlugin.dartFromCheck("Darts: Dragon dart x 1,234. Scales: 16,383 (100.0%)."));
		org.junit.Assert.assertEquals(25849, CombatCheckPlugin.dartFromCheck("Darts: Amethyst dart x 7. Scales: 12 (0.1%)."));
		org.junit.Assert.assertEquals(0, CombatCheckPlugin.dartFromCheck("Darts: None. Scales: 0 (0.0%)."));
		org.junit.Assert.assertEquals(-1, CombatCheckPlugin.dartFromCheck("Your blowpipe is empty."));
		org.junit.Assert.assertTrue(CombatCheckPlugin.readsDarts("Blazing blowpipe"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.readsDarts("Drygore blowpipe"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Dagannoth"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Vanguard"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Vorkath"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm(null));
	}
}
