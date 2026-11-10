package com.combatcheck;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Ezekiel of the Fractured Archive calling on Saradomin ("Saradomin, give me strength."), read from what he says. The
 * call gives him back some of his Defence (recorded fights: the Obligator's hits landed about 80% of the time after it,
 * against about 87% after one special attack and 70% with none), and the OSRS Wiki does not say how much. So from the
 * call to the end of the fight his Defence is not known: the line says "Def ?" once a special attack is counted, with
 * the rows by weapon under it, and the figures keep his full Defence, rather than ones worked out from a Defence he no
 * longer has.
 */
final class EzekielStrength
{
	/** Ezekiel's NPC id. */
	static final int EZEKIEL = 16652;
	/** The start of what he says when he calls on Saradomin. */
	static final String CALL = "Saradomin, give me strength";

	/** Each Ezekiel that called on Saradomin in this fight, by the NPC itself. */
	private final Set<Object> called = Collections.newSetFromMap(new WeakHashMap<>());

	/** What a monster says: Ezekiel's call marks his Defence as not known for the rest of the fight. */
	void says(Object npc, int npcId, String text)
	{
		if (npcId == EZEKIEL && text != null && text.startsWith(CALL))
		{
			called.add(npc);
		}
	}

	/** True once this Ezekiel has called on Saradomin. */
	boolean called(Object npc)
	{
		return npc != null && called.contains(npc);
	}

	/** The NPC is gone: a new fight starts from nothing. */
	void forget(Object npc)
	{
		called.remove(npc);
	}

	void clear()
	{
		called.clear();
	}
}
