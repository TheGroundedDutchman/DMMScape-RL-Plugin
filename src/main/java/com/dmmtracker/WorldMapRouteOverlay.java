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
 * DMMScape Companion - World Map Route Overlay
 * Renders route lines between plan targets on the world map
 */
package com.dmmtracker;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.RenderOverview;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Overlay that renders route lines between plan targets on the world map.
 * Similar to Quest Helper's world map route lines.
 */
public class WorldMapRouteOverlay extends Overlay
{
	private static final int ARROW_HEAD_SIZE = 6;
	private static final int MIN_ARROW_DISTANCE = 18;
	private static final double ARROW_POSITION = 0.78;
	private static final int OUTLINE_ALPHA = 140;
	private static final Color TELEPORT_COLOR = new Color(0x4DA3FF);
	private static final float[] TELEPORT_DASH = new float[]{4f, 4f};

	@Inject
	private Client client;

	@Inject
	private TargetService targetService;

	@Inject
	private DMMTrackerConfig config;

	public WorldMapRouteOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.LOW);
		setMovable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if ((!config.enableTargetSync() && targetService.getFocusTarget() == null)
			|| !config.enableRouteDrawing() || !config.showWorldMapRoute())
		{
			return null;
		}

		// Check if world map is open
		Widget worldMapWidget = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
		if (worldMapWidget == null || worldMapWidget.isHidden())
		{
			return null;
		}

		RenderOverview renderOverview = client.getRenderOverview();
		if (renderOverview == null)
		{
			return null;
		}

		List<TargetPoint> allTargets = targetService.getFilteredTargetsWithCoords(config);
		if (allTargets.isEmpty())
		{
			return null;
		}

		// Enable anti-aliasing
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Sort by order and limit segments
		List<TargetPoint> targets = new ArrayList<>(allTargets);
		targets.sort(Comparator.comparingInt(TargetPoint::getOrder));

		int maxSegments = config.maxRouteSegments();
		if (maxSegments > 0 && targets.size() > maxSegments)
		{
			targets = targets.subList(0, maxSegments);
		}

		// Get world map bounds for clipping
		Rectangle mapBounds = worldMapWidget.getBounds();
		if (mapBounds == null || mapBounds.width <= 0 || mapBounds.height <= 0)
		{
			return null;
		}

		Color routeColor = config.routeColor();
		int lineWidth = Math.max(1, config.routeLineWidth());
		boolean showArrows = config.showRouteArrows();

		// Draw route from player position (if available) to first target
		if (client.getLocalPlayer() != null && !targets.isEmpty())
		{
			WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
			TargetPoint firstTarget = targets.get(0);
			WorldPoint firstTargetPoint = firstTarget.getWorldPoint();
			if (firstTargetPoint != null)
			{
				boolean teleportSegment = isTeleportTarget(firstTarget);
				Color color = teleportSegment ? TELEPORT_COLOR : routeColor;
				int width = teleportSegment ? 1 : lineWidth;
				drawWorldMapLine(graphics, renderOverview, mapBounds, playerWp, firstTargetPoint, color, width, showArrows && !teleportSegment, teleportSegment);
			}
		}

		// Draw lines between subsequent targets
		for (int i = 0; i < targets.size() - 1; i++)
		{
			TargetPoint fromTarget = targets.get(i);
			TargetPoint toTarget = targets.get(i + 1);
			WorldPoint from = fromTarget.getWorldPoint();
			WorldPoint to = toTarget.getWorldPoint();
			if (from != null && to != null)
			{
				boolean teleportSegment = isTeleportTarget(toTarget);
				Color color = teleportSegment ? TELEPORT_COLOR : routeColor;
				int width = teleportSegment ? 1 : lineWidth;
				drawWorldMapLine(graphics, renderOverview, mapBounds, from, to, color, width, showArrows && !teleportSegment, teleportSegment);
			}
		}
		return null;
	}

	/**
	 * Draws a route line between two world points on the world map.
	 */
	private void drawWorldMapLine(Graphics2D graphics, RenderOverview renderOverview, Rectangle mapBounds,
		WorldPoint from, WorldPoint to, Color color, int lineWidth, boolean showArrow, boolean teleportSegment)
	{
		Point fromPoint = worldPointToMapPoint(renderOverview, mapBounds, from);
		Point toPoint = worldPointToMapPoint(renderOverview, mapBounds, to);

		if (fromPoint == null || toPoint == null)
		{
			return;
		}

		// Clip to map bounds
		int[] clipped = clipLineToRectangle(
			fromPoint.getX(), fromPoint.getY(),
			toPoint.getX(), toPoint.getY(),
			mapBounds.x, mapBounds.y,
			mapBounds.x + mapBounds.width, mapBounds.y + mapBounds.height
		);

		if (clipped == null)
		{
			return;
		}

		Point start = new Point(clipped[0], clipped[1]);
		Point end = new Point(clipped[2], clipped[3]);
		drawRouteSegment(graphics, start, end, color, lineWidth, teleportSegment);
		drawRouteChevron(graphics, start, end, color, lineWidth, showArrow);
	}

	/**
	 * Converts a world point to a point on the world map widget.
	 */
	private Point worldPointToMapPoint(RenderOverview renderOverview, Rectangle mapBounds, WorldPoint worldPoint)
	{
		if (worldPoint == null)
		{
			return null;
		}

		float pixelsPerTile = renderOverview.getWorldMapZoom();

		// Get the world map's center position (in world coordinates as a Point)
		Point mapCenterPoint = renderOverview.getWorldMapPosition();
		if (mapCenterPoint == null)
		{
			return null;
		}

		// Calculate offset from map center
		int dx = worldPoint.getX() - mapCenterPoint.getX();
		int dy = worldPoint.getY() - mapCenterPoint.getY();

		// Convert to screen coordinates (Y is inverted in screen space)
		int screenX = (int) (mapBounds.x + mapBounds.width / 2 + dx * pixelsPerTile);
		int screenY = (int) (mapBounds.y + mapBounds.height / 2 - dy * pixelsPerTile);

		return new Point(screenX, screenY);
	}

	/**
	 * Cohen-Sutherland line clipping to rectangle.
	 */
	private int[] clipLineToRectangle(int x1, int y1, int x2, int y2, int minX, int minY, int maxX, int maxY)
	{
		int code1 = computeOutCode(x1, y1, minX, minY, maxX, maxY);
		int code2 = computeOutCode(x2, y2, minX, minY, maxX, maxY);

		while (true)
		{
			if ((code1 | code2) == 0)
			{
				return new int[]{x1, y1, x2, y2};
			}
			else if ((code1 & code2) != 0)
			{
				return null;
			}
			else
			{
				int codeOut = code1 != 0 ? code1 : code2;
				int x, y;

				if ((codeOut & 8) != 0)
				{
					x = x1 + (x2 - x1) * (maxY - y1) / (y2 - y1);
					y = maxY;
				}
				else if ((codeOut & 4) != 0)
				{
					x = x1 + (x2 - x1) * (minY - y1) / (y2 - y1);
					y = minY;
				}
				else if ((codeOut & 2) != 0)
				{
					y = y1 + (y2 - y1) * (maxX - x1) / (x2 - x1);
					x = maxX;
				}
				else
				{
					y = y1 + (y2 - y1) * (minX - x1) / (x2 - x1);
					x = minX;
				}

				if (codeOut == code1)
				{
					x1 = x;
					y1 = y;
					code1 = computeOutCode(x1, y1, minX, minY, maxX, maxY);
				}
				else
				{
					x2 = x;
					y2 = y;
					code2 = computeOutCode(x2, y2, minX, minY, maxX, maxY);
				}
			}
		}
	}

	private int computeOutCode(int x, int y, int minX, int minY, int maxX, int maxY)
	{
		int code = 0;
		if (x < minX) code |= 1;
		else if (x > maxX) code |= 2;
		if (y < minY) code |= 4;
		else if (y > maxY) code |= 8;
		return code;
	}

	private void drawRouteSegment(Graphics2D graphics, Point from, Point to, Color color, int lineWidth, boolean teleportSegment)
	{
		Stroke oldStroke = graphics.getStroke();
		if (teleportSegment)
		{
			graphics.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, TELEPORT_DASH, 0));
			graphics.setColor(color);
			graphics.drawLine(from.getX(), from.getY(), to.getX(), to.getY());
			graphics.setStroke(oldStroke);
			return;
		}

		int outlineWidth = Math.max(2, lineWidth + 2);

		graphics.setStroke(new BasicStroke(outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(new Color(0, 0, 0, OUTLINE_ALPHA));
		graphics.drawLine(from.getX(), from.getY(), to.getX(), to.getY());

		graphics.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(color);
		graphics.drawLine(from.getX(), from.getY(), to.getX(), to.getY());
		graphics.setStroke(oldStroke);
	}

	private void drawRouteChevron(Graphics2D graphics, Point from, Point to, Color color, int lineWidth, boolean showArrow)
	{
		if (!showArrow)
		{
			return;
		}

		double dx = to.getX() - from.getX();
		double dy = to.getY() - from.getY();
		double length = Math.hypot(dx, dy);
		if (length < MIN_ARROW_DISTANCE)
		{
			return;
		}

		double angle = Math.atan2(dy, dx);
		double t = ARROW_POSITION;
		int baseX = (int) (from.getX() + dx * t);
		int baseY = (int) (from.getY() + dy * t);

		int size = Math.max(3, Math.min(ARROW_HEAD_SIZE, (int) (length * 0.2)));
		drawChevronAt(graphics, baseX, baseY, angle, size, color, lineWidth);

		if (length > MIN_ARROW_DISTANCE * 2)
		{
			int offset = Math.min(10, (int) (length * 0.15));
			int base2X = (int) (baseX - Math.cos(angle) * offset);
			int base2Y = (int) (baseY - Math.sin(angle) * offset);
			drawChevronAt(graphics, base2X, base2Y, angle, Math.max(2, size - 2), color, lineWidth);
		}
	}

	private void drawChevronAt(Graphics2D graphics, int baseX, int baseY, double angle, int size, Color color, int lineWidth)
	{
		double spread = Math.toRadians(28);
		double backAngle = angle + Math.PI;

		int leftX = (int) (baseX + Math.cos(backAngle + spread) * size);
		int leftY = (int) (baseY + Math.sin(backAngle + spread) * size);
		int rightX = (int) (baseX + Math.cos(backAngle - spread) * size);
		int rightY = (int) (baseY + Math.sin(backAngle - spread) * size);

		Stroke oldStroke = graphics.getStroke();
		int strokeWidth = Math.max(1, lineWidth);

		graphics.setStroke(new BasicStroke(strokeWidth + 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(new Color(0, 0, 0, OUTLINE_ALPHA));
		graphics.drawLine(baseX, baseY, leftX, leftY);
		graphics.drawLine(baseX, baseY, rightX, rightY);

		graphics.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(color);
		graphics.drawLine(baseX, baseY, leftX, leftY);
		graphics.drawLine(baseX, baseY, rightX, rightY);
		graphics.setStroke(oldStroke);
	}

	private boolean isTeleportTarget(TargetPoint target)
	{
		return target != null && "teleport".equalsIgnoreCase(target.getType());
	}
}
