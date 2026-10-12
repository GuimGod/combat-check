# Combat Check

An attention check for your own gear. With the weapon **you** chose to hold, it reminds you of these:

1. **The attack style.** Which melee style of the weapon in your hand deals the most damage to the monster you are fighting, when another one is selected.
2. **The switch pieces you left behind.** The pieces that go with the weapon in your hand and are still sitting in your inventory. A misclick during a seven-piece switch is easy to miss. After the count it writes what they still add ("4 left +18%"); the figure shrinks with each piece you put on.
3. **A much better weapon left in your inventory.** When a weapon you are carrying, with the pieces that go with it, would deal much more to the monster you are fighting than what you have on, it shows that weapon's icon and how much more ("+75%"), and can mark it in the inventory ("Mark them in the inventory", off by default). While an attack prayer is on, a weapon of another combat class is counted with the best attack prayer you have for its own class (with Rigour on, a sword in the inventory is compared with Piety). Never sooner than 3 seconds after that became true: it reminds you of a switch you left undone, it does not tell you what is coming.

4. **The attack prayer.** While you fight, it reminds you when no attack prayer is on, naming the best one you have for the weapon in your hand ("Piety off", "Eagle eye off"; below Chivalry melee takes two prayers, so it says "Melee prayer off"), and when the one that is on belongs to another style ("Wrong prayer: Rigour", with a sword in hand). Each of the two has its own switch. The notice carries the prayer's icon, crossed out when it is the wrong one. It goes away about 5 seconds after you stop attacking, and is not shown with no prayer points left. It looks only at your own weapon and your own prayers: it has nothing to do with protection prayers or with what the monster does.

5. **A prayer left on.** Out of combat for about 10 seconds with a prayer still draining, it says "Prayer still on", with the icons of the prayers that are on. Preserve, Rapid Heal and Rapid Restore are left alone. Only an NPC you can attack counts as combat: a pet following you, a banker or a grave you clicked on does not hold the reminder back.

The figures come from the standard accuracy and max hit formulas for melee, ranged and staves with their own attack, checked against the OSRS Wiki calculator. Weapons whose strength depends on what the monster is are counted against that monster: demonbane weapons on demons (scaled for Duke Sucellus and Yama), the dragon hunter lance and crossbow on dragons, keris partisans on kalphites, the leaf-bladed battleaxe, the revenant weapons inside the Wilderness, the crystal bows with crystal armour, the Colossal blade, the Tonalztics of Ralos and the Void sets. Ruby and diamond bolts (e) are counted with the monster's current health (the ruby effect takes a share of it, so it is worth less as the fight goes on): when the other of the two, in your inventory, would deal more now, it is shown in a box of its own, in green ("Diamond +3%"). With a crossbow in hand and no bolts on at all, the bolts to put on are shown the same way: the better of the two kinds against the monster you are fighting, or, with no monster yet, the ruby ones first, then the diamond ones, then any others. While the Bolt Swap Reminder plugin is on, it gives this reminder and this plugin leaves it out. When the better weapon in your inventory is a crossbow, it is compared with the best bolts you have on you, and those bolts are shown and marked with it, so the two are switched together; with no bolts at all, a crossbow is not suggested. A staff with an elemental spell of the standard spellbook on autocast is counted too: the spell's max hit at your Magic level, the monster's weakness to its element when it has one (from the OSRS Wiki's monster data), the charged tome of that element and the Harmonised nightmare staff's faster casting. No figure is shown, and no weapon is suggested, for what is still not modelled: the other enchanted bolts, a blowpipe whose darts have not been seen, a staff with any other spell on autocast, the Soulreaper axe, Verac's flail, chinchompas and a few others. Ammunition kept in Dizana's quiver is counted as the one being shot when the ammunition slot holds none of that kind; the bonus the quiver itself gives is not counted.

## What is on

The first section of the settings has one switch for each reminder: the attack style ("Combat check"), switch pieces left behind, pieces to take off, better weapon in the inventory, ruby or diamond bolts (and whether their difference is shown all the time or only once the other bolts deal more), no attack prayer on, wrong attack prayer on, prayer left on, and the request to Check a blowpipe. The Slayer task is always read: it has no switch. Everything else in the settings is how each one looks and how long it waits.

## Where it is drawn

Every notice is in one overlay, at the bottom right by default. Hold Alt and drag it to move it, like any RuneLite overlay. Only the outline of the best style's button stays where the button is.

## Language

The words the plugin writes on screen ("Wrong prayer", "2 left", "Prayer still on") can be shown in English, Portuguese, Spanish, German or French: the "Language" setting. Names of items, prayers and attack styles stay as the game has them, and the settings panel itself is in English.

## What it never does

- It never names what a monster is weak to. Its defences and elemental weakness, as the OSRS Wiki lists them, only go into the figure of what your own gear would do to it.
- It never anticipates anything a monster will do.
- The Toxic blowpipe (and the Blazing one) is counted with the darts inside it. The game only tells which darts those are in the message of its Check option, so the plugin reads that message and remembers the darts for the account; until it has seen one, "Check your blowpipe" is shown while the blowpipe is in hand (it has its own switch), and the blowpipe gets no figure. Arrows, bolts or javelins left in the ammunition slot give a blowpipe or a thrown weapon nothing.
- It never suggests another weapon against a monster that changes form or phase during the fight (Verzik, the Nylocas, Zulrah, the Grotesque Guardians, the Nightmare, the Hunllef, the Phantom Muspah and others): which weapon each form calls for is the fight itself. Once you have changed weapon yourself, the pieces that go with it are listed as usual, and so is the best attack style.
- It does not click, equip, change the attack style or touch any menu. It only draws.
- It says nothing in a fight with another player. While you target a player or a player targets you, and for a minute after, every reminder is off: it is a tool for fighting monsters.

## The delays

Four waits are yours to set, in game ticks (0.6 seconds each):

| Reminder | Setting | Default |
|---|---|---|
| Attack style | "Attack style (bosses)": how long the wrong style must stay selected, against a boss | 0 |
| Switch pieces | "Switch pieces": how long a piece must stay unworn; raise it if the reminder flashes during a normal switch | 0 |
| Attack prayer | "Attack prayer": how long it must stay wrong or off | 0 |
| Prayer left on | "Prayer left on": how long out of combat with a prayer on, and how long the prayer has been on | 17 (about 10 seconds) |

And these are fixed, so there is less to set up:

| What | Fixed at |
|---|---|
| A boss, for the attack style wait | combat level 150 and up. Against anything else the style is pointed out at once |
| Better weapon in the inventory | never sooner than 5 ticks (3 seconds) after it became true |
| The attack prayer reminder after the fight | stays 8 ticks (about 5 seconds) after your last attack |
| The ruby or diamond bolts reminder | stays 25 ticks (15 seconds) after your last attack, at the last health the monster showed |
| The monster you are fighting | the last one you attacked, while it is alive, for up to 100 ticks (a minute) without attacking it; once it dies it is kept 30 seconds on a Slayer task of it (for the next one) and 3 seconds otherwise; forgotten at once when you die, walk more than 20 tiles away, attack a monster that is not in the table, log out or hop |
| The best attack style | pointed out for any gain at all; styles that tie are left alone |

## The attack style

- A small notice in the plugin's overlay: the weapon's icon and the best style ("Jab (crush)"). Red while another style is selected.
- An outline around that style's button while the Combat Options tab is open.
- Works for any weapon and any worn gear: the attack bonuses come from RuneLite's item stats.
- The notice ends with the gain in percent ("Chop (slash) +12%"); "Show the gain %" turns every percentage off.

It compares the styles with the standard accuracy and max hit formulas, using your boosted Attack and Strength levels, your active prayers, your worn bonuses and the monster's defence against stab, slash and crush. Effects that change every style alike (Slayer helmet, Salve amulet, weapons that hit several times) are left out, since they do not change which style is best. The few that depend on the damage type are included: Inquisitor's armour on crush, Osmumten's fang on stab, and the Corporeal Beast.

With a bow, or a staff on autocast, there is no melee style in use and the plugin says nothing.

## The switch pieces

The plugin looks at the combat class of the weapon in your hand (melee, ranged or magic) and at your inventory. A carried piece is "left behind" when it offers that class more than what you are wearing in its slot (attack bonus plus damage bonus). No monster's stats are involved.

- Their icons are drawn in the plugin's overlay, in inventory order, with "N left".
- Optionally (off by default), each one is also marked in your inventory, like RuneLite's Inventory Tags: an outline of the item, a tint over the item, a box, a circle or a filled box, in the colour, thickness and intensity (5% to 100%) you pick, with an optional pulse.
- A worn piece that does less than an empty slot with the weapon in your hand (melee gloves with a staff: a negative attack bonus and nothing to make up for it), when nothing you carry is better there, is shown in the same row crossed out ("1 off") and circled in the Worn Equipment tab. Can be turned off.
- A piece disappears when you put it on; everything disappears when the switch is complete.
- The weapon and the ammunition are your own choice and are never listed. A shield is not asked for while you hold a two-handed weapon.
- By default this only runs while a monster you attacked is remembered, so gearing up at a bank does not light up.

## Limits

- The best attack style is worked out for melee weapons only; the other reminders cover melee, ranged and magic.
- The monster must be in the bundled table (2,817 NPC ids). An id the table does not list is looked up by the monster's name, so the same monster in another place (a Hellhound in the Catacombs of Kourend) is counted with the figures of the first one of that name.
- A monster whose defence changes during the fight is compared at its listed values (Vardorvis at full health).
- The switch pieces are chosen by their bonuses. An item you carry on purpose that beats what you wear in its slot will be marked.

## Conditional effects of armour

Some armour gives more than the numbers on the item, under a condition. The plugin knows these (sizes from the OSRS Wiki DPS calculator):

| Armour | Condition | How it counts |
|---|---|---|
| Crystal helm, legs, body | A crystal bow or Bow of Faerdhinen in hand | Each piece ranks above its plain bonuses (5%, 10%, 15% accuracy and half that in damage) |
| Virtus mask, top, bottom | Ancient Magicks cast from a staff | 3% more magic damage each |
| Tome of fire, water, earth (charged) | A spell of that element on autocast | A tenth more magic damage; it is not asked off for a shield with better numbers |
| Inquisitor's helm, hauberk, plateskirt | A crush style selected | A little more each (0.5% to 1%) |
| Barrows sets (Ahrim's, Dharok's, Guthan's, Karil's, Torag's, Verac's), at any state of wear | The brother's weapon in hand and his helm, body and legs all on you | The three pieces are asked for together and none is asked off for a piece with better numbers; with a piece missing each is compared like any other. A broken piece ("0") is no part of a set |
| Dharok's set | All four pieces worn | The max hit grows with the hitpoints you have lost, as in the game |
| Void Knight set | The helm of the style in use is worn or carried | The whole set of that style is asked for; only the helm changes between styles |

And some pieces are worn for a reason that is not in their bonuses. The plugin never asks you to take these off for a piece with better numbers: Amulet of avarice, Void, Obsidian armour and Berserker necklace, the Barrows sets and Amulet of the damned, the Moons of Peril sets, and the Lightbearer. A Ring of shadows is a teleport and is never asked for.

Melee pieces are compared by the damage type of the style you have selected (stab, slash or crush), not by their best one.

## Slayer task

The plugin reads your Slayer task from the game itself (how many are left and the task's name) and knows, from a bundled table by NPC id, which monsters belong to each task. No other plugin is involved.

- On a monster of your task, a Slayer helmet or Black mask (of any colour) left in the inventory is listed with the other switch pieces, and its bonus is in the figure: melee for any of them, ranged and magic for the imbued ones. It is listed only when the set with it deals more to that monster than what you have on your head now.
- Against the undead a Salve amulet is listed the same way. The two do not stack: with a Salve amulet that works for the style in use, the helmet is not asked for.
- Off task the helmet is compared like any other helmet; so is the amulet against anything that is not undead.
- When the plugin cannot tell (no monster yet, a task it does not know, the "Bosses" task), nothing changes: the helmet is neither asked for nor asked off.

A blessing left in the inventory is listed when the ammunition slot is empty and the weapon in hand does not use it (melee, magic, a blowpipe, a crystal bow, thrown weapons). Ammunition itself is never asked for.

## Data

Weapon categories, their styles and the monsters' defence come from the [OSRS Wiki](https://oldschool.runescape.wiki) through its DPS calculator project; each monster's Slayer category comes from the wiki's monster infoboxes. The tests compare the recommended style with the calculator's over 2,350 weapon and monster pairs, and the figure of a set with the calculator's over 387 set and monster pairs.
