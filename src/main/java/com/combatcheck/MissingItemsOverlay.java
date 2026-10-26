/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.util.ImageUtil;

/**
 * Marks, in the inventory, the pieces of the gear switch that were left behind, in the shape
 * and colour the player picked. The mark is on the item itself, like RuneLite's Inventory
 * Tags; a piece loses it by being equipped.
 */
class MissingItemsOverlay extends WidgetItemOverlay
{
	/** One pulse, from faint to full and back, in milliseconds. */
	private static final long PULSE_MS = 1000;

	private final ItemManager itemManager;
	private final CombatCheckPlugin plugin;
	private final CombatCheckConfig config;
	/** The item's shape filled with a colour, by item and colour; drawn translucent for the tint. */
	private final Map<Long, BufferedImage> tints = new HashMap<>();

	@Inject
	MissingItemsOverlay(ItemManager itemManager, CombatCheckPlugin plugin, CombatCheckConfig config)
	{
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
		showOnEquipment();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		CombatCheckConfig.Highlight shape = config.inventoryHighlight();
		boolean equipment = widgetItem.getWidget() != null
			&& WidgetUtil.componentToInterface(widgetItem.getWidget().getId()) == InterfaceID.WORNITEMS;
		if (equipment)
		{
			// a worn piece to take off: always marked, with a circle unless another shape was picked
			if (!plugin.takeOff().contains(plugin.itemOf(itemId)))
			{
				return;
			}
			if (shape == CombatCheckConfig.Highlight.OFF)
			{
				shape = CombatCheckConfig.Highlight.CIRCLE;
			}
		}
		else if (shape == CombatCheckConfig.Highlight.OFF || !(plugin.missing().contains(plugin.itemOf(itemId))
			|| (plugin.weaponAdvice() != null && (plugin.weaponAdvice().itemId == plugin.itemOf(itemId)
				|| plugin.weaponAdvice().alsoItemId == plugin.itemOf(itemId)))
			|| (plugin.boltAdvice() != null && plugin.boltAdvice().itemId == plugin.itemOf(itemId))))
		{
			return;
		}
		Color chosen = config.highlightColour();
		int intensity = Math.max(5, Math.min(100, config.highlightIntensity()));
		Color colour = new Color(chosen.getRed(), chosen.getGreen(), chosen.getBlue(), chosen.getAlpha() * intensity / 100);
		if (config.highlightPulse())
		{
			// from 35% to 100% of the chosen opacity
			double phase = (System.currentTimeMillis() % PULSE_MS) / (double) PULSE_MS;
			double strength = 0.35 + 0.65 * (0.5 - 0.5 * Math.cos(2 * Math.PI * phase));
			colour = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), (int) Math.round(colour.getAlpha() * strength));
		}
		Rectangle b = widgetItem.getCanvasBounds();
		int width = Math.max(1, config.highlightWidth());
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(colour);
		graphics.setStroke(new BasicStroke(width));
		switch (shape)
		{
			case ITEM_OUTLINE:
				BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), colour);
				graphics.drawImage(outline, b.x, b.y, null);
				break;
			case ITEM_TINT:
				// the item's own shape washed with the colour, at the colour's opacity
				int rgb = colour.getRGB() & 0xFFFFFF;
				BufferedImage tint = tints.computeIfAbsent(((long) itemId << 24) | rgb, k -> ImageUtil.fillImage(
					itemManager.getImage(itemId, widgetItem.getQuantity(), false), new Color(rgb)));
				Composite before = graphics.getComposite();
				graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, colour.getAlpha() / 255f));
				graphics.drawImage(tint, b.x, b.y, null);
				graphics.setComposite(before);
				break;
			case BOX:
				graphics.drawRect(b.x - 1, b.y - 1, b.width + 1, b.height + 1);
				break;
			case CIRCLE:
				graphics.drawOval(b.x - 2, b.y - 2, b.width + 3, b.height + 3);
				break;
			case FILL:
				graphics.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha() / 3));
				graphics.fillRect(b.x - 1, b.y - 1, b.width + 2, b.height + 2);
				graphics.setColor(colour);
				graphics.drawRect(b.x - 1, b.y - 1, b.width + 1, b.height + 1);
				break;
			default:
				break;
		}
	}
}
