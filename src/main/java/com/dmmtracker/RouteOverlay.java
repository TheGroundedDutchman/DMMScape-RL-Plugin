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
 * DMMScape Companion - Route Overlay
 * Renders route lines between plan targets in the 3D game scene
 * Inspired by Quest Helper's WorldLines.java patterns
 */
package com.dmmtracker;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Overlay that renders route lines between plan targets in the 3D scene.
 * Lines are drawn from player position to first target, then between subsequent targets.
 * Supports direction arrows and configurable colors/width.
 */
public class RouteOverlay extends Overlay
{
	private static final int ARROW_HEAD_SIZE = 8;
	private static final int SCENE_SIZE = 104; // Tiles visible in scene
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

	public RouteOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if ((!config.enableTargetSync() && targetService.getFocusTarget() == null)
			|| !config.enableRouteDrawing() || !config.showSceneRoute())
		{
			return null;
		}

		List<TargetPoint> allTargets = targetService.getFilteredTargetsWithCoords(config);
		if (allTargets.isEmpty())
		{
			return null;
		}

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		// Enable anti-aliasing
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
		int playerPlane = playerWp.getPlane();

		// Get targets on same plane, sorted by order
		List<TargetPoint> visibleTargets = new ArrayList<>();
		for (TargetPoint target : allTargets)
		{
			if (target.getWorldPoint() == null)
			{
				continue;
			}
			if (target.getWorldPoint().getPlane() != playerPlane)
			{
				continue;
			}
			visibleTargets.add(target);
		}

		if (visibleTargets.isEmpty())
		{
			return null;
		}

		visibleTargets.sort(Comparator.comparingInt(TargetPoint::getOrder));

		// Limit segments
		int maxSegments = config.maxRouteSegments();
		if (maxSegments > 0 && visibleTargets.size() > maxSegments)
		{
			visibleTargets = visibleTargets.subList(0, maxSegments);
		}

		Color routeColor = config.routeColor();
		int lineWidth = Math.max(1, config.routeLineWidth());
		boolean showArrows = config.showRouteArrows();

		// Draw line from player to first target
		if (!visibleTargets.isEmpty())
		{
			TargetPoint firstTarget = visibleTargets.get(0);
			WorldPoint firstTargetPoint = firstTarget.getWorldPoint();
			boolean teleportSegment = isTeleportTarget(firstTarget);
			Color color = teleportSegment ? TELEPORT_COLOR : routeColor;
			int width = teleportSegment ? 1 : lineWidth;
			drawRouteLine(graphics, playerWp, firstTargetPoint, color, width, showArrows && !teleportSegment, teleportSegment);
		}

		// Draw lines between subsequent targets
		for (int i = 0; i < visibleTargets.size() - 1; i++)
		{
			TargetPoint fromTarget = visibleTargets.get(i);
			TargetPoint toTarget = visibleTargets.get(i + 1);
			WorldPoint from = fromTarget.getWorldPoint();
			WorldPoint to = toTarget.getWorldPoint();
			boolean teleportSegment = isTeleportTarget(toTarget);
			Color color = teleportSegment ? TELEPORT_COLOR : routeColor;
			int width = teleportSegment ? 1 : lineWidth;
			drawRouteLine(graphics, from, to, color, width, showArrows && !teleportSegment, teleportSegment);
		}
		return null;
	}

	/**
	 * Draws a route line between two world points in the scene.
	 * Uses tile-by-tile rendering for long distances (like Quest Helper).
	 * Handles instanced areas by checking if the player is in an instance.
	 */
	private void drawRouteLine(Graphics2D graphics, WorldPoint from, WorldPoint to, Color color, int lineWidth, boolean showArrow, boolean teleportSegment)
	{
		if (from == null || to == null)
		{
			return;
		}

		// Check if player is in an instanced region
		boolean isInInstance = client.isInInstancedRegion();

		// For points within the scene, draw direct line
		// In instanced areas, LocalPoint.fromWorld may return null for non-instance coordinates
		LocalPoint fromLocal = isInInstance
			? LocalPoint.fromWorld(client, from.getX(), from.getY())
			: LocalPoint.fromWorld(client, from);
		LocalPoint toLocal = isInInstance
			? LocalPoint.fromWorld(client, to.getX(), to.getY())
			: LocalPoint.fromWorld(client, to);

		if (fromLocal != null && toLocal != null)
		{
			// Both points in scene - draw direct line
			Point fromScreen = Perspective.localToCanvas(client, fromLocal, from.getPlane());
			Point toScreen = Perspective.localToCanvas(client, toLocal, to.getPlane());

			if (fromScreen != null && toScreen != null)
			{
				drawRouteSegment(graphics, fromScreen, toScreen, color, lineWidth, teleportSegment);
				drawRouteChevron(graphics, fromScreen, toScreen, color, lineWidth, showArrow);
			}
		}
		else
		{
			// One or both points outside scene - draw clipped line
			drawClippedLine(graphics, from, to, color, lineWidth, showArrow, teleportSegment);
		}
	}

	/**
	 * Draws a line that may extend beyond the visible scene.
	 * Clips the line to scene boundaries.
	 */
	private void drawClippedLine(Graphics2D graphics, WorldPoint from, WorldPoint to, Color color, int lineWidth, boolean showArrow, boolean teleportSegment)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
		int baseX = playerWp.getX() - SCENE_SIZE / 2;
		int baseY = playerWp.getY() - SCENE_SIZE / 2;

		// Clip line to scene bounds
		int[] clipped = clipLineToScene(
			from.getX() - baseX, from.getY() - baseY,
			to.getX() - baseX, to.getY() - baseY,
			0, 0, SCENE_SIZE, SCENE_SIZE
		);

		if (clipped == null)
		{
			return; // Line completely outside scene
		}

		// Convert clipped coordinates back to world and then to screen
		int clippedFromX = baseX + clipped[0];
		int clippedFromY = baseY + clipped[1];
		int clippedToX = baseX + clipped[2];
		int clippedToY = baseY + clipped[3];

		WorldPoint clippedFrom = new WorldPoint(clippedFromX, clippedFromY, from.getPlane());
		WorldPoint clippedTo = new WorldPoint(clippedToX, clippedToY, to.getPlane());

		LocalPoint fromLocal = LocalPoint.fromWorld(client, clippedFrom);
		LocalPoint toLocal = LocalPoint.fromWorld(client, clippedTo);

		if (fromLocal == null || toLocal == null)
		{
			return;
		}

		Point fromScreen = Perspective.localToCanvas(client, fromLocal, from.getPlane());
		Point toScreen = Perspective.localToCanvas(client, toLocal, to.getPlane());

		if (fromScreen != null && toScreen != null)
		{
			drawRouteSegment(graphics, fromScreen, toScreen, color, lineWidth, teleportSegment);
			drawRouteChevron(graphics, fromScreen, toScreen, color, lineWidth, showArrow);
		}
	}

	/**
	 * Cohen-Sutherland line clipping algorithm.
	 * Returns null if line is completely outside, or clipped coordinates [x1,y1,x2,y2].
	 */
	private int[] clipLineToScene(int x1, int y1, int x2, int y2, int minX, int minY, int maxX, int maxY)
	{
		int code1 = computeOutCode(x1, y1, minX, minY, maxX, maxY);
		int code2 = computeOutCode(x2, y2, minX, minY, maxX, maxY);

		while (true)
		{
			if ((code1 | code2) == 0)
			{
				// Both inside
				return new int[]{x1, y1, x2, y2};
			}
			else if ((code1 & code2) != 0)
			{
				// Both outside same region
				return null;
			}
			else
			{
				int codeOut = code1 != 0 ? code1 : code2;
				int x, y;

				if ((codeOut & 8) != 0) // Top
				{
					x = x1 + (x2 - x1) * (maxY - y1) / (y2 - y1);
					y = maxY;
				}
				else if ((codeOut & 4) != 0) // Bottom
				{
					x = x1 + (x2 - x1) * (minY - y1) / (y2 - y1);
					y = minY;
				}
				else if ((codeOut & 2) != 0) // Right
				{
					y = y1 + (y2 - y1) * (maxX - x1) / (x2 - x1);
					x = maxX;
				}
				else // Left
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
		if (x < minX) code |= 1; // Left
		else if (x > maxX) code |= 2; // Right
		if (y < minY) code |= 4; // Bottom
		else if (y > maxY) code |= 8; // Top
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

		int size = Math.max(4, Math.min(ARROW_HEAD_SIZE, (int) (length * 0.2)));
		drawChevronAt(graphics, baseX, baseY, angle, size, color, lineWidth);

		if (length > MIN_ARROW_DISTANCE * 2)
		{
			int offset = Math.min(12, (int) (length * 0.15));
			int base2X = (int) (baseX - Math.cos(angle) * offset);
			int base2Y = (int) (baseY - Math.sin(angle) * offset);
			drawChevronAt(graphics, base2X, base2Y, angle, Math.max(3, size - 2), color, lineWidth);
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
