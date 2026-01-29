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
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.Text;

/**
 * Custom dropdown renderer that matches RuneLite's dark theme.
 * Provides consistent styling for combobox items with proper colors for
 * selected and unselected states.
 */
public class DarkDropdownRenderer extends DefaultListCellRenderer
{
	@Override
	public Component getListCellRendererComponent(
		JList<?> list,
		Object value,
		int index,
		boolean isSelected,
		boolean cellHasFocus)
	{
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		setBorder(new EmptyBorder(4, 6, 4, 6));

		if (isSelected)
		{
			setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			setForeground(Color.WHITE);
		}
		else
		{
			setBackground(ColorScheme.DARK_GRAY_COLOR);
			setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		}

		// Format enum values with title case
		if (value instanceof Enum)
		{
			setText(Text.titleCase((Enum<?>) value));
		}
		else if (value != null)
		{
			setText(value.toString());
		}

		return this;
	}
}
