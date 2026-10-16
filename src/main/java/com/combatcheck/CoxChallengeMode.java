/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import net.runelite.api.ChatMessageType;
import net.runelite.client.util.Text;

/**
 * Whether the Chambers of Xeric raid is a Challenge Mode one, from the game's own messages at the raid's start. The
 * varbit RAIDS_CHALLENGE_MODE read 0 inside every Challenge Mode raid measured (14 raids of 2026-10-14 to 2026-10-16,
 * the player not the party's leader), so the raid was scaled as a normal one. What told them apart, in each of the 14,
 * is the game's message on the tick the raid begins, right after "The raid has begun!": "Map Layout: Challenge Mode
 * (Full)." (a notification of the raid, as the other raid messages).
 */
final class CoxChallengeMode
{
	static final String BEGUN = "The raid has begun!";
	static final String CHALLENGE_LAYOUT = "Map Layout: Challenge Mode";

	private CoxChallengeMode()
	{
	}

	/**
	 * What a message says about the raid's mode: true for the Challenge Mode layout, false for the start of a raid (a
	 * new raid, whose mode its layout message says on the same tick), null for anything else. Only the game's own
	 * messages count: a player typing the same words says nothing.
	 */
	static Boolean read(ChatMessageType type, String message)
	{
		if (type != ChatMessageType.FRIENDSCHATNOTIFICATION && type != ChatMessageType.GAMEMESSAGE)
		{
			return null;
		}
		String text = Text.removeTags(message == null ? "" : message).trim();
		if (text.startsWith(CHALLENGE_LAYOUT))
		{
			return true;
		}
		return text.equals(BEGUN) ? false : null;
	}
}
