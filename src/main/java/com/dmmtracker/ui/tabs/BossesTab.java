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

import com.dmmtracker.BossNameMapper;
import com.dmmtracker.ProgressStore;
import com.dmmtracker.RequirementChecker;
import com.dmmtracker.data.BossData;
import com.dmmtracker.data.BossRequirementData;
import com.dmmtracker.data.DataLoader;
import com.dmmtracker.ui.DmmColors;
import com.dmmtracker.ui.ScrollPaneUtils;
import com.dmmtracker.ui.components.CollapsiblePanel;
import com.dmmtracker.ui.components.EmptyStatePanel;
import com.dmmtracker.ui.components.ListItemPanel;
import com.dmmtracker.ui.components.UiComponents;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.EmptyBorder;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bosses tab with collapsible categories.
 */
public class BossesTab extends JPanel
{
	private static final List<String> CATEGORY_ORDER = List.of("slayer", "wilderness", "godwars", "raids", "dt2", "other");
	private static final Map<String, Color> CATEGORY_COLORS = Map.of(
		"slayer", DmmColors.CATEGORY_SLAYER,
		"wilderness", DmmColors.CATEGORY_WILDERNESS,
		"godwars", DmmColors.CATEGORY_GODWARS,
		"raids", DmmColors.CATEGORY_RAIDS,
		"dt2", DmmColors.CATEGORY_DT2,
		"other", DmmColors.CATEGORY_OTHER
	);
	private static final String CARD_CONTENT = "content";
	private static final String CARD_EMPTY = "empty";

	private final DataLoader dataLoader;
	private final ProgressStore progressStore;
	private final RequirementChecker requirementChecker;
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);
	private final JPanel contentPanel = new JPanel();
	private final EmptyStatePanel emptyStatePanel = new EmptyStatePanel();
	private final JLabel summaryLabel = new JLabel();
	private final JCheckBox reqMetFilter = UiComponents.createCheckBox("Req Met");
	private final JCheckBox incompleteFilter = UiComponents.createCheckBox("Hide done");
	private final Map<String, CollapsiblePanel> categoryPanels = new LinkedHashMap<>();

	private Consumer<BossData> addToPlanHandler;

	public BossesTab(DataLoader dataLoader, ProgressStore progressStore, RequirementChecker requirementChecker)
	{
		this.dataLoader = dataLoader;
		this.progressStore = progressStore;
		this.requirementChecker = requirementChecker;

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
		SwingUtilities.invokeLater(() ->
		{
			updateFilterState();
			rebuildContent();
		});
	}

	/**
	 * Updates the filter checkbox state based on game state availability.
	 */
	private void updateFilterState()
	{
		boolean gameStateAvailable = requirementChecker != null && requirementChecker.isGameStateAvailable();
		if (reqMetFilter.isSelected() && !gameStateAvailable)
		{
			reqMetFilter.setToolTipText("Using DMM base stats (level 1 + autocompleted quests) - log in for actual levels");
			reqMetFilter.setForeground(DmmColors.WARNING);
		}
		else
		{
			reqMetFilter.setToolTipText("Only show bosses you can access (slayer & quest requirements)");
			reqMetFilter.setForeground(Color.WHITE);
		}
	}

	private JPanel buildHeader()
	{
		JPanel headerWrapper = new JPanel();
		headerWrapper.setLayout(new BoxLayout(headerWrapper, BoxLayout.Y_AXIS));
		headerWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Top row: title and summary
		JPanel topRow = new JPanel(new BorderLayout(8, 0));
		topRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		topRow.setBorder(new EmptyBorder(6, 8, 2, 8));

		JLabel title = new JLabel("Boss Kills");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
		title.setForeground(Color.WHITE);

		summaryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		summaryLabel.setFont(FontManager.getRunescapeSmallFont());

		topRow.add(title, BorderLayout.WEST);
		topRow.add(summaryLabel, BorderLayout.EAST);

		// Bottom row: filters
		JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		filterRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		filterRow.setBorder(new EmptyBorder(2, 8, 6, 8));

		reqMetFilter.setFont(FontManager.getRunescapeSmallFont());
		reqMetFilter.setForeground(Color.WHITE);
		reqMetFilter.setOpaque(false);
		reqMetFilter.setFocusPainted(false);
		reqMetFilter.setToolTipText("Only show bosses you can access (requires being logged in)");
		reqMetFilter.getAccessibleContext().setAccessibleName("Requirements Met Filter");
		reqMetFilter.getAccessibleContext().setAccessibleDescription("Filter to show only bosses where slayer and quest requirements are met");
		reqMetFilter.addActionListener(e ->
		{
			updateFilterState();
			SwingUtilities.invokeLater(this::rebuildContent);
		});

		incompleteFilter.setFont(FontManager.getRunescapeSmallFont());
		incompleteFilter.setForeground(Color.WHITE);
		incompleteFilter.setOpaque(false);
		incompleteFilter.setFocusPainted(false);
		incompleteFilter.setToolTipText("Show only bosses with 0 KC");
		incompleteFilter.getAccessibleContext().setAccessibleName("Incomplete Filter");
		incompleteFilter.getAccessibleContext().setAccessibleDescription("Filter to show only bosses with zero kill count");
		incompleteFilter.addActionListener(e -> SwingUtilities.invokeLater(this::rebuildContent));

		filterRow.add(reqMetFilter);
		filterRow.add(incompleteFilter);

		headerWrapper.add(topRow);
		headerWrapper.add(filterRow);
		return headerWrapper;
	}

	private JPanel buildBody()
	{
		JPanel body = new JPanel(new BorderLayout());
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);

		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(contentPanel);
		ScrollPaneUtils.applyRuneliteStyle(scrollPane);

		emptyStatePanel.setEmptyState("No Boss Data", "Boss data is loading or unavailable.");

		cardPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		cardPanel.add(scrollPane, CARD_CONTENT);
		cardPanel.add(emptyStatePanel, CARD_EMPTY);

		body.add(cardPanel, BorderLayout.CENTER);
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

		boolean filterReqMet = reqMetFilter.isSelected();
		boolean filterIncomplete = incompleteFilter.isSelected();

		// Group bosses by category
		Map<String, List<BossData>> grouped = new LinkedHashMap<>();
		for (String cat : CATEGORY_ORDER)
		{
			grouped.put(cat, new ArrayList<>());
		}

		for (BossData boss : dataLoader.getBosses())
		{
			String cat = boss.getCategory();
			if (cat == null || !grouped.containsKey(cat))
			{
				cat = "other";
			}
			grouped.get(cat).add(boss);
		}

		int totalBosses = 0;
		int showingBosses = 0;

		for (String categoryId : CATEGORY_ORDER)
		{
			List<BossData> bosses = grouped.get(categoryId);
			if (bosses.isEmpty())
			{
				continue;
			}

			totalBosses += bosses.size();

			// Filter bosses
			List<BossData> filtered = new ArrayList<>();
			for (BossData boss : bosses)
			{
				String bossKey = boss.getId();
				if (bossKey == null || bossKey.isEmpty())
				{
					bossKey = BossNameMapper.getBossId(boss.getName());
				}
				int kc = progressStore.getBossKillCount(bossKey != null && !bossKey.isEmpty() ? bossKey : boss.getName());
				boolean hasKills = kc > 0;

				// Skip bosses with kills if incomplete filter is on
				if (filterIncomplete && hasKills)
				{
					continue;
				}

				// Check requirements if filter is enabled
				// RequirementChecker falls back to DMM base stats when not logged in
				if (filterReqMet && requirementChecker != null)
				{
					BossRequirementData req = dataLoader.getBossRequirement(boss.getName());
					if (req != null)
					{
						// Check if player meets slayer level and quest requirements
						if (!requirementChecker.meetsBossRequirements(req.getSlayerLevel(), req.getQuest()))
						{
							continue;
						}
					}
				}
				filtered.add(boss);
			}

			showingBosses += filtered.size();

			if (filtered.isEmpty())
			{
				continue;
			}

			Color categoryColor = CATEGORY_COLORS.getOrDefault(categoryId, ColorScheme.LIGHT_GRAY_COLOR);

			// Create collapsible panel for this category
			String categoryName = dataLoader.getCategoryDisplayName(categoryId);
			CollapsiblePanel categoryPanel = new CollapsiblePanel(categoryName);
			categoryPanel.setSummaryText(filtered.size() + " bosses");
			categoryPanel.setHeaderAccentColor(categoryColor);
			categoryPanel.setExpanded(expandedState.getOrDefault(categoryId, false));
			categoryPanels.put(categoryId, categoryPanel);

			JPanel categoryContent = categoryPanel.getContentPanel();

			int index = 0;
			for (BossData boss : filtered)
			{
				JPanel bossRow = createBossRow(boss, categoryId, index);
				categoryContent.add(bossRow);
				index++;
			}

			contentPanel.add(categoryPanel);
		}

		summaryLabel.setText(showingBosses + "/" + totalBosses + " bosses");

		// Show empty state if no bosses
		if (totalBosses == 0)
		{
			cardLayout.show(cardPanel, CARD_EMPTY);
		}
		else if (showingBosses == 0 && (filterReqMet || filterIncomplete))
		{
			emptyStatePanel.setNoResultsState("No bosses match your current filters. Try adjusting filter options.");
			cardLayout.show(cardPanel, CARD_EMPTY);
		}
		else
		{
			cardLayout.show(cardPanel, CARD_CONTENT);
		}

		contentPanel.revalidate();
		contentPanel.repaint();
	}

	private JPanel createBossRow(BossData boss, String category, int index)
	{
		ListItemPanel row = new ListItemPanel(index);

		// Left: KC label (fixed width)
		String bossKey = boss.getId();
		if (bossKey == null || bossKey.isEmpty())
		{
			bossKey = BossNameMapper.getBossId(boss.getName());
		}
		int kc = progressStore.getBossKillCount(bossKey != null && !bossKey.isEmpty() ? bossKey : boss.getName());
		String kcText = kc >= 0 ? kc + " KC" : "-- KC";
		JLabel kcLabel = new JLabel(kcText);
		kcLabel.setFont(FontManager.getRunescapeSmallFont());
		kcLabel.setForeground(kc >= 0 ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.LIGHT_GRAY_COLOR.darker());
		kcLabel.setPreferredSize(new Dimension(42, 16));
		kcLabel.setToolTipText(kc >= 0 ? "Kill count: " + kc : "Kill count unknown");

		// Center: vertical panel with points on top, name below
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.setOpaque(false);

		// Points label (top)
		JLabel pointsLabel = new JLabel("+" + boss.getPointsPerKill() + " pts");
		pointsLabel.setFont(FontManager.getRunescapeSmallFont());
		int pts = boss.getPointsPerKill();
		if (pts >= 10)
		{
			pointsLabel.setForeground(DmmColors.POINTS_HIGH); // Gold for high value
		}
		else if (pts >= 5)
		{
			pointsLabel.setForeground(DmmColors.POINTS_MEDIUM); // Green for medium
		}
		else
		{
			pointsLabel.setForeground(DmmColors.POINTS_LOW); // Blue for low
		}
		pointsLabel.setAlignmentX(LEFT_ALIGNMENT);

		// Boss name (bottom)
		JLabel nameLabel = new JLabel(boss.getName());
		nameLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setAlignmentX(LEFT_ALIGNMENT);

		centerPanel.add(pointsLabel);
		centerPanel.add(nameLabel);

		row.setLeftComponent(kcLabel);
		row.setCenterComponent(centerPanel);

		// Click handlers for double-click wiki and right-click menu
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
				{
					LinkBrowser.browse(boss.getWikiUrl());
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				showMenu(e, boss);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				showMenu(e, boss);
			}
		});

		return row;
	}

	private void showMenu(MouseEvent e, BossData boss)
	{
		if (!e.isPopupTrigger()) return;

		JPopupMenu menu = new JPopupMenu();

		JMenuItem addToPlan = new JMenuItem("Add to Plan");
		addToPlan.addActionListener(ev ->
		{
			if (addToPlanHandler != null) addToPlanHandler.accept(boss);
		});
		menu.add(addToPlan);

		JMenuItem openWiki = new JMenuItem("Open Wiki");
		openWiki.addActionListener(ev -> LinkBrowser.browse(boss.getWikiUrl()));
		menu.add(openWiki);

		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	public void setAddToPlanHandler(Consumer<BossData> handler)
	{
		this.addToPlanHandler = handler;
	}

	/**
	 * Expands the category containing the specified boss or category name.
	 * @param nameOrCategory The boss name or category id to expand
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

		// Try to find by boss name - search through bosses to find category
		for (BossData boss : dataLoader.getBosses())
		{
			if (boss.getName().equalsIgnoreCase(nameOrCategory))
			{
				String cat = boss.getCategory();
				if (cat != null && categoryPanels.containsKey(cat))
				{
					categoryPanels.get(cat).setExpanded(true);
				}
				return;
			}
		}
	}
}
