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
import com.dmmtracker.RequirementChecker;
import com.dmmtracker.data.DataLoader;
import com.dmmtracker.data.DiaryData;
import com.dmmtracker.data.DiaryQuestRequirement;
import com.dmmtracker.data.DiarySkillRequirement;
import com.dmmtracker.data.DiaryTaskData;
import com.dmmtracker.data.DiaryTierData;
import com.dmmtracker.ui.DmmColors;
import com.dmmtracker.ui.IconManager;
import com.dmmtracker.ui.ScrollPaneUtils;
import com.dmmtracker.ui.components.CollapsiblePanel;
import com.dmmtracker.ui.components.EmptyStatePanel;
import com.dmmtracker.ui.components.ListItemPanel;
import com.dmmtracker.ui.components.UiComponents;
import com.dmmtracker.ui.components.UiIconFactory;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.EmptyBorder;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import net.runelite.api.coords.WorldPoint;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Achievement Diaries tab - shows all diary tasks with collapsible sections per region and tier,
 * checkboxes for completion, and progress bars per region and tier.
 */
public class DiariesTab extends JPanel
{
	private static final String WIKI_BASE = "https://oldschool.runescape.wiki/w/Special:Search?search=";
	private static final List<String> TIER_ORDER = List.of("easy", "medium", "hard", "elite");
	private static final Map<String, Color> TIER_COLORS = Map.of(
		"easy", DmmColors.TIER_EASY,
		"medium", DmmColors.TIER_MEDIUM,
		"hard", DmmColors.TIER_HARD,
		"elite", DmmColors.TIER_ELITE
	);
	private static final Color[] TIER_COLOR_ARRAY = {
		DmmColors.TIER_EASY, // easy
		DmmColors.TIER_MEDIUM, // medium
		DmmColors.TIER_HARD, // hard
		DmmColors.TIER_ELITE  // elite
	};

	private final DataLoader dataLoader;
	private final ProgressStore progressStore;
	private final RequirementChecker requirementChecker;
	private final JLabel summaryLabel = new JLabel();
	private final JCheckBox reqMetFilter = UiComponents.createCheckBox("Req Met");
	private final JCheckBox incompleteFilter = UiComponents.createCheckBox("Hide done");
	private final JPanel contentPanel = new JPanel();
	private final JButton clearNavButton = UiComponents.createSmallButton("Clear Nav");
	private JScrollPane scrollPane;
	private final Map<String, CollapsiblePanel> regionPanels = new LinkedHashMap<>();
	private final Map<String, CollapsiblePanel> tierPanels = new LinkedHashMap<>();
	private final Map<String, JProgressBar> regionProgressBars = new LinkedHashMap<>();
	private final Map<String, JProgressBar> tierProgressBars = new LinkedHashMap<>();
	private final EmptyStatePanel emptyStatePanel = new EmptyStatePanel();
	private LocationNavigateHandler locationNavigateHandler;
	private Runnable clearNavigationHandler;
	private LocationFocusChecker locationFocusChecker;

	public interface LocationNavigateHandler
	{
		void onNavigate(DiaryTaskData task, String region, String tier);
	}

	public interface LocationFocusChecker
	{
		boolean isFocused(DiaryTaskData task, String region, String tier);
		boolean hasFocus();
	}

	public DiariesTab(DataLoader dataLoader, ProgressStore progressStore, RequirementChecker requirementChecker)
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
	 * Updates tooltip based on whether game data is available or using DMM base stats.
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
			reqMetFilter.setToolTipText("Only show tasks where you meet skill & quest requirements");
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

		JLabel title = new JLabel("Achievement Diaries");
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
		reqMetFilter.setToolTipText("Only show tasks where you meet skill & quest requirements (requires being logged in)");
		reqMetFilter.getAccessibleContext().setAccessibleName("Requirements Met Filter");
		reqMetFilter.getAccessibleContext().setAccessibleDescription("Filter to show only diary tasks where skill and quest requirements are met");
		reqMetFilter.addActionListener(e ->
		{
			updateFilterState();
			SwingUtilities.invokeLater(this::rebuildContent);
		});

		incompleteFilter.setFont(FontManager.getRunescapeSmallFont());
		incompleteFilter.setForeground(Color.WHITE);
		incompleteFilter.setOpaque(false);
		incompleteFilter.setFocusPainted(false);
		incompleteFilter.setToolTipText("Only show incomplete diary tasks");
		incompleteFilter.getAccessibleContext().setAccessibleName("Incomplete Filter");
		incompleteFilter.getAccessibleContext().setAccessibleDescription("Filter to show only incomplete achievement diary tasks");
		incompleteFilter.addActionListener(e -> SwingUtilities.invokeLater(this::rebuildContent));

		filterRow.add(reqMetFilter);
		filterRow.add(incompleteFilter);

		clearNavButton.setEnabled(false);
		clearNavButton.setToolTipText("Clear custom navigation target");
		clearNavButton.addActionListener(e ->
		{
			if (clearNavigationHandler != null)
			{
				clearNavigationHandler.run();
			}
		});
		filterRow.add(clearNavButton);

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
		Map<String, Boolean> regionExpandedState = new HashMap<>();
		for (Map.Entry<String, CollapsiblePanel> entry : regionPanels.entrySet())
		{
			regionExpandedState.put(entry.getKey(), entry.getValue().isExpanded());
		}
		Map<String, Boolean> tierExpandedState = new HashMap<>();
		for (Map.Entry<String, CollapsiblePanel> entry : tierPanels.entrySet())
		{
			tierExpandedState.put(entry.getKey(), entry.getValue().isExpanded());
		}

		contentPanel.removeAll();
		regionPanels.clear();
		tierPanels.clear();
		regionProgressBars.clear();
		tierProgressBars.clear();

		boolean filterReqMet = reqMetFilter.isSelected();
		boolean filterIncomplete = incompleteFilter.isSelected();

		int totalCompleted = 0;
		int totalTasks = 0;

		boolean autoExpand = regionExpandedState.isEmpty() && tierExpandedState.isEmpty();
		int regionIndex = 0;

		for (DiaryData diary : dataLoader.getDiaries())
		{
			String regionName = diary.getName();
			int regionCompleted = 0;
			int regionTaskCount = 0;

			// Collect tier progress for the 4-segment header bar
			int[] tierCompleted = new int[4];
			int[] tierTotals = new int[4];

			// Build tier panels for this region
			List<TierPanelData> tierPanelsData = new ArrayList<>();

			int tierIndex = 0;
			for (String tierName : TIER_ORDER)
			{
				DiaryTierData tier = diary.getTier(tierName);
				if (tier == null)
				{
					tierIndex++;
					continue;
				}

				List<DiaryTaskData> tasks = tier.getTasks();
				if (tasks == null || tasks.isEmpty())
				{
					tierIndex++;
					continue;
				}

				totalTasks += tasks.size();
				regionTaskCount += tasks.size();
				tierTotals[tierIndex] = tasks.size();

				// Count completed tasks for this tier
				int completed = 0;
				for (DiaryTaskData task : tasks)
				{
					String legacyKey = buildLegacyDiaryKey(regionName, tierName, task);
					if (isDiaryTaskCompleted(task.getId(), legacyKey))
					{
						completed++;
					}
				}
				tierCompleted[tierIndex] = completed;
				regionCompleted += completed;
				totalCompleted += completed;

				// Filter tasks based on active filters
				List<DiaryTaskData> filteredTasks = new ArrayList<>();
				for (DiaryTaskData task : tasks)
				{
					String legacyKey = buildLegacyDiaryKey(regionName, tierName, task);
					boolean isCompleted = isDiaryTaskCompleted(task.getId(), legacyKey);

					// Skip completed tasks if incomplete filter is on
					if (filterIncomplete && isCompleted)
					{
						continue;
					}

					// Skip tasks where requirements are not met (skills AND quests)
					// RequirementChecker falls back to DMM base stats when not logged in
					if (filterReqMet && requirementChecker != null)
					{
						List<DiarySkillRequirement> skillReqs = task.getSkillRequirements();
						List<DiaryQuestRequirement> questReqs = task.getQuestRequirements();

						if (!requirementChecker.meetsAllRequirements(skillReqs, questReqs))
						{
							continue;
						}
					}
					filteredTasks.add(task);
				}

				if (!filteredTasks.isEmpty())
				{
					tierPanelsData.add(new TierPanelData(tierName, filteredTasks, completed, tasks.size()));
				}

				tierIndex++;
			}

			if (tierPanelsData.isEmpty())
			{
				continue;
			}

			// Create region collapsible panel
			CollapsiblePanel regionPanel = new CollapsiblePanel(regionName);
			regionPanel.setSummaryText(regionCompleted + "/" + regionTaskCount);
			regionPanel.setHeaderProgressSegments(tierCompleted, tierTotals, TIER_COLOR_ARRAY);
			boolean expandRegion = regionExpandedState.getOrDefault(regionName, false);
			if (autoExpand && regionIndex == 0)
			{
				expandRegion = true;
			}
			regionPanel.setExpanded(expandRegion);
			regionPanels.put(regionName, regionPanel);

			JPanel regionContent = regionPanel.getContentPanel();

			// Add region progress bar (4-segment)
			JPanel regionProgressPanel = createSegmentedProgressPanel(regionName, tierCompleted, tierTotals);
			regionProgressBars.put(regionName, (JProgressBar) regionProgressPanel.getClientProperty("progressBar"));
			regionContent.add(regionProgressPanel);

			// Add tier panels inside region
			int tierPanelIndex = 0;
			for (TierPanelData tierData : tierPanelsData)
			{
				boolean expandTier = autoExpand && regionIndex == 0 && tierPanelIndex == 0;
				CollapsiblePanel tierSection = createTierSection(tierData, regionName, tierExpandedState, expandTier);
				regionContent.add(tierSection);
				tierPanelIndex++;
			}

			contentPanel.add(regionPanel);
			regionIndex++;
		}

		// Show empty state if no content
		if (contentPanel.getComponentCount() == 0)
		{
			if (dataLoader.getDiaries().isEmpty())
			{
				emptyStatePanel.setEmptyState("No Data Available", "Diary data is still loading...");
			}
			else if (filterReqMet || filterIncomplete)
			{
				emptyStatePanel.setNoResultsState("No tasks match your current filters. Try adjusting filter options.");
			}
			else
			{
				emptyStatePanel.setEmptyState("No Diaries", "No achievement diary data available.");
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

		updateClearNavButton();
	}

	private JPanel createSegmentedProgressPanel(String regionName, int[] completed, int[] totals)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(6, 10, 6, 10));

		SegmentedProgressBar bar = new SegmentedProgressBar(completed, totals, TIER_COLOR_ARRAY);
		bar.setPreferredSize(new Dimension(0, 16));

		// Store a reference for later updates
		panel.putClientProperty("segmentedBar", bar);
		panel.putClientProperty("progressBar", null); // Compatibility

		panel.add(bar, BorderLayout.CENTER);
		return panel;
	}

	private CollapsiblePanel createTierSection(
		TierPanelData tierData,
		String regionName,
		Map<String, Boolean> tierExpandedState,
		boolean defaultExpanded
	)
	{
		String tierName = tierData.tierName;
		List<DiaryTaskData> tasks = tierData.tasks;
		int tierCompleted = tierData.completed;
		int tierTotal = tierData.total;

		Color tierColor = TIER_COLORS.getOrDefault(tierName, ColorScheme.LIGHT_GRAY_COLOR);

		CollapsiblePanel tierPanel = new CollapsiblePanel(capitalize(tierName));
		tierPanel.setSummaryText(tierCompleted + "/" + tierTotal);
		tierPanel.setHeaderAccentColor(tierColor);
		tierPanel.setHeaderProgress(tierCompleted, tierTotal, tierColor);

		String tierKey = regionName + ":" + tierName;
		tierPanel.setExpanded(tierExpandedState.getOrDefault(tierKey, defaultExpanded));
		tierPanels.put(tierKey, tierPanel);

		JPanel tierContent = tierPanel.getContentPanel();

		// Add tier progress bar inside content
		JProgressBar tierProgressBar = createProgressBar(tierKey, tierCompleted, tierTotal, tierColor);
		tierProgressBars.put(tierKey, tierProgressBar);

		JPanel tierProgressPanel = new JPanel(new BorderLayout());
		tierProgressPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tierProgressPanel.setBorder(new EmptyBorder(6, 10, 6, 10));
		tierProgressPanel.add(tierProgressBar, BorderLayout.CENTER);
		tierContent.add(tierProgressPanel);

		// Add tasks
		int index = 0;
		for (DiaryTaskData task : tasks)
		{
			JPanel taskRow = createTaskRow(task, tierName, regionName, index);
			tierContent.add(taskRow);
			index++;
		}

		return tierPanel;
	}

	private JProgressBar createProgressBar(String key, int completed, int total, Color color)
	{
		JProgressBar progressBar = new JProgressBar(0, Math.max(1, total));
		progressBar.setValue(completed);
		progressBar.setStringPainted(true);
		progressBar.setString(completed + "/" + total);
		progressBar.setFont(FontManager.getRunescapeSmallFont());
		progressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		progressBar.setForeground(color);
		progressBar.setBorderPainted(false);
		progressBar.setPreferredSize(new Dimension(0, 16));
		return progressBar;
	}

	private String buildLegacyDiaryKey(String region, String tier, DiaryTaskData task)
	{
		if (region == null || tier == null || task == null)
		{
			return null;
		}
		String desc = task.getDescription();
		if (desc == null || desc.isEmpty())
		{
			return null;
		}
		return region + ":" + tier + ":" + desc;
	}

	private boolean isDiaryTaskCompleted(String taskId, String legacyKey)
	{
		if (taskId != null && !taskId.isEmpty() && progressStore.isDiaryTaskCompleted(taskId))
		{
			return true;
		}
		return legacyKey != null && !legacyKey.isEmpty() && progressStore.isDiaryTaskCompleted(legacyKey);
	}

	private void setDiaryTaskCompleted(String taskId, String legacyKey, boolean completed)
	{
		if (taskId != null && !taskId.isEmpty())
		{
			progressStore.setDiaryTaskCompleted(taskId, completed);
			if (legacyKey != null && !legacyKey.equals(taskId))
			{
				progressStore.setDiaryTaskCompleted(legacyKey, false);
			}
			return;
		}

		if (legacyKey != null && !legacyKey.isEmpty())
		{
			progressStore.setDiaryTaskCompleted(legacyKey, completed);
		}
	}

	private JPanel createTaskRow(DiaryTaskData task, String tier, String region, int index)
	{
		ListItemPanel row = new ListItemPanel(index);

		String taskId = task.getId();
		String legacyKey = buildLegacyDiaryKey(region, tier, task);

		// Left: checkbox
		boolean isCompleted = isDiaryTaskCompleted(taskId, legacyKey);

		// Check if requirements are met (for text color)
		// When not logged in, RequirementChecker falls back to DMM base stats + autocompleted quests
		List<DiarySkillRequirement> skillReqs = task.getSkillRequirements();
		List<DiaryQuestRequirement> questReqs = task.getQuestRequirements();
		boolean requirementsMet = requirementChecker != null && requirementChecker.meetsAllRequirements(skillReqs, questReqs);

		JCheckBox checkbox = UiComponents.createCheckBox();
		checkbox.setSelected(isCompleted);
		checkbox.addActionListener(e ->
		{
			boolean selected = checkbox.isSelected();
			setDiaryTaskCompleted(taskId, legacyKey, selected);
			// Update text styling - descLabel is in textPanel (center component)
			java.awt.Component centerComp = row.getComponent(1);
			if (centerComp instanceof JPanel)
			{
				JPanel textPanel = (JPanel) centerComp;
				if (textPanel.getComponentCount() > 0 && textPanel.getComponent(0) instanceof JLabel)
				{
					JLabel label = (JLabel) textPanel.getComponent(0);
					updateTaskLabelStyle(label, task.getDescription(), selected, requirementsMet);
				}
			}
			if (incompleteFilter.isSelected())
			{
				SwingUtilities.invokeLater(this::rebuildContent);
				return;
			}
			// Update progress without rebuilding
			updateProgressOnly(region, tier);
		});

		// Center: description with text wrapping
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		textPanel.setOpaque(false);

		String desc = task.getDescription();
		JLabel descLabel = new JLabel();
		descLabel.setFont(FontManager.getRunescapeSmallFont());
		descLabel.setAlignmentX(LEFT_ALIGNMENT);
		updateTaskLabelStyle(descLabel, desc, isCompleted, requirementsMet);

		textPanel.add(descLabel);

		// Add skill requirements row if present
		if (skillReqs != null && !skillReqs.isEmpty())
		{
			JPanel skillsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
			skillsPanel.setOpaque(false);
			skillsPanel.setAlignmentX(LEFT_ALIGNMENT);

			for (DiarySkillRequirement req : skillReqs)
			{
				JPanel skillBadge = createSkillBadge(req);
				skillsPanel.add(skillBadge);
			}

			textPanel.add(Box.createVerticalStrut(2));
			textPanel.add(skillsPanel);
		}

		// Right panel: location icon above points
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.setOpaque(false);

		// Location button (icon only, no text)
		JButton locationButton = new JButton();
		locationButton.setIcon(UiIconFactory.createLocationIcon(DmmColors.INFO, 14));
		locationButton.setBorder(new EmptyBorder(2, 4, 2, 4));
		locationButton.setContentAreaFilled(false);
		locationButton.setFocusPainted(false);
		locationButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		locationButton.setToolTipText("Navigate to location");
		locationButton.setAlignmentX(CENTER_ALIGNMENT);

		WorldPoint taskLocation = task.getWorldPoint();
		boolean isFocused = locationFocusChecker != null && locationFocusChecker.isFocused(task, region, tier);
		if (taskLocation == null)
		{
			locationButton.setEnabled(false);
			locationButton.setIcon(UiIconFactory.createLocationIcon(DmmColors.DISABLED, 14));
			locationButton.setToolTipText("No location available");
		}
		else
		{
			if (isFocused)
			{
				locationButton.setIcon(UiIconFactory.createLocationIcon(DmmColors.SUCCESS_BRIGHT, 14));
				locationButton.setToolTipText("Navigation active (click to clear)");
				row.setHighlighted(true);
			}
			locationButton.addActionListener(e ->
			{
				if (locationNavigateHandler != null)
				{
					locationNavigateHandler.onNavigate(task, region, tier);
				}
			});
		}

		// Points label
		JLabel pointsLabel = new JLabel(task.getPoints() + " pts");
		pointsLabel.setFont(FontManager.getRunescapeSmallFont());
		pointsLabel.setForeground(DmmColors.GOLD);
		pointsLabel.setAlignmentX(CENTER_ALIGNMENT);

		rightPanel.add(locationButton);
		rightPanel.add(pointsLabel);

		row.setLeftComponent(checkbox);
		row.setCenterComponent(textPanel);
		row.setRightComponent(rightPanel);

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
	 * Updates the task label style based on completion and requirement status.
	 * - Completed: green text with strikethrough
	 * - Requirements not met: red text (can't do yet)
	 * - Requirements met: white text (ready to do)
	 */
	private void updateTaskLabelStyle(JLabel label, String description, boolean isCompleted, boolean requirementsMet)
	{
		String desc = description != null ? description : "";

		if (isCompleted)
		{
			// Green + strikethrough for completed
			label.setText("<html><body style='width: 150px'><span style='text-decoration: line-through; color: #00C000;'>" + desc + "</span></body></html>");
			label.setForeground(DmmColors.SUCCESS);
		}
		else if (!requirementsMet)
		{
			// Red for requirements NOT met (can't do yet)
			label.setText("<html><body style='width: 150px'><span style='color: #FF6B6B;'>" + desc + "</span></body></html>");
			label.setForeground(DmmColors.ERROR);
		}
		else
		{
			// White for requirements met (ready to do)
			label.setText("<html><body style='width: 150px'>" + desc + "</body></html>");
			label.setForeground(Color.WHITE);
		}
	}

	/**
	 * Creates a skill badge showing skill icon and required level.
	 */
	private JPanel createSkillBadge(DiarySkillRequirement req)
	{
		JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
		badge.setOpaque(false);

		// Skill icon
		JLabel iconLabel = new JLabel();
		iconLabel.setIcon(IconManager.getSkillIcon(req.getSkill(), 14, () ->
		{
			// Refresh icon when loaded
			iconLabel.setIcon(IconManager.getSkillIcon(req.getSkill(), 14));
			iconLabel.revalidate();
			iconLabel.repaint();
		}));

		// Level label
		JLabel levelLabel = new JLabel(String.valueOf(req.getLevel()));
		levelLabel.setFont(FontManager.getRunescapeSmallFont());
		levelLabel.setForeground(req.isBoostable() ? DmmColors.GOLD : DmmColors.WARNING);
		levelLabel.setToolTipText(req.getSkill() + " " + req.getLevel() + (req.isBoostable() ? "" : " (cannot boost)"));

		badge.add(iconLabel);
		badge.add(levelLabel);

		return badge;
	}

	/**
	 * Updates progress bars and summaries without rebuilding the UI.
	 */
	private void updateProgressOnly(String region, String tier)
	{
		// Find the diary data
		DiaryData diary = null;
		for (DiaryData d : dataLoader.getDiaries())
		{
			if (d.getName().equals(region))
			{
				diary = d;
				break;
			}
		}
		if (diary == null) return;

		// Recalculate all tier progress for this region
		int[] tierCompleted = new int[4];
		int[] tierTotals = new int[4];
		int regionCompleted = 0;
		int regionTotal = 0;

		int tierIndex = 0;
		for (String t : TIER_ORDER)
		{
			DiaryTierData td = diary.getTier(t);
			if (td == null)
			{
				tierIndex++;
				continue;
			}
			List<DiaryTaskData> tasks = td.getTasks();
			if (tasks == null)
			{
				tierIndex++;
				continue;
			}

			tierTotals[tierIndex] = tasks.size();
			regionTotal += tasks.size();
			int completed = 0;
			for (DiaryTaskData task : tasks)
			{
				String legacyKey = buildLegacyDiaryKey(region, t, task);
				if (isDiaryTaskCompleted(task.getId(), legacyKey))
				{
					completed++;
				}
			}
			tierCompleted[tierIndex] = completed;
			regionCompleted += completed;

			// Update tier progress bar
			String tierKey = region + ":" + t;
			JProgressBar tierBar = tierProgressBars.get(tierKey);
			if (tierBar != null)
			{
				tierBar.setValue(completed);
				tierBar.setString(completed + "/" + tasks.size());
			}

			// Update tier panel summary and header progress
			CollapsiblePanel tierPanel = tierPanels.get(tierKey);
			if (tierPanel != null)
			{
				tierPanel.setSummaryText(completed + "/" + tasks.size());
				tierPanel.setHeaderProgress(completed, tasks.size(), TIER_COLORS.getOrDefault(t, ColorScheme.LIGHT_GRAY_COLOR));
			}

			tierIndex++;
		}

		// Update region panel
		CollapsiblePanel regionPanel = regionPanels.get(region);
		if (regionPanel != null)
		{
			regionPanel.setSummaryText(regionCompleted + "/" + regionTotal);
			regionPanel.setHeaderProgressSegments(tierCompleted, tierTotals, TIER_COLOR_ARRAY);
		}

		// Update overall summary
		updateOverallSummary();
	}

	private void updateOverallSummary()
	{
		int totalCompleted = 0;
		int totalTasks = 0;

		for (DiaryData diary : dataLoader.getDiaries())
		{
			String region = diary.getName();
			for (String tier : TIER_ORDER)
			{
				DiaryTierData tierData = diary.getTier(tier);
				if (tierData == null) continue;
				List<DiaryTaskData> tasks = tierData.getTasks();
				if (tasks == null) continue;
				totalTasks += tasks.size();
				for (DiaryTaskData task : tasks)
				{
					String legacyKey = buildLegacyDiaryKey(region, tier, task);
					if (isDiaryTaskCompleted(task.getId(), legacyKey))
					{
						totalCompleted++;
					}
				}
			}
		}

		summaryLabel.setText(totalCompleted + "/" + totalTasks);
	}

	private void showMenu(MouseEvent e, DiaryTaskData task)
	{
		if (!e.isPopupTrigger()) return;

		JPopupMenu menu = new JPopupMenu();
		JMenuItem openWiki = new JMenuItem("Open Wiki");
		openWiki.addActionListener(ev -> openWiki(task));
		menu.add(openWiki);
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	private void openWiki(DiaryTaskData task)
	{
		String desc = task.getDescription();
		if (desc == null || desc.isEmpty())
		{
			LinkBrowser.browse("https://oldschool.runescape.wiki/w/Achievement_Diary");
			return;
		}
		String encoded = URLEncoder.encode(desc, StandardCharsets.UTF_8);
		LinkBrowser.browse(WIKI_BASE + encoded);
	}

	private String capitalize(String s)
	{
		if (s == null || s.isEmpty()) return "";
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	/**
	 * Expands the region panel containing the specified diary name.
	 * @param diaryName The diary region name to expand (e.g., "Varrock", "Ardougne")
	 */
	public void expandCategory(String diaryName)
	{
		if (diaryName == null)
		{
			return;
		}

		// Try exact match first
		if (regionPanels.containsKey(diaryName))
		{
			regionPanels.get(diaryName).setExpanded(true);
			return;
		}

		// Try case-insensitive match
		for (Map.Entry<String, CollapsiblePanel> entry : regionPanels.entrySet())
		{
			if (entry.getKey().equalsIgnoreCase(diaryName))
			{
				entry.getValue().setExpanded(true);
				return;
			}
		}
	}

	private static class TierPanelData
	{
		final String tierName;
		final List<DiaryTaskData> tasks;
		final int completed;
		final int total;

		TierPanelData(String tierName, List<DiaryTaskData> tasks, int completed, int total)
		{
			this.tierName = tierName;
			this.tasks = tasks;
			this.completed = completed;
			this.total = total;
		}
	}

	/**
	 * A progress bar that displays multiple colored segments.
	 */
	private static class SegmentedProgressBar extends JPanel
	{
		private final int[] completed;
		private final int[] totals;
		private final Color[] colors;

		SegmentedProgressBar(int[] completed, int[] totals, Color[] colors)
		{
			this.completed = completed;
			this.totals = totals;
			this.colors = colors;
			setBackground(ColorScheme.DARKER_GRAY_COLOR);
		}

		@Override
		protected void paintComponent(java.awt.Graphics g)
		{
			super.paintComponent(g);
			java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
			g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

			int width = getWidth();
			int height = getHeight();

			int totalTasks = 0;
			for (int t : totals)
			{
				totalTasks += t;
			}
			if (totalTasks == 0) totalTasks = 1;

			int x = 0;
			for (int i = 0; i < totals.length; i++)
			{
				if (totals[i] == 0) continue;

				int segmentWidth = (int) ((double) totals[i] / totalTasks * width);
				if (i == totals.length - 1 || (i < totals.length - 1 && totals[i + 1] == 0))
				{
					segmentWidth = width - x;
				}

				// Background
				g2.setColor(darken(colors[i], 0.3f));
				g2.fillRect(x, 0, segmentWidth, height);

				// Filled portion
				int filledWidth = (int) ((double) completed[i] / totals[i] * segmentWidth);
				g2.setColor(colors[i]);
				g2.fillRect(x, 0, filledWidth, height);

				x += segmentWidth;
			}

			// Draw text
			g2.setColor(Color.WHITE);
			g2.setFont(FontManager.getRunescapeSmallFont());
			int totalCompleted = 0;
			for (int c : completed) totalCompleted += c;
			String text = totalCompleted + "/" + totalTasks;
			java.awt.FontMetrics fm = g2.getFontMetrics();
			int textX = (width - fm.stringWidth(text)) / 2;
			int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
			g2.drawString(text, textX, textY);

			g2.dispose();
		}

		private Color darken(Color c, float factor)
		{
			return new Color(
				Math.max(0, (int) (c.getRed() * factor)),
				Math.max(0, (int) (c.getGreen() * factor)),
				Math.max(0, (int) (c.getBlue() * factor))
			);
		}
	}

	public void setLocationNavigateHandler(LocationNavigateHandler handler)
	{
		this.locationNavigateHandler = handler;
	}

	public void setClearNavigationHandler(Runnable handler)
	{
		this.clearNavigationHandler = handler;
	}

	public void setLocationFocusChecker(LocationFocusChecker checker)
	{
		this.locationFocusChecker = checker;
		updateClearNavButton();
	}

	private void updateClearNavButton()
	{
		if (clearNavButton == null)
		{
			return;
		}
		boolean hasFocus = locationFocusChecker != null && locationFocusChecker.hasFocus();
		clearNavButton.setEnabled(hasFocus);
		clearNavButton.setForeground(hasFocus ? DmmColors.WARNING : ColorScheme.LIGHT_GRAY_COLOR);
	}
}
