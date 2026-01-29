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

package com.dmmtracker.ui.tabs;

import com.dmmtracker.ProgressStore;
import com.dmmtracker.data.DataLoader;
import com.dmmtracker.data.SigilData;
import com.dmmtracker.ui.DmmColors;
import com.dmmtracker.ui.IconManager;
import com.dmmtracker.ui.ScrollPaneUtils;
import com.dmmtracker.ui.components.CollapsiblePanel;
import com.dmmtracker.ui.components.EmptyStatePanel;
import com.dmmtracker.ui.components.ListItemPanel;
import com.dmmtracker.ui.components.UiComponents;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.EmptyBorder;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.components.IconTextField;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sigils tab with collapsible categories (Combat/Skilling/Utility).
 */
public class SigilsTab extends JPanel
{
	private static final String WIKI_BASE = "https://oldschool.runescape.wiki/w/";
	private static final List<String> CATEGORY_ORDER = List.of("combat", "skilling", "utility", "prayer");
	private static final Map<String, Color> CATEGORY_COLORS = Map.of(
		"combat", DmmColors.TYPE_COMBAT,
		"skilling", DmmColors.TYPE_SKILLING,
		"utility", DmmColors.TYPE_UTILITY,
		"prayer", DmmColors.TYPE_PRAYER
	);

	private final DataLoader dataLoader;
	private final ProgressStore progressStore;
	private final JPanel contentPanel = new JPanel();
	private final JLabel summaryLabel = new JLabel();
	private final Map<String, CollapsiblePanel> categoryPanels = new LinkedHashMap<>();
	private final EmptyStatePanel emptyStatePanel = new EmptyStatePanel();
	private final IconTextField searchField;

	public SigilsTab(DataLoader dataLoader, ProgressStore progressStore)
	{
		this.dataLoader = dataLoader;
		this.progressStore = progressStore;

		searchField = UiComponents.createSearchField("Search sigils...");
		searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
		{
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e)
			{
				rebuildContent();
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e)
			{
				rebuildContent();
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e)
			{
				rebuildContent();
			}
		});

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = buildHeader();
		JPanel body = buildBody();
		JSplitPane splitPane = UiComponents.createVerticalSplit(header, body);
		add(splitPane, BorderLayout.CENTER);

		dataLoader.addUpdateListener(() -> SwingUtilities.invokeLater(this::rebuildContent));
		rebuildContent();
	}

	public void refresh()
	{
		rebuildContent();
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Title row
		JPanel titleRow = new JPanel(new BorderLayout(8, 0));
		titleRow.setOpaque(false);
		titleRow.setBorder(new EmptyBorder(6, 8, 4, 8));

		JLabel title = new JLabel("Sigils");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
		title.setForeground(Color.WHITE);

		summaryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		summaryLabel.setFont(FontManager.getRunescapeSmallFont());

		titleRow.add(title, BorderLayout.WEST);
		titleRow.add(summaryLabel, BorderLayout.EAST);

		// Search row
		JPanel searchRow = new JPanel(new BorderLayout());
		searchRow.setOpaque(false);
		searchRow.setBorder(new EmptyBorder(0, 8, 6, 8));
		searchRow.add(searchField, BorderLayout.CENTER);

		header.add(titleRow);
		header.add(searchRow);
		return header;
	}

	private JPanel buildBody()
	{
		JPanel body = new JPanel(new BorderLayout());
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);

		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(contentPanel);
		ScrollPaneUtils.applyRuneliteStyle(scrollPane);

		body.add(scrollPane, BorderLayout.CENTER);
		return body;
	}

	private void rebuildContent()
	{
		Map<String, Boolean> expandedState = new HashMap<>();
		for (Map.Entry<String, CollapsiblePanel> entry : categoryPanels.entrySet())
		{
			expandedState.put(entry.getKey(), entry.getValue().isExpanded());
		}

		contentPanel.removeAll();
		categoryPanels.clear();

		String searchText = searchField.getText().toLowerCase().trim();
		boolean hasFilter = !searchText.isEmpty();

		// Group sigils by category
		Map<String, List<SigilData>> grouped = new LinkedHashMap<>();
		for (String cat : CATEGORY_ORDER)
		{
			grouped.put(cat, new ArrayList<>());
		}

		int totalSigils = 0;
		for (SigilData sigil : dataLoader.getSigils())
		{
			totalSigils++;
			String cat = sigil.getCategory();
			if (cat == null || !grouped.containsKey(cat.toLowerCase()))
			{
				cat = "utility"; // Default to utility if unknown
			}
			else
			{
				cat = cat.toLowerCase();
			}
			grouped.get(cat).add(sigil);
		}

		int showingSigils = 0;

		for (String categoryId : CATEGORY_ORDER)
		{
			List<SigilData> sigils = grouped.get(categoryId);
			if (sigils.isEmpty())
			{
				continue;
			}

			// Filter sigils by search
			List<SigilData> filtered = hasFilter
				? sigils.stream()
					.filter(s -> matchesSearch(s, searchText))
					.collect(Collectors.toList())
				: sigils;

			if (filtered.isEmpty())
			{
				continue;
			}

			showingSigils += filtered.size();

			Color categoryColor = CATEGORY_COLORS.getOrDefault(categoryId, ColorScheme.LIGHT_GRAY_COLOR);

			// Create collapsible panel for this category
			String categoryName = capitalize(categoryId);
			CollapsiblePanel categoryPanel = new CollapsiblePanel(categoryName);
			categoryPanel.setSummaryText(filtered.size() + " sigils");
			categoryPanel.setHeaderAccentColor(categoryColor);
			categoryPanel.setExpanded(expandedState.getOrDefault(categoryId, hasFilter));
			categoryPanels.put(categoryId, categoryPanel);

			JPanel categoryContent = categoryPanel.getContentPanel();

			int index = 0;
			for (SigilData sigil : filtered)
			{
				JPanel sigilRow = createSigilRow(sigil, categoryId, index);
				categoryContent.add(sigilRow);
				index++;
			}

			contentPanel.add(categoryPanel);
		}

		// Show empty state if no content
		if (contentPanel.getComponentCount() == 0)
		{
			if (totalSigils == 0)
			{
				emptyStatePanel.setEmptyState("No Data Available", "Sigil and prayer data is still loading...");
			}
			else if (hasFilter)
			{
				emptyStatePanel.setEmptyState("No Results", "No sigils match \"" + searchText + "\"");
			}
			else
			{
				emptyStatePanel.setEmptyState("No Sigils", "No sigil data available.");
			}
			contentPanel.add(emptyStatePanel);
		}

		summaryLabel.setText(showingSigils + "/" + totalSigils + " sigils");

		contentPanel.revalidate();
		contentPanel.repaint();
	}

	private boolean matchesSearch(SigilData sigil, String searchText)
	{
		String name = sigil.getName();
		if (name != null && name.toLowerCase().contains(searchText))
		{
			return true;
		}
		String desc = sigil.getShortDesc();
		if (desc != null && desc.toLowerCase().contains(searchText))
		{
			return true;
		}
		return false;
	}

	private JPanel createSigilRow(SigilData sigil, String category, int index)
	{
		ListItemPanel row = new ListItemPanel(index);

		// Left: skill icon (use effectIcon for skill icons like Combat, Defence, etc.)
		JLabel iconLabel = new JLabel();
		iconLabel.setPreferredSize(new Dimension(20, 20));
		String iconUrl = sigil.getEffectIcon();
		if (iconUrl != null && !iconUrl.isEmpty())
		{
			ImageIcon icon = IconManager.getRemoteIcon(iconUrl, 18, 18, () ->
			{
				ImageIcon loadedIcon = IconManager.getRemoteIcon(iconUrl, 18, 18, null);
				iconLabel.setIcon(loadedIcon);
				iconLabel.repaint();
			});
			iconLabel.setIcon(icon);
		}

		// Center: Name and description (with wrapping)
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		textPanel.setOpaque(false);

		JLabel nameLabel = new JLabel(sigil.getName());
		nameLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setAlignmentX(LEFT_ALIGNMENT);

		// Description with HTML wrapping
		String desc = sigil.getShortDesc();
		if (desc != null && !desc.isEmpty())
		{
			JLabel descLabel = new JLabel("<html><body style='width: 140px'>" + desc + "</body></html>");
			descLabel.setFont(FontManager.getRunescapeSmallFont());
			descLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			descLabel.setAlignmentX(LEFT_ALIGNMENT);
			textPanel.add(nameLabel);
			textPanel.add(Box.createVerticalStrut(2));
			textPanel.add(descLabel);
		}
		else
		{
			textPanel.add(nameLabel);
		}

		// Right: cost
		JLabel costLabel = new JLabel(sigil.getCost() + " pts");
		costLabel.setFont(FontManager.getRunescapeSmallFont());
		costLabel.setForeground(DmmColors.GOLD); // Gold color
		costLabel.setPreferredSize(new Dimension(50, 16));

		row.setLeftComponent(iconLabel);
		row.setCenterComponent(textPanel);
		row.setRightComponent(costLabel);

		// Click handlers for double-click wiki and right-click menu
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
				{
					openWiki(sigil);
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				showMenu(e, sigil);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				showMenu(e, sigil);
			}
		});

		return row;
	}

	private void showMenu(MouseEvent e, SigilData sigil)
	{
		if (!e.isPopupTrigger()) return;

		JPopupMenu menu = new JPopupMenu();

		JMenuItem openWiki = new JMenuItem("Open Wiki");
		openWiki.addActionListener(ev -> openWiki(sigil));
		menu.add(openWiki);

		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	private void openWiki(SigilData sigil)
	{
		String name = sigil.getName();
		if (name == null || name.isEmpty())
		{
			LinkBrowser.browse(WIKI_BASE + "Deadman:_Apocalypse");
			return;
		}
		String encoded = URLEncoder.encode(name.replace(" ", "_"), StandardCharsets.UTF_8);
		LinkBrowser.browse(WIKI_BASE + encoded);
	}

	private String capitalize(String s)
	{
		if (s == null || s.isEmpty()) return "";
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	/**
	 * Expands the category containing the specified sigil or category name.
	 * @param nameOrCategory The sigil name or category id to expand
	 */
	public void expandCategory(String nameOrCategory)
	{
		if (nameOrCategory == null)
		{
			return;
		}

		String lower = nameOrCategory.toLowerCase();

		// Try to find by category id first
		if (categoryPanels.containsKey(lower))
		{
			categoryPanels.get(lower).setExpanded(true);
			return;
		}

		// Try to find by sigil name - search through sigils to find category
		for (SigilData sigil : dataLoader.getSigils())
		{
			if (sigil.getName().equalsIgnoreCase(nameOrCategory))
			{
				String cat = sigil.getCategory();
				if (cat != null)
				{
					cat = cat.toLowerCase();
					if (categoryPanels.containsKey(cat))
					{
						categoryPanels.get(cat).setExpanded(true);
					}
				}
				return;
			}
		}
	}
}
