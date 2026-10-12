/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.combatcheck;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the reminders above the Combat Options tab: the weapon's icon with its best style (and
 * an outline around that style's button while the tab is open), and the icons of the switch
 * pieces still in the inventory, in inventory order. Red while something is pending; the style
 * notice turns green when the best style is selected and "show even when selected" is on.
 */
class CombatCheckOverlay extends Overlay
{
	/** The Combat Options tab in the three game layouts. */
	private static final int[] COMBAT_TABS = {
		InterfaceID.ToplevelOsrsStretch.STONE0,
		InterfaceID.ToplevelPreEoc.STONE0,
		InterfaceID.Toplevel.STONE0,
	};
	/** The four style buttons of the Combat Options tab, by position. */
	private static final int[] STYLE_BUTTONS = {
		InterfaceID.CombatInterface._0,
		InterfaceID.CombatInterface._1,
		InterfaceID.CombatInterface._2,
		InterfaceID.CombatInterface._3,
	};
	/** The picture on each of those buttons. */
	private static final int[] STYLE_ICONS = {
		InterfaceID.CombatInterface._0_ICON,
		InterfaceID.CombatInterface._1_ICON,
		InterfaceID.CombatInterface._2_ICON,
		InterfaceID.CombatInterface._3_ICON,
	};
	private static final Color WRONG_BACK = new Color(40, 0, 0, 200);
	private static final Color WRONG_EDGE = new Color(255, 60, 60);
	private static final Color RIGHT_BACK = new Color(0, 40, 0, 200);
	private static final Color RIGHT_EDGE = new Color(60, 220, 60);
	private static final int PAD = 2;
	/** Icons are drawn at this height; item icons are 36x32. */
	private static final int ICON_H = 18;
	/** The bolts box is a little larger: the same look as the Bolt Swap Reminder plugin's. */
	private static final int BOLT_ICON_H = 22;
	private int iconH = ICON_H;
	/** At most this many missing pieces are drawn as icons; the count says the rest. */
	private static final int MAX_ICONS = 8;

	private final Client client;
	private final ItemManager itemManager;
	private final CombatCheckPlugin plugin;
	private final CombatCheckConfig config;
	private final Map<Integer, BufferedImage> icons = new HashMap<>();
	private final Map<Integer, BufferedImage> sprites = new HashMap<>();
	private final SpriteManager spriteManager;

	@Inject
	CombatCheckOverlay(Client client, ItemManager itemManager, SpriteManager spriteManager, CombatCheckPlugin plugin,
		CombatCheckConfig config)
	{
		this.spriteManager = spriteManager;
		this.client = client;
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		CombatCheckPlugin.Advice advice = plugin.advice();
		List<Integer> missing = config.showMissingRow() ? plugin.missing() : Collections.emptyList();
		List<Integer> off = config.showMissingRow() ? plugin.takeOff() : Collections.emptyList();
		boolean rows = !missing.isEmpty() || !off.isEmpty();
		CombatCheckPlugin.WeaponAdvice weapon = plugin.weaponAdvice();
		CombatCheckPlugin.WeaponAdvice bolts = plugin.boltAdvice();
		String prayer = plugin.prayerNotice();
		String leftOn = plugin.leftOnNotice();
		String blowpipe = plugin.blowpipeNotice();
		if (advice == null && !rows && weapon == null && bolts == null && prayer == null && leftOn == null && blowpipe == null)
		{
			return null;
		}
		if (advice != null && config.outlineButton() && advice.bestIndex >= 0 && advice.bestIndex < STYLE_BUTTONS.length)
		{
			Rectangle button = bounds(STYLE_BUTTONS[advice.bestIndex]);
			if (button != null)
			{
				graphics.setColor(advice.selected ? RIGHT_EDGE : WRONG_EDGE);
				graphics.setStroke(new BasicStroke(2));
				graphics.drawRect(button.x, button.y, button.width - 1, button.height - 1);
			}
		}
		Rectangle tab = null;
		for (int id : COMBAT_TABS)
		{
			tab = bounds(id);
			if (tab != null)
			{
				break;
			}
		}
		if (tab == null)
		{
			return null;
		}
		graphics.setFont(FontManager.getRunescapeSmallFont());
		graphics.setStroke(new BasicStroke(1));
		FontMetrics fm = graphics.getFontMetrics();
		int h = Math.max(ICON_H, fm.getHeight()) + 2 * PAD;
		// one row above the tab's edge, leaving the row right above the tab to other notices
		int y = tab.y - 2 * h - 5;
		if (advice != null && config.showBox())
		{
			// the weapon's icon; with nothing in hand, the picture on the style's own button (a fist, a foot)
			BufferedImage picture = advice.weaponItemId > 0 ? icon(advice.weaponItemId) : styleIcon(advice.bestIndex);
			box(graphics, fm, tab.x, y, h, new BufferedImage[]{picture}, 0, advice.text,
				advice.selected ? RIGHT_BACK : WRONG_BACK, advice.selected ? RIGHT_EDGE : WRONG_EDGE);
			y -= h + 3;
		}
		if (rows)
		{
			// the pieces to put on, then the ones to take off, crossed out
			List<Integer> all = new ArrayList<>(missing);
			all.addAll(off);
			int n = Math.min(MAX_ICONS, all.size());
			BufferedImage[] pictures = new BufferedImage[n];
			for (int i = 0; i < n; i++)
			{
				pictures[i] = icon(all.get(i));
			}
			String left = String.format(Words.of(config.language(), Words.Key.PIECES_LEFT), missing.size());
			String takeOff = String.format(Words.of(config.language(), Words.Key.PIECES_OFF), off.size());
			String text = missing.isEmpty() ? takeOff : off.isEmpty() ? left : left + ", " + takeOff;
			String more = plugin.piecesGainText();
			if (more != null)
			{
				text += " " + more;
			}
			box(graphics, fm, tab.x, y, h, pictures, Math.max(0, n - Math.min(n, missing.size())), text, WRONG_BACK, WRONG_EDGE);
			y -= h + 3;
		}
		if (weapon != null)
		{
			// a weapon in the inventory whose set would deal much more: its icon and how much
			box(graphics, fm, tab.x, y, h, weapon.alsoItemId > 0
				? new BufferedImage[]{icon(weapon.itemId), icon(weapon.alsoItemId)} : new BufferedImage[]{icon(weapon.itemId)}, 0, weapon.text, WRONG_BACK, WRONG_EDGE);
			y -= h + 3;
		}
		if (bolts != null)
		{
			// the other bolts, in a box of their own and in green: it names what to change to ("Diamond +3%")
			graphics.setFont(FontManager.getRunescapeFont());
			FontMetrics big = graphics.getFontMetrics();
			iconH = BOLT_ICON_H;
			int bh = Math.max(BOLT_ICON_H, big.getHeight()) + 2 * PAD + 2;
			y -= bh - h;
			box(graphics, big, tab.x, y, bh, new BufferedImage[]{icon(bolts.itemId)}, 0, Words.bolts(config.language(), bolts.text),
				RIGHT_BACK, RIGHT_EDGE);
			iconH = ICON_H;
			graphics.setFont(FontManager.getRunescapeSmallFont());
			y -= h + 3;
		}
		if (prayer != null)
		{
			// the prayer the notice names, as its icon: crossed out when it is the wrong one that is on
			boolean wrong = prayer.startsWith(WRONG_PRAYER);
			String named = wrong ? prayer.substring(WRONG_PRAYER.length()) : prayer.endsWith(" off") ? prayer.substring(0, prayer.length() - 4) : "";
			int spriteId = prayerSprite(named);
			BufferedImage symbol = spriteId <= 0 ? null : sprites.computeIfAbsent(spriteId, id -> spriteManager.getSprite(id, 0));
			box(graphics, fm, tab.x, y, h, symbol == null ? new BufferedImage[0] : new BufferedImage[]{symbol}, wrong ? 1 : 0,
				Words.prayerNotice(config.language(), prayer),
				WRONG_BACK, WRONG_EDGE);
			y -= h + 3;
		}
		if (leftOn != null)
		{
			// the prayers that are on, as their icons: at most four, so the box stays small
			List<BufferedImage> symbols = new java.util.ArrayList<>();
			for (String name : plugin.leftOnPrayers())
			{
				int spriteId = prayerSprite(name);
				BufferedImage symbol = spriteId <= 0 || symbols.size() >= 4 ? null
					: sprites.computeIfAbsent(spriteId, id -> spriteManager.getSprite(id, 0));
				if (symbol != null)
				{
					symbols.add(symbol);
				}
			}
			box(graphics, fm, tab.x, y, h, symbols.toArray(new BufferedImage[0]), 0,
				Words.of(config.language(), Words.Key.PRAYER_STILL_ON), WRONG_BACK, WRONG_EDGE);
			y -= h + 3;
		}
		if (blowpipe != null)
		{
			box(graphics, fm, tab.x, y, h, new BufferedImage[0], 0, Words.of(config.language(), Words.Key.CHECK_BLOWPIPE),
				WRONG_BACK, WRONG_EDGE);
		}
		return null;
	}

	/** A box with a row of pictures and a few words after them; the last {@code crossed} pictures get an X over them. */
	private static final String WRONG_PRAYER = "Wrong prayer: ";

	/** The sprite of an attack prayer by its name as the notice writes it ("Rigour", "Eagle eye"), or -1. */
	static int prayerSprite(String name)
	{
		switch (name == null ? "" : name.toLowerCase(java.util.Locale.ROOT))
		{
			case "thick skin":
				return 115;
			case "rock skin":
				return 118;
			case "protect item":
				return 123;
			case "steel skin":
				return 124;
			case "protect from magic":
				return 127;
			case "protect from missiles":
				return 128;
			case "protect from melee":
				return 129;
			case "redemption":
				return 130;
			case "retribution":
				return 131;
			case "smite":
				return 132;
			case "burst of strength":
				return 116;
			case "clarity of thought":
				return 117;
			case "superhuman strength":
				return 119;
			case "improved reflexes":
				return 120;
			case "ultimate strength":
				return 125;
			case "incredible reflexes":
				return 126;
			case "sharp eye":
				return 133;
			case "mystic will":
				return 134;
			case "hawk eye":
				return 502;
			case "mystic lore":
				return 503;
			case "eagle eye":
				return 504;
			case "mystic might":
				return 505;
			case "chivalry":
				return 945;
			case "piety":
				return 946;
			case "rigour":
				return 1420;
			case "augury":
				return 1421;
			case "deadeye":
				return 1422;
			case "mystic vigour":
				return 1423;
			default:
				return -1;
		}
	}

	private void box(Graphics2D graphics, FontMetrics fm, int x, int y, int h, BufferedImage[] images, int crossed, String text,
		Color back, Color edge)
	{
		int iconsW = 0;
		int[] widths = new int[images.length];
		for (int i = 0; i < images.length; i++)
		{
			widths[i] = images[i] == null ? 0 : images[i].getWidth() * iconH / Math.max(1, images[i].getHeight());
			iconsW += images[i] == null ? 0 : widths[i] + PAD;
		}
		int w = iconsW + fm.stringWidth(text) + 2 * PAD;
		graphics.setColor(back);
		graphics.fillRect(x, y, w, h);
		graphics.setColor(edge);
		graphics.drawRect(x, y, w, h);
		int cx = x + PAD;
		for (int i = 0; i < images.length; i++)
		{
			if (images[i] != null)
			{
				int iy = y + (h - iconH) / 2;
				graphics.drawImage(images[i], cx, iy, widths[i], iconH, null);
				if (i >= images.length - crossed)
				{
					graphics.setColor(WRONG_EDGE);
					graphics.setStroke(new BasicStroke(2));
					graphics.drawLine(cx + 2, iy + 2, cx + widths[i] - 3, iy + iconH - 3);
					graphics.drawLine(cx + widths[i] - 3, iy + 2, cx + 2, iy + iconH - 3);
					graphics.setStroke(new BasicStroke(1));
				}
				cx += widths[i] + PAD;
			}
		}
		int baseline = y + (h - fm.getHeight()) / 2 + fm.getAscent();
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, cx + 1, baseline + 1);
		graphics.setColor(Color.YELLOW);
		graphics.drawString(text, cx, baseline);
	}

	private BufferedImage icon(int itemId)
	{
		return itemId <= 0 ? null : icons.computeIfAbsent(itemId, itemManager::getImage);
	}

	/** The sprite the game shows on a style's button in the Combat Options tab, or null. */
	private BufferedImage styleIcon(int styleIndex)
	{
		if (styleIndex < 0 || styleIndex >= STYLE_ICONS.length)
		{
			return null;
		}
		Widget w = client.getWidget(STYLE_ICONS[styleIndex]);
		int sprite = w == null ? -1 : w.getSpriteId();
		return sprite <= 0 ? null : sprites.computeIfAbsent(sprite, id -> spriteManager.getSprite(id, 0));
	}

	/** The on-screen box of a widget that is loaded and showing, or null. */
	private Rectangle bounds(int widgetId)
	{
		Widget w = client.getWidget(widgetId);
		if (w == null || w.isHidden())
		{
			return null;
		}
		Rectangle b = w.getBounds();
		return b == null || b.width <= 0 || b.height <= 0 ? null : b;
	}
}
