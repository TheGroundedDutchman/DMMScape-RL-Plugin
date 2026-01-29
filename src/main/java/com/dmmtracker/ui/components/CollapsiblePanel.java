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

import com.dmmtracker.ui.DmmColors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.SwingUtil;

public class CollapsiblePanel extends JPanel
{
	private static final int ACCENT_STRIPE_WIDTH = 3;
	private static final int ANIMATION_DURATION_MS = 150;
	private static final int ANIMATION_STEPS = 10;
	private static final Color HEADER_HOVER_COLOR = DmmColors.HOVER_BACKGROUND;
	private static final Color TITLE_EXPANDED_COLOR = Color.WHITE;
	private static final Color TITLE_COLLAPSED_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color SUMMARY_EXPANDED_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color SUMMARY_COLLAPSED_COLOR = DmmColors.TEXT_MUTED;

	private final JPanel header = new JPanel(new BorderLayout());
	private final JPanel content = new JPanel();
	private final JLabel iconLabel = new JLabel();
	private final JLabel titleLabel = new JLabel();
	private final JLabel summaryLabel = new JLabel();
	private final JButton toggleButton = new JButton();
	private final ArrowIcon collapsedIcon = new ArrowIcon(10, SwingConstants.EAST, ColorScheme.LIGHT_GRAY_COLOR);
	private final ArrowIcon expandedIcon = new ArrowIcon(10, SwingConstants.SOUTH, ColorScheme.LIGHT_GRAY_COLOR);
	private final SegmentedProgressBar headerProgressBar = new SegmentedProgressBar();
	private boolean expanded = false;
	private Color headerAccentColor = null;
	private Timer animationTimer;
	private int targetHeight;
	private int currentAnimationHeight;
	private boolean animating = false;

	// Custom panel that paints the accent stripe on the left edge
	private final JPanel headerWrapper = new JPanel(new BorderLayout())
	{
		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if (headerAccentColor != null)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(headerAccentColor);
				g2.fillRect(0, 0, ACCENT_STRIPE_WIDTH, getHeight());
				g2.dispose();
			}
		}
	};

	public CollapsiblePanel(String title)
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		headerWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerWrapper.setLayout(new BorderLayout());

		header.setOpaque(false);
		header.setBorder(new EmptyBorder(6, 8 + ACCENT_STRIPE_WIDTH, 6, 8));

		titleLabel.setText(title);
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(FontManager.getRunescapeSmallFont());

		summaryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		summaryLabel.setFont(FontManager.getRunescapeSmallFont());
		summaryLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		SwingUtil.removeButtonDecorations(toggleButton);
		toggleButton.setIcon(collapsedIcon);
		toggleButton.setOpaque(false);
		toggleButton.setPreferredSize(new Dimension(16, 16));
		toggleButton.setFocusable(false);
		toggleButton.setToolTipText("Click to expand");
		toggleButton.getAccessibleContext().setAccessibleName("Toggle " + title);
		toggleButton.getAccessibleContext().setAccessibleDescription("Expand or collapse the " + title + " section");
		toggleButton.addActionListener(e -> toggle());

		JPanel left = new JPanel();
		left.setOpaque(false);
		left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
		iconLabel.setVisible(false);
		left.add(iconLabel);
		left.add(titleLabel);
		left.add(Box.createHorizontalStrut(6));

		header.add(left, BorderLayout.WEST);
		header.add(summaryLabel, BorderLayout.CENTER);
		header.add(toggleButton, BorderLayout.EAST);
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		// Progress bar below header (shown when collapsed)
		headerProgressBar.setPreferredSize(new Dimension(0, 4));
		headerProgressBar.setVisible(false);

		headerWrapper.add(header, BorderLayout.CENTER);
		headerWrapper.add(headerProgressBar, BorderLayout.SOUTH);

		MouseAdapter toggleHandler = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				toggle();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				header.setBackground(HEADER_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				header.setBackground(null);
				header.setOpaque(false);
			}
		};
		header.addMouseListener(toggleHandler);
		titleLabel.addMouseListener(toggleHandler);
		summaryLabel.addMouseListener(toggleHandler);
		iconLabel.addMouseListener(toggleHandler);
		headerWrapper.addMouseListener(toggleHandler);
		headerProgressBar.addMouseListener(toggleHandler);
		headerWrapper.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		headerProgressBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setVisible(false); // Collapsed by default

		add(headerWrapper, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);
	}

	public JPanel getContentPanel()
	{
		return content;
	}

	public void setSummaryText(String text)
	{
		summaryLabel.setText(text);
	}

	/**
	 * Sets an icon displayed before the title text.
	 */
	public void setTitleIcon(javax.swing.Icon icon)
	{
		if (icon != null)
		{
			iconLabel.setIcon(icon);
			iconLabel.setBorder(new EmptyBorder(0, 0, 0, 4));
			iconLabel.setVisible(true);
		}
		else
		{
			iconLabel.setIcon(null);
			iconLabel.setVisible(false);
		}
	}

	/**
	 * Sets an accent color shown as a left border stripe on the header.
	 */
	public void setHeaderAccentColor(Color color)
	{
		this.headerAccentColor = color;
		headerWrapper.repaint();
	}

	/**
	 * Sets progress for a single-segment progress bar.
	 */
	public void setHeaderProgress(int completed, int total, Color color)
	{
		headerProgressBar.setSegments(new int[]{completed}, new int[]{total}, new Color[]{color});
		updateProgressBarVisibility();
	}

	/**
	 * Sets progress for a multi-segment progress bar (e.g., easy/med/hard/elite).
	 */
	public void setHeaderProgressSegments(int[] completed, int[] totals, Color[] colors)
	{
		headerProgressBar.setSegments(completed, totals, colors);
		updateProgressBarVisibility();
	}

	private void updateProgressBarVisibility()
	{
		boolean shouldShow = !expanded && headerProgressBar.hasData();
		if (headerProgressBar.isVisible() != shouldShow)
		{
			headerProgressBar.setVisible(shouldShow);
			headerWrapper.revalidate();
			headerWrapper.repaint();
		}
	}

	public void setExpanded(boolean expanded)
	{
		setExpanded(expanded, true);
	}

	public void setExpanded(boolean expanded, boolean animate)
	{
		if (this.expanded == expanded)
		{
			return;
		}

		// Stop any running animation
		if (animationTimer != null && animationTimer.isRunning())
		{
			animationTimer.stop();
		}

		this.expanded = expanded;
		toggleButton.setIcon(expanded ? expandedIcon : collapsedIcon);
		toggleButton.setToolTipText(expanded ? "Click to collapse" : "Click to expand");
		updateProgressBarVisibility();
		applyDimmerState(expanded);

		if (animate && isShowing())
		{
			animateExpansion(expanded);
		}
		else
		{
			content.setVisible(expanded);
			revalidate();
			repaint();
			if (getParent() != null)
			{
				getParent().revalidate();
				getParent().repaint();
			}
		}
	}

	private void animateExpansion(boolean expanding)
	{
		if (expanding)
		{
			content.setVisible(true);
			content.revalidate();
			targetHeight = content.getPreferredSize().height;
			currentAnimationHeight = 0;
		}
		else
		{
			targetHeight = 0;
			currentAnimationHeight = content.getHeight();
		}

		animating = true;
		int stepDelay = ANIMATION_DURATION_MS / ANIMATION_STEPS;
		int heightStep = Math.max(1, Math.abs(targetHeight - currentAnimationHeight) / ANIMATION_STEPS);

		animationTimer = new Timer(stepDelay, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (expanding)
				{
					currentAnimationHeight = Math.min(currentAnimationHeight + heightStep, targetHeight);
				}
				else
				{
					currentAnimationHeight = Math.max(currentAnimationHeight - heightStep, 0);
				}

				content.setPreferredSize(new Dimension(content.getWidth(), currentAnimationHeight));
				revalidate();
				repaint();

				if (getParent() != null)
				{
					getParent().revalidate();
					getParent().repaint();
				}

				boolean done = expanding ? currentAnimationHeight >= targetHeight : currentAnimationHeight <= 0;
				if (done)
				{
					animationTimer.stop();
					animating = false;
					content.setPreferredSize(null);
					if (!expanding)
					{
						content.setVisible(false);
					}
					revalidate();
					repaint();
					if (getParent() != null)
					{
						getParent().revalidate();
						getParent().repaint();
					}
				}
			}
		});
		animationTimer.start();
	}

	public boolean isExpanded()
	{
		return expanded;
	}

	/**
	 * Applies visual dimming to the header when collapsed.
	 * Expanded sections have brighter, more prominent text.
	 * Collapsed sections have dimmed text to indicate inactive state.
	 */
	private void applyDimmerState(boolean isExpanded)
	{
		if (isExpanded)
		{
			titleLabel.setForeground(TITLE_EXPANDED_COLOR);
			summaryLabel.setForeground(SUMMARY_EXPANDED_COLOR);
			headerWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		}
		else
		{
			titleLabel.setForeground(TITLE_COLLAPSED_COLOR);
			summaryLabel.setForeground(SUMMARY_COLLAPSED_COLOR);
			headerWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		}
	}

	private void toggle()
	{
		setExpanded(!expanded);
	}

	@Override
	public Dimension getPreferredSize()
	{
		if (!expanded)
		{
			// When collapsed, only use header height
			Dimension headerSize = headerWrapper.getPreferredSize();
			return new Dimension(getWidth() > 0 ? getWidth() : headerSize.width, headerSize.height);
		}
		return super.getPreferredSize();
	}

	@Override
	public Dimension getMinimumSize()
	{
		if (!expanded)
		{
			Dimension headerSize = headerWrapper.getMinimumSize();
			return new Dimension(0, headerSize.height);
		}
		return super.getMinimumSize();
	}

	@Override
	public Dimension getMaximumSize()
	{
		Dimension pref = getPreferredSize();
		return new Dimension(Integer.MAX_VALUE, pref.height);
	}

	/**
	 * A compact progress bar that can display multiple segments.
	 */
	private static class SegmentedProgressBar extends JPanel
	{
		private int[] completed = new int[0];
		private int[] totals = new int[0];
		private Color[] colors = new Color[0];

		SegmentedProgressBar()
		{
			setOpaque(true);
			setBackground(ColorScheme.DARKER_GRAY_COLOR);
		}

		void setSegments(int[] completed, int[] totals, Color[] colors)
		{
			this.completed = completed;
			this.totals = totals;
			this.colors = colors;
			repaint();
		}

		boolean hasData()
		{
			return totals.length > 0;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if (totals.length == 0) return;

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int width = getWidth();
			int height = getHeight();

			// Calculate total tasks across all segments
			int totalTasks = 0;
			for (int t : totals)
			{
				totalTasks += t;
			}
			if (totalTasks == 0) totalTasks = 1;

			// Draw each segment
			int x = 0;
			for (int i = 0; i < totals.length; i++)
			{
				int segmentWidth = (int) ((double) totals[i] / totalTasks * width);
				if (i == totals.length - 1)
				{
					segmentWidth = width - x; // Ensure we fill to the end
				}

				// Background for segment
				g2.setColor(darken(colors[i], 0.3f));
				g2.fillRect(x, 0, segmentWidth, height);

				// Filled portion
				if (totals[i] > 0)
				{
					int filledWidth = (int) ((double) completed[i] / totals[i] * segmentWidth);
					g2.setColor(colors[i]);
					g2.fillRect(x, 0, filledWidth, height);
				}

				x += segmentWidth;
			}

			g2.dispose();
		}

		private Color darken(Color c, float factor)
		{
			return new Color(
				Math.max(0, (int) (c.getRed() * factor)),
				Math.max(0, (int) (c.getGreen() * factor)),
				Math.max(0, (int) (c.getBlue() * factor))
			);
		}
	}
}
