# Combat Check

An attention check for your own gear. With the weapon **you** chose to hold, it reminds you of these:

1. **The attack style.** Which melee style of the weapon in your hand deals the most damage to the monster you are fighting, when another one is selected.
2. **The switch pieces you left behind.** The pieces that go with the weapon in your hand and are still sitting in your inventory. A misclick during a seven-piece switch is easy to miss. After the count it writes what they still add ("4 left +18%"); the figure shrinks with each piece you put on.
3. **A better weapon left in your inventory.** When a weapon you are carrying, with the pieces that go with it, would deal more to the monster you are fighting than the weapon in your hand with its own pieces (by at least the "Weapon: minimum gain" setting, 1% by default), it shows that weapon's icon and how much more ("+75%"), and can mark it in the inventory ("Mark them in the inventory", off by default). While an attack prayer is on, a weapon of another combat class is counted with the best attack prayer you have for its own class (with Rigour on, a sword in the inventory is compared with Piety), and so is the weapon in your hand, so a prayer left on from another style is not read as a better weapon. Never sooner than 3 seconds after that became true: it reminds you of a switch you left undone, it does not tell you what is coming. With a weapon in hand that is held for its special attack (dragon claws, a godsword, a dragon warhammer, the Voidwaker and the like), it does not point back at the other weapon: when to go back is your own timing. The same with a Venator bow in hand: its worth is in the arrow that bounces to other monsters, and the figures here are for one monster only; and with a burst or a barrage of the Ancient Magicks on autocast, which hit up to nine monsters (a rush and a blitz hit one and are compared as usual). The Tonalztics of Ralos is one of the weapons held for their special attack: it lowers the monster's defence.

4. **The attack prayer.** While you fight, it reminds you when no attack prayer is on, naming the best one you have for the weapon in your hand ("Piety off", "Eagle eye off"; without Piety or Chivalry, which need the Knight Waves Training Grounds, melee takes two prayers, so it says "Melee prayer off"; Rigour and Augury need Defence 70 besides their scroll, so below it a bow is counted with Deadeye and a staff with Mystic vigour), and when the one that is on belongs to another style ("Wrong prayer: Rigour", with a sword in hand). Each of the two has its own switch. The notice carries the prayer's icon, crossed out when it is the wrong one. It goes away about 5 seconds after you stop attacking, and is not shown with no prayer points left. It looks only at your own weapon and your own prayers: it has nothing to do with protection prayers or with what the monster does.

5. **A prayer left on.** Out of combat for about 10 seconds with a prayer still draining, it says "Prayer still on", with the icons of the prayers that are on. Preserve, Rapid Heal and Rapid Restore are left alone. Only an NPC you can attack counts as combat: a pet following you, a banker or a grave you clicked on does not hold the reminder back.

The figures come from the standard accuracy and max hit formulas for melee, ranged and staves with their own attack, checked against the OSRS Wiki calculator. Weapons whose strength depends on what the monster is are counted against that monster: demonbane weapons on demons (scaled for Duke Sucellus and Yama; the Arclight without charges as the Darklight, as the OSRS Wiki says, where the calculator keeps its 70%), the dragon hunter lance, crossbow and wand on dragons, keris partisans on kalphites (the Keris partisan of amascut with the stab and strength of the OSRS Wiki and the calculator, which RuneLite's item data lacks, and less of both outside the Tombs of Amascut), the leaf-bladed battleaxe, the revenant weapons inside the Wilderness with their ether in them, the Crystal bow and the Bow of Faerdhinen with the crystal armour of Prifddinas (not the Gauntlet's bow or armour, and not a piece without charges), the Colossal blade, the Tonalztics of Ralos (twice charged, once uncharged), the Dark bow's two arrows, the Comp ogre bow (the ammunition's strength alone), the TzHaar weapons with the obsidian armour and the Berserker necklace, Ahrim's set with the Amulet of the damned and the Void sets (the Void mage set too). A weapon that cannot attack without its charges (Tumeken's shadow, the Sanguinesti staff, the Eye of Ayak and the Warped sceptre uncharged, Craw's bow and the Webweaver bow without ether, the Bow of Faerdhinen and the Crystal bow inactive) gets no figure and is never suggested; the Scythe of vitur and the Venator bow attack uncharged and are counted. The raids' monsters are counted as the raid scales them, with the party, mode, raid level and path levels read from the game (see "Chambers of Xeric", "Theatre of Blood" and "Tombs of Amascut"), and so are the other fights the calculator has a rule of its own for (see "Other fights with a rule of their own"). Ruby and diamond bolts (e) are counted with the monster's current health (the ruby effect takes a share of it, so it is worth less as the fight goes on): when the other of the two, in your inventory, would deal more now, it is shown in a box of its own, in green ("Diamond +3%"). With a crossbow in hand and no bolts on at all, the bolts to put on are shown the same way: the better of the two kinds against the monster you are fighting, or, with no monster yet, the ruby ones first, then the diamond ones, then any others. While the Bolt Swap Reminder plugin is on, it gives this reminder and this plugin leaves it out. When the better weapon in your inventory is a crossbow, it is compared with the best bolts you have on you, and those bolts are shown and marked with it, so the two are switched together; with no bolts at all, a crossbow is not suggested. Karil's crossbow is counted with bolt racks only. A bow or a crossbow counts only the ammunition it can fire, as the calculator lists it: no dragon arrows in a Magic shortbow (i), no dragon bolts in a Rune crossbow, the brutal arrows in a Comp ogre bow; a weapon with only ammunition it cannot fire is not suggested, and such bolts are not asked for. The Ancient Magicks on autocast are counted with their own max hits, on the one monster attacked (with a burst or a barrage in hand no other weapon is pointed at). A staff with an elemental spell of the standard spellbook on autocast is counted too: the spell's max hit at your Magic level, the monster's weakness to its element when it has one (from the OSRS Wiki's monster data), the charged tome of that element and the Harmonised nightmare staff's faster casting. No figure is shown, and no weapon is suggested, for what is still not modelled: the enchanted bolts whose effect deals damage of its own other than ruby and diamond (opal, pearl, dragonstone, onyx), a blowpipe whose darts have not been seen, a staff with a spell on autocast that is neither an elemental one of the standard spellbook nor one of the Ancient Magicks (Magic Dart and Iban Blast among them, and the Staff of the dead in melee: a staff always counts as magic), the Soulreaper axe, Verac's flail, chinchompas and a few others. The Gauntlet's staves are counted with their own max hit. Ammunition kept in Dizana's quiver is counted as the one being shot when the quiver is worn (or put on by the switch, from the inventory) and the ammunition slot holds none of that kind; with the quiver in the bank its ammunition is not counted. The bonus the quiver itself gives is not counted.

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
- Against a monster that changes form or phase during the fight it only points at a better weapon of the same combat class as the one in your hand (a better melee weapon while you hold a melee weapon), never at a change of class (the Nylocas, Zulrah, the Grotesque Guardians, the Nightmare, the Hunllef, the Phantom Muspah, Nex and others): which weapon each form calls for is the fight itself. Against Akkha in the Tombs of Amascut no weapon of the style he is praying against is pointed at, and in his enrage, where he prays against magic and missiles, only a melee one is. Against the Royal Titans no ranged weapon is compared: their ranged accuracy is six times while they stand out of melee distance, which the plugin cannot tell. Once you have changed weapon yourself, the pieces that go with it are listed as usual, and so is the best attack style.
- It does not click, equip, change the attack style or touch any menu. It only draws.
- It says nothing in a fight with another player. Where players can attack each other (the Wilderness, or wherever the game sets its PvP area varbit, as on a PvP world: what RuneLite's Player Indicators reads for its "PvP" setting), while you target a player or a player targets you, and for a minute after, every reminder is off: it is a tool for fighting monsters. Anywhere else a player targeting you, or targeted, is following or trading (the game calls all of these "interacting"): an alt or a friend following you keeps nothing off. In the Wilderness a player following you cannot be told from one attacking you, and silences it too.

## The delays

Four waits are yours to set, in game ticks (0.6 seconds each):

| Reminder | Setting | Default |
|---|---|---|
| Attack style | "Attack style (bosses)": how long the wrong style must stay selected, against a boss | 0 |
| Switch pieces | "Switch pieces": how long a piece must stay unworn; raise it if the reminder flashes during a normal switch | 0 |
| Attack prayer | "Attack prayer": how long it must stay wrong or off | 0 |
| Prayer left on | "Prayer left on": how long out of combat with a prayer on, and how long the prayer has been on | 17 (about 10 seconds) |

Two reminders are shown for a while and then keep quiet, so that doing something on purpose is not nagged about:

| Reminder | Shown for | Shown again when |
|---|---|---|
| No attack prayer on | "Attack prayer: show for", 5 seconds by default | you turn an attack prayer on (and then fight without one again) |
| Pieces to put on or take off | "Switch pieces: show for", 5 seconds by default | you change a piece of armour; a shield counts, the weapon and the ammunition do not |

Dying, logging in or hopping lets each be shown once more. "Attack prayer: keep on screen" and "Switch pieces: keep on screen" keep them there until the mistake is put right, as before. The wrong attack prayer always stays until it is put right.

And these are fixed, so there is less to set up:

| What | Fixed at |
|---|---|
| A boss, for the attack style wait | combat level 150 and up. Against anything else the style is pointed out at once |
| Better weapon in the inventory | never sooner than 5 ticks (3 seconds) after it became true |
| The attack prayer reminder after the fight | stays 8 ticks (about 5 seconds) after your last attack |
| The ruby or diamond bolts reminder | stays 25 ticks (15 seconds) after your last attack, at the last health the monster showed |
| The monster you are fighting | the last one you attacked, while it is alive, for up to 100 ticks (a minute) without attacking it; once it dies or leaves the scene it is kept about 10 seconds (17 ticks) on a Slayer task of it (for the next one) and about a second otherwise; forgotten at once when you die, walk more than 20 tiles away, attack a monster that is not in the table, log out or hop |
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
- The monster must be in the table (3,965 NPC ids in the one bundled; see "The monster table"). Every id the OSRS Wiki gives a version of a monster has a row of its own with that version's numbers (Vorkath after the quest, the Lizardman shamans of the caves, the lighthouse's Dagannoths, Dusk's last form...). An id the table does not list (a Deadman Mode boss, a monster newer than the table) is looked up by the monster's name and counted with the figures of the first one of that name. The awakened fights of Desert Treasure II's bosses (the Leviathan, Duke Sucellus, Vardorvis, the Whisperer) have the same NPC ids as the fights after the quest and rows of their own: they are counted once the game's boss health display shows more hitpoints than the fight after the quest has (2700 against 900 on the Leviathan), and as the fight after the quest until it shows them.
- Yama's magic defence is counted as while the one Yama attacks uses magic (+60, the calculator's "Tank using magic"): with a magic weapon in hand you are taken to be holding the boss. In a duo where the other player holds it with melee it is -30, and magic deals more than the figure says; Yama's third phase is counted as the first two.
- Against a vampyre of tier 2 or 3 an ordinary weapon deals nothing, as in the game, and the weapons that hurt one are counted as the calculator counts them: the Hallowed flail a quarter more accurate and a quarter more each hit, the Sunspear a quarter more accurate and a half more each hit, a silver weapon a tenth more each hit on tier 1 and no more than 10 each hit on tier 2 (the calculator's limit; the OSRS Wiki speaks of full damage), and Efaritay's aid a tenth more each hit of those first, 15% more accuracy with a silver weapon, and half of each hit of any other weapon on tier 2. The calculator's code never takes silver bolts for silver (its list names them in a way it does not match): here they are silver, under the same rules. The Blisterwood and Ivandis weapons and the Rod of Ivandis have no figure.
- The Tormented Demon is counted with half of the attacks meeting it without defence (each of those lands) and half rolling against its defence, in every combat class: the plugin does not yet read from the game whether its flames are out, which is when every attack lands. It stays a monster of phases: only a weapon of the class in your hand is ever pointed at.
- Fareed takes melee only with Ice gloves on: the plugin does not look at the gloves.
- Karil's set with the Amulet of the damned is counted without its second hit (a quarter of the attacks, half the damage): the calculator does not count it either.
- The rat bone weapons are counted as ordinary ones: without the 10 more max hit they deal to rats and without their attack on every tick against Scurrius's Giant rats.
- A monster whose defence changes during the fight is compared at its listed values (Vardorvis at full health, awakened or not), except Araxxor, who is counted enraged from a quarter of his hitpoints on (35 more Defence), as the calculator counts him. The Wardens of the Tombs of Amascut's third phase are counted enraged (Defence 180) from the moment the phantom of Amascut behind them opens her mouth, which is when they enrage.
- The switch pieces are chosen by their bonuses. An item you carry on purpose that beats what you wear in its slot will be marked.

## Conditional effects of armour

Some armour gives more than the numbers on the item, under a condition. The plugin knows these (sizes from the OSRS Wiki DPS calculator):

| Armour | Condition | How it counts |
|---|---|---|
| Crystal helm, legs, body (of Prifddinas, charged) | A Crystal bow or Bow of Faerdhinen in hand | Each piece ranks above its plain bonuses (5%, 10%, 15% accuracy and half that in damage) |
| Obsidian helmet, platebody, platelegs | A TzHaar weapon in hand, all three worn | A tenth more accuracy and max hit |
| Berserker necklace | A TzHaar weapon in hand | A fifth more on each hit |
| Ahrim's set and Amulet of the damned | Ahrim's staff in hand, all five worn | A quarter of the spells deal 30% more |
| Void mage helm with the Void set | All four worn | 45% more effective Magic level; 5% more magic damage with the Elite set |
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
- A boss task is of the boss the game names (the game's boss task id, read the way RuneLite's Slayer plugin reads it), and of the ones the OSRS Wiki lets kill in its place: Artio for Callisto, Spindel for Venenatis, Calvar'ion for Vet'ion, the Deranged archaeologist for the Crazy one (not on Konar's task), the six brothers of the Barrows, the three Dagannoth Kings, Dusk and Dawn, the Abyssal Sire's respiratory systems. The awakened fights of Desert Treasure II's bosses are off task: Slayer equipment does nothing there.
- A task of Krystilia's counts only inside the Wilderness. A task of Konar's counts wherever the monster is: the helmet does nothing outside the place she gave, but the plugin does not know where that place begins and ends.
- When the plugin cannot tell (no monster yet, a task it does not know, a boss task whose boss the game does not name), nothing changes: the helmet is neither asked for nor asked off.

A blessing left in the inventory is listed when the ammunition slot is empty and the weapon in hand does not use it (melee, magic, a blowpipe, a crystal bow, thrown weapons). Ammunition itself is never asked for.

## Defence drains

Everything above is worked out with the Defence the monster has left after special attacks: Dragon warhammer, Elder maul, Bandos godsword, Arclight and Darklight (and the Arclight without charges, which works as the Darklight), Emberlight, Tonalztics of Ralos (charged or not: uncharged it hits once), Accursed sceptre, Seercull, Eye of Ayak, bone dagger, Dorgeshuun crossbow and Barrelchest anchor. Your own are seen as they land, the way RuneLite's Special Attack Counter sees them. Your party's arrive through that plugin's party messages, so it must be on for you and for whoever specs; it sends nothing for the Arclight and the Tonalztics without charges. This is always on: there is no option for it.

Each special attack is taken off the levels the one before left, in the order they landed, as the game does it (OSRS Wiki: "each reduction is calculated from the current temporary level"): a Bandos godsword's hit before a Dragon warhammer leaves more Defence than after it. Each one is worked out as the OSRS Wiki DPS calculator works it out (same rounding, the same floors: Verzik and Vardorvis cannot be drained, Nex stops at 250...), a Bandos godsword's damage beyond the Defence left going on to Strength, Attack, Magic and Ranged; the calculator takes them in one order of its own, and its figures are the plugin's when they landed in that order. The game's rules the calculator does not have come from the OSRS Wiki:

- Tekton: a miss still lowers 5%, or 10 levels with the godsword.
- The Accursed sceptre takes Defence and Magic to 85% of the full levels and no lower: on a level already lowered more it does nothing. The Seercull lowers Magic only while it is full.
- The bone dagger and the Dorgeshuun crossbow lower Defence by their damage only while it is full. The Barrelchest anchor lowers Defence by a tenth of its damage (rounded down: the wiki does not say how it rounds), or Attack, Ranged or Magic once Defence is down to 1.
- Floors: the Phantom Muspah stops at 125, the Doom of Mokhaiotl at 60, the Fragment of Seren at 60 and the Menaphite Akh at 100.
- Fights immune to drains take none of them: the awakened fights of Desert Treasure II's bosses (from the moment they are counted as awakened, see "Limits"), Demonic Brutus and the Moons of Peril. Yama with a contract is drained as any other Yama: the wiki's page on drains lists contracted fights as immune, Yama's own page says the drains act on them, and the plugin does not tell a contracted fight from another.
- The Alchemical Hydra: in each of its first three phases, from the start of the phase until the right vent sprays it (its roar, "Roaaaaaaaaaaar!"), its hitsplats are a quarter of the damage, and a Bandos godsword's and a Barrelchest anchor's drain go by the damage before that cut: they are counted from four times the hitsplat (the game's rounding of the quarter is not known: up to 3 levels off).

What was counted on a monster is dropped when it dies, goes away or changes into a form with another Defence, and when the game gives the Defence back on a change of form: the Phantom Muspah entering its shielded phase, Galvek going on to his next phase. The Nightmare wins back 20 Defence a minute and Phosani's Nightmare one level every 3 seconds, counted from the last special attack on them (OSRS Wiki; the calculator has no time).

Not counted: the Fragment of Seren shows a hit above 22 as 22 to 24, and the drain goes by the hit (OSRS Wiki): a godsword hit that shows 22 to 24 is counted as shown. A special attack that lands for 0 is counted as a miss, as the Special Attack Counter counts it. The Doom of Mokhaiotl's Defence comes back on each delve as long as the game brings it back as a new monster (not yet seen in the game).

### Special attacks outside the party (estimated)

With "Estimate specs" on (on by default since a ToB with 4 players decided 6 of 6 and the CoX measurement had no wrong decision; one option for the Elder maul, the Dragon warhammer and the Bandos godsword), the Elder maul and Dragon warhammer special attacks of the other players on the monster you fight are counted too when their specs do not come through the party: players outside your party, or in it without the Special Attack Counter. The client shows no message for them, so they are estimated from what it shows, and counted only when there is no doubt:

- the player's target is that very monster (not another of the same kind) and the player starts the weapon's special attack animation with that weapon seen in hand;
- on the tick the hit lands (2 ticks after the animation for the Elder maul, 1 for the Dragon warhammer, measured in a Chambers of Xeric raid against the party's Special Attack Counter), every hitsplat on the monster that is not yours is above 0: it hit (35% or 30% of the Defence left, as above); or every one is 0: it missed, which lowers nothing except Tekton's 5%.

Not counted: a tick with some hitsplats at 0 and some above (in a big raid several players hit on the same tick and a hitsplat does not say whose it is; in the raid measured, those undecided ticks held all the misses of the Elder maul), a tick with no hitsplat, and every other special attack (the Bandos godsword, below, and the rest drain by their damage, which a shared tick does not tell). That leaves the Defence a little higher than it is rather than ever lower. Nothing is estimated with the game's hitsplat tinting turned off (not measured), for your own special attacks, or for a player whose special attacks have come through the party once: theirs are counted exactly, and an estimate made before their message comes is replaced by it, so no one is counted twice. An estimate takes its place among the other drains by the tick it landed.

The Bandos godsword special attacks of the same players are counted too (not measured yet with a small team: in the Chambers of Xeric raids measured every Bandos godsword special attack landed on a tick with 2 to 20 hitsplats, where it is not counted), only when there is no doubt whose hitsplat is the godsword's: its special attack animation (7642, or 7643 for the ornament) with the godsword seen in hand, the player aiming at that very monster, and on the tick the hit lands (1 tick after the animation, measured) exactly one hitsplat on the monster that is not yours. That hitsplat's damage is the drain, as above: Defence first, what is left over on to Strength, Attack, Magic and Ranged. A 0 is a miss and lowers nothing, except on Tekton, where it drains 10 (OSRS Wiki). Two hitsplats or more, or none, and it is not counted. The party's message replaces it as for the others.

While the Defence used counts an estimated special attack, the figures on screen get "~" ("~+12%") and the first notice says "spec estimated" (in the language chosen).

## Chambers of Xeric

Inside the raid, with "Chambers of Xeric scaling" on (the default), the raid's monsters are counted with the levels and hitpoints the raid gives them, as the OSRS Wiki DPS calculator scales them: the party size the raid was scaled for, Challenge Mode and the party's highest combat level, all read from the game. Challenge Mode is read from its varbit, which reads 0 for a member who is not the party's leader (14 Challenge Mode raids measured), and from the game's own message on the tick the raid begins, "Map Layout: Challenge Mode (Full).", which every one of them had; it is kept for each account until the next raid begins, so a relog inside the raid keeps it. A big party raises the Skeletal Mystics' Magic level, for instance, and with it what the Twisted bow deals to them; Tekton's Defence grows less than the others' in Challenge Mode, and the Glowing crystal's not at all. The game does not give the party's highest Hitpoints level, so it is taken as 99, and the Guardians' hitpoints are worked out with 99 Mining for each member. The special attacks counted on a monster (see "Defence drains") are taken off the scaled levels, in the order they landed. Outside the raid, or turned off, the table's levels are used as they are.

## Theatre of Blood

Inside the raid, with "Theatre of Blood scaling" on (the default), its monsters are counted with the hitpoints the party gives them, as the OSRS Wiki DPS calculator scales them: in normal and Hard Mode a party of three or fewer has 75% of them and four 87.5%; Entry Mode has its own table. The party is read from the game: the places of the party that hold someone. Only the hitpoints scale in the Theatre (they count for ruby bolts); Hard Mode and Entry Mode monsters have levels of their own, found by the id the game gives each boss in combat (Sotetseg in a normal raid is not the Entry Mode one; Verzik's transitions count as the phase the wiki lists them in; the Hard Mode wave Nylocas as the normal ones, as the wiki lists them). Verzik's first phase is counted as the calculator counts it: each hit up to 10 for melee and 3 for anything else. On Xarpus and Verzik's second phase, in every mode, the Scythe of vitur (Holy and Sanguine too) is counted at one attack every 16/3 ticks (about 5.33) instead of 5, so 15/16 of the calculator's figure: the melee has to step back from Xarpus's poison and Verzik's bombs, and a 5-tick weapon loses one tick of its cycle (OSRS Wiki, Theatre of Blood/Strategies: "This uses 15/16 available ticks to damage the boss, or effectively a 5.33 tick attack speed"). The other 5-tick weapons keep their speed. On Xarpus this changes the comparison of melee weapons; on Verzik's second phase it is also what Tumeken's shadow and the other carried weapons are compared against. Outside the raid, turned off, or with no party read, the table's hitpoints are used as they are.

## Tombs of Amascut

Inside the raid, with "Tombs of Amascut scaling" on (the default), its monsters are counted with the raid level, the path levels and the party size read from the game, as the calculator does it: the monster's defence roll grows with the raid level ((250 + raid level) / 250 of it, after the defence drains; not for Kephri's scarab overlords), and its hitpoints with the raid level, the path level and the party (they count for ruby bolts and for the Keris partisan of the sun). The raid's own rules count either way: Tumeken's shadow multiplies its bonuses by four instead of three, Osmumten's fang rolls its accuracy twice the raid's way, and the Keris partisan of the sun is a quarter more accurate below a quarter of the monster's hitpoints. On the Wardens of the second phase every attack lands, and ranged and magic deal 15% to 60% of the max hit by how much the attack roll beats a third of the Warden's defence roll (the Twisted bow's accuracy counts twice there); every melee hit on a Warden's core is a max hit, the core counted as the calculator counts it (the Warden whose core is out). The Wardens of the third phase are counted enraged (Defence 180, and the defence drains counted before are dropped) once the phantom of Amascut behind them opens her mouth.

## Other fights with a rule of their own

As the calculator counts them: the Nightmare's and Phosani's totems take twice the damage of magic (every id of the four totems); the Hueycoatl's tail limits each hit to 4, or 9 with a crush style while crush is your highest attack bonus (then a miss deals 1) or an earth spell; the Corporeal Beast halves each hit of ranged too, not only of melee without a corpbane stab, and a ruby bolt's effect is whole; every magic attack lands at its max hit on the Royal Titans' elementals, and so do the classes the calculator lists for a few other monsters (the Tombs of Amascut's baboons, the Judge of Yama); Araxxor's Acidic and Mirrorback araxytes take the max hit of every attack of the Noxious halberd, of a crush style while crush is your highest attack bonus and of a crossbow while ranged is, and so does the Ruptura araxyte (the OSRS Wiki has the same sentence on the three pages; the calculator has the rule for the two others only, and the wiki decides the mechanics); every attack lands on the Spawn of the Abyssal Sire; Scurrius's Giant rats and the Flowers (Hespori's, A Night at the Theatre's) die in one hit, so the faster weapon is the better one there; on the Abyssal Sire's Respiratory systems every hit that lands is at least half the max hit, and a demonbane weapon's hit that lands (the Scorching bow's) kills one. The Twinflame staff hits twice with bolts, blasts and waves, every 6 ticks, and like the smoke battlestaves adds a tenth to the accuracy and damage of the standard spellbook; Torag's hammers, the Sulphur blades, the Glacial temotli and the Earthbound tecpatl hit twice, each up to half the max; Osmumten's fang's damage range is narrowed with every style. Flat armour (the Drakes, the Wyrms, the golems, the Veiled kraken...) takes its value off every melee and ranged hit that lands, down to 0, and a ruby or diamond bolt's effect is taken by the monster's rules like any hit (its flat armour, Zulrah's cap). The Fragment of Seren takes a hit above 22 as a random 22, 23 or 24. The Tormented Demon's fire shield takes a fifth off each hit (at least 1) of anything but a demonbane weapon or an abyssal weapon in melee (whip, tentacle, dagger, bludgeon).

Some monsters take only part of each hit, or nothing from most weapons, and are counted so (the calculator's rules, by the monster):

| Monster | What hurts it |
|---|---|
| Kraken, Cave kraken | Ranged deals a seventh of each hit (at least 1) |
| Slagilith | A third of each hit, unless a pickaxe is in hand |
| Zogre, Skogre, Slash Bash | A quarter of each hit; half with Crumble Undead; all of it with a Comp ogre bow and brutal arrows |
| Kurask, Turoth and the other leafy monsters | Only a leaf-bladed battleaxe, sword or spear, broad arrows or bolts, or Magic Dart |
| Vampyres of tier 3 (Vyrewatch, their Sentinels, Venators...) | Only the Ivandis and Blisterwood flails, the Blisterwood sickle and stake, the Hallowed flail and the Sunspear |
| Vampyres of tier 2 (Juvinates) | Those, the Rod of Ivandis, silver weapons and silver bolts; any weapon with Efaritay's aid on |
| Dusk, the Cyclopes of the Warriors' Guild | Melee only |
| The Doom of Mokhaiotl's shield | A demonbane weapon only; every attack lands, as it does while the Doom is burrowed |
| Fareed | Magic only with a water spell |
| Fire Warrior of Lesarkus | Ranged only, with Ice arrows on |

Where the OSRS Wiki and the calculator disagree on what can reach or hurt a monster, the wiki is followed, and the calculator only for the sums: the Respiratory systems of the Abyssal Sire take melee from a halberd only ("can only be attacked with magic, ranged or a halberd"); the Cave kraken takes no melee at all, a halberd's neither; a halberd hurts Vespula in flight (the game's change of 6 August 2025, which the calculator does not have). The Eclipse Moon is counted as its ordinary fight (flat armour 6), not as its Clone special attack.

## The monster table

The monsters' defences and the weapons' categories come bundled with the plugin, and that is what it starts with. With "Update the monster table" on (the default), the plugin asks its own GitHub repository once at startup whether a newer copy of the same three tables has been published, and uses it: a monster the game added, or one whose defence it changed, is known without waiting for a new version of the plugin. Each file is checked against the SHA-256 the list gives for it, must read as a table and must not be much smaller than the bundled one; anything off, and the bundled table stays. Only numbers and names are read; nothing is asked for during a fight, and nothing of yours is sent. Turned off, the plugin makes no request at all.

## Data

Weapon categories, their styles and the monsters' defence come from the [OSRS Wiki](https://oldschool.runescape.wiki) through its DPS calculator project; each monster's Slayer category (`tools/gen_slayer.py`) and the NPC ids of each version of a monster (`tools/gen_ids.py`) come from the wiki's monster infoboxes. `tools/gen_data.py` rebuilds them; the rules the wiki decides against the calculator are written in it, and a test fails if the table loses one. The tests compare the recommended style with the calculator's over 2,350 weapon and monster pairs.

## Bug or suggestion

Click "Bug or suggestion" in the plugin's settings: it opens a window where you write what is wrong or what you would like. Under your words it shows exactly what goes with them: the plugin's name, the monster you were fighting, the ids of the items you wear and carry, and the map region. No name of yours or of your account.

- "Send the report" sends it to the author's Google Form. Nothing is sent until you press it.
- "Open the report on GitHub" opens a new issue with the text filled in, for you to post.
- "Copy the report" puts it on the clipboard.
