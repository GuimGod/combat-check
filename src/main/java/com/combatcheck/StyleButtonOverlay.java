/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** The outline around the best attack style's button in the Combat Options tab: it stays on the button. */
class StyleButtonOverlay extends Overlay
{
	private static final int[] STYLE_BUTTONS = {
		InterfaceID.CombatInterface._0,
		InterfaceID.CombatInterface._1,
		InterfaceID.CombatInterface._2,
		InterfaceID.CombatInterface._3,
	};
	private static final Color WRONG_EDGE = new Color(255, 60, 60);
	private static final Color RIGHT_EDGE = new Color(60, 220, 60);

	private final Client client;
	private final CombatCheckPlugin plugin;
	private final CombatCheckConfig config;

	@Inject
	StyleButtonOverlay(Client client, CombatCheckPlugin plugin, CombatCheckConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		CombatCheckPlugin.Advice advice = plugin.advice();
		if (advice == null || !config.outlineButton() || advice.bestIndex < 0 || advice.bestIndex >= STYLE_BUTTONS.length)
		{
			return null;
		}
		Widget button = client.getWidget(STYLE_BUTTONS[advice.bestIndex]);
		Rectangle b = button == null || button.isHidden() ? null : button.getBounds();
		if (b != null && b.width > 0 && b.height > 0)
		{
			graphics.setColor(advice.selected ? RIGHT_EDGE : WRONG_EDGE);
			graphics.setStroke(new BasicStroke(2));
			graphics.drawRect(b.x, b.y, b.width - 1, b.height - 1);
		}
		return null;
	}
}
