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
		// Verzik: every phase of her is plain to see
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Verzik Vitur"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Nylocas Vasilias"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Dusk"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Zulrah"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("The Leviathan"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Cerberus"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Kraken"));
		// the Fractured Archive: the Guardian flies and lands, Ocellus changes phase, Bork and the Smorks are a pair;
		// Ezekiel and Xarth keep their form
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Archive Guardian"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Ocellus"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Bork"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("The Smorks"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Xarth"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Ezekiel"));
		// a dead monster: on a task of it, the option's seconds (0, the default: until let go the other ways), about a
		// second otherwise or when the task is not known
		org.junit.Assert.assertEquals(17, CombatCheckPlugin.afterDeathTicks(true, 10));
		org.junit.Assert.assertEquals(Integer.MAX_VALUE, CombatCheckPlugin.afterDeathTicks(true, 0));
		org.junit.Assert.assertEquals(2, CombatCheckPlugin.afterDeathTicks(false, 0));
		org.junit.Assert.assertEquals(2, CombatCheckPlugin.afterDeathTicks(null, 10));
		// Barrows sets
		org.junit.Assert.assertEquals(0, SetEffects.barrowsPart("Dharok's greataxe 75"));
		org.junit.Assert.assertEquals(2, SetEffects.barrowsPart("Karil's leathertop"));
		org.junit.Assert.assertEquals(-1, SetEffects.barrowsPart("Rune platebody"));
		// every state of wear is the same piece, except the broken one
		org.junit.Assert.assertEquals(1, SetEffects.barrowsPart("Dharok's helm 100"));
		org.junit.Assert.assertEquals(3, SetEffects.barrowsPart("Verac's plateskirt 25"));
		org.junit.Assert.assertEquals(1, SetEffects.barrowsPart("Ahrim's hood"));
		org.junit.Assert.assertEquals(-1, SetEffects.barrowsPart("Dharok's helm 0"));
		org.junit.Assert.assertTrue(SetEffects.wholeBarrowsSet(java.util.Arrays.asList("Dharok's greataxe", "Dharok's helm 100",
			"Dharok's platebody", "Dharok's platelegs 25"), "dharok's "));
		org.junit.Assert.assertFalse(SetEffects.wholeBarrowsSet(java.util.Arrays.asList("Dharok's greataxe", "Dharok's helm",
			"Torag's platebody", "Dharok's platelegs"), "dharok's "));
		SetEffects.Context barrows = new SetEffects.Context();
		barrows.weaponName = "Guthan's warspear";
		barrows.ownedNames = java.util.Arrays.asList("Guthan's helm", "Guthan's platebody", "Guthan's chainskirt", "Torva platebody");
		org.junit.Assert.assertTrue(SetEffects.isBarrowsSetPiece("Guthan's platebody", barrows));
		org.junit.Assert.assertFalse(SetEffects.isBarrowsSetPiece("Torva platebody", barrows));
		// kept on with its own weapon and the whole set; with another weapon in hand it is compared like any piece
		org.junit.Assert.assertTrue(SetEffects.keep("Guthan's platebody", barrows));
		SetEffects.Context scythe = new SetEffects.Context();
		scythe.weaponName = "Scythe of vitur";
		scythe.ownedNames = java.util.Arrays.asList("Dharok's helm 100", "Dharok's platebody 100", "Dharok's platelegs 100", "Dharok's greataxe 100");
		org.junit.Assert.assertFalse(SetEffects.keep("Dharok's platebody 100", scythe));
		barrows.ownedNames = java.util.Arrays.asList("Guthan's helm", "Guthan's platebody", "Torva platelegs");
		org.junit.Assert.assertFalse(SetEffects.isBarrowsSetPiece("Guthan's platebody", barrows));
		// the names as the game writes them
		org.junit.Assert.assertTrue(SetEffects.isCrystalBow("Bow of Faerdhinen (c)"));
		org.junit.Assert.assertTrue(SetEffects.isCrystalBow("Crystal bow"));
		org.junit.Assert.assertFalse(SetEffects.isCrystalBow("Blade of Saeldor (c)"));
		org.junit.Assert.assertEquals("-5.0%", CombatCheckPlugin.differenceText(-5));
		org.junit.Assert.assertEquals("-15%", CombatCheckPlugin.differenceText(-15));
		org.junit.Assert.assertEquals(CombatCheckPlugin.gainText(12), CombatCheckPlugin.differenceText(12));
		// the best attack prayer the player has
		org.junit.Assert.assertEquals("Rigour", CombatCheckPlugin.bestPrayer("ranged", 99, true, true, true, true, true, true));
		org.junit.Assert.assertEquals("Deadeye", CombatCheckPlugin.bestPrayer("ranged", 99, true, true, false, true, false, false));
		org.junit.Assert.assertEquals("Eagle eye", CombatCheckPlugin.bestPrayer("ranged", 70, true, true, false, false, false, false));
		org.junit.Assert.assertEquals("Mystic might", CombatCheckPlugin.bestPrayer("magic", 50, false, false, false, false, false, false));
		org.junit.Assert.assertEquals("Chivalry", CombatCheckPlugin.bestPrayer("melee", 65, false, true, false, false, false, false));
		org.junit.Assert.assertEquals("Melee prayer", CombatCheckPlugin.bestPrayer("melee", 43, false, false, false, false, false, false));
		// Prayer 99 without the Knight Waves: neither Piety nor Chivalry
		org.junit.Assert.assertEquals("Melee prayer", CombatCheckPlugin.bestPrayer("melee", 99, false, false, true, true, true, true));
		org.junit.Assert.assertEquals("Piety", CombatCheckPlugin.bestPrayer("melee", 99, true, true, true, true, true, true));
		org.junit.Assert.assertFalse(CombatCheckPlugin.unlockedByKnightWaves(99, 99, 0, 70, 70));
		org.junit.Assert.assertFalse(CombatCheckPlugin.unlockedByKnightWaves(99, 69, 8, 70, 70));
		org.junit.Assert.assertTrue(CombatCheckPlugin.unlockedByKnightWaves(70, 70, 8, 70, 70));
		org.junit.Assert.assertTrue(CombatCheckPlugin.unlockedByKnightWaves(60, 65, 8, 60, 65));
		// a dead monster still faced by the player is not another monster; one that is not the remembered one is
		Object remembered = new Object();
		org.junit.Assert.assertFalse(CombatCheckPlugin.attacksAnother(remembered, remembered, true));
		org.junit.Assert.assertTrue(CombatCheckPlugin.attacksAnother(new Object(), remembered, true));
		org.junit.Assert.assertFalse(CombatCheckPlugin.attacksAnother(new Object(), remembered, false));
		org.junit.Assert.assertTrue(CombatCheckOverlay.prayerSprite("Mystic vigour") > 0);
		net.runelite.api.coords.WorldPoint fight = new net.runelite.api.coords.WorldPoint(1124, 3418, 0);
		org.junit.Assert.assertFalse(CombatCheckPlugin.leftTheFight(new net.runelite.api.coords.WorldPoint(1130, 3420, 0), fight, 80));
		org.junit.Assert.assertTrue(CombatCheckPlugin.leftTheFight(new net.runelite.api.coords.WorldPoint(1117, 3449, 0), fight, 80));
		org.junit.Assert.assertTrue(CombatCheckPlugin.leftTheFight(new net.runelite.api.coords.WorldPoint(1124, 3418, 1), fight, 80));
		org.junit.Assert.assertTrue(CombatCheckPlugin.leftTheFight(fight, fight, 0));
		org.junit.Assert.assertEquals(1420, CombatCheckOverlay.prayerSprite("Rigour"));
		org.junit.Assert.assertEquals(8595, CombatCheckOverlay.prayerSprite("Zeal"));
		// Zeal once, whether RuneLite's Prayer list has it (1.13.4 on) or only its varbit tells (not two Zeal icons in
		// "Prayer still on")
		org.junit.Assert.assertEquals("Zeal", CombatCheckPlugin.prayerName("ZEAL"));
		org.junit.Assert.assertEquals(java.util.Arrays.asList("Zeal"),
			CombatCheckPlugin.withZeal(new java.util.ArrayList<>(java.util.Arrays.asList("Zeal")), true));
		org.junit.Assert.assertEquals(java.util.Arrays.asList("Zeal"),
			CombatCheckPlugin.withZeal(new java.util.ArrayList<>(), true));
		org.junit.Assert.assertEquals(java.util.Arrays.asList("Protect from melee", "Zeal"),
			CombatCheckPlugin.withZeal(new java.util.ArrayList<>(java.util.Arrays.asList(
				CombatCheckPlugin.prayerName("PROTECT_FROM_MELEE"), "Zeal")), true));
		org.junit.Assert.assertEquals(java.util.Arrays.asList("Protect from melee", "Zeal"),
			CombatCheckPlugin.withZeal(new java.util.ArrayList<>(java.util.Arrays.asList("Protect from melee")), true));
		org.junit.Assert.assertEquals(java.util.Collections.emptyList(),
			CombatCheckPlugin.withZeal(new java.util.ArrayList<>(), false));
		org.junit.Assert.assertEquals(-1, CombatCheckOverlay.prayerSprite("Melee prayer"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Dagannoth Rex"));
		// the darts inside the blowpipe, as its Check message names them
		org.junit.Assert.assertEquals(11230, CombatCheckPlugin.dartFromCheck("Darts: Dragon dart x 1,234. Scales: 16,383 (100.0%)."));
		org.junit.Assert.assertEquals(25849, CombatCheckPlugin.dartFromCheck("Darts: Amethyst dart x 7. Scales: 12 (0.1%)."));
		org.junit.Assert.assertEquals(0, CombatCheckPlugin.dartFromCheck("Darts: None. Scales: 0 (0.0%)."));
		org.junit.Assert.assertEquals(-1, CombatCheckPlugin.dartFromCheck("Your blowpipe is empty."));
		org.junit.Assert.assertTrue(CombatCheckPlugin.readsDarts("Blazing blowpipe"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.readsDarts("Drygore blowpipe"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.readsDarts("Toxic blowpipe (empty)"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Dagannoth"));
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Vanguard"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm("Vorkath"));
		org.junit.Assert.assertFalse(CombatCheckPlugin.changesForm(null));
	}

	/**
	 * Akkha prays against the style he attacks with: a carried weapon of that class is never pointed at; in his enrage
	 * he and his shadows pray against magic and missiles, and only melee is.
	 */
	@org.junit.Test
	public void akkhasPrayerLeavesItsClassOut()
	{
		org.junit.Assert.assertFalse(CombatCheckPlugin.classAllowed(11790, StyleCalc.MELEE));
		org.junit.Assert.assertTrue(CombatCheckPlugin.classAllowed(11790, StyleCalc.RANGED));
		org.junit.Assert.assertTrue(CombatCheckPlugin.classAllowed(11790, StyleCalc.MAGIC));
		org.junit.Assert.assertFalse(CombatCheckPlugin.classAllowed(11791, StyleCalc.RANGED));
		org.junit.Assert.assertFalse(CombatCheckPlugin.classAllowed(11792, StyleCalc.MAGIC));
		org.junit.Assert.assertTrue(CombatCheckPlugin.classAllowed(11792, StyleCalc.MELEE));
		for (int id : new int[]{11795, 11798})
		{
			org.junit.Assert.assertTrue(CombatCheckPlugin.classAllowed(id, StyleCalc.MELEE));
			org.junit.Assert.assertFalse(CombatCheckPlugin.classAllowed(id, StyleCalc.RANGED));
			org.junit.Assert.assertFalse(CombatCheckPlugin.classAllowed(id, StyleCalc.MAGIC));
		}
		// Akkha's first form and any other monster: every class
		org.junit.Assert.assertTrue(CombatCheckPlugin.classAllowed(11789, StyleCalc.MAGIC));
		org.junit.Assert.assertTrue(CombatCheckPlugin.classAllowed(11797, StyleCalc.RANGED));
		org.junit.Assert.assertTrue(CombatCheckPlugin.classAllowed(415, StyleCalc.MAGIC));
	}

	/** Nex changes phase (melee, then ranged in her Shadow phase and against Zaros's Deflect Melee): only the same class. */
	@org.junit.Test
	public void nexIsABossOfPhases()
	{
		org.junit.Assert.assertTrue(CombatCheckPlugin.changesForm("Nex"));
	}
}
