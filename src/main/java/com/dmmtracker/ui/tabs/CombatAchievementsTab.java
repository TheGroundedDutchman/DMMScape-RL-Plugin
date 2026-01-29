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

import com.dmmtracker.DMMTrackerConfig;
import com.dmmtracker.ProgressStore;
import com.dmmtracker.RequirementChecker;
import com.dmmtracker.data.BossRequirementData;
import com.dmmtracker.data.CATaskData;
import com.dmmtracker.data.DataLoader;
import com.dmmtracker.data.PluginData;
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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.EmptyBorder;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combat Achievements tab - shows all CA tasks with collapsible sections per tier,
 * checkboxes for completion, and progress bars per tier.
 */
public class CombatAchievementsTab extends JPanel
{
	private static final String WIKI_BASE = "https://oldschool.runescape.wiki/w/Special:Search?search=";
	private static final List<String> TIER_ORDER = List.of("easy", "medium", "hard", "elite", "master", "grandmaster");
	private static final Map<String, Color> TIER_COLORS = Map.of(
		"easy", DmmColors.TIER_EASY,
		"medium", DmmColors.TIER_MEDIUM,
		"hard", DmmColors.TIER_HARD,
		"elite", DmmColors.TIER_ELITE,
		"master", DmmColors.TIER_MASTER,
		"grandmaster", DmmColors.TIER_GRANDMASTER
	);

	private final DataLoader dataLoader;
	private final ProgressStore progressStore;
	private final RequirementChecker requirementChecker;
	private final JLabel summaryLabel = new JLabel();
	private final JCheckBox reqMetFilter = UiComponents.createCheckBox("Req Met");
	private final JCheckBox incompleteFilter = UiComponents.createCheckBox("Hide done");
	private final JPanel contentPanel = new JPanel();
	private JScrollPane scrollPane;
	private final Map<String, CollapsiblePanel> tierPanels = new LinkedHashMap<>();
	private final Map<String, JProgressBar> tierProgressBars = new LinkedHashMap<>();
	private final EmptyStatePanel emptyStatePanel = new EmptyStatePanel();
	private boolean filtersInitialized = false;

	public CombatAchievementsTab(DataLoader dataLoader, ProgressStore progressStore, RequirementChecker requirementChecker)
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
			reqMetFilter.setToolTipText("Only show tasks for bosses you can access (slayer & quest requirements)");
			reqMetFilter.setForeground(Color.WHITE);
		}
	}

	/**
	 * Initialize filter defaults from config. Should be called once on first refresh.
	 */
	public void initializeFilters(DMMTrackerConfig config)
	{
		if (filtersInitialized || config == null)
		{
			return;
		}
		reqMetFilter.setSelected(config.defaultReqMetFilter());
		incompleteFilter.setSelected(config.defaultIncompleteFilter());
		filtersInitialized = true;
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

		JLabel title = new JLabel("Combat Achievements");
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
		reqMetFilter.setToolTipText("Only show tasks for bosses you can access (requires being logged in)");
		reqMetFilter.getAccessibleContext().setAccessibleName("Requirements Met Filter");
		reqMetFilter.getAccessibleContext().setAccessibleDescription("Filter to show only tasks for bosses you can access");
		reqMetFilter.addActionListener(e ->
		{
			updateFilterState();
			SwingUtilities.invokeLater(this::rebuildContent);
		});

		incompleteFilter.setFont(FontManager.getRunescapeSmallFont());
		incompleteFilter.setForeground(Color.WHITE);
		incompleteFilter.setOpaque(false);
		incompleteFilter.setFocusPainted(false);
		incompleteFilter.setToolTipText("Only show incomplete tasks");
		incompleteFilter.getAccessibleContext().setAccessibleName("Incomplete Filter");
		incompleteFilter.getAccessibleContext().setAccessibleDescription("Filter to show only incomplete combat achievement tasks");
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
		contentPanel.setBorder(new EmptyBorder(0, 0, 0, 16));

		scrollPane = new JScrollPane(contentPanel);
		ScrollPaneUtils.applyRuneliteStyle(scrollPane);

		body.add(scrollPane, BorderLayout.CENTER);
		return body;
	}

	private void rebuildContent()
	{
		Map<String, Boolean> expandedState = new HashMap<>();
		for (Map.Entry<String, CollapsiblePanel> entry : tierPanels.entrySet())
		{
			expandedState.put(entry.getKey(), entry.getValue().isExpanded());
		}

		contentPanel.removeAll();
		tierPanels.clear();
		tierProgressBars.clear();

		boolean filterReqMet = reqMetFilter.isSelected();
		boolean filterIncomplete = incompleteFilter.isSelected();

		Map<String, PluginData.CATierData> tiers = dataLoader.getCATiers();
		int totalTasks = 0;
		int totalCompleted = 0;

		for (String tierName : TIER_ORDER)
		{
			PluginData.CATierData tierData = tiers.get(tierName);
			if (tierData == null) continue;

			List<CATaskData> tasks = tierData.getTasks();
			if (tasks == null || tasks.isEmpty()) continue;

			totalTasks += tasks.size();

			// Count completed tasks for this tier
			int tierCompleted = 0;
			for (CATaskData task : tasks)
			{
				if (isCATaskCompleted(task.getId(), task.getName()))
				{
					tierCompleted++;
				}
			}
			totalCompleted += tierCompleted;

			// Filter tasks based on active filters
			List<CATaskData> filteredTasks = new ArrayList<>();
			for (CATaskData task : tasks)
			{
				boolean isCompleted = isCATaskCompleted(task.getId(), task.getName());

				// Skip completed tasks if incomplete filter is on
				if (filterIncomplete && isCompleted)
				{
					continue;
				}

				// Check requirements if filter is enabled
				// RequirementChecker falls back to DMM base stats when not logged in
				if (filterReqMet && requirementChecker != null)
				{
					String monster = task.getMonster();
					if (monster != null && !monster.isEmpty())
					{
						BossRequirementData req = dataLoader.getBossRequirement(monster);
						if (req != null)
						{
							// Check if player meets slayer level and quest requirements
							if (!requirementChecker.meetsBossRequirements(req.getSlayerLevel(), req.getQuest()))
							{
								continue;
							}
						}
					}
				}
				filteredTasks.add(task);
			}

			if (filteredTasks.isEmpty())
			{
				continue;
			}

			Color tierColor = TIER_COLORS.getOrDefault(tierName, ColorScheme.LIGHT_GRAY_COLOR);

			// Create collapsible panel for this tier
			String tierDisplayName = capitalize(tierName);
			CollapsiblePanel tierPanel = new CollapsiblePanel(tierDisplayName);
			tierPanel.setTitleIcon(IconManager.getCATierIcon(tierName, 14));
			tierPanel.setSummaryText(tierCompleted + "/" + tasks.size());
			tierPanel.setHeaderAccentColor(tierColor);
			tierPanel.setHeaderProgress(tierCompleted, tasks.size(), tierColor);
			tierPanel.setExpanded(expandedState.getOrDefault(tierName, false));
			tierPanels.put(tierName, tierPanel);

			JPanel tierContent = tierPanel.getContentPanel();

			// Add progress bar at the top of tier content
			JProgressBar progressBar = createProgressBar(tierName, tierCompleted, tasks.size());
			tierProgressBars.put(tierName, progressBar);

			JPanel progressPanel = new JPanel(new BorderLayout());
			progressPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
			progressPanel.setBorder(new EmptyBorder(6, 10, 6, 10));
			progressPanel.add(progressBar, BorderLayout.CENTER);
			tierContent.add(progressPanel);

			// Add tasks to the tier panel
			int index = 0;
			for (CATaskData task : filteredTasks)
			{
				JPanel taskRow = createTaskRow(task, tierName, index);
				tierContent.add(taskRow);
				index++;
			}

			contentPanel.add(tierPanel);
		}

		// Show empty state if no content
		if (contentPanel.getComponentCount() == 0)
		{
			if (dataLoader.getCATiers().isEmpty())
			{
				emptyStatePanel.setEmptyState("No Data Available", "Combat achievement data is still loading...");
			}
			else if (filterReqMet || filterIncomplete)
			{
				emptyStatePanel.setNoResultsState("No tasks match your current filters. Try adjusting filter options.");
			}
			else
			{
				emptyStatePanel.setEmptyState("No Combat Achievements", "No combat achievement data available.");
			}
			contentPanel.add(emptyStatePanel);
		}

		// Update summary
		summaryLabel.setText(totalCompleted + "/" + totalTasks);

		// Force complete UI refresh
		contentPanel.revalidate();
		contentPanel.repaint();
		if (scrollPane != null)
		{
			scrollPane.revalidate();
			scrollPane.repaint();
			// Reset scroll position to top
			scrollPane.getVerticalScrollBar().setValue(0);
		}
		// Also revalidate parent
		revalidate();
		repaint();
	}

	private JProgressBar createProgressBar(String tier, int completed, int total)
	{
		JProgressBar progressBar = new JProgressBar(0, Math.max(1, total));
		progressBar.setValue(completed);
		progressBar.setStringPainted(true);
		progressBar.setString(completed + "/" + total);
		progressBar.setFont(FontManager.getRunescapeSmallFont());
		progressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		progressBar.setForeground(TIER_COLORS.getOrDefault(tier, ColorScheme.LIGHT_GRAY_COLOR));
		progressBar.setBorderPainted(false);
		progressBar.setPreferredSize(new Dimension(0, 16));
		return progressBar;
	}

	private boolean isCATaskCompleted(String taskId, String legacyName)
	{
		if (taskId != null && !taskId.isEmpty() && progressStore.isCATaskCompleted(taskId))
		{
			return true;
		}
		return legacyName != null && !legacyName.isEmpty() && progressStore.isCATaskCompleted(legacyName);
	}

	private void setCATaskCompleted(String taskId, String legacyName, boolean completed)
	{
		if (taskId != null && !taskId.isEmpty())
		{
			progressStore.setCATaskCompleted(taskId, completed);
			if (legacyName != null && !legacyName.equals(taskId))
			{
				progressStore.setCATaskCompleted(legacyName, false);
			}
			return;
		}

		if (legacyName != null && !legacyName.isEmpty())
		{
			progressStore.setCATaskCompleted(legacyName, completed);
		}
	}

	private JPanel createTaskRow(CATaskData task, String tier, int index)
	{
		ListItemPanel row = new ListItemPanel(index);

		String taskId = task.getId();
		String legacyName = task.getName();

		// Left: checkbox
		boolean isCompleted = isCATaskCompleted(taskId, legacyName);
		JCheckBox checkbox = UiComponents.createCheckBox();
		checkbox.setSelected(isCompleted);
		checkbox.addActionListener(e ->
		{
			boolean selected = checkbox.isSelected();
			setCATaskCompleted(taskId, legacyName, selected);
			// Update text color - textPanel is at center (index 1)
			java.awt.Component centerComp = row.getComponent(1);
			if (centerComp instanceof JPanel)
			{
				JPanel textPanel = (JPanel) centerComp;
				if (textPanel.getComponentCount() > 0 && textPanel.getComponent(0) instanceof JLabel)
				{
					((JLabel) textPanel.getComponent(0)).setForeground(selected ? DmmColors.SUCCESS : Color.WHITE);
				}
			}
			if (incompleteFilter.isSelected())
			{
				SwingUtilities.invokeLater(this::rebuildContent);
				return;
			}
			// Update progress without rebuilding
			updateTierProgressOnly(tier);
		});

		// Center: name and description with wrapping
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		textPanel.setOpaque(false);

		JLabel nameLabel = new JLabel(task.getName());
		nameLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		nameLabel.setForeground(isCompleted ? DmmColors.SUCCESS : Color.WHITE);
		nameLabel.setAlignmentX(LEFT_ALIGNMENT);

		String desc = task.getDescription();
		if (desc != null && !desc.isEmpty())
		{
			JLabel descLabel = new JLabel("<html><body style='width: 160px'>" + desc + "</body></html>");
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

		Integer points = task.getPoints();
		if (points != null)
		{
			JLabel pointsLabel = new JLabel(points + " pts");
			pointsLabel.setFont(FontManager.getRunescapeSmallFont());
			pointsLabel.setForeground(DmmColors.GOLD);
			JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
			infoPanel.setOpaque(false);
			infoPanel.add(pointsLabel);
			textPanel.add(Box.createVerticalStrut(4));
			textPanel.add(infoPanel);
		}

		row.setLeftComponent(checkbox);
		row.setCenterComponent(textPanel);

		// Click handlers for double-click wiki and right-click menu
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
				{
					openWiki(task);
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				showMenu(e, task);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				showMenu(e, task);
			}
		});

		return row;
	}

	/**
	 * Updates tier progress without rebuilding the UI.
	 */
	private void updateTierProgressOnly(String tier)
	{
		Map<String, PluginData.CATierData> tiers = dataLoader.getCATiers();
		PluginData.CATierData tierData = tiers.get(tier);
		if (tierData == null) return;

		List<CATaskData> tasks = tierData.getTasks();
		if (tasks == null) return;

		int completed = 0;
		for (CATaskData task : tasks)
		{
			if (isCATaskCompleted(task.getId(), task.getName()))
			{
				completed++;
			}
		}

		Color tierColor = TIER_COLORS.getOrDefault(tier, ColorScheme.LIGHT_GRAY_COLOR);

		// Update progress bar
		JProgressBar progressBar = tierProgressBars.get(tier);
		if (progressBar != null)
		{
			progressBar.setValue(completed);
			progressBar.setString(completed + "/" + tasks.size());
		}

		// Update collapsible panel summary and header progress
		CollapsiblePanel panel = tierPanels.get(tier);
		if (panel != null)
		{
			panel.setSummaryText(completed + "/" + tasks.size());
			panel.setHeaderProgress(completed, tasks.size(), tierColor);
		}

		// Update overall summary
		updateOverallSummary();
	}

	private void updateOverallSummary()
	{
		Map<String, PluginData.CATierData> tiers = dataLoader.getCATiers();
		int totalCompleted = 0;
		int totalTasks = 0;

		for (String tierName : TIER_ORDER)
		{
			PluginData.CATierData tierData = tiers.get(tierName);
			if (tierData == null) continue;

			List<CATaskData> tasks = tierData.getTasks();
			if (tasks == null) continue;

			totalTasks += tasks.size();
			for (CATaskData task : tasks)
			{
				if (isCATaskCompleted(task.getId(), task.getName()))
				{
					totalCompleted++;
				}
			}
		}

		summaryLabel.setText(totalCompleted + "/" + totalTasks);
	}

	private void showMenu(MouseEvent e, CATaskData task)
	{
		if (!e.isPopupTrigger()) return;

		JPopupMenu menu = new JPopupMenu();
		JMenuItem openWiki = new JMenuItem("Open Wiki");
		openWiki.addActionListener(ev -> openWiki(task));
		menu.add(openWiki);
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	private void openWiki(CATaskData task)
	{
		String name = task.getName();
		if (name == null || name.isEmpty())
		{
			LinkBrowser.browse("https://oldschool.runescape.wiki/w/Combat_Achievements");
			return;
		}
		String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
		LinkBrowser.browse(WIKI_BASE + encoded);
	}

	private String capitalize(String s)
	{
		if (s == null || s.isEmpty()) return "";
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	/**
	 * Expands the tier panel containing the specified tier name.
	 * @param tierName The tier name to expand (e.g., "easy", "medium", "hard", etc.)
	 */
	public void expandCategory(String tierName)
	{
		if (tierName == null)
		{
			return;
		}

		String lower = tierName.toLowerCase();
		if (tierPanels.containsKey(lower))
		{
			tierPanels.get(lower).setExpanded(true);
		}
	}
}
