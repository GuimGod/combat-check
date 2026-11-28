/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * On each monster whose Defence a special attack lowered, while it is on screen, attacked or not ("Defence above
 * monsters"): the Defence icon with how much more its Defence can be lowered ("Def 54"), and under it a row per weapon
 * counted with the weapon's icon ("BGS x1", "DWH x1"), as in the line of the monster fought. Where ("Defence position"):
 * over its health bar, just under it or at its feet, never on the bar. The lines are worked out each game tick by the
 * plugin ({@link CombatCheckPlugin#above()}).
 */
class DefenceAboveOverlay extends Overlay
{
	/** The game draws a monster's health bar at the point of its height plus this (in local units). */
	private static final int BAR_HEIGHT = 15;
	/**
	 * The pixels the bar takes around that point: its top 3 over it and down to 7 under it (a boss's bar, the tallest,
	 * up to 10 high), with room over it for a second bar when a monster has two.
	 */
	static final int BAR_UP = 3 + 12;
	static final int BAR_DOWN = 7;
	/** Pixels kept clear between the bar and the lines. */
	static final int CLEAR = 3;
	/** Icons are drawn at this height, a little over the small font's. */
	static final int ICON_H = 14;
	private static final int GAP = 2;
	static final int LINE_H = ICON_H + 1;
	private static final Color TEXT = Color.WHITE;

	private final Client client;
	private final CombatCheckPlugin plugin;
	private final CombatCheckConfig config;
	private final SkillIconManager skillIcons;
	private final ItemManager itemManager;
	private final Map<Integer, BufferedImage> icons = new HashMap<>();

	@Inject
	DefenceAboveOverlay(Client client, CombatCheckPlugin plugin, CombatCheckConfig config, SkillIconManager skillIcons,
		ItemManager itemManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.skillIcons = skillIcons;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<CombatCheckPlugin.Above> above = config.showDefenceAbove() ? plugin.above() : null;
		if (above == null || above.isEmpty())
		{
			return null;
		}
		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = graphics.getFontMetrics();
		BufferedImage defence = skillIcons.getSkillImage(Skill.DEFENCE, true);
		CombatCheckConfig.DefencePosition position = config.defencePosition();
		for (CombatCheckPlugin.Above a : above)
		{
			NPC npc = a.npc;
			if (npc == null || npc.isDead() || a.line == null)
			{
				continue;
			}
			// where the game draws its health bar; null while the monster is off screen
			LocalPoint local = npc.getLocalLocation();
			if (local == null || npc.getWorldView() == null)
			{
				continue;
			}
			Point bar = Perspective.localToCanvas(client, local, npc.getWorldView().getPlane(),
				npc.getLogicalHeight() + BAR_HEIGHT);
			if (bar == null)
			{
				continue;
			}
			int x = bar.getX();
			int feetY = bar.getY();
			if (position == CombatCheckConfig.DefencePosition.FEET)
			{
				// the near edge of the tiles it stands on (a big boss's middle is inside its body)
				Polygon tiles = npc.getCanvasTilePoly();
				if (tiles != null)
				{
					Rectangle r = tiles.getBounds();
					x = (int) r.getCenterX();
					feetY = (int) r.getMaxY();
				}
			}
			// the Defence first and the weapons under it
			int y = firstBaseline(position, bar.getY(), feetY, fm.getAscent(), fm.getDescent(), a.line.rows.size());
			line(graphics, fm, x, y, defence, a.line.head);
			for (DefenceLine.Row row : a.line.rows)
			{
				y += LINE_H;
				line(graphics, fm, x, y, icon(row.itemId), row.text);
			}
		}
		return null;
	}

	/** Where a line's icon starts for its words' baseline {@code y}: centred on the words' height. */
	static int iconTop(int y, int ascent)
	{
		return y - ascent + (ascent - ICON_H) / 2;
	}

	/** How far a line reaches over its words' baseline, its icon included. */
	static int lineUp(int ascent)
	{
		return Math.max(ascent, -iconTop(0, ascent));
	}

	/** How far a line reaches under its words' baseline: its icon, or the words with their shadow (1 px). */
	static int lineDown(int ascent, int descent)
	{
		return Math.max(descent + 1, iconTop(0, ascent) + ICON_H);
	}

	/**
	 * The baseline of the first of the lines ({@code rows} weapon rows under the Defence) for the bar's point
	 * {@code barY} and the ground's {@code feetY}: over the bar, the last line ending {@link #CLEAR} over it; under the
	 * bar, the first starting {@link #CLEAR} under it; at the feet, the first starting there, or under the bar when the
	 * monster is so small on screen that its feet are by its bar.
	 */
	static int firstBaseline(CombatCheckConfig.DefencePosition position, int barY, int feetY, int ascent, int descent,
		int rows)
	{
		int up = lineUp(ascent);
		int underBar = barY + BAR_DOWN + CLEAR + up;
		if (position == CombatCheckConfig.DefencePosition.ABOVE_BAR)
		{
			int lastBaseline = barY - BAR_UP - CLEAR - lineDown(ascent, descent);
			return lastBaseline - rows * LINE_H;
		}
		if (position == CombatCheckConfig.DefencePosition.FEET)
		{
			return Math.max(feetY + up, underBar);
		}
		return underBar;
	}

	/** One line centred on {@code cx}, its words' baseline at {@code y}: the icon, then the words. */
	private static void line(Graphics2D graphics, FontMetrics fm, int cx, int y, BufferedImage icon, String text)
	{
		int iconW = icon == null ? 0 : icon.getWidth() * ICON_H / Math.max(1, icon.getHeight());
		int width = (icon == null ? 0 : iconW + GAP) + fm.stringWidth(text);
		int x = cx - width / 2;
		if (icon != null)
		{
			graphics.drawImage(icon, x, iconTop(y, fm.getAscent()), iconW, ICON_H, null);
			x += iconW + GAP;
		}
		OverlayUtil.renderTextLocation(graphics, new Point(x, y), text, TEXT);
	}

	private BufferedImage icon(int itemId)
	{
		return itemId <= 0 ? null : icons.computeIfAbsent(itemId, itemManager::getImage);
	}
}
