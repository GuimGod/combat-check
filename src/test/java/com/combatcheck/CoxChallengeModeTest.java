/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.runelite.api.ChatMessageType;
import org.junit.Test;

/**
 * Challenge Mode read from the game's message at the raid's start (the varbit read 0 in the 14 Challenge Mode raids
 * measured, 2026-10-14 to 2026-10-16), and what it changes on the Deathly ranger in a raid of 23: the Twisted bow first
 * in Challenge Mode, Tumeken's shadow first in a normal raid.
 */
public class CoxChallengeModeTest
{
	@Test
	public void theGamesMessagesTellTheMode()
	{
		// as the client's log has them (2026-10-16 12:56:10), on the tick the raid begins
		assertEquals(Boolean.FALSE, CoxChallengeMode.read(ChatMessageType.FRIENDSCHATNOTIFICATION,
			"<col=ef20ff>The raid has begun!</col>"));
		assertEquals(Boolean.TRUE, CoxChallengeMode.read(ChatMessageType.FRIENDSCHATNOTIFICATION,
			"Map Layout: Challenge Mode (Full)."));
		assertEquals(Boolean.TRUE, CoxChallengeMode.read(ChatMessageType.GAMEMESSAGE, "Map Layout: Challenge Mode (Full)."));
		// a player typing the same words says nothing, nor do the raid's other messages
		assertNull(CoxChallengeMode.read(ChatMessageType.PUBLICCHAT, "Map Layout: Challenge Mode (Full)."));
		assertNull(CoxChallengeMode.read(ChatMessageType.FRIENDSCHAT, "The raid has begun!"));
		assertNull(CoxChallengeMode.read(ChatMessageType.FRIENDSCHATNOTIFICATION,
			"<col=ef20ff>Your party has entered the dungeons! Come and join them now.</col>"));
		assertNull(CoxChallengeMode.read(ChatMessageType.FRIENDSCHATNOTIFICATION, null));
	}

	/**
	 * The Deathly ranger in a raid of 23 at combat level 126, by the OSRS Wiki calculator (2026-10-16): the Twisted bow
	 * with Seeking dragon arrows, Masori (f), Dizana's max cape, Necklace of rupture, Zaryte vambraces, Avernic treads
	 * (max) and Lightbearer, Rigour and +13 Ranged, against Tumeken's shadow with Ancestral, the Imbued Zamorak max
	 * cape, Occult necklace, Confliction gauntlets, the same treads and ring, Augury and +13 Magic. Challenge Mode:
	 * Twisted bow 12.14 per second, Shadow 10.09. Normal: Twisted bow 10.26, Shadow 10.49. The plugin's "Shadow +1.3%"
	 * in a Challenge Mode raid was the normal raid's figure.
	 */
	@Test
	public void deathlyRangerInARaidOf23() throws java.io.IOException
	{
		StyleData data = new StyleData();
		StyleData.Monster ranger = data.monster(7559);

		SetCalc.Player ranged = new SetCalc.Player();
		ranged.ranged = 112;
		ranged.prayerRanged = 120;
		ranged.prayerRangedStrength = 123;
		SetCalc.Player magic = new SetCalc.Player();
		magic.magic = 112;
		magic.prayerMagic = 125;
		magic.prayerMagicDamage = 4;

		SetCalc.Loadout tbow = new SetCalc.Loadout();
		tbow.weaponName = "Twisted bow";
		tbow.category = "Bow";
		tbow.speed = 6;
		tbow.styles = data.styles(20997);
		tbow.stab = -3;
		tbow.slash = -3;
		tbow.crush = -3;
		tbow.magic = 4;
		tbow.ranged = 243;
		tbow.strength = 6;
		tbow.rangedStrength = 104;
		tbow.magicDamage = 2;
		SetCalc.Loadout shadow = new SetCalc.Loadout();
		shadow.weaponName = "Tumeken's shadow";
		shadow.category = "Powered Staff";
		shadow.speed = 5;
		shadow.styles = data.styles(27275);
		shadow.stab = 5;
		shadow.slash = 5;
		shadow.crush = 5;
		shadow.magic = 162;
		shadow.ranged = -6;
		shadow.strength = 6;
		shadow.rangedStrength = 3;
		shadow.magicDamage = 25;

		SetCalc.Target cm = ranger.target(CoxScaling.levels(ranger, new CoxScaling.Raid(23, true)));
		SetCalc.Target normal = ranger.target(CoxScaling.levels(ranger, new CoxScaling.Raid(23, false)));
		double cmBow = SetCalc.perTick(tbow, ranged, cm, false) / 0.6;
		double cmShadow = SetCalc.perTick(shadow, magic, cm, false) / 0.6;
		double bow = SetCalc.perTick(tbow, ranged, normal, false) / 0.6;
		double staff = SetCalc.perTick(shadow, magic, normal, false) / 0.6;
		assertEquals("Challenge Mode, Twisted bow", 12.14, cmBow, 0.12);
		assertEquals("Challenge Mode, Shadow", 10.09, cmShadow, 0.1);
		assertEquals("normal, Twisted bow", 10.26, bow, 0.1);
		assertEquals("normal, Shadow", 10.49, staff, 0.1);

		// Challenge Mode: the bow is first and the Shadow is not pointed at with it in hand (the default minimum gain, 1%)
		assertTrue(cmBow + " over " + cmShadow, SetCalc.gain(cmShadow, cmBow) >= 1);
		assertTrue(SetCalc.gain(cmBow, cmShadow) < 1);
		// normal: the other way round
		assertTrue(staff + " over " + bow, SetCalc.gain(bow, staff) >= 1);
		assertTrue(SetCalc.gain(staff, bow) < 1);
	}
}
