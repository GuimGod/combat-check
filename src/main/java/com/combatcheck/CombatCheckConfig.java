/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("combatcheck")
public interface CombatCheckConfig extends Config
{
	@ConfigSection(
		name = "What is on",
		description = "Every reminder the plugin has, each with its own switch",
		position = -10
	)
	String master = "master";

	@ConfigItem(
		keyName = "styleReminder",
		name = "Combat check",
		description = "Point at the best melee style of the weapon in your hand against the monster you are fighting. Off: nothing about attack styles is shown",
		position = 1,
		section = master
	)
	default boolean styleReminder()
	{
		return true;
	}

	@ConfigSection(
		name = "Delays",
		description = "How long a mistake must be left standing before the plugin reminds you",
		position = 0
	)
	String delays = "delays";

	@Range(min = 0, max = 50)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "styleDelay",
		name = "Attack style (bosses)",
		description = "Against a boss (combat level 150 and up), how long the wrong style must stay selected before the reminder. Against anything else it is pointed out at once",
		position = 1,
		section = delays
	)
	default int styleDelay()
	{
		return 0;
	}

	/** When the attack prayer reminder speaks. */
	enum Language
	{
		ENGLISH("English"),
		PORTUGUESE("Portugu\u00eas"),
		SPANISH("Espa\u00f1ol"),
		GERMAN("Deutsch"),
		FRENCH("Fran\u00e7ais");

		private final String label;

		Language(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "language",
		name = "Language",
		description = "The language of the words the plugin writes on screen. Names of items, prayers and attack styles stay as the game has them",
		position = -20
	)
	default Language language()
	{
		return Language.ENGLISH;
	}

	enum PrayerReminder
	{
		OFF("Off"),
		NONE_ON("When none is on"),
		WRONG("When the wrong one is on"),
		BOTH("Both");

		private final String label;

		PrayerReminder(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "boltReminder",
		name = "Ruby or diamond bolts",
		description = "With a crossbow and both ruby and diamond bolts (e) on you, show the ones in your inventory when they would deal more to the monster at its current health (\"Diamond +3%\")",
		position = 5,
		section = master
	)
	default boolean boltReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "boltAlwaysShow",
		name = "Bolts: show the difference all the time",
		description = "Show the other bolts and the difference even while the ones you have on are still the better ones (\"Diamond -5%\", in red), so you can see the switch coming. Off: only once the other bolts deal more, and the notice goes away when you switch",
		position = 6,
		section = master
	)
	default boolean boltAlwaysShow()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerNoneOn",
		name = "No attack prayer on",
		description = "While fighting, remind you when no attack prayer is on, naming the one for the weapon in your hand (\"Piety off\")",
		position = 7,
		section = master
	)
	default boolean prayerNoneOn()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerWrong",
		name = "Wrong attack prayer on",
		description = "While fighting, remind you when the attack prayer that is on belongs to another style than your weapon (\"Wrong prayer: Rigour\")",
		position = 8,
		section = master
	)
	default boolean prayerWrong()
	{
		return true;
	}

	@Range(min = 0, max = 50)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "prayerDelay",
		name = "Attack prayer",
		description = "How long the attack prayer must stay wrong or off before the reminder. 0 reminds you at once",
		position = 4,
		section = delays
	)
	default int prayerDelay()
	{
		return 0;
	}

	@Range(min = 1, max = 120)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "prayerShowFor",
		name = "Attack prayer: show for",
		description = "With \"Attack prayer: keep on screen\" off, how long the \"no attack prayer on\" reminder stays on screen. After that it keeps quiet until you turn an attack prayer on: fighting without one on purpose is not nagged about. The \"wrong prayer\" reminder always stays until it is put right",
		position = 6,
		section = delays
	)
	default int prayerShowFor()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "prayerKeep",
		name = "Attack prayer: keep on screen",
		description = "Keep the \"no attack prayer on\" reminder on screen until an attack prayer is on, however long that takes (on by default). Off: it stays for \"Attack prayer: show for\"",
		position = 7,
		section = delays
	)
	default boolean prayerKeep()
	{
		return true;
	}

	@Range(min = 1, max = 120)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "switchShowFor",
		name = "Switch pieces: show for",
		description = "With \"Switch pieces: keep on screen\" off, how long the pieces to put on or take off stay on screen. After that they are not shown again until you change a piece of armour (a shield counts; the weapon and the ammunition do not): changing only the weapon does not bring them back",
		position = 8,
		section = delays
	)
	default int switchShowFor()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "switchKeep",
		name = "Switch pieces: keep on screen",
		description = "Keep the pieces to put on or take off on screen until the switch is complete, however long that takes (on by default). Off: they stay for \"Switch pieces: show for\"",
		position = 9,
		section = delays
	)
	default boolean switchKeep()
	{
		return true;
	}

	@Range(min = 0, max = 60)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "prayerAfterAttack",
		name = "Attack prayer: after the last attack",
		description = "How long after your last attack on the monster the attack prayer reminder stays. 0 (the default) keeps it for as long as the monster is the one you fight: until it dies, a minute passes without an attack, you walk away or you attack another",
		position = 10,
		section = delays
	)
	default int prayerAfterAttack()
	{
		return 0;
	}

	@Range(min = 0, max = 60)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "boltsAfterAttack",
		name = "Bolts: after the last attack",
		description = "How long after your last attack on the monster the ruby or diamond bolts reminder stays. 0 (the default) keeps it for as long as the monster is the one you fight: until it dies, a minute passes without an attack, you walk away or you attack another",
		position = 11,
		section = delays
	)
	default int boltsAfterAttack()
	{
		return 0;
	}

	@Range(min = 0, max = 60)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "afterKillOnTask",
		name = "After a kill on task",
		description = "On a Slayer task of the monster, how long it stays the one you fight after it dies, so its reminders (weapon, pieces, prayer, bolts) stay for the next one. 0 (the default) keeps it until a minute passes without an attack, you walk away or you attack another. Off a task a dead monster is let go at once",
		position = 12,
		section = delays
	)
	default int afterKillOnTask()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "blowpipeReminder",
		name = "Blowpipe: ask for a Check",
		description = "With a Toxic or Blazing blowpipe in hand whose darts the plugin has not seen yet, ask you to use its Check option once: the game only tells which darts are inside in that message. Off: the blowpipe simply gets no figure until you check it",
		position = 10,
		section = master
	)
	default boolean blowpipeReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerLeftOn",
		name = "Prayer left on reminder",
		description = "Out of combat, remind you that a prayer is still on and draining. Preserve, Rapid Heal and Rapid Restore are left alone",
		position = 9,
		section = master
	)
	default boolean prayerLeftOn()
	{
		return true;
	}

	@Range(min = 1, max = 200)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "prayerLeftOnDelay",
		name = "Prayer left on",
		description = "How long you must be out of combat, and the prayer must have been on, before the reminder: praying a little ahead of a fight does not set it off. 17 ticks are about 10 seconds",
		position = 5,
		section = delays
	)
	default int prayerLeftOnDelay()
	{
		return 17;
	}

	@Range(min = 0, max = 20)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "switchDelay",
		name = "Switch pieces",
		description = "How long a piece that goes with your weapon must stay in your inventory before the reminder. 0 reminds you at once; raise it if the reminder flashes during a normal switch",
		position = 2,
		section = delays
	)
	default int switchDelay()
	{
		return 0;
	}

	@ConfigSection(
		name = "Attack style",
		description = "The best melee style of the weapon in your hand against the monster you are fighting",
		position = 10
	)
	String style = "style";

	@ConfigItem(
		keyName = "showBox",
		name = "Show the notice",
		description = "Draw the weapon's icon and the name of the best style above the Combat Options tab",
		position = 11,
		section = style
	)
	default boolean showBox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "outlineButton",
		name = "Outline the best style",
		description = "Draw an outline around the best style's button while the Combat Options tab is open",
		position = 12,
		section = style
	)
	default boolean outlineButton()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alwaysShow",
		name = "Show even when it is selected",
		description = "Keep the notice and the outline when the best style is already selected (in green)",
		position = 13,
		section = style
	)
	default boolean alwaysShow()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showGain",
		name = "Show the gain %",
		description = "Write how much more damage you would deal, in percent: after the best style's name, after the count of switch pieces left, and after the attack prayer reminder (what the prayer would add to the gear worn)",
		position = 11,
		section = master
	)
	default boolean showGain()
	{
		return true;
	}

	@ConfigItem(
		keyName = "estimateSpecs",
		name = "Estimate specs",
		description = "Also count the Elder maul, Dragon warhammer and Bandos godsword special attacks of players whose "
			+ "specs do not come through the party, from their special attack animation and the hitsplats on the monster. "
			+ "Only when the hit leaves no doubt. The figures that use one are marked with ~ and \"spec estimated\"",
		position = 13,
		section = master
	)
	default boolean estimateSpecs()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDefence",
		name = "Show the Defence left",
		description = "While a special attack that lowers Defence is counted on the monster you fight, a line with its "
			+ "Defence before them and now, and how many were counted (\"Def 246 -> 158 (3 specs)\"): yours, your party's "
			+ "and, with \"Estimate specs\", the others' (then the figure gets ~). Not for a monster whose Defence cannot be "
			+ "lowered. With a magic weapon it gives the Magic level instead, and only when it was lowered (except where magic "
			+ "rolls against Defence). The Special Attack Counter shows each weapon's drain; this shows the level the figures use",
		position = 14,
		section = master
	)
	default boolean showDefence()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBulwarkSpec",
		name = "Show Dinh's bulwark's spec",
		description = "With Dinh's bulwark in hand in its Pummel style and half your special attack energy left, a line with "
			+ "what its Shield Bash deals: on the monster you fight (two hits), and in a multicombat area on each other "
			+ "monster within 5 tiles (one hit, up to 10 monsters in all), as mean and max (OSRS Wiki, Dinh's bulwark)",
		position = 18,
		section = master
	)
	default boolean showBulwarkSpec()
	{
		return true;
	}

	@ConfigItem(
		keyName = "coxScaling",
		name = "Chambers of Xeric scaling",
		description = "Inside the Chambers of Xeric, work everything out with the monsters' levels and hitpoints scaled "
			+ "for the raid, as the DPS calculator scales them: the party size the raid was scaled for, Challenge Mode and "
			+ "the party's highest combat level, read from the game. The party's highest Hitpoints level is taken as 99. "
			+ "Defence drains are counted on top of the scaled levels",
		position = 15,
		section = master
	)
	default boolean coxScaling()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tobScaling",
		name = "Theatre of Blood scaling",
		description = "Inside the Theatre of Blood, work everything out with the monsters' hitpoints scaled for the party, "
			+ "as the DPS calculator scales them: the party size is read from the game (the places of the party that hold "
			+ "someone). Only the hitpoints scale there (they count for ruby bolts); Hard Mode and Entry Mode monsters have "
			+ "levels of their own either way",
		position = 16,
		section = master
	)
	default boolean tobScaling()
	{
		return true;
	}

	@ConfigItem(
		keyName = "toaScaling",
		name = "Tombs of Amascut scaling",
		description = "Inside the Tombs of Amascut, work everything out with the raid level, the path levels and the party "
			+ "size read from the game, as the DPS calculator does: the monsters' defence rolls grow with the raid level "
			+ "(not the scarab overlords'), and their hitpoints with the raid level, the path level and the party. "
			+ "Tumeken's shadow, Osmumten's fang and the Keris partisan of the sun follow the raid's own rules either way",
		position = 17,
		section = master
	)
	default boolean toaScaling()
	{
		return true;
	}

	@ConfigSection(
		name = "Gear switch",
		description = "The pieces that go with the weapon in your hand and are still in your inventory",
		position = 20
	)
	String gearSwitch = "gearSwitch";

	@ConfigItem(
		keyName = "weaponNotice",
		name = "Better weapon in the inventory",
		description = "When a weapon you are carrying, with the pieces that go with it, would deal much more to the monster you are fighting than what you have on, show its icon and how much more. Only weapons and monsters the plugin can work out; see the README",
		position = 4,
		section = master
	)
	default boolean weaponNotice()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankWeapon",
		name = "Better weapon in the bank",
		description = "Outside the raids, name the weapon of the bank (the one last seen open) that would deal the most to the "
			+ "monster you are fighting in place of the one in your hand, with everything else you have on (a weapon of two "
			+ "hands without your shield), and how much more: \"Bank: <weapon> +X%\". Only a weapon, never armour or a shield; "
			+ "not one without charges or without ammunition you have on",
		position = 29,
		section = master
	)
	default boolean bankWeapon()
	{
		return true;
	}

	// 10% by default; it can be turned off and the percent changed
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "bankMinimumGain",
		name = "Bank weapon: minimum gain",
		description = "Only name a weapon of the bank when it would deal at least this much more than what you have on",
		position = 21,
		section = gearSwitch
	)
	default double bankMinimumGain()
	{
		return BankHint.DEFAULT_MINIMUM;
	}

	// a decimal number from 0; the whole numbers saved when it was an int read the same ("1" is 1.0). 0 points at any
	// gain that shows as +0.1% or more; above 200 counts as 200 (CombatCheckPlugin.minimumGain,
	// CombatCheckPlugin.pointsAt)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "weaponMinimumGain",
		name = "Weapon: minimum gain",
		description = "Only point at a carried weapon when its set would deal at least this much more than what you have on. Steps of 0.1: 0.5 is half a percent, 0 any gain (from +0.1%)",
		position = 20,
		section = gearSwitch
	)
	default double weaponMinimumGain()
	{
		return 1;
	}

	/** What the weapon notice compares a carried weapon's set with what you have on by. */
	enum WeaponComparison
	{
		DAMAGE_PER_SECOND("Damage per second"),
		TIME_TO_KILL("Time to kill");

		private final String label;

		WeaponComparison(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	// the damage per second by default; the time to kill, which counts what a hit deals beyond the hitpoints left as
	// wasted, is the other choice (CombatCheckPlugin.comparedGain)
	@ConfigItem(
		keyName = "weaponComparison",
		name = "Compare weapons by",
		description = "Damage per second: the weapon whose set deals the most per second. Time to kill: the one that kills the monster soonest from its full hitpoints, a hit beyond the hitpoints left being wasted, so a fast weapon can beat a slow one on a monster of few hitpoints (marked \"(low HP)\")",
		position = 21,
		section = gearSwitch
	)
	default WeaponComparison weaponComparison()
	{
		return WeaponComparison.DAMAGE_PER_SECOND;
	}

	@ConfigItem(
		keyName = "missingPieces",
		name = "Switch pieces left behind",
		description = "Show the inventory pieces that go with the weapon in your hand and are not worn yet. Each one disappears as you put it on",
		position = 2,
		section = master
	)
	default boolean missingPieces()
	{
		return true;
	}

	/** How a piece left behind is marked in the inventory. */
	enum Highlight
	{
		OFF("Off"),
		ITEM_OUTLINE("Item outline"),
		ITEM_TINT("Item tint"),
		BOX("Box"),
		CIRCLE("Circle"),
		FILL("Filled box");

		private final String label;

		Highlight(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "inventoryHighlight",
		name = "Mark them in the inventory",
		description = "How each piece left behind is marked in your inventory. Off by default",
		position = 22,
		section = gearSwitch
	)
	default Highlight inventoryHighlight()
	{
		return Highlight.OFF;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColour",
		name = "Mark colour",
		description = "Colour of the mark in the inventory",
		position = 24,
		section = gearSwitch
	)
	default Color highlightColour()
	{
		return new Color(255, 60, 60);
	}

	@Range(min = 5, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "highlightIntensity",
		name = "Mark intensity",
		description = "How strong the mark is, for every shape: 100 is the full colour, lower values make it more see-through",
		position = 25,
		section = gearSwitch
	)
	default int highlightIntensity()
	{
		return 100;
	}

	@Range(min = 1, max = 5)
	@ConfigItem(
		keyName = "highlightWidth",
		name = "Mark thickness",
		description = "Line thickness of the box and of the circle, in pixels",
		position = 26,
		section = gearSwitch
	)
	default int highlightWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "highlightPulse",
		name = "Pulse",
		description = "Make the mark fade in and out, to catch the eye",
		position = 27,
		section = gearSwitch
	)
	default boolean highlightPulse()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showMissingRow",
		name = "Show them above the combat tab",
		description = "Draw the icons of the pieces left behind, in inventory order, next to the notice",
		position = 23,
		section = gearSwitch
	)
	default boolean showMissingRow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "takeOffPieces",
		name = "Pieces to take off",
		description = "Show, crossed out, a worn piece that does less than an empty slot with the weapon in your hand (melee gloves with a staff), when nothing you carry is better there. It is also marked in the Worn Equipment tab",
		position = 3,
		section = master
	)
	default boolean takeOffPieces()
	{
		return true;
	}

	@ConfigItem(
		keyName = "onlyInCombat",
		name = "Only while fighting",
		description = "Check the pieces only while a monster you attacked is remembered, so gearing up at a bank does not light up",
		position = 28,
		section = gearSwitch
	)
	default boolean onlyInCombat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "reportBug",
		name = "Bug or suggestion",
		description = "Click to open a window where you write what is wrong or what you would like. It shows what goes with your words (the monster and the items you wear and carry) and sends it to the plugin's author only when you press Send",
		position = 100
	)
	default boolean reportBug()
	{
		return false;
	}

	@ConfigItem(
		keyName = "updateData",
		name = "Update the monster table",
		description = "When the plugin starts, ask its GitHub repository whether a newer table of monsters and weapons has been published, and use it: a monster the game added is known without waiting for a new version of the plugin. One small request at startup, nothing during a fight; only numbers and names are read. Off: the plugin uses the table it came with and makes no request",
		position = 101
	)
	default boolean updateData()
	{
		return true;
	}
}
