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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import net.runelite.client.ui.ColorScheme;

/**
 * A styled list item panel with hover effects and visual separation.
 * Provides consistent look and feel for list rows across tabs.
 */
public class ListItemPanel extends JPanel
{
	private static final Color HOVER_COLOR = ColorScheme.DARK_GRAY_HOVER_COLOR;
	private static final Color PRESS_COLOR = ColorScheme.MEDIUM_GRAY_COLOR;
	private static final int BORDER_SIZE = 1;
	private static final int BASE_PADDING_TOP = 6;
	private static final int BASE_PADDING_BOTTOM = 6;
	private static final int BASE_PADDING_LEFT = 8;
	private static final int BASE_PADDING_RIGHT = 8;

	private final Color baseBackground;
	private final Color separatorColor;
	private boolean hovering = false;
	private boolean pressed = false;
	private boolean highlighted = false;
	private int extraRightPadding = 0;

	/**
	 * Creates a list item panel with alternating background color.
	 * @param index Row index for alternating colors (even/odd)
	 */
	public ListItemPanel(int index)
	{
		this(index % 2 == 0 ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
	}

	/**
	 * Creates a list item panel with a specific background color.
	 */
	public ListItemPanel(Color background)
	{
		this.baseBackground = background;
		this.separatorColor = ColorScheme.DARK_GRAY_COLOR.brighter();

		setLayout(new BorderLayout(6, 0));
		setOpaque(true);
		setBackground(baseBackground);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		applyBorder();

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				hovering = true;
				updateBackground();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hovering = false;
				pressed = false;
				updateBackground();
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				pressed = true;
				updateBackground();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				pressed = false;
				updateBackground();
			}
		});
	}

	private void updateBackground()
	{
		if (pressed)
		{
			setBackground(PRESS_COLOR);
		}
		else if (hovering)
		{
			setBackground(HOVER_COLOR);
		}
		else
		{
			setBackground(baseBackground);
		}
	}

	private void applyBorder()
	{
		Border lineBorder;
		Border paddingBorder;
		int rightPadding = BASE_PADDING_RIGHT + extraRightPadding;

		if (highlighted)
		{
			lineBorder = BorderFactory.createMatteBorder(0, 3, BORDER_SIZE, 0, ColorScheme.BRAND_ORANGE);
			paddingBorder = BorderFactory.createEmptyBorder(BASE_PADDING_TOP, 5, BASE_PADDING_BOTTOM, rightPadding);
		}
		else
		{
			lineBorder = BorderFactory.createMatteBorder(0, 0, BORDER_SIZE, 0, separatorColor);
			paddingBorder = BorderFactory.createEmptyBorder(BASE_PADDING_TOP, BASE_PADDING_LEFT, BASE_PADDING_BOTTOM, rightPadding);
		}

		setBorder(BorderFactory.createCompoundBorder(lineBorder, paddingBorder));
	}

	/**
	 * Adds a component to the west (left) side of the panel.
	 */
	public void setLeftComponent(JComponent component)
	{
		add(component, BorderLayout.WEST);
	}

	/**
	 * Adds a component to the center of the panel.
	 */
	public void setCenterComponent(JComponent component)
	{
		add(component, BorderLayout.CENTER);
	}

	/**
	 * Adds a component to the east (right) side of the panel.
	 */
	public void setRightComponent(JComponent component)
	{
		add(component, BorderLayout.EAST);
	}

	/**
	 * Highlights this row to indicate active/selected state.
	 */
	public void setHighlighted(boolean highlighted)
	{
		this.highlighted = highlighted;
		applyBorder();
	}

	/**
	 * Adds extra right padding to avoid scrollbars overlapping content.
	 */
	public void setExtraRightPadding(int padding)
	{
		extraRightPadding = Math.max(0, padding);
		applyBorder();
	}
}
