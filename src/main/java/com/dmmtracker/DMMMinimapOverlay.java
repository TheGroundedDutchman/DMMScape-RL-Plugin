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
 * DMMScape Companion - Minimap Overlay
 * Renders direction arrows and markers on the minimap for target locations
 */
package com.dmmtracker;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
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
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Overlay that renders direction arrows on the minimap pointing toward targets.
 * When target is visible on minimap, shows a marker at its location.
 * When target is off-screen, shows an arrow at the minimap edge.
 */
public class DMMMinimapOverlay extends Overlay
{
	private static final int ARROW_SIZE = 10;
	private static final int MARKER_SIZE = 6;
	private static final int MINIMAP_RADIUS = 70;
	private static final int EDGE_PADDING = 8;
	private static final int OUTLINE_ALPHA = 140;
	private static final Color TELEPORT_COLOR = new Color(0x4DA3FF);
	private static final float[] TELEPORT_DASH = new float[]{4f, 4f};

	@Inject
	private Client client;

	@Inject
	private TargetService targetService;

	@Inject
	private DMMTrackerConfig config;

	public DMMMinimapOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if ((!config.enableTargetSync() && targetService.getFocusTarget() == null) || !config.showTargetOverlay())
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

		// Enable anti-aliasing for smoother shapes
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
		List<TargetPoint> visibleTargets = new ArrayList<>();
		for (TargetPoint target : allTargets)
		{
			if (target.getWorldPoint() == null)
			{
				continue;
			}
			if (target.getWorldPoint().getPlane() != playerWp.getPlane())
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
		int maxTargets = Math.max(1, config.maxOverlayTargets());

		Point minimapCenter = getMinimapCenter();
		if (minimapCenter == null)
		{
			return null;
		}

		// Draw route lines on minimap if enabled
		if (config.enableRouteDrawing() && config.showMinimapRoute())
		{
			drawMinimapRouteLines(graphics, playerWp, visibleTargets, minimapCenter);
		}

		// Draw minimap markers for visible targets
		// Check if player is in an instanced region for proper LocalPoint conversion
		boolean isInInstance = client.isInInstancedRegion();
		Color overlayColor = config.targetOverlayColor();
		for (int i = 0; i < Math.min(maxTargets, visibleTargets.size()); i++)
		{
			TargetPoint target = visibleTargets.get(i);
			WorldPoint wp = target.getWorldPoint();
			LocalPoint targetLp = isInInstance
				? LocalPoint.fromWorld(client, wp.getX(), wp.getY())
				: LocalPoint.fromWorld(client, wp);
			if (targetLp == null)
			{
				continue;
			}

			Point minimapPoint = Perspective.localToMinimap(client, targetLp);
			if (minimapPoint != null && isOnMinimap(minimapPoint, minimapCenter))
			{
				drawMinimapMarker(graphics, minimapPoint, overlayColor);
			}
		}

		// Draw direction arrow to the next target if it's off minimap
		TargetPoint next = visibleTargets.isEmpty() ? null : visibleTargets.get(0);
		if (next != null && next.getWorldPoint() != null)
		{
			WorldPoint nextWp = next.getWorldPoint();
			LocalPoint nextLp = isInInstance
				? LocalPoint.fromWorld(client, nextWp.getX(), nextWp.getY())
				: LocalPoint.fromWorld(client, nextWp);
			if (nextLp != null)
			{
				Point minimapPoint = Perspective.localToMinimap(client, nextLp);
				if (minimapPoint == null)
				{
					double angle = calculateAngle(playerWp, next.getWorldPoint());
					drawDirectionArrow(graphics, minimapCenter, angle, overlayColor);
				}
			}
		}

		return null;
	}

	/**
	 * Draws a circular marker at the target location on the minimap.
	 */
	private void drawMinimapMarker(Graphics2D graphics, Point point, Color color)
	{
		int x = point.getX() - MARKER_SIZE / 2;
		int y = point.getY() - MARKER_SIZE / 2;

		// Draw filled circle with border
		graphics.setColor(color);
		graphics.fillOval(x, y, MARKER_SIZE, MARKER_SIZE);

		graphics.setColor(new Color(0, 0, 0, OUTLINE_ALPHA));
		graphics.drawOval(x, y, MARKER_SIZE, MARKER_SIZE);
	}

	/**
	 * Draws an arrow at the edge of the minimap pointing toward the target.
	 */
	private void drawDirectionArrow(Graphics2D graphics, Point center, double angle, Color color)
	{
		// Calculate position on minimap edge
		int edgeRadius = MINIMAP_RADIUS - EDGE_PADDING;
		int arrowX = center.getX() + (int) (Math.sin(angle) * edgeRadius);
		int arrowY = center.getY() - (int) (Math.cos(angle) * edgeRadius);

		// Create arrow shape pointing outward
		Polygon arrow = createArrowShape();

		// Transform to position and rotation
		AffineTransform transform = new AffineTransform();
		transform.translate(arrowX, arrowY);
		transform.rotate(angle);

		AffineTransform oldTransform = graphics.getTransform();
		Stroke oldStroke = graphics.getStroke();
		graphics.transform(transform);

		// Draw arrow
		graphics.setColor(color);
		graphics.fillPolygon(arrow);

		graphics.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(new Color(0, 0, 0, OUTLINE_ALPHA));
		graphics.drawPolygon(arrow);

		graphics.setStroke(oldStroke);
		graphics.setTransform(oldTransform);
	}

	/**
	 * Creates an arrow polygon pointing upward (will be rotated).
	 */
	private Polygon createArrowShape()
	{
		Polygon arrow = new Polygon();
		int tipY = -ARROW_SIZE;
		int midY = -ARROW_SIZE / 6;
		int tailY = ARROW_SIZE / 2;
		int halfWidth = ARROW_SIZE / 2;

		arrow.addPoint(0, tipY);
		arrow.addPoint(halfWidth, midY);
		arrow.addPoint(0, tailY);
		arrow.addPoint(-halfWidth, midY);
		return arrow;
	}

	/**
	 * Calculates the angle from player to target.
	 * Returns angle in radians where 0 is north, increasing clockwise.
	 */
	private double calculateAngle(WorldPoint from, WorldPoint to)
	{
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		// atan2 gives angle from positive X axis, we want from positive Y (north)
		return Math.atan2(dx, dy);
	}

	/**
	 * Gets the center point of the minimap widget.
	 */
	private Point getMinimapCenter()
	{
		Widget minimapWidget = client.getWidget(ComponentID.MINIMAP_CONTAINER);
		if (minimapWidget == null || minimapWidget.isHidden())
		{
			return new Point(
				client.getCanvasWidth() - 93,
				93
			);
		}

		return new Point(
			minimapWidget.getCanvasLocation().getX() + minimapWidget.getWidth() / 2,
			minimapWidget.getCanvasLocation().getY() + minimapWidget.getHeight() / 2
		);
	}

	/**
	 * Checks if a point is within the minimap circle.
	 */
	private boolean isOnMinimap(Point point, Point center)
	{
		int dx = point.getX() - center.getX();
		int dy = point.getY() - center.getY();
		int radiusSq = MINIMAP_RADIUS * MINIMAP_RADIUS;
		return dx * dx + dy * dy < radiusSq;
	}

	/**
	 * Draws route lines between targets on the minimap.
	 */
	private void drawMinimapRouteLines(Graphics2D graphics, WorldPoint playerWp, List<TargetPoint> targets, Point minimapCenter)
	{
		if (targets.isEmpty())
		{
			return;
		}

		// Sort by order and limit segments
		List<TargetPoint> sorted = new ArrayList<>(targets);
		sorted.sort(Comparator.comparingInt(TargetPoint::getOrder));

		int maxSegments = config.maxRouteSegments();
		if (maxSegments > 0 && sorted.size() > maxSegments)
		{
			sorted = sorted.subList(0, maxSegments);
		}

		Color routeColor = config.routeColor();
		int lineWidth = Math.max(1, config.routeLineWidth());

		// Draw line from player to first target
		if (!sorted.isEmpty())
		{
			TargetPoint firstTarget = sorted.get(0);
			WorldPoint firstWp = firstTarget.getWorldPoint();
			if (firstWp != null)
			{
				boolean teleportSegment = isTeleportTarget(firstTarget);
				Color color = teleportSegment ? TELEPORT_COLOR : routeColor;
				int width = teleportSegment ? 1 : lineWidth;
				drawMinimapRouteLine(graphics, playerWp, firstWp, minimapCenter, color, width, teleportSegment);
			}
		}

		// Draw lines between subsequent targets
		for (int i = 0; i < sorted.size() - 1; i++)
		{
			TargetPoint fromTarget = sorted.get(i);
			TargetPoint toTarget = sorted.get(i + 1);
			WorldPoint from = fromTarget.getWorldPoint();
			WorldPoint to = toTarget.getWorldPoint();
			if (from != null && to != null)
			{
				boolean teleportSegment = isTeleportTarget(toTarget);
				Color color = teleportSegment ? TELEPORT_COLOR : routeColor;
				int width = teleportSegment ? 1 : lineWidth;
				drawMinimapRouteLine(graphics, from, to, minimapCenter, color, width, teleportSegment);
			}
		}
	}

	private void drawMinimapRouteSegment(Graphics2D graphics, int x1, int y1, int x2, int y2, Color color, int lineWidth, boolean teleportSegment)
	{
		Stroke oldStroke = graphics.getStroke();
		if (teleportSegment)
		{
			graphics.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, TELEPORT_DASH, 0));
			graphics.setColor(color);
			graphics.drawLine(x1, y1, x2, y2);
			graphics.setStroke(oldStroke);
			return;
		}

		int outlineWidth = Math.max(2, lineWidth + 2);

		graphics.setStroke(new BasicStroke(outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(new Color(0, 0, 0, OUTLINE_ALPHA));
		graphics.drawLine(x1, y1, x2, y2);

		graphics.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(color);
		graphics.drawLine(x1, y1, x2, y2);
		graphics.setStroke(oldStroke);
	}

	/**
	 * Draws a single route line between two world points on the minimap.
	 * Clips the line to the minimap circle.
	 * Handles instanced areas properly.
	 */
	private void drawMinimapRouteLine(Graphics2D graphics, WorldPoint from, WorldPoint to, Point minimapCenter, Color color, int lineWidth, boolean teleportSegment)
	{
		boolean isInInstance = client.isInInstancedRegion();
		LocalPoint fromLocal = isInInstance
			? LocalPoint.fromWorld(client, from.getX(), from.getY())
			: LocalPoint.fromWorld(client, from);
		LocalPoint toLocal = isInInstance
			? LocalPoint.fromWorld(client, to.getX(), to.getY())
			: LocalPoint.fromWorld(client, to);

		Point fromMinimap = fromLocal != null ? Perspective.localToMinimap(client, fromLocal) : null;
		Point toMinimap = toLocal != null ? Perspective.localToMinimap(client, toLocal) : null;

		// If both points are outside minimap bounds, skip
		if (fromMinimap == null && toMinimap == null)
		{
			return;
		}

		// Calculate screen positions, projecting off-screen points to minimap edge
		int x1, y1, x2, y2;

		if (fromMinimap != null)
		{
			x1 = fromMinimap.getX();
			y1 = fromMinimap.getY();
		}
		else
		{
			// Project from center toward the target direction
			double angle = calculateAngle(from, to);
			x1 = minimapCenter.getX() + (int) (Math.sin(angle) * (MINIMAP_RADIUS - EDGE_PADDING));
			y1 = minimapCenter.getY() - (int) (Math.cos(angle) * (MINIMAP_RADIUS - EDGE_PADDING));
		}

		if (toMinimap != null)
		{
			x2 = toMinimap.getX();
			y2 = toMinimap.getY();
		}
		else
		{
			// Project toward the target direction
			if (client.getLocalPlayer() != null)
			{
				WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
				double angle = calculateAngle(playerWp, to);
				x2 = minimapCenter.getX() + (int) (Math.sin(angle) * (MINIMAP_RADIUS - EDGE_PADDING));
				y2 = minimapCenter.getY() - (int) (Math.cos(angle) * (MINIMAP_RADIUS - EDGE_PADDING));
			}
			else
			{
				return;
			}
		}

		// Clip line to minimap circle and draw
		int[] clipped = clipLineToCircle(x1, y1, x2, y2, minimapCenter.getX(), minimapCenter.getY(), MINIMAP_RADIUS - 2);
		if (clipped != null)
		{
			drawMinimapRouteSegment(graphics, clipped[0], clipped[1], clipped[2], clipped[3], color, lineWidth, teleportSegment);
		}
	}

	/**
	 * Clips a line segment to a circle.
	 * Returns [x1, y1, x2, y2] of clipped line, or null if completely outside.
	 */
	private int[] clipLineToCircle(int x1, int y1, int x2, int y2, int cx, int cy, int r)
	{
		// Check if both points are inside
		boolean p1Inside = isInsideCircle(x1, y1, cx, cy, r);
		boolean p2Inside = isInsideCircle(x2, y2, cx, cy, r);

		if (p1Inside && p2Inside)
		{
			return new int[]{x1, y1, x2, y2};
		}

		// Calculate line-circle intersection
		double dx = x2 - x1;
		double dy = y2 - y1;
		double fx = x1 - cx;
		double fy = y1 - cy;

		double a = dx * dx + dy * dy;
		double b = 2 * (fx * dx + fy * dy);
		double c = fx * fx + fy * fy - r * r;

		double discriminant = b * b - 4 * a * c;
		if (discriminant < 0)
		{
			return null; // No intersection
		}

		discriminant = Math.sqrt(discriminant);
		double t1 = (-b - discriminant) / (2 * a);
		double t2 = (-b + discriminant) / (2 * a);

		// Clamp t values to [0, 1]
		double tMin = Math.max(0, Math.min(t1, t2));
		double tMax = Math.min(1, Math.max(t1, t2));

		if (tMin > 1 || tMax < 0)
		{
			return null; // Line segment doesn't intersect circle
		}

		int newX1 = p1Inside ? x1 : (int) (x1 + tMin * dx);
		int newY1 = p1Inside ? y1 : (int) (y1 + tMin * dy);
		int newX2 = p2Inside ? x2 : (int) (x1 + tMax * dx);
		int newY2 = p2Inside ? y2 : (int) (y1 + tMax * dy);

		return new int[]{newX1, newY1, newX2, newY2};
	}

	private boolean isInsideCircle(int x, int y, int cx, int cy, int r)
	{
		int dx = x - cx;
		int dy = y - cy;
		return dx * dx + dy * dy <= r * r;
	}

	private boolean isTeleportTarget(TargetPoint target)
	{
		return target != null && "teleport".equalsIgnoreCase(target.getType());
	}
}
