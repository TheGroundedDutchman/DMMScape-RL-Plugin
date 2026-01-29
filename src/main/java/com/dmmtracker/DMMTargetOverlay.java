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
 * DMMScape Companion - Target Overlay
 * Renders tile highlights for the current target
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
import net.runelite.client.ui.overlay.OverlayUtil;
import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Overlay that highlights the tile of the current target.
 * Shows tile outline and target name when player is within render distance.
 */
public class DMMTargetOverlay extends Overlay
{
	private static final int RENDER_DISTANCE = 50;
	private static final int NAME_HEIGHT_OFFSET = 150;
	private static final float STROKE_WIDTH = 2.0f;

	@Inject
	private Client client;

	@Inject
	private TargetService targetService;

	@Inject
	private DMMTrackerConfig config;

	public DMMTargetOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.MED);
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

		WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();

		List<TargetPoint> visibleTargets = new ArrayList<>();
		for (TargetPoint target : allTargets)
		{
			WorldPoint targetWp = target.getWorldPoint();
			if (targetWp == null)
			{
				continue;
			}

			if (targetWp.getPlane() != playerWp.getPlane())
			{
				continue;
			}

			if (playerWp.distanceTo(targetWp) > RENDER_DISTANCE)
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

		Color overlayColor = config.targetOverlayColor();
		Color fillColor = new Color(
			overlayColor.getRed(),
			overlayColor.getGreen(),
			overlayColor.getBlue(),
			50
		);

		for (int i = 0; i < Math.min(maxTargets, visibleTargets.size()); i++)
		{
			TargetPoint target = visibleTargets.get(i);
			WorldPoint targetWp = target.getWorldPoint();
			if (targetWp == null)
			{
				continue;
			}

			LocalPoint targetLp = LocalPoint.fromWorld(client, targetWp);
			if (targetLp == null)
			{
				continue;
			}

			Polygon tilePoly = Perspective.getCanvasTilePoly(client, targetLp);
			if (tilePoly != null)
			{
				OverlayUtil.renderPolygon(graphics, tilePoly, overlayColor, fillColor, new BasicStroke(STROKE_WIDTH));
			}

			String displayText = formatTargetText(target);
			Point textPoint = Perspective.getCanvasTextLocation(client, graphics, targetLp, displayText, NAME_HEIGHT_OFFSET);
			if (textPoint != null)
			{
				OverlayUtil.renderTextLocation(graphics, textPoint, displayText, overlayColor);
			}
		}

		return null;
	}

	/**
	 * Formats the target text for display.
	 * Shows category prefix if available.
	 */
	private String formatTargetText(TargetPoint target)
	{
		String name = target.getName();
		if (name.length() > 40)
		{
			name = name.substring(0, 37) + "...";
		}
		int order = target.getOrder() + 1;
		return order + ". " + name;
	}
}
