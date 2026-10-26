/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.HitsplatID;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/**
 * The special attacks of players whose specs do not come through the party, estimated from their animation and the
 * hitsplats on the monster ({@link SpecEstimate}), on ticks of a real raid ({@code spec_estimate_cox.txt}: the local
 * "SPEC diagnostic" lines, players as numbers) against the party's Special Attack Counter messages of the same ticks.
 */
public class SpecEstimateTest
{
	private static final Pattern PLAYER = Pattern.compile(
		"^player (\\d+)( \\(me\\))? anim (-?\\d+) frame (-?\\d+)( again)? weapon (-?\\d+) at .*$");
	private static final Pattern OFF = Pattern.compile("^player (\\d+) off target$");
	private static final Pattern HIT = Pattern.compile("^hitsplat npc (\\d+) amount (\\d+) type (\\d+) (mine|others|neither)$");
	private static final Pattern TARGET = Pattern.compile("^target npc (\\d+) id (\\d+) .*$");
	private static final Pattern PARTY = Pattern.compile(
		"^party spec player (\\d+)( \\(me\\))? (?:seen|not seen) weapon (\\S+) hit (\\d+) npc (\\d+).*$");

	/** A party message of the file: the answer key. */
	static final class Message
	{
		final int tick;
		final int player;
		final boolean me;
		final SpecialWeapon weapon;
		final int hit;
		final int npcIndex;

		Message(int tick, int player, boolean me, SpecialWeapon weapon, int hit, int npcIndex)
		{
			this.tick = tick;
			this.player = player;
			this.me = me;
			this.weapon = weapon;
			this.hit = hit;
			this.npcIndex = npcIndex;
		}
	}

	/** What a replay of the file gives: the estimates counted, the drains, and the monsters by index. */
	static final class Replay
	{
		final List<SpecEstimate.Landed> landed = new ArrayList<>();
		/** The special attacks estimated from an animation, decided or not: "tick weapon". */
		final List<String> started = new ArrayList<>();
		final List<Message> messages = new ArrayList<>();
		final DrainTracker drains = new DrainTracker();
		final Map<Integer, Object> npcs = new HashMap<>();
		final Map<Integer, Integer> npcIds = new HashMap<>();

		Object npc(int index)
		{
			return npcs.computeIfAbsent(index, i -> new Object());
		}
	}

	private static Map<Integer, List<String>> ticks(String file) throws IOException
	{
		Map<Integer, List<String>> byTick = new TreeMap<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
			SpecEstimateTest.class.getResourceAsStream(file), StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = r.readLine()) != null)
			{
				if (line.startsWith("#") || line.isEmpty())
				{
					continue;
				}
				String[] f = line.split("\t", 2);
				byTick.computeIfAbsent(Integer.parseInt(f[0]), t -> new ArrayList<>()).add(f[1]);
			}
		}
		return byTick;
	}

	/**
	 * Plays the file's ticks as the plugin sees them: on each tick, the party's messages and the hitsplats (which come
	 * before the tick's GameTick: 0 of 5644 after it in the raid's log), then the GameTick's count of the estimates whose hit
	 * has landed, then the look at the players aiming at the monster. With {@code party} the messages are sent to the
	 * estimate and counted exactly, as the plugin does; without, every player is taken as outside the party.
	 */
	private static Replay replay(boolean party) throws IOException
	{
		return replay(party, false);
	}

	private static Replay replay(boolean party, boolean meAsOther) throws IOException
	{
		return replay("spec_estimate_cox.txt", party, meAsOther);
	}

	/**
	 * The same, and with {@code meAsOther} the player of the client taken as one more player outside the party, the Bandos
	 * godsword estimated: that player's own Bandos godsword special attacks, whose damage the message gives, are then
	 * judged as anybody else's would be.
	 */
	static Replay replay(String file, boolean party, boolean meAsOther) throws IOException
	{
		Replay out = new Replay();
		SpecEstimate e = new SpecEstimate();
		e.bgs(meAsOther);
		Set<Integer> aiming = new HashSet<>();
		int target = -1;
		for (Map.Entry<Integer, List<String>> tick : ticks(file).entrySet())
		{
			int t = tick.getKey();
			List<String> players = new ArrayList<>();
			for (String line : tick.getValue())
			{
				Matcher m;
				if ((m = PARTY.matcher(line)).matches())
				{
					Message msg = new Message(t, Integer.parseInt(m.group(1)), m.group(2) != null,
						SpecialWeapon.valueOf(m.group(3)), Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)));
					out.messages.add(msg);
					if (party && !msg.me)
					{
						e.partySpec(msg.player);
						Object npc = out.npc(msg.npcIndex);
						out.drains.dropEstimated(msg.npcIndex, npc, msg.player, msg.weapon,
							t - CombatCheckPlugin.ESTIMATE_REPLACED_TICKS);
						out.drains.add(msg.npcIndex, npc, msg.weapon, msg.hit,
							DefenceDrains.tekton(out.npcIds.getOrDefault(msg.npcIndex, -1)), t);
					}
				}
				else if ((m = HIT.matcher(line)).matches())
				{
					e.hitsplat(t, out.npc(Integer.parseInt(m.group(1))), Integer.parseInt(m.group(2)),
						Integer.parseInt(m.group(3)), m.group(4).equals("mine") && !meAsOther);
				}
				else if (line.equals("world hop"))
				{
					// another world: the plugin clears the estimate (SpecEstimate.clear), and the file's numbers start again
					e.clear();
					aiming.clear();
				}
				else if ((m = TARGET.matcher(line)).matches())
				{
					target = Integer.parseInt(m.group(1));
					out.npcIds.put(target, Integer.parseInt(m.group(2)));
				}
				else
				{
					players.add(line);
				}
			}
			for (SpecEstimate.Landed l : e.resolve(t))
			{
				out.landed.add(l);
				out.drains.addEstimated(l.npcIndex, l.npc, l.weapon, l.hit, l.tekton, l.tick, l.playerId);
			}
			for (String line : players)
			{
				Matcher m;
				if ((m = PLAYER.matcher(line)).matches())
				{
					int n = Integer.parseInt(m.group(1));
					aiming.add(n);
					if (m.group(5) != null)
					{
						e.animationSet(n);
					}
					if (e.seen(t, n, m.group(2) != null && !meAsOther, Integer.parseInt(m.group(3)),
						Integer.parseInt(m.group(4)), Integer.parseInt(m.group(6)), target, out.npc(target),
						DefenceDrains.tekton(out.npcIds.get(target))))
					{
						out.started.add(t + " " + SpecEstimate.weapon(Integer.parseInt(m.group(3)),
							Integer.parseInt(m.group(6)), meAsOther));
					}
				}
				else if ((m = OFF.matcher(line)).matches())
				{
					aiming.remove(Integer.parseInt(m.group(1)));
				}
				else
				{
					throw new AssertionError("a line of the file not read: " + line);
				}
			}
			e.endTick(aiming);
		}
		return out;
	}

	private static String key(int player, SpecialWeapon weapon, int npcIndex)
	{
		return player + " " + weapon + " " + npcIndex;
	}

	/**
	 * Every player taken as outside the party: each estimate counted agrees with that player's own message when there is
	 * one (it hit), and each message is either estimated or one of the ticks the method leaves undecided.
	 */
	@Test
	public void theRaidsTicksAgainstTheAnswerKey() throws IOException
	{
		Replay r = replay(false);
		Map<String, List<Message>> byKey = new HashMap<>();
		for (Message m : r.messages)
		{
			if (!m.me)
			{
				byKey.computeIfAbsent(key(m.player, m.weapon, m.npcIndex), k -> new ArrayList<>()).add(m);
			}
		}
		Set<Message> matched = new HashSet<>();
		int checked = 0;
		for (SpecEstimate.Landed l : r.landed)
		{
			assertEquals("only hits here (no miss on Tekton was undecided)", 1, l.hit);
			Message mine = null;
			for (Message m : byKey.getOrDefault(key(l.playerId, l.weapon, l.npcIndex), Collections.emptyList()))
			{
				// the message comes on the tick of the hit (the Elder maul's) or the next one
				if (m.tick >= l.tick - 1 && m.tick <= l.tick && !matched.contains(m))
				{
					mine = m;
				}
			}
			if (mine != null)
			{
				matched.add(mine);
				checked++;
				assertTrue("player " + l.playerId + " on tick " + l.tick + ": counted as a hit, the message says "
					+ mine.hit, mine.hit > 0);
			}
		}
		// Tekton 11 and Vasa 5; the 5 others are of players 10, 11, 14, 1 and 7, who sent no message (hits as far as the
		// hitsplats tell: every one above 0)
		assertEquals("estimates checked against a message", 16, checked);
		assertEquals("estimates counted", 21, r.landed.size());
		// the messages not estimated: undecided ticks, or an animation before the file starts
		List<String> notEstimated = new ArrayList<>();
		for (Message m : r.messages)
		{
			if (!m.me && !matched.contains(m))
			{
				notEstimated.add(m.tick + " " + m.player + " " + m.weapon + " " + m.hit);
			}
		}
		assertEquals(java.util.Arrays.asList(
			// before the file's first tick: their animations are not in it
			"651 8 ELDER_MAUL 1", "651 9 DRAGON_WARHAMMER 0",
			// Tekton, tick 652: two Elder mauls on hitsplats 0 and 17; one missed, one hit, and nothing says which
			"652 5 ELDER_MAUL 1", "652 2 ELDER_MAUL 0",
			// Vasa, tick 2918: two Dragon warhammers on hitsplats 0, 49, 25 and 37
			"2919 13 DRAGON_WARHAMMER 25", "2919 16 DRAGON_WARHAMMER 37"), notEstimated);
	}

	/** The three cases measured, one by one: a decided Elder maul, a decided Dragon warhammer, an undecided one. */
	@Test
	public void aDecidedMaulADecidedHammerAndOneThatCannotCount() throws IOException
	{
		Replay r = replay(false);
		Map<String, SpecEstimate.Landed> byKey = new HashMap<>();
		for (SpecEstimate.Landed l : r.landed)
		{
			byKey.put(l.playerId + " " + l.weapon + " " + l.tick, l);
		}
		// Vasa: the Elder maul of player 5 at tick 2918 lands at 2920 on hitsplats 13, 24 and 45: it hit (the message: 1)
		assertTrue(byKey.containsKey("5 ELDER_MAUL 2921"));
		// Vasa: the Dragon warhammer of player 12 at 2918 lands at 2919, the only hitsplat 73 (the message: 73)
		assertTrue(byKey.containsKey("12 DRAGON_WARHAMMER 2920"));
		// Tekton: the Elder maul of player 2 at 650 lands at 652 on 0 and 17. It missed (the message: 0): counted as a hit it
		// would have taken 35% of Defence Tekton still had. Undecided, it is not counted, not even as Tekton's 5% miss
		assertFalse(byKey.containsKey("2 ELDER_MAUL 653"));
		assertFalse(byKey.containsKey("5 ELDER_MAUL 653"));
	}

	/**
	 * A party member's special attack is counted once: by the message, with no estimate of it, when the message comes
	 * before the estimate is decided, and the estimate taken out when it comes after. Once a player has sent a message,
	 * that player is never estimated again. On Tekton every message of an Elder maul or a Dragon warhammer counts (a miss
	 * takes 5%), so the drains there are the messages plus the estimates of the players who never sent one, each once.
	 */
	@Test
	public void noPlayerIsCountedTwice() throws IOException
	{
		Replay withParty = replay(true);
		Set<Integer> senders = new HashSet<>();
		int messages = 0;
		for (Message m : withParty.messages)
		{
			if (!m.me)
			{
				senders.add(m.player);
				messages += m.npcIndex == TEKTON ? 1 : 0;
			}
		}
		int outsiders = 0;
		for (SpecEstimate.Landed l : withParty.landed)
		{
			outsiders += l.npcIndex == TEKTON && !senders.contains(l.playerId) ? 1 : 0;
		}
		Object tekton = withParty.npc(TEKTON);
		assertEquals(15, messages);
		// players 10, 11, 14, 1 and 7 sent nothing on these ticks
		assertEquals(5, outsiders);
		assertEquals(messages + outsiders, withParty.drains.counted(TEKTON, tekton));
		assertTrue(withParty.drains.estimated(TEKTON, tekton));
		// the same ticks with everybody taken as outside: more estimates, and none of the messages
		Replay alone = replay(false);
		assertEquals(21, alone.drains.counted(TEKTON, alone.npc(TEKTON)) + alone.drains.counted(VASA, alone.npc(VASA)));
	}

	private static final int TEKTON = 63645;
	private static final int VASA = 63470;

	/** The message coming after the estimate takes it out; the one coming before keeps the player from being estimated. */
	@Test
	public void theMessageReplacesTheEstimate()
	{
		Object tekton = new Object();
		// player 7 outside the party as far as anyone knows: Elder maul at tick 100, hitsplats 30 and 12 at 102
		SpecEstimate e = new SpecEstimate();
		DrainTracker drains = new DrainTracker();
		assertTrue(e.seen(100, 7, false, 11124, 0, 21003, 5, tekton, true));
		e.endTick(Collections.singleton(7));
		e.hitsplat(102, tekton, 30, HitsplatID.DAMAGE_OTHER, false);
		e.hitsplat(102, tekton, 12, HitsplatID.DAMAGE_OTHER, false);
		e.hitsplat(102, tekton, 40, HitsplatID.DAMAGE_ME, true);
		assertTrue(e.resolve(101).isEmpty());
		List<SpecEstimate.Landed> landed = e.resolve(102);
		assertEquals(1, landed.size());
		SpecEstimate.Landed l = landed.get(0);
		drains.addEstimated(l.npcIndex, l.npc, l.weapon, l.hit, l.tekton, l.tick, l.playerId);
		assertTrue(drains.estimated(5, tekton));
		assertEquals(134, drains.of(5, tekton).apply(205, 205, 0, false, 0).defence);
		// then the message of it comes: it is counted once, exactly
		e.partySpec(7);
		drains.dropEstimated(5, tekton, 7, SpecialWeapon.ELDER_MAUL, 103 - CombatCheckPlugin.ESTIMATE_REPLACED_TICKS);
		drains.add(5, tekton, SpecialWeapon.ELDER_MAUL, 1, true, 103);
		assertFalse(drains.estimated(5, tekton));
		assertEquals(134, drains.of(5, tekton).apply(205, 205, 0, false, 0).defence);
		// the next one of that player is not estimated at all
		assertFalse(e.seen(110, 7, false, 11124, 0, 21003, 5, tekton, true));
		// a message before the estimate is decided: nothing estimated
		SpecEstimate f = new SpecEstimate();
		assertTrue(f.seen(200, 8, false, 1378, 0, 13576, 5, tekton, true));
		f.partySpec(8);
		f.hitsplat(201, tekton, 22, HitsplatID.DAMAGE_OTHER, false);
		assertTrue(f.resolve(201).isEmpty());
	}

	/** What is never estimated, and a miss: it counts only on Tekton. */
	@Test
	public void whatIsNotEstimated()
	{
		Object npc = new Object();
		SpecEstimate e = new SpecEstimate();
		// the player's own, a Bandos godsword (its option off), the maul animation with another weapon seen, a frame past
		// the first
		assertFalse(e.seen(10, 1, true, 11124, 0, 21003, 3, npc, false));
		assertFalse(e.seen(10, 2, false, 7642, 0, 11804, 3, npc, false));
		assertFalse(e.seen(10, 3, false, 11124, 0, 13576, 3, npc, false));
		assertFalse(e.seen(10, 4, false, 11124, 5, 21003, 3, npc, false));
		assertNull(SpecEstimate.weapon(12394, 31113));
		// the same animation on the next tick, not set again, is the same attack
		assertTrue(e.seen(10, 5, false, 1378, 0, 26710, 3, npc, false));
		assertFalse(e.seen(11, 5, false, 1378, 1, 26710, 3, npc, false));
		// its hit misses with every hitsplat 0 (a block): on a monster other than Tekton nothing is lowered
		e.hitsplat(11, npc, 0, HitsplatID.BLOCK_OTHER, false);
		e.hitsplat(11, npc, 3, HitsplatID.BURN, false);
		assertTrue(e.resolve(11).isEmpty());
		// on Tekton the same miss takes 5%
		Object tekton = new Object();
		assertTrue(e.seen(20, 6, false, 1378, 0, 13576, 4, tekton, true));
		e.hitsplat(21, tekton, 0, HitsplatID.BLOCK_OTHER, false);
		List<SpecEstimate.Landed> landed = e.resolve(21);
		assertEquals(1, landed.size());
		assertEquals(0, landed.get(0).hit);
		// with no hitsplat at all on that tick: nothing
		assertTrue(e.seen(30, 9, false, 1378, 0, 13576, 4, tekton, true));
		assertTrue(e.resolve(31).isEmpty());
	}

	/**
	 * The two Bandos godswords of the raid's file are the client player's own (its message: 38 on Tekton). Taken as another
	 * player's, neither is counted: on the tick each lands there are 3 and 4 hitsplats (0, 17, 38; 26, 50, 13, 20), and
	 * nothing says which is the godsword's. So it was with all 20 of that player's in the 12 raids of the measurement.
	 */
	@Test
	public void theRaidsBandosGodswordsHaveMoreThanOneHitsplat() throws IOException
	{
		Replay r = replay(false, true);
		// both are estimated from the animation, and both left out on their tick's hitsplats
		assertTrue(r.started.contains("651 BANDOS_GODSWORD"));
		assertTrue(r.started.contains("657 BANDOS_GODSWORD"));
		assertTrue(r.messages.stream().anyMatch(m -> m.me && m.weapon == SpecialWeapon.BANDOS_GODSWORD && m.hit == 38));
		assertTrue(r.landed.stream().noneMatch(l -> l.weapon == SpecialWeapon.BANDOS_GODSWORD));
	}

	/** The Bandos godsword with its option on: one hitsplat not the player's own on the tick it lands is its damage. */
	@Test
	public void aBandosGodswordWithOneHitsplatCounts()
	{
		Object npc = new Object();
		SpecEstimate e = new SpecEstimate();
		e.bgs(true);
		// player 7, godsword 11804 seen in hand, animation 7642 at tick 100: its hit lands at 101
		assertEquals(1, SpecEstimate.BGS_DELAY);
		assertTrue(e.seen(100, 7, false, 7642, 0, 11804, 5, npc, false));
		e.endTick(Collections.singleton(7));
		// the player's own hitsplat, a burn and a heal are not the godsword's; the 42 is the only other one
		e.hitsplat(101, npc, 30, HitsplatID.DAMAGE_ME, true);
		e.hitsplat(101, npc, 3, HitsplatID.BURN, false);
		e.hitsplat(101, npc, 5, HitsplatID.HEAL, false);
		e.hitsplat(101, npc, 42, HitsplatID.DAMAGE_OTHER, false);
		assertTrue(e.resolve(100).isEmpty());
		List<SpecEstimate.Landed> landed = e.resolve(101);
		assertEquals(1, landed.size());
		SpecEstimate.Landed l = landed.get(0);
		assertEquals(SpecialWeapon.BANDOS_GODSWORD, l.weapon);
		assertEquals(42, l.hit);
		assertEquals("the tick its party message would come", 102, l.tick);
		DrainTracker drains = new DrainTracker();
		drains.addEstimated(l.npcIndex, l.npc, l.weapon, l.hit, l.tekton, l.tick, l.playerId);
		assertTrue(drains.estimated(5, npc));
		// the damage off Defence: 200 - 42
		assertEquals(158, drains.of(5, npc).apply(200, 200, 0, false, 0).defence);
		// what the Defence cannot take goes on to the other levels, as the godsword's rule in DefenceDrains has it
		DefenceDrains.Levels full = new DefenceDrains.Levels();
		full.defence = 20;
		full.strength = 100;
		full.attack = 100;
		full.magic = 100;
		full.ranged = 100;
		DrainTracker low = new DrainTracker();
		low.addEstimated(5, npc, l.weapon, l.hit, false, l.tick, 7);
		DefenceDrains.Levels left = low.of(5, npc).apply(full, false, 0);
		assertEquals(0, left.defence);
		assertEquals(78, left.strength);
		// the ornament's animation with the ornamented godsword counts the same
		assertEquals(SpecialWeapon.BANDOS_GODSWORD, SpecEstimate.weapon(7643, 20370, true));
		assertEquals(SpecialWeapon.BANDOS_GODSWORD, SpecEstimate.weapon(7642, 20370, true));
		// the animation with another weapon seen, or with the option off: not a godsword
		assertNull(SpecEstimate.weapon(7642, 13576, true));
		assertNull(SpecEstimate.weapon(7642, 11804, false));
		assertNull(SpecEstimate.weapon(7642, 11804));
	}

	/** Two hitsplats not the player's own, or none: not counted; a 0 alone is a miss, which counts only on Tekton (10). */
	@Test
	public void aBandosGodswordInDoubtOrMissing()
	{
		Object npc = new Object();
		SpecEstimate e = new SpecEstimate();
		e.bgs(true);
		// two hitsplats of others on the tick: one is the godsword's, nobody can tell which
		assertTrue(e.seen(100, 7, false, 7642, 0, 11804, 5, npc, false));
		e.hitsplat(101, npc, 42, HitsplatID.DAMAGE_OTHER, false);
		e.hitsplat(101, npc, 17, HitsplatID.DAMAGE_OTHER, false);
		assertTrue(e.resolve(101).isEmpty());
		// a 0 with a hit: still two
		assertTrue(e.seen(110, 8, false, 7642, 0, 11804, 5, npc, false));
		e.hitsplat(111, npc, 0, HitsplatID.BLOCK_OTHER, false);
		e.hitsplat(111, npc, 25, HitsplatID.DAMAGE_OTHER, false);
		assertTrue(e.resolve(111).isEmpty());
		// none at all
		assertTrue(e.seen(120, 9, false, 7642, 0, 11804, 5, npc, false));
		assertTrue(e.resolve(121).isEmpty());
		// one hitsplat on the tick before or after does not count: the tick is the one measured
		assertTrue(e.seen(130, 10, false, 7642, 0, 11804, 5, npc, false));
		e.hitsplat(130, npc, 40, HitsplatID.DAMAGE_OTHER, false);
		e.hitsplat(132, npc, 40, HitsplatID.DAMAGE_OTHER, false);
		assertTrue(e.resolve(132).isEmpty());
		// one 0 alone: it missed, and lowers nothing
		assertTrue(e.seen(140, 11, false, 7642, 0, 11804, 5, npc, false));
		e.hitsplat(141, npc, 0, HitsplatID.BLOCK_OTHER, false);
		assertTrue(e.resolve(141).isEmpty());
		// on Tekton the same miss counts, and drains 10 (OSRS Wiki, Bandos godsword)
		Object tekton = new Object();
		assertTrue(e.seen(150, 12, false, 7642, 0, 11804, 4, tekton, true));
		e.hitsplat(151, tekton, 0, HitsplatID.BLOCK_OTHER, false);
		List<SpecEstimate.Landed> landed = e.resolve(151);
		assertEquals(1, landed.size());
		assertEquals(0, landed.get(0).hit);
		DrainTracker drains = new DrainTracker();
		SpecEstimate.Landed l = landed.get(0);
		drains.addEstimated(l.npcIndex, l.npc, l.weapon, l.hit, l.tekton, l.tick, l.playerId);
		assertEquals(195, drains.of(4, tekton).apply(205, 205, 0, false, 0).defence);
	}

	/**
	 * The party's message replaces a godsword estimated, with its own damage; one before the hit lands leaves nothing
	 * estimated. With SpecEstimate's godsword switch off nothing of it is estimated, and turning it off drops the ones
	 * waiting for their hit, but no Elder maul.
	 */
	@Test
	public void theBandosGodswordGivesWayToTheParty()
	{
		Object npc = new Object();
		SpecEstimate e = new SpecEstimate();
		DrainTracker drains = new DrainTracker();
		e.bgs(true);
		assertTrue(e.seen(100, 7, false, 7642, 0, 11804, 5, npc, false));
		e.hitsplat(101, npc, 42, HitsplatID.DAMAGE_OTHER, false);
		SpecEstimate.Landed l = e.resolve(101).get(0);
		drains.addEstimated(l.npcIndex, l.npc, l.weapon, l.hit, l.tekton, l.tick, l.playerId);
		assertEquals(158, drains.of(5, npc).apply(200, 200, 0, false, 0).defence);
		// the message comes: counted once, with the party's damage
		e.partySpec(7);
		drains.dropEstimated(5, npc, 7, SpecialWeapon.BANDOS_GODSWORD, 102 - CombatCheckPlugin.ESTIMATE_REPLACED_TICKS);
		drains.add(5, npc, SpecialWeapon.BANDOS_GODSWORD, 42, false, 102);
		assertFalse(drains.estimated(5, npc));
		assertEquals(1, drains.counted(5, npc));
		assertEquals(158, drains.of(5, npc).apply(200, 200, 0, false, 0).defence);
		assertFalse(e.seen(110, 7, false, 7642, 0, 11804, 5, npc, false));
		// a message before the hit lands: nothing estimated
		assertTrue(e.seen(120, 8, false, 7642, 0, 11804, 5, npc, false));
		e.partySpec(8);
		e.hitsplat(121, npc, 30, HitsplatID.DAMAGE_OTHER, false);
		assertTrue(e.resolve(121).isEmpty());
		// the option off: the godsword is not estimated
		SpecEstimate off = new SpecEstimate();
		assertFalse(off.seen(100, 7, false, 7642, 0, 11804, 5, npc, false));
		// turned off while one waits for its hit: dropped; an Elder maul estimate stays
		SpecEstimate f = new SpecEstimate();
		f.bgs(true);
		assertTrue(f.seen(200, 9, false, 7642, 0, 11804, 5, npc, false));
		assertTrue(f.seen(200, 10, false, 11124, 0, 21003, 5, npc, false));
		f.bgs(false);
		f.hitsplat(201, npc, 42, HitsplatID.DAMAGE_OTHER, false);
		f.hitsplat(202, npc, 30, HitsplatID.DAMAGE_OTHER, false);
		assertTrue(f.resolve(201).isEmpty());
		List<SpecEstimate.Landed> maul = f.resolve(202);
		assertEquals(1, maul.size());
		assertEquals(SpecialWeapon.ELDER_MAUL, maul.get(0).weapon);
	}

	/** The overlay's one mark: "~" on each figure, the note once. */
	@Test
	public void theFiguresAreMarked()
	{
		assertEquals("Slash ~+12% (spec estimated)", CombatCheckOverlay.marked("Slash +12%", "spec estimated"));
		assertEquals("faltam 2 ~+0.4%", CombatCheckOverlay.marked("faltam 2 +0.4%", null));
		assertEquals("Diamond ~+3% (quiver)", CombatCheckOverlay.marked("Diamond +3% (quiver)", null));
		assertEquals("Crush", CombatCheckOverlay.marked("Crush", null));
		assertEquals("spec estimada", Words.of(CombatCheckConfig.Language.PORTUGUESE, Words.Key.SPEC_ESTIMATED));
	}
}
