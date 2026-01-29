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

package com.dmmtracker.ui;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import net.runelite.client.ui.ColorScheme;

/**
 * Shared scroll pane styling for RuneLite panels.
 * Note: RuneLite's FlatLaf automatically provides nice scrollbar styling.
 * This utility just sets the unit increment and disables horizontal scrolling.
 */
public final class ScrollPaneUtils
{
	private ScrollPaneUtils()
	{
	}

	/**
	 * Apply minimal scroll pane configuration following Quest Helper's approach.
	 * FlatLaf handles the actual scrollbar styling - we don't override borders.
	 */
	public static void applyRuneliteStyle(JScrollPane scrollPane)
	{
		if (scrollPane == null)
		{
			return;
		}

		// Set reasonable scroll speed (default is 1px which is unusably slow)
		if (scrollPane.getVerticalScrollBar() != null)
		{
			scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		}

		// Disable horizontal scrolling - panels should fit within 225px width
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		// Ensure viewport background matches the RuneLite dark theme
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Let FlatLaf handle border styling - don't override it
	}
}
