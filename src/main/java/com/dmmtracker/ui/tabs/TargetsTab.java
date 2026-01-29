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

/*
 * DMMScape Companion - Targets Tab
 * Displays and manages plan targets from the DMMScape web app
 */
package com.dmmtracker.ui.tabs;

import com.dmmtracker.ApiConfig;
import com.dmmtracker.DMMTrackerConfig;
import com.dmmtracker.DeviceAuthService;
import com.dmmtracker.RuneliteTokenService;
import com.dmmtracker.SyncService;
import com.dmmtracker.TargetPoint;
import com.dmmtracker.TargetService;
import com.dmmtracker.data.DataLoader;
import com.dmmtracker.ui.DmmColors;
import com.dmmtracker.ui.IconManager;
import com.dmmtracker.ui.ScrollPaneUtils;
import com.dmmtracker.ui.components.LoadingSpinner;
import com.dmmtracker.ui.components.SectionPanel;
import com.dmmtracker.ui.components.UiComponents;
import com.dmmtracker.ui.components.UiIconFactory;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.LinkBrowser;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.EmptyBorder;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Targets tab showing the user's plan targets with completion status.
 * Uses JList with custom cell renderer for efficient rendering of large target lists.
 */
public class TargetsTab extends JPanel
{
	private static final String CONFIG_GROUP = "dmmtracker";
	private static final String CONFIG_SKIP_GUEST_CONFIRM = "skipGuestConfirm";
	private static final String CARD_LIST = "list";
	private static final String CARD_EMPTY = "empty";
	private static final String CARD_FILTERED = "filtered";
	private static final int TEXT_WRAP_WIDTH = 180;
	private static final int SECTION_SPACING = 4;

	private final DataLoader dataLoader;
	private final ConfigManager configManager;
	private final JPanel topPanel = new JPanel();
	private final JLabel syncStatusLabel = new JLabel();
	private final JLabel syncDetailLabel = new JLabel();
	private final JLabel authStatusLabel = new JLabel();
	private final JLabel authDetailLabel = new JLabel();
	private final JLabel authCodeLabel = new JLabel();
	private final JLabel authChoiceLabel = new JLabel();
	private final JLabel tokenErrorLabel = new JLabel();
	private final JLabel planLabel = new JLabel();
	private final JLabel apiKeyLabel = new JLabel();
	private final JLabel targetFetchLabel = new JLabel();
	private final JLabel planHelpLabel = new JLabel();
	private final JCheckBox showCompletedToggle = UiComponents.createCheckBox("Show completed");
	private final JPanel typeFilterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
	private final Set<String> activeTypeFilters = new HashSet<>();
	private final Map<String, JButton> typeFilterButtons = new java.util.LinkedHashMap<>();
	private static final Map<String, Color> TYPE_COLORS = Map.of(
		"boss", DmmColors.CATEGORY_BOSS,
		"diary", DmmColors.TYPE_DIARY,
		"ca", DmmColors.TYPE_CA,
		"sigil", DmmColors.TYPE_SIGIL,
		"lamp", DmmColors.TYPE_LAMP,
		"prayer", DmmColors.INFO,
		"teleport", DmmColors.TYPE_TELEPORT
	);
	private final JButton syncNowButton = new JButton("Sync Now");
	private final JButton refreshTargetsButton = new JButton("Refresh");
	private final LoadingSpinner syncSpinner = new LoadingSpinner(12);
	private final LoadingSpinner refreshSpinner = new LoadingSpinner(12);
	private final JButton guestAccountButton = new JButton("Start as Guest");
	private final JButton linkAccountButton = new JButton("Link Existing Account");
	private final JButton openLinkButton = new JButton("Open Link");
	private final JButton cancelLinkButton = new JButton("Cancel");
	private final JButton unlinkButton = new JButton("Unlink");
	private final JPanel planStatusPanel = new JPanel();
	private final JPanel listContainer = new JPanel(new CardLayout());
	private final JSplitPane splitPane;
	private final JPanel emptyStatePanel = new JPanel();
	private final JPanel filteredStatePanel = new JPanel();
	private final JLabel emptyTitleLabel = new JLabel("No plan yet");
	private final JLabel emptyDetailLabel = new JLabel();
	private final JLabel emptyWarningLabel = new JLabel();
	private final JLabel emptyOrLabel = new JLabel("or");
	private final JButton emptyGuestButton = new JButton("Start as Guest");
	private final JButton emptyLinkButton = new JButton("Link Existing Account");
	private final JButton emptyOpenWebButton = new JButton("Open DMMScape");
	private final JButton emptySettingsButton = UiComponents.createSmallButton("Open Settings (gear)");
	private final JLabel filteredTitleLabel = new JLabel("No matches");
	private final JLabel filteredDetailLabel = new JLabel();
	private final JButton clearFiltersButton = new JButton("Clear filters");

	// Setup Checklist components
	private final JPanel setupChecklistPanel = new JPanel();
	private final JLabel setupTitleLabel = new JLabel("Setup Checklist");
	private final JLabel setupStepLink = new JLabel();
	private final JLabel setupStepSync = new JLabel();
	private final JLabel setupStepPlanSync = new JLabel();
	private final JLabel setupStepPlan = new JLabel();
	private final JLabel setupSummaryLabel = new JLabel();
	private final JPanel setupActionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
	private final JButton setupLinkButton = new JButton("Link Account");
	private final JButton setupGuestButton = new JButton("Start as Guest");
	private final JButton setupEnableSyncButton = new JButton("Enable Auto-Sync");
	private final JButton setupEnablePlanButton = new JButton("Enable Plan Sync");
	private final JButton setupOpenWebButton = new JButton("Open DMMScape");

	private final DefaultListModel<TargetPoint> listModel = new DefaultListModel<>();
	private final JList<TargetPoint> targetList;
	private int hoveredIndex = -1;

	private List<TargetPoint> lastTargets = new ArrayList<>();
	private long lastTargetsTimestamp = -1;
	private long lastLocalUpdateTimestamp = -1;
	private boolean allowEdits = true;
	private boolean filtersInitialized = false;
	private boolean setupCompleteState = false;

	private Runnable manualSyncHandler;
	private Runnable targetRefreshHandler;
	private BiConsumer<TargetPoint, Boolean> targetToggleHandler;
	private java.util.function.Consumer<TargetPoint> targetNavigateHandler;
	private Runnable openWebHandler;
	private Runnable openSettingsHandler;
	private Runnable linkAccountHandler;
	private Runnable guestAccountHandler;
	private DeviceAuthService deviceAuthService;
	private DMMTrackerConfig lastConfig;
	private SectionPanel authSection;
	private SectionPanel syncSection;
	private SectionPanel planSection;
	private final Component setupSpacer = Box.createRigidArea(new Dimension(0, SECTION_SPACING));
	private final Component authSpacer = Box.createRigidArea(new Dimension(0, SECTION_SPACING));
	private final Component syncSpacer = Box.createRigidArea(new Dimension(0, SECTION_SPACING));
	private final Component planSpacer = Box.createRigidArea(new Dimension(0, SECTION_SPACING));

	public TargetsTab(DataLoader dataLoader, ConfigManager configManager)
	{
		this.dataLoader = dataLoader;
		this.configManager = configManager;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		configureSpinner(syncSpinner);
		configureSpinner(refreshSpinner);

		// Top section with sync status and controls
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		topPanel.setBorder(new EmptyBorder(2, 4, 2, 4));

		topPanel.add(buildSetupChecklistSection());
		topPanel.add(setupSpacer);
		authSection = buildAuthSection();
		topPanel.add(authSection);
		topPanel.add(authSpacer);
		syncSection = buildSyncSection();
		topPanel.add(syncSection);
		topPanel.add(syncSpacer);
		planSection = buildTargetInfoSection();
		topPanel.add(planSection);
		topPanel.add(planSpacer);

		// Target list using JList for efficient rendering
		targetList = new JList<>(listModel);
		targetList.setCellRenderer(new TargetCellRenderer());
		targetList.setBackground(ColorScheme.DARK_GRAY_COLOR);
		targetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		targetList.setFixedCellHeight(24);
		targetList.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
		{
			@Override
			public void mouseMoved(java.awt.event.MouseEvent e)
			{
				int index = targetList.locationToIndex(e.getPoint());
				int newHoveredIndex = -1;
				if (index >= 0 && index < listModel.size())
				{
					Rectangle cellBounds = targetList.getCellBounds(index, index);
					if (cellBounds != null && cellBounds.contains(e.getPoint()))
					{
						newHoveredIndex = index;
					}
				}
				if (newHoveredIndex != hoveredIndex)
				{
					hoveredIndex = newHoveredIndex;
					targetList.repaint();
				}
			}
		});
		targetList.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				hoveredIndex = -1;
				targetList.repaint();
			}
		});

		// Handle checkbox clicks via mouse listener
		targetList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (!SwingUtilities.isLeftMouseButton(e))
				{
					return;
				}

				int index = targetList.locationToIndex(e.getPoint());
				if (index < 0 || index >= listModel.size())
				{
					return;
				}

				// Verify the click is within the actual cell bounds (not in blank area)
				Rectangle cellBounds = targetList.getCellBounds(index, index);
				if (cellBounds == null || !cellBounds.contains(e.getPoint()))
				{
					return;
				}

				TargetPoint target = listModel.getElementAt(index);
				// Check if click was in checkbox area (first 28 pixels)
				if (e.getX() - cellBounds.x < 28)
				{
					if (allowEdits && target.isToggleable() && targetToggleHandler != null)
					{
						targetToggleHandler.accept(target, !target.isCompleted());
					}
					return;
				}

				if (targetNavigateHandler != null && target.getWorldPoint() != null)
				{
					targetNavigateHandler.accept(target);
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(targetList);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		ScrollPaneUtils.applyRuneliteStyle(scrollPane);

		listContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listContainer.add(scrollPane, CARD_LIST);
		listContainer.add(buildEmptyStatePanel(), CARD_EMPTY);
		listContainer.add(buildFilteredStatePanel(), CARD_FILTERED);

		splitPane = UiComponents.createVerticalSplit(topPanel, listContainer);
		add(splitPane, BorderLayout.CENTER);

		// Event handlers
		showCompletedToggle.addActionListener(e -> rebuildTargetList());
		syncNowButton.addActionListener(e ->
		{
			if (manualSyncHandler != null) manualSyncHandler.run();
		});
		refreshTargetsButton.addActionListener(e ->
		{
			if (targetRefreshHandler != null) targetRefreshHandler.run();
		});
		emptyGuestButton.addActionListener(e -> guestAccountButton.doClick());
		emptyLinkButton.addActionListener(e -> linkAccountButton.doClick());
		emptyOpenWebButton.addActionListener(e ->
		{
			if (openWebHandler != null)
			{
				openWebHandler.run();
				return;
			}
			LinkBrowser.browse(ApiConfig.webappBase());
		});
		emptySettingsButton.addActionListener(e ->
		{
			if (openSettingsHandler != null)
			{
				openSettingsHandler.run();
			}
		});

		clearFiltersButton.addActionListener(e -> resetFilters());

		authCodeLabel.setVisible(false);
		guestAccountButton.setVisible(false);
		openLinkButton.setVisible(false);
		cancelLinkButton.setVisible(false);
		unlinkButton.setVisible(false);

		linkAccountButton.addActionListener(e ->
		{
			// Use the handler from main plugin if available (it has direct access to deviceAuthService)
			if (linkAccountHandler != null)
			{
				linkAccountHandler.run();
				return;
			}

			// Fallback to local deviceAuthService if handler not set
			if (deviceAuthService == null)
			{
				return;
			}

			if (deviceAuthService.isGuestLinked() && !confirmGuestUpgrade())
			{
				return;
			}

			deviceAuthService.startLinking(new DeviceAuthService.AuthCallback()
			{
				@Override
				public void onStateChange(DeviceAuthService.LinkingState state, String message)
				{
					SwingUtilities.invokeLater(() ->
					{
						updateAuthStatus(deviceAuthService, lastConfig);
						updateEmptyState(lastConfig, deviceAuthService);
					});
				}

				@Override
				public void onSuccess(String username)
				{
					SwingUtilities.invokeLater(() ->
					{
						updateAuthStatus(deviceAuthService, lastConfig);
						updateEmptyState(lastConfig, deviceAuthService);
					});
				}

				@Override
				public void onError(String error)
				{
					SwingUtilities.invokeLater(() ->
					{
						updateAuthStatus(deviceAuthService, lastConfig);
						updateEmptyState(lastConfig, deviceAuthService);
						JOptionPane.showMessageDialog(
							TargetsTab.this,
							"Linking failed: " + (error != null ? error : "Unknown error"),
							"Connection Error",
							JOptionPane.ERROR_MESSAGE
						);
					});
				}
			});

			updateAuthStatus(deviceAuthService, lastConfig);
			updateEmptyState(lastConfig, deviceAuthService);
		});

		guestAccountButton.addActionListener(e ->
		{
			// Use the handler from main plugin if available (it has direct access to deviceAuthService)
			if (guestAccountHandler != null)
			{
				guestAccountHandler.run();
				return;
			}

			// Fallback to local deviceAuthService if handler not set
			if (deviceAuthService == null)
			{
				return;
			}

			if (!confirmGuestStart())
			{
				return;
			}

			startGuestLinking(false);
		});

		openLinkButton.addActionListener(e ->
		{
			if (deviceAuthService == null)
			{
				return;
			}
			String url = deviceAuthService.getVerificationUrl();
			if (url != null && !url.isEmpty())
			{
				LinkBrowser.browse(url);
			}
		});

		cancelLinkButton.addActionListener(e ->
		{
			if (deviceAuthService == null)
			{
				return;
			}
			deviceAuthService.cancelLinking();
			updateAuthStatus(deviceAuthService, lastConfig);
			updateEmptyState(lastConfig, deviceAuthService);
		});

		unlinkButton.addActionListener(e ->
		{
			if (deviceAuthService == null)
			{
				return;
			}
			deviceAuthService.unlink();
			updateAuthStatus(deviceAuthService, lastConfig);
			updateEmptyState(lastConfig, deviceAuthService);
		});
	}

	private SectionPanel buildSyncSection()
	{
		SectionPanel panel = new SectionPanel("Progress Sync");

		JPanel content = panel.getContentPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		syncStatusLabel.setFont(syncStatusLabel.getFont().deriveFont(11f));
		syncDetailLabel.setFont(syncDetailLabel.getFont().deriveFont(10f));
		syncDetailLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		actions.setOpaque(false);
		actions.add(syncNowButton);
		actions.add(syncSpinner);

		content.add(syncStatusLabel);
		content.add(syncDetailLabel);
		content.add(Box.createRigidArea(new Dimension(0, 2)));
		content.add(actions);

		return panel;
	}

	private SectionPanel buildAuthSection()
	{
		SectionPanel panel = new SectionPanel("Account");

		JPanel content = panel.getContentPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		authStatusLabel.setFont(authStatusLabel.getFont().deriveFont(11f));
		authDetailLabel.setFont(authDetailLabel.getFont().deriveFont(10f));
		authDetailLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		authCodeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
		authCodeLabel.setForeground(DmmColors.GOLD);
		authChoiceLabel.setFont(authChoiceLabel.getFont().deriveFont(10f));
		authChoiceLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		authChoiceLabel.setText(wrapHtml(
			"<b>Link Account</b> - get a code to enter at " + ApiConfig.webappHost() + "/link<br>"
				+ "<b>Start as Guest</b> - quick start, link later"
		));
		authChoiceLabel.setVisible(false);
		tokenErrorLabel.setFont(tokenErrorLabel.getFont().deriveFont(10f));
		tokenErrorLabel.setForeground(DmmColors.ERROR);
		tokenErrorLabel.setVisible(false);

		content.add(authStatusLabel);
		content.add(authCodeLabel);  // Code appears prominently right after status
		content.add(authDetailLabel);
		content.add(authChoiceLabel);
		content.add(tokenErrorLabel);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		actions.setOpaque(false);
		actions.add(guestAccountButton);
		actions.add(linkAccountButton);
		actions.add(openLinkButton);
		actions.add(cancelLinkButton);
		actions.add(unlinkButton);

		content.add(Box.createRigidArea(new Dimension(0, 4)));
		content.add(actions);

		return panel;
	}

	private SectionPanel buildTargetInfoSection()
	{
		SectionPanel panel = new SectionPanel("Plan");

		JPanel content = panel.getContentPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		planStatusPanel.setLayout(new BoxLayout(planStatusPanel, BoxLayout.Y_AXIS));
		planStatusPanel.setOpaque(false);

		planLabel.setFont(planLabel.getFont().deriveFont(11f));
		apiKeyLabel.setFont(apiKeyLabel.getFont().deriveFont(10f));
		apiKeyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		targetFetchLabel.setFont(targetFetchLabel.getFont().deriveFont(10f));
		targetFetchLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		planHelpLabel.setFont(planHelpLabel.getFont().deriveFont(10f));
		planHelpLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		planHelpLabel.setText(wrapHtml(
			"Plan sync needs a linked account. " +
				"Click 'Link Existing Account' above to get a code."
		));
		planHelpLabel.setVisible(false);

		planStatusPanel.add(planLabel);
		planStatusPanel.add(apiKeyLabel);
		planStatusPanel.add(targetFetchLabel);
		planStatusPanel.add(planHelpLabel);
		content.add(planStatusPanel);

		JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		controls.setOpaque(false);
		showCompletedToggle.setOpaque(false);
		showCompletedToggle.setForeground(Color.WHITE);
		controls.add(showCompletedToggle);
		controls.add(refreshTargetsButton);
		controls.add(refreshSpinner);

		// Type filter chips
		typeFilterPanel.setOpaque(false);
		buildTypeFilterChips();

		content.add(Box.createRigidArea(new Dimension(0, 2)));
		content.add(controls);
		content.add(typeFilterPanel);

		return panel;
	}

	private JPanel buildSetupChecklistSection()
	{
		setupChecklistPanel.setLayout(new BorderLayout());
		setupChecklistPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setupChecklistPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(DmmColors.INFO),
			new EmptyBorder(4, 4, 4, 4)
		));

		setupTitleLabel.setFont(setupTitleLabel.getFont().deriveFont(Font.BOLD, 11f));
		setupTitleLabel.setForeground(Color.WHITE);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Configure step labels
		configureSetupLabel(setupStepLink);
		configureSetupLabel(setupStepSync);
		configureSetupLabel(setupStepPlanSync);
		configureSetupLabel(setupStepPlan);

		// Set initial text so checklist is visible before first refresh
		setupSummaryLabel.setText("Loading...");
		setSetupStep(setupStepLink, false, "Link account (or API key)");
		setSetupStep(setupStepSync, false, "Enable auto-sync");
		setSetupStep(setupStepPlanSync, false, "Enable plan sync");
		setSetupStep(setupStepPlan, false, "Create a plan on " + ApiConfig.webappHost() + "");

		// Summary label
		setupSummaryLabel.setFont(setupSummaryLabel.getFont().deriveFont(Font.BOLD, 10f));
		setupSummaryLabel.setForeground(DmmColors.PENDING);
		setupSummaryLabel.setAlignmentX(LEFT_ALIGNMENT);

		// Action row
		setupActionRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setupActionRow.setAlignmentX(LEFT_ALIGNMENT);

		// Setup action buttons
		setupLinkButton.addActionListener(e -> linkAccountButton.doClick());
		setupGuestButton.addActionListener(e -> guestAccountButton.doClick());
		setupEnableSyncButton.addActionListener(e ->
		{
			if (configManager != null)
			{
				configManager.setConfiguration(CONFIG_GROUP, "syncEnabled", true);
			}
		});
		setupEnablePlanButton.addActionListener(e ->
		{
			if (configManager != null)
			{
				configManager.setConfiguration(CONFIG_GROUP, "enableTargetSync", true);
			}
		});
		setupOpenWebButton.addActionListener(e ->
		{
			if (openWebHandler != null)
			{
				openWebHandler.run();
			}
			else
			{
				LinkBrowser.browse(ApiConfig.webappBase());
			}
		});

		content.add(setupStepLink);
		content.add(setupStepSync);
		content.add(setupStepPlanSync);
		content.add(setupStepPlan);
		content.add(Box.createRigidArea(new Dimension(0, 2)));
		content.add(setupSummaryLabel);
		content.add(Box.createRigidArea(new Dimension(0, 2)));
		content.add(setupActionRow);

		setupChecklistPanel.add(setupTitleLabel, BorderLayout.NORTH);
		setupChecklistPanel.add(content, BorderLayout.CENTER);

		return setupChecklistPanel;
	}

	private void configureSetupLabel(JLabel label)
	{
		label.setFont(label.getFont().deriveFont(10f));
		label.setAlignmentX(LEFT_ALIGNMENT);
	}

	private void setSetupStep(JLabel label, boolean complete, String text)
	{
		String prefix = complete ? "\u2713 " : "\u2022 ";
		label.setText(prefix + text);
		label.setForeground(complete ? DmmColors.SUCCESS : ColorScheme.LIGHT_GRAY_COLOR);
	}

	private void updateSetupChecklist(
		DMMTrackerConfig config,
		DeviceAuthService authService,
		TargetService targetService,
		boolean isLoggedIn
	)
	{
		if (config == null)
		{
			return;
		}

		boolean hasAuth = (authService != null && authService.isLinked()) ||
			(config.apiKey() != null && !config.apiKey().isEmpty());
		boolean syncEnabled = config.syncEnabled();
		boolean planSyncEnabled = config.enableTargetSync();
		boolean hasPlan = targetService != null &&
			targetService.getTargetsSnapshot() != null &&
			!targetService.getTargetsSnapshot().isEmpty();

		setSetupStep(setupStepLink, hasAuth, "Link account (or API key)");
		setSetupStep(setupStepSync, syncEnabled, "Enable auto-sync");
		setSetupStep(setupStepPlanSync, planSyncEnabled, "Enable plan sync");
		String planStepText = !isLoggedIn && !hasPlan
			? "Login to OSRS"
			: "Create a plan on " + ApiConfig.webappHost() + "";
		setSetupStep(setupStepPlan, hasPlan, planStepText);

		boolean setupComplete = hasAuth && syncEnabled && planSyncEnabled && hasPlan;

		// Determine summary and next action
		String summary;
		if (!hasAuth)
		{
			summary = "Next: link your account";
		}
		else if (!syncEnabled)
		{
			summary = "Next: enable auto-sync";
		}
		else if (!planSyncEnabled)
		{
			summary = "Next: enable plan sync";
		}
		else if (!isLoggedIn && !hasPlan)
		{
			summary = "Next: Login to OSRS";
		}
		else if (!hasPlan)
		{
			summary = "Next: create a plan on " + ApiConfig.webappHost() + "";
		}
		else
		{
			summary = "Setup complete!";
		}
		setupSummaryLabel.setText(summary);
		setupSummaryLabel.setForeground(setupComplete ? DmmColors.SUCCESS : DmmColors.PENDING);
		updateSetupVisibility(setupComplete);

		// Update action row with relevant button
		setupActionRow.removeAll();

		if (!hasAuth)
		{
			setupActionRow.add(setupLinkButton);
			setupActionRow.add(setupGuestButton);
		}
		else if (!syncEnabled)
		{
			setupActionRow.add(setupEnableSyncButton);
		}
		else if (!planSyncEnabled)
		{
			setupActionRow.add(setupEnablePlanButton);
		}
		else if (!hasPlan)
		{
			setupActionRow.add(setupOpenWebButton);
		}
		else
		{
			// Setup complete - show checkmark
			JLabel done = new JLabel("\u2713 All set!");
			done.setForeground(DmmColors.SUCCESS);
			done.setFont(done.getFont().deriveFont(Font.BOLD, 10f));
			setupActionRow.add(done);
		}

		setupActionRow.revalidate();
		setupActionRow.repaint();
	}

	private void updateSetupVisibility(boolean setupComplete)
	{
		boolean showSetup = !setupComplete;
		setupChecklistPanel.setVisible(showSetup);
		setupSpacer.setVisible(showSetup);
		authSection.setVisible(!showSetup);
		authSpacer.setVisible(!showSetup);
		syncSection.setVisible(!showSetup);
		syncSpacer.setVisible(!showSetup);
		planStatusPanel.setVisible(!showSetup);
		planSpacer.setVisible(!showSetup);

		topPanel.revalidate();
		topPanel.repaint();
		updateSetupSplitState(setupComplete);
	}

	private void updateSetupSplitState(boolean setupComplete)
	{
		if (setupCompleteState == setupComplete)
		{
			return;
		}

		setupCompleteState = setupComplete;
		SwingUtilities.invokeLater(() ->
		{
			int min = splitPane.getMinimumDividerLocation();
			if (setupComplete)
			{
				int current = splitPane.getDividerLocation();
				if (current > min + 1)
				{
					splitPane.putClientProperty("dmmtracker.lastDividerLocation", current);
				}
				splitPane.setDividerLocation(min);
				return;
			}

			Integer last = (Integer) splitPane.getClientProperty("dmmtracker.lastDividerLocation");
			int max = splitPane.getMaximumDividerLocation();
			int prefHeight = topPanel.getPreferredSize().height;
			int restore = last != null ? last : Math.max(min, Math.min(prefHeight, max));
			splitPane.setDividerLocation(restore);
		});
	}

	private void buildTypeFilterChips()
	{
		typeFilterPanel.removeAll();
		typeFilterButtons.clear();

		JLabel filterLabel = new JLabel("Filter:");
		filterLabel.setFont(filterLabel.getFont().deriveFont(10f));
		filterLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		typeFilterPanel.add(filterLabel);

		String[] types = {"boss", "diary", "ca", "sigil", "lamp", "prayer", "teleport"};
		String[] labels = {"Boss", "Diary", "CA", "Sigil", "Lamp", "Prayer", "Teleport"};

		for (int i = 0; i < types.length; i++)
		{
			String type = types[i];
			String label = labels[i];
			Color typeColor = TYPE_COLORS.getOrDefault(type, ColorScheme.LIGHT_GRAY_COLOR);

			JButton chip = createTypeFilterChip(label, type, typeColor);
			typeFilterButtons.put(type, chip);
			typeFilterPanel.add(chip);
		}

		typeFilterPanel.revalidate();
	}

	private JButton createTypeFilterChip(String label, String type, Color activeColor)
	{
		JButton chip = new JButton(label);
		chip.setFont(chip.getFont().deriveFont(9f));
		chip.setFocusPainted(false);
		chip.setBorderPainted(false);
		chip.setOpaque(true);
		chip.setMargin(new Insets(1, 4, 1, 4));
		java.awt.FontMetrics fm = chip.getFontMetrics(chip.getFont());
		int textWidth = fm.stringWidth(label);
		int height = Math.max(18, fm.getHeight() + 4);
		int width = textWidth + 20;
		Dimension size = new Dimension(width, height);
		chip.setPreferredSize(size);
		chip.setMinimumSize(size);
		chip.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

		// Initial inactive state
		chip.setBackground(ColorScheme.DARKER_GRAY_COLOR.brighter());
		chip.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		chip.addActionListener(e ->
		{
			if (activeTypeFilters.contains(type))
			{
				activeTypeFilters.remove(type);
				chip.setBackground(ColorScheme.DARKER_GRAY_COLOR.brighter());
				chip.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
			else
			{
				activeTypeFilters.add(type);
				chip.setBackground(activeColor);
				chip.setForeground(Color.WHITE);
			}
			rebuildTargetList();
		});

		return chip;
	}

	private JPanel buildEmptyStatePanel()
	{
		emptyStatePanel.setLayout(new BoxLayout(emptyStatePanel, BoxLayout.Y_AXIS));
		emptyStatePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		emptyStatePanel.setBorder(new EmptyBorder(12, 12, 12, 12));

		emptyTitleLabel.setFont(emptyTitleLabel.getFont().deriveFont(Font.BOLD, 12f));
		emptyTitleLabel.setForeground(Color.WHITE);

		emptyDetailLabel.setFont(emptyDetailLabel.getFont().deriveFont(10f));
		emptyDetailLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		emptyDetailLabel.setText(wrapHtml(
			"Create a plan on " + ApiConfig.webappHost() + ", then link your account here to sync targets. " +
				"Click 'Link Existing Account' to get a code for " + ApiConfig.webappHost() + "/link."
		));

		emptyWarningLabel.setFont(emptyWarningLabel.getFont().deriveFont(10f));
		emptyWarningLabel.setForeground(DmmColors.WARNING);
		emptyWarningLabel.setText(wrapHtml(
			"Guest is for new users. If you've used " + ApiConfig.webappHost() + " before, link instead to avoid split data."
		));

		emptyOrLabel.setFont(emptyOrLabel.getFont().deriveFont(10f));
		emptyOrLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		actions.setOpaque(false);
		actions.add(emptyGuestButton);
		actions.add(emptyOrLabel);
		actions.add(emptyLinkButton);
		actions.add(emptyOpenWebButton);

		JPanel settingsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		settingsRow.setOpaque(false);
		emptySettingsButton.setIcon(UiIconFactory.createGearIcon(DmmColors.INFO, 12));
		emptySettingsButton.setToolTipText("Open plugin settings");
		settingsRow.add(emptySettingsButton);

		emptyStatePanel.add(emptyTitleLabel);
		emptyStatePanel.add(Box.createRigidArea(new Dimension(0, 4)));
		emptyStatePanel.add(emptyDetailLabel);
		emptyStatePanel.add(Box.createRigidArea(new Dimension(0, 6)));
		emptyStatePanel.add(emptyWarningLabel);
		emptyStatePanel.add(Box.createRigidArea(new Dimension(0, 8)));
		emptyStatePanel.add(actions);
		emptyStatePanel.add(Box.createRigidArea(new Dimension(0, 6)));
		emptyStatePanel.add(settingsRow);

		return emptyStatePanel;
	}

	private JPanel buildFilteredStatePanel()
	{
		filteredStatePanel.setLayout(new BoxLayout(filteredStatePanel, BoxLayout.Y_AXIS));
		filteredStatePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		filteredStatePanel.setBorder(new EmptyBorder(12, 12, 12, 12));

		filteredTitleLabel.setFont(filteredTitleLabel.getFont().deriveFont(Font.BOLD, 12f));
		filteredTitleLabel.setForeground(Color.WHITE);

		filteredDetailLabel.setFont(filteredDetailLabel.getFont().deriveFont(10f));
		filteredDetailLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		filteredDetailLabel.setText(wrapHtml("No targets match the current filters."));

		clearFiltersButton.setFont(clearFiltersButton.getFont().deriveFont(10f));
		clearFiltersButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		clearFiltersButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		clearFiltersButton.setFocusPainted(false);
		clearFiltersButton.setBorder(new EmptyBorder(4, 10, 4, 10));
		clearFiltersButton.setAlignmentX(Component.LEFT_ALIGNMENT);

		filteredStatePanel.add(filteredTitleLabel);
		filteredStatePanel.add(Box.createRigidArea(new Dimension(0, 4)));
		filteredStatePanel.add(filteredDetailLabel);
		filteredStatePanel.add(Box.createRigidArea(new Dimension(0, 8)));
		filteredStatePanel.add(clearFiltersButton);

		return filteredStatePanel;
	}

	public void setManualSyncHandler(Runnable handler)
	{
		this.manualSyncHandler = handler;
	}

	public void setTargetRefreshHandler(Runnable handler)
	{
		this.targetRefreshHandler = handler;
	}

	public void setTargetToggleHandler(BiConsumer<TargetPoint, Boolean> handler)
	{
		this.targetToggleHandler = handler;
	}

	public void setTargetNavigateHandler(java.util.function.Consumer<TargetPoint> handler)
	{
		this.targetNavigateHandler = handler;
	}

	public void setOpenWebHandler(Runnable handler)
	{
		this.openWebHandler = handler;
	}

	public void setOpenSettingsHandler(Runnable handler)
	{
		this.openSettingsHandler = handler;
	}

	public void setLinkAccountHandler(Runnable handler)
	{
		this.linkAccountHandler = handler;
	}

	public void setGuestAccountHandler(Runnable handler)
	{
		this.guestAccountHandler = handler;
	}

	public void refresh(
		SyncService syncService,
		TargetService targetService,
		DMMTrackerConfig config,
		DeviceAuthService authService,
		RuneliteTokenService tokenService,
		boolean isLoggedIn
	)
	{
		this.deviceAuthService = authService;
		this.lastConfig = config;

		SwingUtilities.invokeLater(() ->
		{
			// Initialize filter defaults from config on first refresh
			if (!filtersInitialized && config != null)
			{
				showCompletedToggle.setSelected(config.defaultShowCompleted());
				filtersInitialized = true;
			}

			updateSetupChecklist(config, authService, targetService, isLoggedIn);
			updateSyncStatus(syncService);
			updateAuthStatus(authService, config);
			updateTokenStatus(tokenService);
			updateTargetStatus(syncService, targetService, config, authService);
			boolean previousAllowEdits = allowEdits;
			allowEdits = config.allowTargetEdits();

			if (!config.enableTargetSync())
			{
				lastTargets = new ArrayList<>();
				lastTargetsTimestamp = -1;
				lastLocalUpdateTimestamp = -1;
				rebuildTargetList();
				updateEmptyState(config, authService);
				return;
			}

			long targetTimestamp = targetService.getLastFetchTimestamp();
			long localUpdateTimestamp = targetService.getLastLocalUpdateTimestamp();
			if (targetTimestamp != lastTargetsTimestamp || localUpdateTimestamp != lastLocalUpdateTimestamp)
			{
				lastTargetsTimestamp = targetTimestamp;
				lastLocalUpdateTimestamp = localUpdateTimestamp;
				lastTargets = targetService.getTargetsSnapshot();
				rebuildTargetList();
			}

			if (previousAllowEdits != allowEdits)
			{
				rebuildTargetList();
			}

			updateEmptyState(config, authService);
		});
	}

	private void updateSyncStatus(SyncService syncService)
	{
		long now = System.currentTimeMillis();
		String status;
		boolean syncing = syncService.getLastSyncAttemptMs() > syncService.getLastSyncSuccessMs()
			&& now - syncService.getLastSyncAttemptMs() < 30_000;
		if (syncService.getLastSyncError() != null)
		{
			status = "Error";
			syncStatusLabel.setForeground(DmmColors.ERROR);
		}
		else if (syncing)
		{
			status = "Syncing...";
			syncStatusLabel.setForeground(DmmColors.PENDING);
		}
		else if (syncService.getLastSyncSuccessMs() > 0)
		{
			status = "Synced";
			syncStatusLabel.setForeground(DmmColors.SUCCESS);
		}
		else
		{
			status = "Idle";
			syncStatusLabel.setForeground(Color.WHITE);
		}

		syncStatusLabel.setText("Status: " + status);
		if (syncService.getLastSyncError() != null)
		{
			syncDetailLabel.setText("Error: " + syncService.getLastSyncError());
		}
		else if (syncService.getLastSyncSuccessMs() > 0)
		{
			syncDetailLabel.setText("Last: " + formatTimeAgo(syncService.getLastSyncSuccessMs()));
		}
		else
		{
			syncDetailLabel.setText("Last sync: never");
		}

		syncNowButton.setEnabled(!syncing);
		setSpinnerActive(syncSpinner, syncing);
	}

	private void updateAuthStatus(DeviceAuthService authService, DMMTrackerConfig config)
	{
		authCodeLabel.setVisible(false);
		authChoiceLabel.setVisible(false);
		guestAccountButton.setVisible(false);
		linkAccountButton.setVisible(false);
		openLinkButton.setVisible(false);
		cancelLinkButton.setVisible(false);
		unlinkButton.setVisible(false);
		linkAccountButton.setText("Link Existing Account");

		if (authService == null)
		{
			authStatusLabel.setText("Status: unavailable");
			authStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			setAuthDetailText("");
			return;
		}

		if (authService.isLinked())
		{
			if (authService.isGuestLinked())
			{
				authStatusLabel.setText("Status: guest");
				authStatusLabel.setForeground(DmmColors.INFO);
				String username = authService.getLinkedUsername();
				setAuthDetailText(username != null && !username.isEmpty()
					? "Guest: " + username
					: "Guest account");
				authChoiceLabel.setVisible(false);
				linkAccountButton.setText("Upgrade to Account");
				linkAccountButton.setVisible(true);
				unlinkButton.setVisible(true);
				return;
			}
			else
			{
				authStatusLabel.setText("Status: linked");
				authStatusLabel.setForeground(DmmColors.SUCCESS);
				String username = authService.getLinkedUsername();
				setAuthDetailText(username != null && !username.isEmpty()
					? "Account: " + username
					: "Account linked");
				authChoiceLabel.setVisible(false);
				unlinkButton.setVisible(true);
				return;
			}
		}

		DeviceAuthService.LinkingState state = authService.getLinkingState();
		if (state == DeviceAuthService.LinkingState.AWAITING_AUTH || state == DeviceAuthService.LinkingState.POLLING)
		{
			authStatusLabel.setText("Status: awaiting link");
			authStatusLabel.setForeground(DmmColors.PENDING);
			authChoiceLabel.setVisible(false);

			String code = authService.getCurrentUserCode();
			if (code != null && !code.isEmpty())
			{
				authCodeLabel.setText(code);
				authCodeLabel.setVisible(true);

				long remainingMs = authService.getExpiresAt() - System.currentTimeMillis();
				int remainingSeconds = (int) Math.max(0, remainingMs / 1000L);
				setAuthDetailText("Enter this code at " + ApiConfig.webappHost() + "/link\nExpires in " + formatCountdown(remainingSeconds));
			}
			else
			{
				setAuthDetailText("Requesting linking code...");
			}

			if (authService.getVerificationUrl() != null)
			{
				openLinkButton.setVisible(true);
			}
			cancelLinkButton.setVisible(true);
			return;
		}

		if (state == DeviceAuthService.LinkingState.EXPIRED)
		{
			authStatusLabel.setText("Status: expired");
			authStatusLabel.setForeground(DmmColors.WARNING);
			setAuthDetailText("The linking code expired.\nClick 'Link Account' to get a new code.");
			// Don't show choice instructions - just show retry buttons
			authChoiceLabel.setVisible(false);
			guestAccountButton.setVisible(true);
			linkAccountButton.setVisible(true);
			linkAccountButton.setText("Link Account (New Code)");
			return;
		}

		if (state == DeviceAuthService.LinkingState.ERROR)
		{
			authStatusLabel.setText("Status: error");
			authStatusLabel.setForeground(DmmColors.ERROR);
			String error = authService.getLinkingError();
			String errorMsg = error != null ? error : "Linking failed";
			setAuthDetailText(errorMsg + "\nClick 'Link Account' to retry.");
			// Don't show choice instructions during error - just show retry buttons
			authChoiceLabel.setVisible(false);
			guestAccountButton.setVisible(true);
			linkAccountButton.setVisible(true);
			linkAccountButton.setText("Link Account (Retry)");
			return;
		}

		String apiKey = config != null ? config.apiKey() : null;
		if (apiKey != null && !apiKey.isEmpty())
		{
			authStatusLabel.setText("Status: API key");
			authStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			setAuthDetailText("Using API key (advanced).\nYou can still link.");
			authChoiceLabel.setVisible(false);
		}
		else
		{
			authStatusLabel.setText("Status: not linked");
			authStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			setAuthDetailText("Link your account to sync progress and plans.");
			authChoiceLabel.setVisible(true);
		}
		guestAccountButton.setVisible(true);
		linkAccountButton.setVisible(true);
	}

	private void updateTokenStatus(RuneliteTokenService tokenService)
	{
		if (tokenService == null)
		{
			tokenErrorLabel.setVisible(false);
			return;
		}

		String error = tokenService.getLastError();
		if (error != null && !error.isEmpty())
		{
			tokenErrorLabel.setText("Sync token error: " + error);
			tokenErrorLabel.setVisible(true);
		}
		else
		{
			tokenErrorLabel.setVisible(false);
		}
	}

	private void updateTargetStatus(SyncService syncService, TargetService targetService, DMMTrackerConfig config, DeviceAuthService authService)
	{
		boolean targetSyncEnabled = config.enableTargetSync();
		refreshTargetsButton.setEnabled(targetSyncEnabled);
		boolean fetching = targetSyncEnabled && targetService != null && targetService.isFetching();
		setSpinnerActive(refreshSpinner, fetching);
		boolean hasAuth = false;
		if (authService != null && authService.isLinked())
		{
			String username = authService.getLinkedUsername();
			hasAuth = true;
			if (authService.isGuestLinked())
			{
				apiKeyLabel.setText(username != null && !username.isEmpty()
					? "Auth: guest (" + username + ")"
					: "Auth: guest");
			}
			else
			{
				apiKeyLabel.setText(username != null && !username.isEmpty()
					? "Auth: linked as " + username
					: "Auth: linked");
			}
		}
		else if (config.apiKey() == null || config.apiKey().isEmpty())
		{
			apiKeyLabel.setText("Auth: not linked");
		}
		else
		{
			apiKeyLabel.setText("Auth: API key");
			hasAuth = true;
		}

		planHelpLabel.setVisible(!hasAuth);

		if (!targetSyncEnabled)
		{
			planLabel.setText("Plan: disabled");
			targetFetchLabel.setText("Plan sync: disabled");
			targetFetchLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			return;
		}

		String planName = targetService.getPlanName();
		if (planName == null || planName.isEmpty())
		{
			planName = "No plan";
		}
		int totalTargets = targetService.getTargetsSnapshot().size();
		long completed = targetService.getTargetsSnapshot().stream().filter(TargetPoint::isCompleted).count();
		planLabel.setText(planName + " (" + completed + "/" + totalTargets + ")");

		String errorMessage = targetService.getLastErrorMessage();
		if (errorMessage != null)
		{
			targetFetchLabel.setText("Plan sync: " + errorMessage);
			targetFetchLabel.setForeground(DmmColors.ERROR);
		}
		else if (fetching)
		{
			targetFetchLabel.setText("Plan sync: fetching...");
			targetFetchLabel.setForeground(DmmColors.PENDING);
		}
		else if (targetService.getLastSuccessTimestamp() > 0)
		{
			targetFetchLabel.setText("Plan sync: " + formatTimeAgo(targetService.getLastSuccessTimestamp()));
			targetFetchLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		}
		else
		{
			targetFetchLabel.setText("Plan sync: never");
			targetFetchLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		}
	}

	private void updateEmptyState(DMMTrackerConfig config, DeviceAuthService authService)
	{
		if (config == null)
		{
			return;
		}

		boolean targetSyncEnabled = config.enableTargetSync();
		boolean showEmpty = lastTargets.isEmpty();
		boolean showFiltered = !lastTargets.isEmpty() && listModel.isEmpty();
		boolean isLinked = authService != null && authService.isLinked();
		boolean isGuest = authService != null && authService.isGuestLinked();
		boolean hasApiKey = config.apiKey() != null && !config.apiKey().isEmpty();
		boolean hasAuth = isLinked || hasApiKey;
		boolean hideEmptyForSynced = hasAuth && targetSyncEnabled;

		CardLayout layout = (CardLayout) listContainer.getLayout();
		if (showEmpty && !hideEmptyForSynced)
		{
			layout.show(listContainer, CARD_EMPTY);
		}
		else if (showFiltered)
		{
			layout.show(listContainer, CARD_FILTERED);
		}
		else
		{
			layout.show(listContainer, CARD_LIST);
		}

		boolean showGuestOptions = showEmpty && !hideEmptyForSynced && (!isLinked || isGuest);

		emptyGuestButton.setVisible(showGuestOptions);
		emptyLinkButton.setVisible(showGuestOptions);
		emptyOrLabel.setVisible(showGuestOptions);
		emptyWarningLabel.setVisible(showGuestOptions);

		if (showGuestOptions)
		{
			emptyLinkButton.setText(isGuest ? "Upgrade to Account" : "Link Existing Account");
		}

		emptyOpenWebButton.setVisible(showEmpty && !hideEmptyForSynced && isLinked && !isGuest);
		emptySettingsButton.setVisible(showEmpty && !hideEmptyForSynced && (!hasAuth || !targetSyncEnabled));

		if (!targetSyncEnabled)
		{
			emptyDetailLabel.setText(wrapHtml(
				"Target sync is disabled. Enable it in Settings (gear), then link your account or start as guest to sync targets."
			));
		}
		else
		{
			emptyDetailLabel.setText(wrapHtml(
				"Create a plan on " + ApiConfig.webappHost() + ", then link your account here to sync targets. " +
					"Click 'Link Existing Account' to get a code for " + ApiConfig.webappHost() + "/link."
			));
		}

		listContainer.revalidate();
		listContainer.repaint();
	}

	private void handleGuestWebOpen()
	{
		if (deviceAuthService == null)
		{
			return;
		}

		if (deviceAuthService.isGuestLinked())
		{
			openGuestLoginLink();
			return;
		}

		if (!confirmGuestStart())
		{
			return;
		}

		startGuestLinking(true);
	}

	private void startGuestLinking(boolean openWebOnSuccess)
	{
		if (deviceAuthService == null)
		{
			return;
		}

		deviceAuthService.startGuestLinking(new DeviceAuthService.AuthCallback()
		{
			@Override
			public void onStateChange(DeviceAuthService.LinkingState state, String message)
			{
				SwingUtilities.invokeLater(() ->
				{
					updateAuthStatus(deviceAuthService, lastConfig);
					updateEmptyState(lastConfig, deviceAuthService);
				});
			}

			@Override
			public void onSuccess(String username)
			{
				SwingUtilities.invokeLater(() ->
				{
					updateAuthStatus(deviceAuthService, lastConfig);
					updateEmptyState(lastConfig, deviceAuthService);
				});
				if (openWebOnSuccess)
				{
					openGuestLoginLink();
				}
			}

			@Override
			public void onError(String error)
			{
				SwingUtilities.invokeLater(() ->
				{
					updateAuthStatus(deviceAuthService, lastConfig);
					updateEmptyState(lastConfig, deviceAuthService);
					JOptionPane.showMessageDialog(
						TargetsTab.this,
						"Failed to start guest session: " + (error != null ? error : "Unknown error"),
						"Connection Error",
						JOptionPane.ERROR_MESSAGE
					);
				});
			}
		});

		updateAuthStatus(deviceAuthService, lastConfig);
		updateEmptyState(lastConfig, deviceAuthService);
	}

	private void openGuestLoginLink()
	{
		if (deviceAuthService == null)
		{
			return;
		}

		deviceAuthService.requestGuestLoginLink(
			url -> SwingUtilities.invokeLater(() -> LinkBrowser.browse(url)),
			error -> SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
				this,
				"Failed to open guest web login: " + error,
				"Guest Web Login",
				JOptionPane.ERROR_MESSAGE
			))
		);
	}

	private void rebuildTargetList()
	{
		listModel.clear();

		if (lastTargets.isEmpty())
		{
			// Empty state handled by the panel layout
			return;
		}

		List<TargetPoint> sorted = new ArrayList<>(lastTargets);
		sorted.sort(Comparator.comparingInt(TargetPoint::getOrder));

		for (TargetPoint target : sorted)
		{
			// Filter by completion status
			if (!showCompletedToggle.isSelected() && target.isCompleted())
			{
				continue;
			}

			// Filter by type (if any filters are active)
			if (!activeTypeFilters.isEmpty())
			{
				String targetType = target.getType();
				if (targetType == null || !activeTypeFilters.contains(targetType.toLowerCase()))
				{
					continue;
				}
			}

			listModel.addElement(target);
		}

		updateEmptyState(lastConfig, deviceAuthService);
	}

	private void resetFilters()
	{
		activeTypeFilters.clear();
		for (JButton chip : typeFilterButtons.values())
		{
			chip.setBackground(ColorScheme.DARKER_GRAY_COLOR.brighter());
			chip.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		}
		showCompletedToggle.setSelected(true);
		rebuildTargetList();
	}

	private void configureSpinner(LoadingSpinner spinner)
	{
		spinner.setSpinnerColor(DmmColors.PENDING);
		spinner.setTrackColor(ColorScheme.MEDIUM_GRAY_COLOR);
		spinner.setVisible(false);
	}

	private void setSpinnerActive(LoadingSpinner spinner, boolean active)
	{
		if (active)
		{
			spinner.setVisible(true);
			if (!spinner.isSpinning())
			{
				spinner.start();
			}
		}
		else
		{
			spinner.stop();
			spinner.setVisible(false);
		}
	}

	private String formatTimeAgo(long timestamp)
	{
		long delta = System.currentTimeMillis() - timestamp;
		if (delta < 0) return "just now";
		long seconds = delta / 1000;
		if (seconds < 60) return seconds + "s ago";
		long minutes = seconds / 60;
		if (minutes < 60) return minutes + "m ago";
		long hours = minutes / 60;
		return hours + "h ago";
	}

	private String formatCountdown(int seconds)
	{
		int minutes = Math.max(0, seconds) / 60;
		int secs = Math.max(0, seconds) % 60;
		return minutes + ":" + (secs < 10 ? "0" + secs : String.valueOf(secs));
	}

	private boolean shouldShowGuestConfirm()
	{
		if (configManager == null)
		{
			return true;
		}
		String value = configManager.getConfiguration(CONFIG_GROUP, CONFIG_SKIP_GUEST_CONFIRM);
		return value == null || !"true".equalsIgnoreCase(value);
	}

	private boolean confirmGuestStart()
	{
		if (!shouldShowGuestConfirm())
		{
			return true;
		}

		JCheckBox dontShowAgain = UiComponents.createCheckBox("Don't show again");

		JLabel message = new JLabel(
			"<html>We'll create a temporary DMMScape guest account to save your progress. " +
				"When you later link a full account, your guest data will be merged into it.<br>" +
				"Guest login clears any unsaved DMMScape data in your browser!<br>" +
				"If you've already used " + ApiConfig.webappHost() + " with a real account, use <b>Link Existing Account</b> instead.</html>"
		);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);
		message.setAlignmentX(Component.LEFT_ALIGNMENT);
		dontShowAgain.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(message);
		panel.add(Box.createRigidArea(new Dimension(0, 6)));
		panel.add(dontShowAgain);

		int choice = JOptionPane.showOptionDialog(
			this,
			panel,
			"Start as Guest",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.INFORMATION_MESSAGE,
			null,
			new Object[] { "Continue as Guest", "Cancel" },
			"Continue as Guest"
		);

		if (choice == JOptionPane.YES_OPTION && dontShowAgain.isSelected() && configManager != null)
		{
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_SKIP_GUEST_CONFIRM, true);
		}

		return choice == JOptionPane.YES_OPTION;
	}

	private boolean confirmGuestUpgrade()
	{
		int choice = JOptionPane.showOptionDialog(
			this,
			"<html>When you approve the code on " + ApiConfig.webappHost() + "/link, your guest data will be merged into that account.</html>",
			"Upgrade to Account",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.INFORMATION_MESSAGE,
			null,
			new Object[] { "Continue", "Cancel" },
			"Continue"
		);

		return choice == JOptionPane.YES_OPTION;
	}

	private void setAuthDetailText(String text)
	{
		if (text == null || text.isEmpty())
		{
			authDetailLabel.setText("");
			return;
		}
		authDetailLabel.setText(wrapHtmlEscaped(text));
	}

	private static String wrapHtml(String text)
	{
		if (text == null || text.isEmpty())
		{
			return "";
		}
		return "<html><body style='width: " + TEXT_WRAP_WIDTH + "px'>" + text + "</body></html>";
	}

	private static String wrapHtmlEscaped(String text)
	{
		if (text == null || text.isEmpty())
		{
			return "";
		}
		String safe = escapeHtml(text).replace("\n", "<br>");
		return wrapHtml(safe);
	}

	private static String escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	/**
	 * Custom cell renderer for target list items.
	 * Renders checkbox, type icon, order number, name, and completion source indicator.
	 */
	private class TargetCellRenderer extends JPanel implements ListCellRenderer<TargetPoint>
	{
		private final JCheckBox checkbox = UiComponents.createCheckBox();
		private final JLabel typeIcon = new JLabel();
		private final JLabel orderLabel = new JLabel();
		private final JLabel nameLabel = new JLabel();
		private final JLabel sourceLabel = new JLabel();

		public TargetCellRenderer()
		{
			setLayout(new BorderLayout(4, 0));
			setBackground(ColorScheme.DARK_GRAY_COLOR);
			setBorder(new EmptyBorder(3, 6, 3, 6));

			// Icon panel
			typeIcon.setPreferredSize(new Dimension(18, 18));

			// Order number
			orderLabel.setFont(orderLabel.getFont().deriveFont(Font.BOLD, 10f));
			orderLabel.setForeground(DmmColors.INFO);
			orderLabel.setPreferredSize(new Dimension(24, 18));

			// Name label
			nameLabel.setFont(nameLabel.getFont().deriveFont(11f));

			// Source indicator
			sourceLabel.setFont(sourceLabel.getFont().deriveFont(9f));

			// Build layout
			JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
			leftPanel.setOpaque(false);
			leftPanel.add(checkbox);
			leftPanel.add(typeIcon);
			leftPanel.add(orderLabel);

			JPanel centerPanel = new JPanel(new BorderLayout(4, 0));
			centerPanel.setOpaque(false);
			centerPanel.add(nameLabel, BorderLayout.CENTER);
			centerPanel.add(sourceLabel, BorderLayout.EAST);

			add(leftPanel, BorderLayout.WEST);
			add(centerPanel, BorderLayout.CENTER);
		}

		@Override
		public Component getListCellRendererComponent(
			JList<? extends TargetPoint> list,
			TargetPoint target,
			int index,
			boolean isSelected,
			boolean cellHasFocus
		)
		{
			checkbox.setSelected(target.isCompleted());
			checkbox.setEnabled(allowEdits && target.isToggleable());

			// Type icon
			ImageIcon icon = IconManager.getTargetTypeIcon(target.getType(), 16);
			typeIcon.setIcon(icon);

			// Order number
			orderLabel.setText("#" + (target.getOrder() + 1));

			// Name
			nameLabel.setText(target.getName());

			// Completion source indicator
			String source = target.getCompletionSource();
			if ("progress".equalsIgnoreCase(source))
			{
				sourceLabel.setText("[auto]");
				sourceLabel.setForeground(DmmColors.SUCCESS);
			}
			else if ("custom".equalsIgnoreCase(source))
			{
				sourceLabel.setText("[plan]");
				sourceLabel.setForeground(DmmColors.INFO);
			}
			else
			{
				sourceLabel.setText("");
			}

			// Colors based on completion
			if (target.isCompleted())
			{
				nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
				orderLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
			}
			else
			{
				nameLabel.setForeground(Color.WHITE);
				orderLabel.setForeground(DmmColors.INFO);
			}

			// Tooltip
			StringBuilder tooltip = new StringBuilder(target.getName());
			if (target.getType() != null)
			{
				tooltip.append(" (").append(target.getType()).append(")");
			}
			if (target.getCategory() != null && !target.getCategory().isEmpty())
			{
				tooltip.append(" - ").append(target.getCategory());
			}
			setToolTipText(tooltip.toString());

			boolean hovered = index == hoveredIndex && !isSelected;
			setBackground(UiComponents.applyHoverBackground(ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR, hovered, isSelected));
			return this;
		}
	}
}
