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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.IconTextField;

public final class UiComponents
{
	private UiComponents()
	{
	}

	/**
	 * Creates a checkbox with bright, visible check/uncheck icons for dark backgrounds.
	 */
	public static JCheckBox createCheckBox()
	{
		JCheckBox cb = new JCheckBox();
		cb.setOpaque(false);
		cb.setFocusPainted(false);
		int size = 14;
		cb.setIcon(createCheckBoxIcon(size, false));
		cb.setSelectedIcon(createCheckBoxIcon(size, true));
		return cb;
	}

	/**
	 * Creates a labeled checkbox with bright, visible check/uncheck icons for dark backgrounds.
	 */
	public static JCheckBox createCheckBox(String label)
	{
		JCheckBox cb = new JCheckBox(label);
		cb.setOpaque(false);
		cb.setFocusPainted(false);
		int size = 14;
		cb.setIcon(createCheckBoxIcon(size, false));
		cb.setSelectedIcon(createCheckBoxIcon(size, true));
		return cb;
	}

	private static Icon createCheckBoxIcon(int size, boolean checked)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Box background
		g2.setColor(new Color(0x2B2D30));
		g2.fillRoundRect(0, 0, size, size, 3, 3);

		// Border
		g2.setColor(checked ? new Color(0x2ECC71) : new Color(0x808080));
		g2.drawRoundRect(0, 0, size - 1, size - 1, 3, 3);

		if (checked)
		{
			// Draw bright green checkmark
			g2.setColor(new Color(0x2ECC71));
			g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
			// Checkmark path
			int x1 = 3, y1 = size / 2;
			int x2 = size / 2 - 1, y2 = size - 4;
			int x3 = size - 3, y3 = 3;
			g2.drawLine(x1, y1, x2, y2);
			g2.drawLine(x2, y2, x3, y3);
		}

		g2.dispose();
		return new ImageIcon(img);
	}

	public static IconTextField createSearchField(String placeholder)
	{
		IconTextField field = new IconTextField();
		field.setIcon(IconTextField.Icon.SEARCH);
		field.setPreferredSize(new Dimension(0, 30));
		field.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		field.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		field.setForeground(Color.WHITE);
		field.setFont(FontManager.getRunescapeSmallFont());
		field.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		field.putClientProperty("JTextField.placeholderText", placeholder);
		return field;
	}

	public static Color applyHoverBackground(Color normal, Color hover, boolean hovered, boolean selected)
	{
		if (selected)
		{
			return ColorScheme.DARKER_GRAY_COLOR;
		}
		if (hovered)
		{
			return hover;
		}
		return normal;
	}

	/**
	 * Creates a small styled button with hover and press effects.
	 * Consistent with RuneLite's dark theme.
	 */
	public static JButton createSmallButton(String text)
	{
		JButton btn = new JButton(text);
		btn.setFont(FontManager.getRunescapeSmallFont());
		btn.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		btn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		btn.setBorder(new EmptyBorder(2, 6, 2, 6));
		btn.setFocusPainted(false);
		btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		btn.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				btn.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				btn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				btn.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (btn.contains(e.getPoint()))
				{
					btn.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
				}
				else
				{
					btn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				}
			}
		});
		return btn;
	}

	/**
	 * Creates a small styled button with a custom foreground color.
	 */
	public static JButton createSmallButton(String text, Color foreground)
	{
		JButton btn = createSmallButton(text);
		btn.setForeground(foreground);
		return btn;
	}

	/**
	 * Creates a vertical split pane with RuneLite-friendly styling.
	 * Top section uses its preferred height as the initial divider location.
	 */
	public static JSplitPane createVerticalSplit(JComponent top, JComponent bottom)
	{
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
		split.setResizeWeight(0.0);
		split.setContinuousLayout(true);
		split.setDividerSize(10);
		split.setBorder(BorderFactory.createEmptyBorder());
		split.setBackground(ColorScheme.DARK_GRAY_COLOR);
		split.setOneTouchExpandable(false);
		split.setOpaque(false);
		top.setMinimumSize(new Dimension(0, 0));
		bottom.setMinimumSize(new Dimension(0, 0));

		final JButton[] toggleRef = {null};
		split.setUI(new BasicSplitPaneUI()
		{
			@Override
			public BasicSplitPaneDivider createDefaultDivider()
			{
				BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this)
				{
					@Override
					public void doLayout()
					{
						super.doLayout();
						JButton toggle = toggleRef[0];
						if (toggle == null)
						{
							return;
						}
						int width = getWidth();
						int height = getHeight();
						int size = Math.min(10, Math.min(width, height));
						if (size <= 0)
						{
							return;
						}
						int x = (width - size) / 2;
						int y = (height - size) / 2;
						toggle.setBounds(x, y, size, size);
					}
				};
				divider.setLayout(null);
				divider.setBackground(ColorScheme.DARKER_GRAY_COLOR);

				JButton toggle = new JButton("▼");
				toggle.setFont(getToggleFont(toggle.getFont(), 9f));
				toggle.setMargin(new Insets(0, 0, 0, 0));
				toggle.setFocusPainted(false);
				toggle.setBorder(BorderFactory.createEmptyBorder());
				toggle.setContentAreaFilled(false);
				toggle.setOpaque(false);
				toggle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				toggle.addActionListener(e -> toggleSplitCollapse(split));

				toggleRef[0] = toggle;
				divider.add(toggle);
				return divider;
			}
		});

		split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt ->
		{
			JButton toggle = toggleRef[0];
			if (toggle != null)
			{
				updateSplitToggle(split, toggle);
			}
		});

		final boolean[] initialized = {false};
		split.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				if (initialized[0])
				{
					return;
				}
				int prefHeight = top.getPreferredSize().height;
				if (prefHeight <= 0)
				{
					prefHeight = Math.max(80, split.getHeight() / 5);
				}
				int min = split.getMinimumDividerLocation();
				int max = split.getMaximumDividerLocation();
				split.setDividerLocation(Math.max(min, Math.min(prefHeight, max)));
				initialized[0] = true;
				JButton toggle = toggleRef[0];
				if (toggle != null)
				{
					updateSplitToggle(split, toggle);
				}
			}
		});
		SwingUtilities.invokeLater(() ->
		{
			JButton toggle = toggleRef[0];
			if (toggle != null)
			{
				updateSplitToggle(split, toggle);
			}
			split.revalidate();
		});
		return split;
	}

	private static Font getToggleFont(Font base, float size)
	{
		if (base == null)
		{
			return new Font(Font.SANS_SERIF, Font.BOLD, Math.round(size));
		}
		return base.deriveFont(Font.BOLD, size);
	}

	private static void toggleSplitCollapse(JSplitPane split)
	{
		int min = split.getMinimumDividerLocation();
		int current = split.getDividerLocation();
		boolean collapsed = current <= min + 1;
		if (collapsed)
		{
			Integer last = (Integer) split.getClientProperty("dmmtracker.lastDividerLocation");
			int prefHeight = split.getTopComponent().getPreferredSize().height;
			int max = split.getMaximumDividerLocation();
			int restore = last != null ? last : Math.max(min, Math.min(prefHeight, max));
			split.setDividerLocation(restore);
		}
		else
		{
			split.putClientProperty("dmmtracker.lastDividerLocation", current);
			split.setDividerLocation(min);
		}
	}

	private static void updateSplitToggle(JSplitPane split, JButton toggle)
	{
		int min = split.getMinimumDividerLocation();
		int current = split.getDividerLocation();
		boolean collapsed = current <= min + 1;
		toggle.setText(collapsed ? "▲" : "▼");
		toggle.setToolTipText(collapsed ? "Expand" : "Collapse");
	}
}
