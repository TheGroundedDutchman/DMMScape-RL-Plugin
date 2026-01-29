/*
 * Copyright (c) 2026, DMMScape
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * DMMScape Companion - World Map Legend Overlay
 * Renders a collapsible, draggable legend and layer toggles on the world map.
 */
package com.dmmtracker;

import com.dmmtracker.ui.DmmColors;
import com.dmmtracker.ui.IconManager;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.ImageUtil;

public class WorldMapLegendOverlay extends Overlay implements MouseListener
{
	private static final String CONFIG_GROUP = "dmmtracker";

	private static final int PANEL_PADDING = 6;
	private static final int HEADER_PADDING_Y = 4;
	private static final int ROW_PADDING_Y = 2;
	private static final int ROW_GAP = 6;
	private static final int CHECKBOX_SIZE = 10;
	private static final int DOT_SIZE = 8;
	private static final int ICON_SIZE = 12;
	private static final int MINI_ICON_SIZE = 10;
	private static final int MINI_ICON_GAP = 6;
	private static final int PANEL_MARGIN = 12;
	private static final int HEADER_BODY_GAP = 4;
	private static final int DRAG_THRESHOLD = 4;
	private static final long DRAG_SUPPRESS_MS = 200;

	private static final Color PANEL_BG = new Color(35, 35, 35, 220);
	private static final Color HEADER_BG = new Color(25, 25, 25, 235);
	private static final Color BORDER_COLOR = new Color(0, 0, 0, 140);

	private static final String KEY_PLAN_MARKERS = "legendPlanMarkers";
	private static final String KEY_ROUTE = "legendRoute";
	private static final String KEY_TELEPORT_LAYER = "legendTeleports";
	private static final String KEY_COMPLETED = "legendCompleted";
	private static final String KEY_DIARY = "legendDiary";
	private static final String KEY_QUEST = "legendQuest";
	private static final String KEY_BOSS = "legendBoss";
	private static final String KEY_CA = "legendCa";
	private static final String KEY_SIGIL = "legendSigil";
	private static final String KEY_LAMP = "legendLamp";
	private static final String KEY_PRAYER = "legendPrayer";
	private static final String KEY_TELEPORT_TARGET = "legendTeleportTarget";
	private static final String KEY_CUSTOM = "legendCustom";

	@Inject
	private Client client;

	@Inject
	private DMMTrackerConfig config;

	@Inject
	private ConfigManager configManager;

	private final Map<String, Rectangle> itemBounds = new HashMap<>();
	private final Map<String, BufferedImage> iconCache = new HashMap<>();
	private Rectangle headerBounds;
	private Rectangle miniBounds;
	private Rectangle panelBounds;
	private Rectangle mapBounds;
	private boolean mapVisible = false;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private boolean dragArmed = false;
	private boolean dragging = false;
	private boolean suppressClick = false;
	private int dragOffsetX;
	private int dragOffsetY;
	private int dragStartX;
	private int dragStartY;
	private long lastDragTime = 0;

	public WorldMapLegendOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
		setMovable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		itemBounds.clear();
		headerBounds = null;
		miniBounds = null;
		panelBounds = null;
		mapBounds = null;
		mapVisible = false;

		if (!config.showWorldMapLegend())
		{
			return null;
		}

		Widget worldMapWidget = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
		if (worldMapWidget == null || worldMapWidget.isHidden())
		{
			return null;
		}

		mapBounds = worldMapWidget.getBounds();
		if (mapBounds == null || mapBounds.width <= 0 || mapBounds.height <= 0)
		{
			return null;
		}
		mapVisible = true;

		List<LegendItem> items = buildLegendItems();
		List<LegendItem> miniItems = buildMiniLegendItems(items);
		boolean collapsed = config.worldMapLegendCollapsed();

		Font headerFont = FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD);
		Font bodyFont = FontManager.getRunescapeSmallFont();
		FontMetrics headerMetrics = graphics.getFontMetrics(headerFont);
		FontMetrics bodyMetrics = graphics.getFontMetrics(bodyFont);

		int headerHeight = headerMetrics.getHeight() + HEADER_PADDING_Y * 2;
		int rowHeight = bodyMetrics.getHeight() + ROW_PADDING_Y * 2;

		String title = collapsed ? "Legend" : "Legend & Layers";
		String indicator = collapsed ? "[+]" : "[-]";
		int headerTextWidth = headerMetrics.stringWidth(title);
		int indicatorWidth = headerMetrics.stringWidth(indicator);

		int maxLabelWidth = 0;
		for (LegendItem item : items)
		{
			maxLabelWidth = Math.max(maxLabelWidth, bodyMetrics.stringWidth(item.label));
		}

		if (collapsed)
		{
			int miniIconsWidth = miniItems.size() * MINI_ICON_SIZE + Math.max(0, miniItems.size() - 1) * MINI_ICON_GAP;
			panelHeight = headerHeight;
			panelWidth = PANEL_PADDING * 2 + headerTextWidth + ROW_GAP + miniIconsWidth + ROW_GAP + indicatorWidth;
		}
		else
		{
			int bodyHeight = HEADER_BODY_GAP + (items.size() * rowHeight) + PANEL_PADDING;
			panelHeight = headerHeight + bodyHeight;
			int rowWidth = PANEL_PADDING * 2 + CHECKBOX_SIZE + ROW_GAP + ICON_SIZE + ROW_GAP + DOT_SIZE + ROW_GAP + maxLabelWidth;
			int headerWidth = PANEL_PADDING * 2 + headerTextWidth + ROW_GAP + indicatorWidth;
			panelWidth = Math.max(rowWidth, headerWidth);
		}

		resolvePanelPosition(mapBounds, panelWidth, panelHeight);

		graphics.setColor(PANEL_BG);
		graphics.fillRect(panelX, panelY, panelWidth, panelHeight);
		graphics.setColor(BORDER_COLOR);
		graphics.drawRect(panelX, panelY, panelWidth, panelHeight);
		panelBounds = new Rectangle(panelX, panelY, panelWidth, panelHeight);

		graphics.setColor(HEADER_BG);
		graphics.fillRect(panelX, panelY, panelWidth, headerHeight);

		graphics.setFont(headerFont);
		int headerTextY = panelY + (headerHeight - headerMetrics.getHeight()) / 2 + headerMetrics.getAscent();
		graphics.setColor(DmmColors.GOLD);
		graphics.drawString(title, panelX + PANEL_PADDING, headerTextY);
		graphics.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		graphics.drawString(indicator, panelX + panelWidth - PANEL_PADDING - indicatorWidth, headerTextY);

		if (collapsed)
		{
			int iconX = panelX + PANEL_PADDING + headerTextWidth + ROW_GAP;
			int iconY = panelY + (headerHeight - MINI_ICON_SIZE) / 2;
			for (LegendItem item : miniItems)
			{
				drawLegendIcon(graphics, item, iconX, iconY, MINI_ICON_SIZE);
				drawMiniDot(graphics, item, iconX + MINI_ICON_SIZE - 3, iconY + MINI_ICON_SIZE - 3);
				iconX += MINI_ICON_SIZE + MINI_ICON_GAP;
			}
			miniBounds = new Rectangle(panelX, panelY, panelWidth, headerHeight);
			return null;
		}

		headerBounds = new Rectangle(panelX, panelY, panelWidth, headerHeight);

		graphics.setFont(bodyFont);
		int rowY = panelY + headerHeight + HEADER_BODY_GAP;

		for (LegendItem item : items)
		{
			Rectangle rowRect = new Rectangle(panelX, rowY, panelWidth, rowHeight);
			itemBounds.put(item.key, rowRect);

			int checkboxX = panelX + PANEL_PADDING;
			int checkboxY = rowY + (rowHeight - CHECKBOX_SIZE) / 2;
			drawCheckbox(graphics, checkboxX, checkboxY, item.active);

			int iconX = checkboxX + CHECKBOX_SIZE + ROW_GAP;
			int iconY = rowY + (rowHeight - ICON_SIZE) / 2;
			drawLegendIcon(graphics, item, iconX, iconY, ICON_SIZE);

			int dotX = iconX + ICON_SIZE + ROW_GAP;
			int dotY = rowY + (rowHeight - DOT_SIZE) / 2;
			drawDot(graphics, dotX, dotY, item.color, item.active);

			int textY = rowY + (rowHeight - bodyMetrics.getHeight()) / 2 + bodyMetrics.getAscent();
			graphics.setColor(item.active ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
			graphics.drawString(item.label, dotX + DOT_SIZE + ROW_GAP, textY);

			rowY += rowHeight;
		}

		return null;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		if (event == null || event.isConsumed())
		{
			return event;
		}

		if (event.getButton() != MouseEvent.BUTTON1)
		{
			return event;
		}

		if (!isLegendVisible())
		{
			return event;
		}

		if (suppressClick && System.currentTimeMillis() - lastDragTime < DRAG_SUPPRESS_MS)
		{
			return event;
		}
		suppressClick = false;

		java.awt.Point point = event.getPoint();
		if (miniBounds != null && miniBounds.contains(point))
		{
			toggleLegendCollapsed();
			event.consume();
			return event;
		}

		if (headerBounds != null && headerBounds.contains(point))
		{
			toggleLegendCollapsed();
			event.consume();
			return event;
		}

		if (config.worldMapLegendCollapsed())
		{
			return event;
		}

		for (Map.Entry<String, Rectangle> entry : itemBounds.entrySet())
		{
			if (entry.getValue().contains(point))
			{
				toggleLegendItem(entry.getKey());
				event.consume();
				return event;
			}
		}

		return event;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		if (event == null || event.isConsumed() || event.getButton() != MouseEvent.BUTTON1)
		{
			return event;
		}

		if (!isLegendVisible())
		{
			return event;
		}

		java.awt.Point point = event.getPoint();
		Rectangle dragBounds = headerBounds != null ? headerBounds : miniBounds;
		if (dragBounds != null && dragBounds.contains(point))
		{
			dragArmed = true;
			dragging = false;
			dragStartX = point.x;
			dragStartY = point.y;
			dragOffsetX = point.x - panelX;
			dragOffsetY = point.y - panelY;
		}

		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		if (dragging)
		{
			dragging = false;
			dragArmed = false;
			suppressClick = true;
			lastDragTime = System.currentTimeMillis();
			if (event != null)
			{
				event.consume();
			}
			return event;
		}

		dragArmed = false;
		return event;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		if (event == null || event.isConsumed() || !dragArmed || mapBounds == null)
		{
			return event;
		}

		int dx = Math.abs(event.getX() - dragStartX);
		int dy = Math.abs(event.getY() - dragStartY);
		if (!dragging && (dx > DRAG_THRESHOLD || dy > DRAG_THRESHOLD))
		{
			dragging = true;
		}

		if (dragging)
		{
			int newX = event.getX() - dragOffsetX;
			int newY = event.getY() - dragOffsetY;
			newX = clamp(newX, mapBounds.x + PANEL_MARGIN, mapBounds.x + mapBounds.width - panelWidth - PANEL_MARGIN);
			newY = clamp(newY, mapBounds.y + PANEL_MARGIN, mapBounds.y + mapBounds.height - panelHeight - PANEL_MARGIN);
			panelX = newX;
			panelY = newY;
			configManager.setConfiguration(CONFIG_GROUP, "worldMapLegendX", newX);
			configManager.setConfiguration(CONFIG_GROUP, "worldMapLegendY", newY);
			event.consume();
		}

		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		return event;
	}

	private boolean isLegendVisible()
	{
		return config.showWorldMapLegend() && mapVisible;
	}

	private void resolvePanelPosition(Rectangle bounds, int width, int height)
	{
		int storedX = config.worldMapLegendX();
		int storedY = config.worldMapLegendY();
		if (storedX >= 0 && storedY >= 0)
		{
			panelX = storedX;
			panelY = storedY;
		}
		else
		{
			panelX = bounds.x + bounds.width - width - PANEL_MARGIN;
			panelY = bounds.y + PANEL_MARGIN;
		}

		panelX = clamp(panelX, bounds.x + PANEL_MARGIN, bounds.x + bounds.width - width - PANEL_MARGIN);
		panelY = clamp(panelY, bounds.y + PANEL_MARGIN, bounds.y + bounds.height - height - PANEL_MARGIN);
	}

	private int clamp(int value, int min, int max)
	{
		if (max < min)
		{
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}

	private List<LegendItem> buildLegendItems()
	{
		List<LegendItem> items = new ArrayList<>();
		items.add(new LegendItem(KEY_PLAN_MARKERS, "Plan markers", DmmColors.INFO, config.showWorldMapTargets(), LegendIcon.PLAN));
		items.add(new LegendItem(KEY_ROUTE, "Route", config.routeColor(), config.enableRouteDrawing() && config.showWorldMapRoute(), LegendIcon.ROUTE));
		items.add(new LegendItem(KEY_TELEPORT_LAYER, "Teleports", DmmColors.TYPE_TELEPORT, config.showWorldMapTeleports(), LegendIcon.TELEPORT_LAYER));
		items.add(new LegendItem(KEY_COMPLETED, "Completed", DmmColors.SUCCESS, config.mapShowCompleted(), LegendIcon.COMPLETED));
		items.add(new LegendItem(KEY_DIARY, "Diary targets", DmmColors.TYPE_DIARY, config.mapShowDiaryTargets(), LegendIcon.DIARY));
		items.add(new LegendItem(KEY_QUEST, "Quest targets", DmmColors.INFO, config.mapShowQuestTargets(), LegendIcon.QUEST));
		items.add(new LegendItem(KEY_BOSS, "Boss targets", DmmColors.CATEGORY_BOSS, config.mapShowBossTargets(), LegendIcon.BOSS));
		items.add(new LegendItem(KEY_CA, "CA targets", DmmColors.TYPE_CA, config.mapShowCaTargets(), LegendIcon.CA));
		items.add(new LegendItem(KEY_SIGIL, "Sigil targets", DmmColors.TYPE_SIGIL, config.mapShowSigilTargets(), LegendIcon.SIGIL));
		items.add(new LegendItem(KEY_LAMP, "Lamp targets", DmmColors.TYPE_LAMP, config.mapShowLampTargets(), LegendIcon.LAMP));
		items.add(new LegendItem(KEY_PRAYER, "Prayer targets", DmmColors.TYPE_PRAYER, config.mapShowPrayerTargets(), LegendIcon.PRAYER));
		items.add(new LegendItem(KEY_TELEPORT_TARGET, "Teleport targets", DmmColors.TYPE_TELEPORT, config.mapShowTeleportTargets(), LegendIcon.TELEPORT_TARGET));
		items.add(new LegendItem(KEY_CUSTOM, "Custom targets", DmmColors.WARNING, config.mapShowCustomTargets(), LegendIcon.CUSTOM));
		return items;
	}

	private List<LegendItem> buildMiniLegendItems(List<LegendItem> items)
	{
		List<LegendItem> mini = new ArrayList<>();
		addMiniItem(mini, items, KEY_PLAN_MARKERS);
		addMiniItem(mini, items, KEY_ROUTE);
		addMiniItem(mini, items, KEY_DIARY);
		addMiniItem(mini, items, KEY_QUEST);
		addMiniItem(mini, items, KEY_TELEPORT_LAYER);
		return mini;
	}

	private void addMiniItem(List<LegendItem> mini, List<LegendItem> items, String key)
	{
		for (LegendItem item : items)
		{
			if (item.key.equals(key))
			{
				mini.add(item);
				return;
			}
		}
	}

	private void toggleLegendCollapsed()
	{
		boolean newState = !config.worldMapLegendCollapsed();
		configManager.setConfiguration(CONFIG_GROUP, "worldMapLegendCollapsed", newState);
	}

	private void toggleLegendItem(String key)
	{
		switch (key)
		{
			case KEY_PLAN_MARKERS:
				configManager.setConfiguration(CONFIG_GROUP, "showWorldMapTargets", !config.showWorldMapTargets());
				break;
			case KEY_ROUTE:
				boolean routeEnabled = config.enableRouteDrawing() && config.showWorldMapRoute();
				if (!routeEnabled)
				{
					configManager.setConfiguration(CONFIG_GROUP, "enableRouteDrawing", true);
					configManager.setConfiguration(CONFIG_GROUP, "showWorldMapRoute", true);
				}
				else
				{
					configManager.setConfiguration(CONFIG_GROUP, "showWorldMapRoute", false);
				}
				break;
			case KEY_TELEPORT_LAYER:
				configManager.setConfiguration(CONFIG_GROUP, "showWorldMapTeleports", !config.showWorldMapTeleports());
				break;
			case KEY_COMPLETED:
				configManager.setConfiguration(CONFIG_GROUP, "mapShowCompleted", !config.mapShowCompleted());
				break;
			case KEY_DIARY:
				configManager.setConfiguration(CONFIG_GROUP, "mapShowDiaryTargets", !config.mapShowDiaryTargets());
				break;
			case KEY_QUEST:
				configManager.setConfiguration(CONFIG_GROUP, "mapShowQuestTargets", !config.mapShowQuestTargets());
				break;
			case KEY_BOSS:
				configManager.setConfiguration(CONFIG_GROUP, "mapShowBossTargets", !config.mapShowBossTargets());
				break;
			case KEY_CA:
				configManager.setConfiguration(CONFIG_GROUP, "mapShowCaTargets", !config.mapShowCaTargets());
				break;
			case KEY_SIGIL:
				configManager.setConfiguration(CONFIG_GROUP, "mapShowSigilTargets", !config.mapShowSigilTargets());
				break;
			case KEY_LAMP:
				configManager.setConfiguration(CONFIG_GROUP, "mapShowLampTargets", !config.mapShowLampTargets());
				break;
			case KEY_PRAYER:
				configManager.setConfiguration(CONFIG_GROUP, "mapShowPrayerTargets", !config.mapShowPrayerTargets());
				break;
			case KEY_TELEPORT_TARGET:
				configManager.setConfiguration(CONFIG_GROUP, "mapShowTeleportTargets", !config.mapShowTeleportTargets());
				break;
			case KEY_CUSTOM:
				configManager.setConfiguration(CONFIG_GROUP, "mapShowCustomTargets", !config.mapShowCustomTargets());
				break;
			default:
				break;
		}
	}

	private void drawCheckbox(Graphics2D graphics, int x, int y, boolean checked)
	{
		graphics.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		graphics.drawRect(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE);
		if (checked)
		{
			graphics.setColor(DmmColors.GOLD);
			graphics.fillRect(x + 1, y + 1, CHECKBOX_SIZE - 1, CHECKBOX_SIZE - 1);
			graphics.setColor(Color.BLACK);
			int startX = x + 2;
			int startY = y + CHECKBOX_SIZE / 2;
			graphics.drawLine(startX, startY, startX + 3, startY + 3);
			graphics.drawLine(startX + 3, startY + 3, startX + 8, startY - 2);
		}
	}

	private void drawLegendIcon(Graphics2D graphics, LegendItem item, int x, int y, int size)
	{
		BufferedImage icon = getLegendIcon(item.icon, size);
		if (icon == null)
		{
			return;
		}

		Composite previous = graphics.getComposite();
		if (!item.active)
		{
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		}
		graphics.drawImage(icon, x, y, null);
		graphics.setComposite(previous);
	}

	private void drawDot(Graphics2D graphics, int x, int y, Color color, boolean active)
	{
		Color dotColor = color != null ? color : ColorScheme.LIGHT_GRAY_COLOR;
		if (!active)
		{
			dotColor = withAlpha(dotColor, 120);
		}

		graphics.setColor(Color.BLACK);
		graphics.drawOval(x, y, DOT_SIZE, DOT_SIZE);
		graphics.setColor(dotColor);
		graphics.fillOval(x + 1, y + 1, DOT_SIZE - 2, DOT_SIZE - 2);
	}

	private void drawMiniDot(Graphics2D graphics, LegendItem item, int x, int y)
	{
		Color dotColor = item.color != null ? item.color : ColorScheme.LIGHT_GRAY_COLOR;
		if (!item.active)
		{
			dotColor = withAlpha(dotColor, 120);
		}
		graphics.setColor(Color.BLACK);
		graphics.drawOval(x, y, 4, 4);
		graphics.setColor(dotColor);
		graphics.fillOval(x + 1, y + 1, 3, 3);
	}

	private Color withAlpha(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private BufferedImage getLegendIcon(LegendIcon iconType, int size)
	{
		if (iconType == null)
		{
			return null;
		}

		switch (iconType)
		{
			case ROUTE:
				return createRouteIcon(size, config.routeColor());
			case COMPLETED:
				return iconCache.computeIfAbsent("completed_" + size, key -> createCheckIcon(size, DmmColors.SUCCESS));
			case CUSTOM:
				return iconCache.computeIfAbsent("custom_" + size, key -> createPinIcon(size, DmmColors.WARNING));
			default:
				break;
		}

		String cacheKey = iconType.name() + "_" + size;
		BufferedImage cached = iconCache.get(cacheKey);
		if (cached != null)
		{
			return cached;
		}

		BufferedImage base = null;
		switch (iconType)
		{
			case PLAN:
				base = IconManager.getImage(IconManager.SECTION_PLAN);
				break;
			case TELEPORT_LAYER:
				base = IconManager.getImage(IconManager.LAW_RUNE_DETAIL);
				if (base == null)
				{
					base = IconManager.getImage(IconManager.SKILL_MAGIC);
				}
				break;
			case DIARY:
				base = IconManager.getImage(IconManager.SECTION_DIARY);
				break;
			case QUEST:
				base = IconManager.getImage(IconManager.SECTION_QUEST);
				break;
			case BOSS:
				base = IconManager.getImage(IconManager.CATEGORY_BOSS);
				break;
			case CA:
				base = IconManager.getImage(IconManager.SECTION_CA);
				break;
			case SIGIL:
				base = IconManager.getImage(IconManager.SECTION_SIGIL);
				break;
			case LAMP:
				base = IconManager.getImage(IconManager.SKILL_PRAYER);
				break;
			case PRAYER:
				base = IconManager.getImage(IconManager.SKILL_PRAYER);
				break;
			case TELEPORT_TARGET:
				base = IconManager.getImage(IconManager.SKILL_MAGIC);
				break;
			default:
				break;
		}

		if (base == null)
		{
			base = IconManager.getImage(IconManager.PLACEHOLDER);
		}

		if (base != null && (base.getWidth() != size || base.getHeight() != size))
		{
			base = ImageUtil.resizeImage(base, size, size);
		}

		iconCache.put(cacheKey, base);
		return base;
	}

	private BufferedImage createRouteIcon(int size, Color color)
	{
		BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int y = size / 2;
		g.setColor(Color.BLACK);
		g.drawLine(1, y + 1, size - 3, y + 1);
		g.setColor(color != null ? color : DmmColors.INFO);
		g.drawLine(1, y, size - 3, y);
		g.drawLine(size - 4, y - 2, size - 1, y);
		g.drawLine(size - 4, y + 2, size - 1, y);
		g.dispose();
		return icon;
	}

	private BufferedImage createCheckIcon(int size, Color color)
	{
		BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.BLACK);
		g.drawLine(2, size / 2 + 1, size / 2, size - 3);
		g.drawLine(size / 2, size - 3, size - 3, 3);
		g.setColor(color != null ? color : DmmColors.SUCCESS);
		g.drawLine(2, size / 2, size / 2, size - 4);
		g.drawLine(size / 2, size - 4, size - 3, 3);
		g.dispose();
		return icon;
	}

	private BufferedImage createPinIcon(int size, Color color)
	{
		BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int centerX = size / 2;
		int centerY = size / 2 - 1;
		g.setColor(Color.BLACK);
		g.drawOval(centerX - 3, centerY - 3, 6, 6);
		g.drawLine(centerX, centerY + 3, centerX, size - 2);
		g.setColor(color != null ? color : DmmColors.WARNING);
		g.fillOval(centerX - 2, centerY - 2, 5, 5);
		g.drawLine(centerX, centerY + 2, centerX, size - 3);
		g.dispose();
		return icon;
	}

	private enum LegendIcon
	{
		PLAN,
		ROUTE,
		TELEPORT_LAYER,
		COMPLETED,
		DIARY,
		QUEST,
		BOSS,
		CA,
		SIGIL,
		LAMP,
		PRAYER,
		TELEPORT_TARGET,
		CUSTOM
	}

	private static final class LegendItem
	{
		private final String key;
		private final String label;
		private final Color color;
		private final boolean active;
		private final LegendIcon icon;

		private LegendItem(String key, String label, Color color, boolean active, LegendIcon icon)
		{
			this.key = key;
			this.label = label;
			this.color = color;
			this.active = active;
			this.icon = icon;
		}
	}
}
