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
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

/**
 * A styled section panel with a header title and optional controls.
 * Provides consistent look for grouped content across tabs.
 */
public class SectionPanel extends JPanel
{
	private static final int BORDER_RADIUS = 4;

	private final JPanel headerPanel;
	private final JLabel titleLabel;
	private final JPanel controlsPanel;
	private final JPanel contentPanel;

	/**
	 * Creates a section panel with the specified title.
	 */
	public SectionPanel(String title)
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(0, 0, 0, 0)
		));

		// Header
		headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		headerPanel.setBorder(new EmptyBorder(4, 6, 4, 6));

		titleLabel = new JLabel(title);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
		titleLabel.setForeground(Color.WHITE);

		controlsPanel = new JPanel();
		controlsPanel.setOpaque(false);
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));

		headerPanel.add(titleLabel, BorderLayout.WEST);
		headerPanel.add(controlsPanel, BorderLayout.EAST);

		// Content
		contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		contentPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

		add(headerPanel, BorderLayout.NORTH);
		add(contentPanel, BorderLayout.CENTER);
	}

	/**
	 * Sets the section title.
	 */
	public void setTitle(String title)
	{
		titleLabel.setText(title);
	}

	/**
	 * Sets the title color (useful for color-coded sections).
	 */
	public void setTitleColor(Color color)
	{
		titleLabel.setForeground(color);
	}

	/**
	 * Adds a control component to the header (right side).
	 */
	public void addHeaderControl(JComponent control)
	{
		controlsPanel.add(control);
	}

	/**
	 * Gets the content panel for adding child components.
	 */
	public JPanel getContentPanel()
	{
		return contentPanel;
	}

	/**
	 * Adds a component to the content area.
	 */
	public void addContent(JComponent component)
	{
		contentPanel.add(component);
	}

	/**
	 * Sets whether the header is visible.
	 */
	public void setHeaderVisible(boolean visible)
	{
		headerPanel.setVisible(visible);
	}

	/**
	 * Creates a simple info row with a label and value.
	 */
	public static JPanel createInfoRow(String label, String value)
	{
		return createInfoRow(label, value, ColorScheme.LIGHT_GRAY_COLOR);
	}

	/**
	 * Creates a simple info row with a label, value, and custom value color.
	 */
	public static JPanel createInfoRow(String label, String value, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setOpaque(false);

		JLabel labelComponent = new JLabel(label);
		labelComponent.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		labelComponent.setFont(labelComponent.getFont().deriveFont(10f));

		JLabel valueComponent = new JLabel(value);
		valueComponent.setForeground(valueColor);
		valueComponent.setFont(valueComponent.getFont().deriveFont(Font.BOLD, 10f));

		row.add(labelComponent, BorderLayout.WEST);
		row.add(valueComponent, BorderLayout.EAST);

		return row;
	}
}
