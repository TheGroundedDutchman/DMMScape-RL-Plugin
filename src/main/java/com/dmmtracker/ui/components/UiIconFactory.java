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
package com.dmmtracker.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public final class UiIconFactory
{
	private UiIconFactory()
	{
	}

	public static Icon createBadgeIcon(String letter, Color color, int size)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g.setColor(color);
		g.fillOval(0, 0, size - 1, size - 1);

		g.setColor(Color.WHITE);
		int fontSize = Math.max(10, size - 6);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
		FontMetrics fm = g.getFontMetrics();
		int x = (size - fm.stringWidth(letter)) / 2;
		int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
		g.drawString(letter, x, y);

		g.dispose();
		return new ImageIcon(img);
	}

	public static Icon createSearchIcon(Color color, int size)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		int circleSize = (int) (size * 0.6);
		int padding = 2;
		int strokeWidth = Math.max(2, size / 8);

		g.setColor(color);
		g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		// Draw magnifying glass circle
		g.drawOval(padding, padding, circleSize, circleSize);

		// Draw handle
		int handleStartX = padding + circleSize - (int) (strokeWidth * 0.3);
		int handleStartY = padding + circleSize - (int) (strokeWidth * 0.3);
		int handleEndX = size - padding - 1;
		int handleEndY = size - padding - 1;
		g.drawLine(handleStartX, handleStartY, handleEndX, handleEndY);

		g.dispose();
		return new ImageIcon(img);
	}

	public static Icon createRefreshIcon(Color color, int size)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		int strokeWidth = Math.max(2, size / 8);
		g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(color);

		int padding = Math.max(2, strokeWidth);
		int diameter = size - padding * 2;
		int radius = diameter / 2;
		int center = size / 2;

		int startAngle = 35;
		int arcAngle = 290;
		g.drawArc(padding, padding, diameter, diameter, startAngle, arcAngle);

		double endAngle = Math.toRadians(startAngle + arcAngle);
		int tipX = center + (int) Math.round(Math.cos(endAngle) * radius);
		int tipY = center - (int) Math.round(Math.sin(endAngle) * radius);

		double tangent = endAngle + Math.PI / 2;
		double wing = Math.toRadians(25);
		int arrowSize = Math.max(4, size / 4);

		int x1 = (int) Math.round(tipX - Math.cos(tangent + wing) * arrowSize);
		int y1 = (int) Math.round(tipY + Math.sin(tangent + wing) * arrowSize);
		int x2 = (int) Math.round(tipX - Math.cos(tangent - wing) * arrowSize);
		int y2 = (int) Math.round(tipY + Math.sin(tangent - wing) * arrowSize);

		g.drawLine(tipX, tipY, x1, y1);
		g.drawLine(tipX, tipY, x2, y2);

		g.dispose();
		return new ImageIcon(img);
	}

	public static Icon createGearIcon(Color color, int size)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		int strokeWidth = Math.max(2, size / 8);
		g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(color);

		int center = size / 2;
		int outerRadius = size / 2 - strokeWidth;
		int innerRadius = Math.max(3, size / 4);

		// Outer gear ring
		g.drawOval(center - outerRadius, center - outerRadius, outerRadius * 2, outerRadius * 2);

		// Inner hub
		g.drawOval(center - innerRadius, center - innerRadius, innerRadius * 2, innerRadius * 2);

		// Simple teeth
		int teeth = 8;
		int toothLength = Math.max(3, size / 6);
		for (int i = 0; i < teeth; i++)
		{
			double angle = (Math.PI * 2 / teeth) * i;
			int x1 = (int) Math.round(center + Math.cos(angle) * (innerRadius + 1));
			int y1 = (int) Math.round(center + Math.sin(angle) * (innerRadius + 1));
			int x2 = (int) Math.round(center + Math.cos(angle) * (innerRadius + toothLength));
			int y2 = (int) Math.round(center + Math.sin(angle) * (innerRadius + toothLength));
			g.drawLine(x1, y1, x2, y2);
		}

		g.dispose();
		return new ImageIcon(img);
	}

	public static Icon createLocationIcon(Color color, int size)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int center = size / 2;
		int radius = Math.max(2, size / 4);
		int circleY = Math.max(1, size / 6);
		int baseY = Math.min(size - 3, circleY + radius + 2);
		int tipY = size - 2;
		int baseHalf = Math.max(2, radius);

		g.setColor(color);
		g.fillOval(center - radius, circleY, radius * 2, radius * 2);

		int[] xPoints = { center, center - baseHalf, center + baseHalf };
		int[] yPoints = { tipY, baseY, baseY };
		g.fillPolygon(xPoints, yPoints, 3);

		g.dispose();
		return new ImageIcon(img);
	}
}
