/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Above each monster whose Defence a special attack lowered, while it is on screen, attacked or not ("Defence above
 * monsters"): the Defence icon with how much more its Defence can be lowered ("Def 54"), and under it a row per weapon
 * counted with the weapon's icon ("BGS x1", "DWH x1"), as in the line of the monster fought. The lines are worked out
 * each game tick by the plugin ({@link CombatCheckPlugin#above()}).
 */
class DefenceAboveOverlay extends Overlay
{
	/** The lowest line's height above the monster's head: over the health bar and the names other plugins draw there. */
	private static final int HEIGHT_ABOVE = 60;
	/** Icons are drawn at this height, a little over the small font's. */
	private static final int ICON_H = 14;
	private static final int GAP = 2;
	private static final int LINE_H = ICON_H + 1;
	private static final Color TEXT = Color.WHITE;

	private final CombatCheckPlugin plugin;
	private final CombatCheckConfig config;
	private final SkillIconManager skillIcons;
	private final ItemManager itemManager;
	private final Map<Integer, BufferedImage> icons = new HashMap<>();

	@Inject
	DefenceAboveOverlay(CombatCheckPlugin plugin, CombatCheckConfig config, SkillIconManager skillIcons,
		ItemManager itemManager)
	{
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
		for (CombatCheckPlugin.Above a : above)
		{
			NPC npc = a.npc;
			if (npc == null || npc.isDead() || a.line == null)
			{
				continue;
			}
			// null while the monster is off screen; with no words, the point is the middle above it
			Point at = npc.getCanvasTextLocation(graphics, "", npc.getLogicalHeight() + HEIGHT_ABOVE);
			if (at == null)
			{
				continue;
			}
			// the Defence on top, the weapons under it, the last one at the point
			int y = at.getY() - a.line.rows.size() * LINE_H;
			line(graphics, fm, at.getX(), y, defence, a.line.head);
			for (DefenceLine.Row row : a.line.rows)
			{
				y += LINE_H;
				line(graphics, fm, at.getX(), y, icon(row.itemId), row.text);
			}
		}
		return null;
	}

	/** One line centred on {@code cx}, its words' baseline at {@code y}: the icon, then the words. */
	private static void line(Graphics2D graphics, FontMetrics fm, int cx, int y, BufferedImage icon, String text)
	{
		int iconW = icon == null ? 0 : icon.getWidth() * ICON_H / Math.max(1, icon.getHeight());
		int width = (icon == null ? 0 : iconW + GAP) + fm.stringWidth(text);
		int x = cx - width / 2;
		if (icon != null)
		{
			graphics.drawImage(icon, x, y - fm.getAscent() + (fm.getAscent() - ICON_H) / 2, iconW, ICON_H, null);
			x += iconW + GAP;
		}
		OverlayUtil.renderTextLocation(graphics, new Point(x, y), text, TEXT);
	}

	private BufferedImage icon(int itemId)
	{
		return itemId <= 0 ? null : icons.computeIfAbsent(itemId, itemManager::getImage);
	}
}
