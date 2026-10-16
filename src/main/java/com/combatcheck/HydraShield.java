/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;

/**
 * The Alchemical Hydra's damage reduction, read from what the game shows (OSRS Wiki, Alchemical Hydra/Strategies: "During
 * the first three phases, the Hydra reduces incoming damage by 75% until it is lured over and sprayed by the appropriate
 * chemical vent. The Hydra will Roaaaaaaaaaaar! when hit by the correct vent"; Alchemical Hydra: "At the start of each
 * phase, it has a 75% damage reduction [...] Barring the last phase, the damage reduction is reapplied"). While it is up,
 * the hitsplat of a hit is a quarter of the damage rolled, and two special attacks lower the levels by the damage rolled:
 * the Bandos godsword ("BGS spec applies before Hydra's 75% damage reduction", the strategies' equipment) and the
 * Barrelchest anchor ("It's 10% of the damage *rolled*", Barrelchest anchor, Mod Ash). Their drains are worked out from
 * four times the hitsplat (the game's rounding of the quarter is not known: up to 3 off).
 */
final class HydraShield
{
	/** The three phases that start with the reduction, by the ids the OSRS Wiki gives them: Serpentine, Electric, Fire. */
	private static final int[] SHIELDED = {8615, 8619, 8620};
	/** The last phase, Extinguished, which has none ("its damage reduction ability does not reset"), and its death. */
	private static final int[] UNSHIELDED = {8621, 8622};

	/** The game tick each Hydra's phase began on, while its reduction is up; by the NPC itself. */
	private final Map<Object, Integer> since = new HashMap<>();

	/**
	 * The NPC seen in a form on game tick {@code tick}, spawned or changed into it: a phase that starts with the reduction
	 * puts it up, the last phase takes it off, and the change of colour between two phases keeps what was.
	 */
	void form(Object npc, int npcId, int tick)
	{
		if (has(SHIELDED, npcId))
		{
			since.put(npc, tick);
		}
		else if (has(UNSHIELDED, npcId))
		{
			since.remove(npc);
		}
	}

	/**
	 * What the NPC says over its head on game tick {@code tick}: the Hydra's roar when the right vent sprays it takes the
	 * reduction off. A roar on the tick its phase began is the change of colour's ("as the Hydra roars and changes
	 * colour", the strategies), not the vent's.
	 */
	void says(Object npc, int npcId, String text, int tick)
	{
		Integer start = since.get(npc);
		if (start != null && tick > start && has(SHIELDED, npcId) && roar(text))
		{
			since.remove(npc);
		}
	}

	/** True while this Hydra's damage reduction is up. */
	boolean shielded(Object npc)
	{
		return since.containsKey(npc);
	}

	/** Drops this NPC: it died or went away. */
	void forget(Object npc)
	{
		since.remove(npc);
	}

	void clear()
	{
		since.clear();
	}

	/** The roar, which the wiki writes "Roaaaaaaaaaaar!" (the a's are not counted). */
	static boolean roar(String text)
	{
		return text != null && text.matches("Roa+r!*");
	}

	/**
	 * The damage a special attack's drain goes by, from its hitsplat: four times it for the Bandos godsword and the
	 * Barrelchest anchor while the reduction is up, the hitsplat itself otherwise.
	 */
	static int rolled(SpecialWeapon weapon, int hit, boolean shielded)
	{
		return shielded && hit > 0 && (weapon == SpecialWeapon.BANDOS_GODSWORD || weapon == SpecialWeapon.BARRELCHEST_ANCHOR)
			? hit * 4 : hit;
	}

	private static boolean has(int[] ids, int id)
	{
		return Arrays.stream(ids).anyMatch(i -> i == id);
	}
}
