/*
 * Copyright (c) 2026, DMMScape
 * Based on Quest Helper's DynamicPaddedGridLayout
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.function.Function;

/**
 * Grid layout implementation with support for cells with unequal size.
 * Padding is only applied to visible cells, similar to HTML table padding.
 */
public class DynamicPaddedGridLayout extends GridLayout
{
	public DynamicPaddedGridLayout(int rows, int cols, int horizontalPadding, int verticalPadding)
	{
		super(rows, cols, horizontalPadding, verticalPadding);
	}

	@Override
	public Dimension preferredLayoutSize(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			return calculateSize(parent, Component::getPreferredSize);
		}
	}

	@Override
	public Dimension minimumLayoutSize(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			return calculateSize(parent, Component::getMinimumSize);
		}
	}

	@Override
	public void layoutContainer(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			final Insets insets = parent.getInsets();
			final int numComponents = parent.getComponentCount();
			int numRows = getRows();
			int numColumns = getColumns();

			if (numComponents == 0)
			{
				return;
			}

			if (numRows > 0)
			{
				numColumns = (numComponents + numRows - 1) / numRows;
			}
			else
			{
				numRows = (numComponents + numColumns - 1) / numColumns;
			}

			final int horizontalPadding = getHgap();
			final int verticalPadding = getVgap();

			// Scaling factors
			final Dimension pd = preferredLayoutSize(parent);
			final Insets parentInsets = parent.getInsets();
			int horizontalBorder = parentInsets.left + parentInsets.right;
			int verticalBorder = parentInsets.top + parentInsets.bottom;
			final double sw = (1.0 * parent.getWidth() - horizontalBorder) / (pd.width - horizontalBorder);
			final double sh = (1.0 * parent.getHeight() - verticalBorder) / (pd.height - verticalBorder);

			final int[] w = new int[numColumns];
			final int[] h = new int[numRows];

			// Calculate dimensions for all components + apply scaling
			for (int i = 0; i < numComponents; i++)
			{
				final int r = i / numColumns;
				final int c = i % numColumns;
				final Component comp = parent.getComponent(i);
				final Dimension d = comp.getPreferredSize();
				d.width = (int) (sw * d.width);
				d.height = (int) (sh * d.height);

				if (w[c] < d.width)
				{
					w[c] = d.width;
				}

				if (h[r] < d.height)
				{
					h[r] = d.height;
				}
			}

			// Apply new bounds to all child components
			for (int c = 0, x = insets.left; c < numColumns; c++)
			{
				int componentWidth = w[c];

				for (int r = 0, y = insets.top; r < numRows; r++)
				{
					int i = r * numColumns + c;

					int componentHeight = h[r];

					if (i < numComponents)
					{
						parent.getComponent(i).setBounds(x, y, componentWidth, componentHeight);
					}

					if (componentHeight > 0)
					{
						y += componentHeight + verticalPadding;
					}
				}

				if (componentWidth > 0)
				{
					x += componentWidth + horizontalPadding;
				}
			}
		}
	}

	/**
	 * Calculate outer size of the layout based on its children and sizer
	 */
	private Dimension calculateSize(final Container parent, final Function<Component, Dimension> sizer)
	{
		final int numComponents = parent.getComponentCount();
		int numRows = getRows();
		int numColumns = getColumns();
		int numVisibleRows = 0;
		int numVisibleColumns = 0;

		if (numRows > 0)
		{
			numColumns = (numComponents + numRows - 1) / numRows;
		}
		else
		{
			numRows = (numComponents + numColumns - 1) / numColumns;
		}

		final int[] w = new int[numColumns];
		final int[] h = new int[numRows];

		// Calculate dimensions for all components
		for (int i = 0; i < numComponents; i++)
		{
			final int r = i / numColumns;
			final int c = i % numColumns;
			final Component comp = parent.getComponent(i);
			final Dimension d = sizer.apply(comp);

			if (w[c] < d.width)
			{
				w[c] = d.width;
			}

			if (h[r] < d.height)
			{
				h[r] = d.height;
			}
		}

		// Calculate total width and height of the layout
		int totalWidth = 0;
		int totalHeight = 0;

		for (int j = 0; j < numColumns; j++)
		{
			int columnWidth = w[j];
			if (columnWidth > 0)
			{
				totalWidth += columnWidth;
				numVisibleColumns += 1;
			}
		}

		for (int i = 0; i < numRows; i++)
		{
			int rowHeight = h[i];
			if (rowHeight > 0)
			{
				totalHeight += rowHeight;
				numVisibleRows += 1;
			}
		}

		final Insets insets = parent.getInsets();

		// Apply insets and padding, accounting only for visible columns & rows
		int paddingWidth = (numVisibleColumns > 1) ? (numVisibleColumns - 1) * getHgap() : 0;
		int paddingHeight = (numVisibleRows > 1) ? (numVisibleRows - 1) * getVgap() : 0;

		return new Dimension(
			insets.left + insets.right + totalWidth + paddingWidth,
			insets.top + insets.bottom + totalHeight + paddingHeight
		);
	}
}
