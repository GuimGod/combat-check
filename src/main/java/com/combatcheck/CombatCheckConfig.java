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
		description = "Write how much more damage you would deal, in percent: after the best style's name, and after the count of switch pieces left",
		position = 11,
		section = master
	)
	default boolean showGain()
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

	@Range(min = 1, max = 200)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "weaponMinimumGain",
		name = "Weapon: minimum gain",
		description = "Only point at a carried weapon when its set would deal at least this much more than what you have on",
		position = 20,
		section = gearSwitch
	)
	default int weaponMinimumGain()
	{
		return 1;
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
}
