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

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 * Factory class for creating properly configured Swing UI elements.
 * Follows RuneLite/Quest Helper best practices:
 * - JLabels disable HTML rendering for security and performance
 * - JTextAreas are configured for read-only display with word wrapping
 * - JTextPanes are non-editable and transparent
 */
public final class JGenerator
{
	private JGenerator()
	{
	}

	/**
	 * Creates a JLabel with HTML rendering disabled.
	 * Disabling HTML prevents XSS-style attacks and improves rendering performance.
	 */
	public static JLabel makeJLabel()
	{
		JLabel label = new JLabel();
		label.putClientProperty("html.disable", Boolean.TRUE);
		return label;
	}

	/**
	 * Creates a JLabel with the specified text and HTML rendering disabled.
	 */
	public static JLabel makeJLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.putClientProperty("html.disable", Boolean.TRUE);
		return label;
	}

	/**
	 * Creates a non-editable, transparent JTextPane for rich text display.
	 */
	public static JTextPane makeJTextPane()
	{
		JTextPane pane = new JTextPane();
		pane.setEditable(false);
		pane.setFocusable(false);
		pane.setOpaque(false);
		return pane;
	}

	/**
	 * Creates a non-editable, transparent JTextPane with the specified text.
	 */
	public static JTextPane makeJTextPane(String text)
	{
		JTextPane pane = new JTextPane();
		pane.setEditable(false);
		pane.setFocusable(false);
		pane.setOpaque(false);
		pane.setText(text);
		return pane;
	}

	/**
	 * Creates a read-only JTextArea with word wrapping, suitable for multi-line text display.
	 * The text area is transparent and non-focusable.
	 */
	public static JTextArea makeJTextArea()
	{
		JTextArea area = new JTextArea();
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setOpaque(false);
		area.setEditable(false);
		area.setFocusable(false);
		area.setBackground(UIManager.getColor("Label.background"));
		area.setBorder(new EmptyBorder(0, 0, 0, 0));
		return area;
	}

	/**
	 * Creates a read-only JTextArea with word wrapping and the specified text.
	 */
	public static JTextArea makeJTextArea(String text)
	{
		JTextArea area = new JTextArea(text);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setOpaque(false);
		area.setEditable(false);
		area.setFocusable(false);
		area.setBackground(UIManager.getColor("Label.background"));
		area.setBorder(new EmptyBorder(0, 0, 0, 0));
		return area;
	}
}
