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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import javax.swing.JComponent;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;

/**
 * An animated loading spinner component.
 * Displays a circular spinner that rotates continuously when visible.
 */
public class LoadingSpinner extends JComponent
{
	private static final int DEFAULT_SIZE = 24;
	private static final int STROKE_WIDTH = 3;
	private static final int ANIMATION_DELAY_MS = 50;
	private static final int ARC_LENGTH = 270;

	private int angle = 0;
	private Timer animationTimer;
	private Color spinnerColor = ColorScheme.BRAND_ORANGE;
	private Color trackColor = ColorScheme.MEDIUM_GRAY_COLOR;

	public LoadingSpinner()
	{
		this(DEFAULT_SIZE);
	}

	public LoadingSpinner(int size)
	{
		setPreferredSize(new Dimension(size, size));
		setMinimumSize(new Dimension(size, size));
		setMaximumSize(new Dimension(size, size));
		setOpaque(false);

		animationTimer = new Timer(ANIMATION_DELAY_MS, e ->
		{
			angle = (angle + 10) % 360;
			repaint();
		});
	}

	/**
	 * Sets the color of the spinning arc.
	 */
	public void setSpinnerColor(Color color)
	{
		this.spinnerColor = color;
		repaint();
	}

	/**
	 * Sets the color of the background track.
	 */
	public void setTrackColor(Color color)
	{
		this.trackColor = color;
		repaint();
	}

	/**
	 * Starts the spinner animation.
	 */
	public void start()
	{
		if (!animationTimer.isRunning())
		{
			animationTimer.start();
		}
	}

	/**
	 * Stops the spinner animation.
	 */
	public void stop()
	{
		animationTimer.stop();
	}

	/**
	 * Returns whether the spinner is currently animating.
	 */
	public boolean isSpinning()
	{
		return animationTimer.isRunning();
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visible)
		{
			start();
		}
		else
		{
			stop();
		}
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		if (isVisible())
		{
			start();
		}
	}

	@Override
	public void removeNotify()
	{
		super.removeNotify();
		stop();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		int size = Math.min(getWidth(), getHeight());
		int x = (getWidth() - size) / 2;
		int y = (getHeight() - size) / 2;
		int arcSize = size - STROKE_WIDTH;

		g2.setStroke(new BasicStroke(STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		// Draw track (background circle)
		g2.setColor(trackColor);
		g2.draw(new Arc2D.Double(
			x + STROKE_WIDTH / 2.0,
			y + STROKE_WIDTH / 2.0,
			arcSize,
			arcSize,
			0,
			360,
			Arc2D.OPEN
		));

		// Draw spinning arc
		g2.setColor(spinnerColor);
		g2.draw(new Arc2D.Double(
			x + STROKE_WIDTH / 2.0,
			y + STROKE_WIDTH / 2.0,
			arcSize,
			arcSize,
			angle,
			ARC_LENGTH,
			Arc2D.OPEN
		));

		g2.dispose();
	}
}
