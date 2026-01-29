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

import com.dmmtracker.DMMTrackerConfig;
import com.dmmtracker.ApiConfig;
import com.dmmtracker.DMMTrackerPlugin;
import com.dmmtracker.DeviceAuthService;
import com.dmmtracker.ProgressStore;
import com.dmmtracker.RequirementChecker;
import com.dmmtracker.RuneliteTokenService;
import com.dmmtracker.SyncService;
import com.dmmtracker.TargetService;
import com.dmmtracker.data.BossData;
import com.dmmtracker.data.DataLoader;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import com.dmmtracker.ui.components.UiIconFactory;
import com.dmmtracker.ui.tabs.BossesTab;
import com.dmmtracker.ui.tabs.CombatAchievementsTab;
import com.dmmtracker.ui.tabs.DiariesTab;
import com.dmmtracker.ui.tabs.GlobalSearchTab;
import com.dmmtracker.ui.tabs.SigilsTab;
import com.dmmtracker.ui.tabs.TargetsTab;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

/**
 * Main tabbed panel for DMMScape plugin.
 * Contains tabs for: Targets, Bosses, Sigils, Diaries, CAs
 * Search is accessible via header icon, settings via gear icon.
 */
public class DMMTabbedPanel extends PluginPanel
{
	private static final String VIEW_MAIN = "main";
	private static final String VIEW_SEARCH = "search";
	private static final String VIEW_SETTINGS = "settings";
	private static final int TAB_ICON_SIZE = 18;
	private static final int PLAN_TAB_ICON_SIZE = TAB_ICON_SIZE * 2;
	private static final Color TAB_BG_DEFAULT = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color TAB_BG_HOVER = ColorScheme.DARK_GRAY_HOVER_COLOR;
	private static final Color TAB_BG_SELECTED = ColorScheme.MEDIUM_GRAY_COLOR;

	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);

	private final Client client;
	private final JTabbedPane tabs;
	private final Map<JComponent, JButton> tabButtons = new HashMap<>();
	private final TargetsTab targetsTab;
	private final BossesTab bossesTab;
	private final SigilsTab sigilsTab;
	private final DiariesTab diariesTab;
	private final CombatAchievementsTab caTab;
	private final GlobalSearchTab searchTab;
	private final SettingsPanel settingsPanel;

	private final JButton recentButton = new JButton();
	private final JLabel syncStatusPill = new JLabel();
	private final JButton searchButton = new JButton();
	private final JButton settingsButton = new JButton();
	private final JPopupMenu recentMenu = new JPopupMenu();

	private Runnable manualSyncHandler;
	private Runnable forceSyncHandler;
	private Runnable targetRefreshHandler;
	private BiConsumer<com.dmmtracker.TargetPoint, Boolean> targetToggleHandler;
	private java.util.function.Consumer<com.dmmtracker.TargetPoint> targetNavigateHandler;
	private Runnable openWebHandler;
	private Runnable unsyncHandler;
	private Runnable unlinkHandler;
	private DMMTrackerConfig lastConfig;
	private DeviceAuthService lastAuthService;

	private String currentView = VIEW_MAIN;
	private int lastTabIndex = 0;

	@Inject
	public DMMTabbedPanel(
		DataLoader dataLoader,
		ProgressStore progressStore,
		ConfigManager configManager,
		Client client,
		RequirementChecker requirementChecker
	)
	{
		this.client = client;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header with title and icons
		JPanel header = createHeader();
		add(header, BorderLayout.NORTH);

		// Initialize tabs
		tabs = new JTabbedPane();
		tabs.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabs.setForeground(Color.WHITE);

		// Requirement checker for skill-based filtering
		// Falls back to DMM base stats (level 1, 10 HP) + autocompleted quests when not logged in
		requirementChecker.setDmmAutoCompletedQuests(dataLoader.getDmmAutoCompletedQuests());

		targetsTab = new TargetsTab(dataLoader, configManager);
		bossesTab = new BossesTab(dataLoader, progressStore, requirementChecker);
		sigilsTab = new SigilsTab(dataLoader, progressStore);
		diariesTab = new DiariesTab(dataLoader, progressStore, requirementChecker);
		caTab = new CombatAchievementsTab(dataLoader, progressStore, requirementChecker);
		searchTab = new GlobalSearchTab(dataLoader, progressStore);
		searchTab.setNavigationHandler(this::navigateToTabAndItem);
		settingsPanel = new SettingsPanel(configManager);

		// Set up settings panel handlers
		settingsPanel.setCloseHandler(this::showMainView);
		targetsTab.setOpenSettingsHandler(this::toggleSettingsView);

		ImageIcon bossesIcon = getTabIcon(IconManager.CATEGORY_BOSS, "B", new Color(0xE74C3C));
		ImageIcon sigilsIcon = getTabIcon(IconManager.SECTION_SIGIL, "$", new Color(0xF39C12));
		ImageIcon diariesIcon = getTabIcon(IconManager.SECTION_DIARY, "D", new Color(0x9B59B6));
		ImageIcon caIcon = getTabIcon(IconManager.SECTION_CA, "C", new Color(0x1ABC9C));
		ImageIcon planIcon = getTabIcon(IconManager.SECTION_PLAN, "P", new Color(0x27AE60), PLAN_TAB_ICON_SIZE);

		// Add tabs for content (tab buttons are rendered separately for layout control)
		tabs.addTab(null, bossesIcon, bossesTab);
		tabs.setToolTipTextAt(tabs.indexOfComponent(bossesTab), "Bosses");

		tabs.addTab(null, sigilsIcon, sigilsTab);
		tabs.setToolTipTextAt(tabs.indexOfComponent(sigilsTab), "Sigils / Lamps / Prayers");

		tabs.addTab(null, diariesIcon, diariesTab);
		tabs.setToolTipTextAt(tabs.indexOfComponent(diariesTab), "Achievement Diaries");

		tabs.addTab(null, caIcon, caTab);
		tabs.setToolTipTextAt(tabs.indexOfComponent(caTab), "Combat Achievements");

		tabs.addTab(null, planIcon, targetsTab);
		tabs.setToolTipTextAt(tabs.indexOfComponent(targetsTab), "Plan");

		JPanel tabBar = createTabBar(bossesIcon, sigilsIcon, diariesIcon, caIcon, planIcon);
		hideTabbedPaneHeader(tabs);

		// Track tab changes
		tabs.addChangeListener(e ->
		{
			lastTabIndex = tabs.getSelectedIndex();
			updateTabSelection();
		});

		// Select targets tab by default
		tabs.setSelectedComponent(targetsTab);
		updateTabSelection();

		// Main view with tabs
		JPanel mainView = new JPanel(new BorderLayout());
		mainView.setBackground(ColorScheme.DARK_GRAY_COLOR);
		mainView.add(tabBar, BorderLayout.NORTH);
		mainView.add(tabs, BorderLayout.CENTER);

		// Card panel for switching views
		cardPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		cardPanel.add(mainView, VIEW_MAIN);
		cardPanel.add(searchTab, VIEW_SEARCH);
		cardPanel.add(settingsPanel, VIEW_SETTINGS);

		add(cardPanel, BorderLayout.CENTER);
	}

	private JPanel createHeader()
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(new EmptyBorder(6, 8, 6, 8));

		String versionTag = ApiConfig.versionTag(DMMTrackerPlugin.PLUGIN_VERSION);
		String titleText = versionTag.isEmpty() ? "DMMScape" : "DMMScape " + versionTag;
		JLabel title = new JLabel(titleText);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
		title.setForeground(Color.WHITE);

		// Search button (positioned left, next to logo)
		searchButton.setIcon(UiIconFactory.createSearchIcon(new Color(0x3498DB), 16));
		searchButton.setToolTipText("Search all content");
		installHeaderButton(searchButton);
		searchButton.addActionListener(e -> toggleSearchView());

		// Recent sync button
		recentButton.setIcon(UiIconFactory.createRefreshIcon(new Color(0x3498DB), 16));
		recentButton.setToolTipText("Recent sync activity");
		installHeaderButton(recentButton);
		recentButton.addActionListener(e -> recentMenu.show(recentButton, 0, recentButton.getHeight()));

		JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		leftGroup.setOpaque(false);
		ImageIcon logoIcon = IconManager.getIcon(IconManager.DMMSCAPE_LOGO, 20, 20);
		if (logoIcon != null)
		{
			JLabel logoLabel = new JLabel(logoIcon);
			leftGroup.add(logoLabel);
		}
		leftGroup.add(searchButton); // Search icon next to logo
		leftGroup.add(title);

		// Sync status pill
		syncStatusPill.setFont(syncStatusPill.getFont().deriveFont(Font.BOLD, 9f));
		syncStatusPill.setOpaque(true);
		syncStatusPill.setBorder(new EmptyBorder(2, 6, 2, 6));
		syncStatusPill.setVisible(false); // Hidden by default, shown when there's a status

		// Settings button
		settingsButton.setIcon(UiIconFactory.createBadgeIcon("\u2699", new Color(0x95A5A6), 16));
		settingsButton.setToolTipText("Settings");
		installHeaderButton(settingsButton);
		settingsButton.addActionListener(e -> toggleSettingsView());

		JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		rightGroup.setOpaque(false);
		rightGroup.add(syncStatusPill);
		rightGroup.add(recentButton);
		rightGroup.add(settingsButton);

		header.add(leftGroup, BorderLayout.WEST);
		header.add(rightGroup, BorderLayout.EAST);

		return header;
	}

	private JPanel createTabBar(ImageIcon bossesIcon, ImageIcon sigilsIcon, ImageIcon diariesIcon, ImageIcon caIcon, ImageIcon planIcon)
	{
		JPanel bar = new JPanel();
		bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
		bar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bar.setBorder(new EmptyBorder(0, 4, 4, 4));

		JPanel topRow = new JPanel(new GridLayout(1, 4, 2, 0));
		topRow.setOpaque(false);
		topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

		JPanel bottomRow = new JPanel(new GridLayout(1, 1, 2, 0));
		bottomRow.setOpaque(false);
		bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		bottomRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

		addTabButton(topRow, bossesTab, bossesIcon, "Bosses");
		addTabButton(topRow, sigilsTab, sigilsIcon, "Sigils / Lamps / Prayers");
		addTabButton(topRow, diariesTab, diariesIcon, "Achievement Diaries");
		addTabButton(topRow, caTab, caIcon, "Combat Achievements");
		addTabButton(bottomRow, targetsTab, planIcon, "Plan", 2);

		bar.add(topRow);
		bar.add(Box.createVerticalStrut(2));
		bar.add(bottomRow);

		return bar;
	}

	private void addTabButton(JPanel row, JComponent tabComponent, ImageIcon icon, String tooltip)
	{
		addTabButton(row, tabComponent, icon, tooltip, 4);
	}

	private void addTabButton(JPanel row, JComponent tabComponent, ImageIcon icon, String tooltip, int verticalPadding)
	{
		JButton button = new JButton(icon);
		button.setToolTipText(tooltip);
		installTabButton(button, verticalPadding);
		button.addActionListener(e -> tabs.setSelectedComponent(tabComponent));
		row.add(button);
		tabButtons.put(tabComponent, button);
	}

	private void installTabButton(JButton button, int verticalPadding)
	{
		SwingUtil.removeButtonDecorations(button);
		button.setFocusPainted(false);
		button.setOpaque(true);
		button.setBackground(TAB_BG_DEFAULT);
		button.setBorder(new EmptyBorder(verticalPadding, 6, verticalPadding, 6));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseEntered(java.awt.event.MouseEvent evt)
			{
				if (!isTabButtonSelected(button))
				{
					button.setBackground(TAB_BG_HOVER);
				}
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent evt)
			{
				updateTabButtonState(button, isTabButtonSelected(button));
			}
		});
	}

	private void updateTabSelection()
	{
		Component selected = tabs.getSelectedComponent();
		for (Map.Entry<JComponent, JButton> entry : tabButtons.entrySet())
		{
			updateTabButtonState(entry.getValue(), entry.getKey() == selected);
		}
	}

	private void updateTabButtonState(JButton button, boolean selected)
	{
		button.putClientProperty("selected", selected);
		button.setBackground(selected ? TAB_BG_SELECTED : TAB_BG_DEFAULT);
	}

	private boolean isTabButtonSelected(JButton button)
	{
		return Boolean.TRUE.equals(button.getClientProperty("selected"));
	}

	private void hideTabbedPaneHeader(JTabbedPane pane)
	{
		pane.setUI(new BasicTabbedPaneUI()
		{
			@Override
			protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight)
			{
				return 0;
			}

			@Override
			protected int calculateTabAreaWidth(int tabPlacement, int vertRunCount, int maxTabWidth)
			{
				return 0;
			}

			@Override
			protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex)
			{
			}
		});
	}

	private void installHeaderButton(JButton button)
	{
		SwingUtil.removeButtonDecorations(button);
		button.setFocusPainted(false);
		button.setOpaque(true);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setBorder(new EmptyBorder(4, 4, 4, 4));
		button.setPreferredSize(new Dimension(24, 24));
		button.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseEntered(java.awt.event.MouseEvent evt)
			{
				button.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent evt)
			{
				button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
	}

	private void toggleSearchView()
	{
		if (VIEW_SEARCH.equals(currentView))
		{
			showMainView();
		}
		else
		{
			currentView = VIEW_SEARCH;
			cardLayout.show(cardPanel, VIEW_SEARCH);
			searchTab.focusSearch();
		}
	}

	private void toggleSettingsView()
	{
		if (VIEW_SETTINGS.equals(currentView))
		{
			showMainView();
		}
		else
		{
			currentView = VIEW_SETTINGS;
			cardLayout.show(cardPanel, VIEW_SETTINGS);
		}
	}

	private void showMainView()
	{
		currentView = VIEW_MAIN;
		cardLayout.show(cardPanel, VIEW_MAIN);
	}

	/**
	 * Navigates to a specific tab and optionally expands/scrolls to a specific item.
	 * @param tabName The tab to navigate to: "bosses", "sigils", "diaries", "cas"
	 * @param itemId The item identifier to expand/scroll to (category name, boss name, etc.)
	 */
	private void navigateToTabAndItem(String tabName, String itemId)
	{
		showMainView();

		switch (tabName.toLowerCase())
		{
			case "bosses":
				bossesTab.expandCategory(itemId);
				tabs.setSelectedComponent(bossesTab);
				break;
			case "sigils":
				sigilsTab.expandCategory(itemId);
				tabs.setSelectedComponent(sigilsTab);
				break;
			case "diaries":
				diariesTab.expandCategory(itemId);
				tabs.setSelectedComponent(diariesTab);
				break;
			case "cas":
				caTab.expandCategory(itemId);
				tabs.setSelectedComponent(caTab);
				break;
			default:
				tabs.setSelectedComponent(targetsTab);
		}
	}

	/**
	 * Gets a tab icon, preferring actual icons with fallback to generated letters.
	 */
	private ImageIcon getTabIcon(String iconName, String fallbackLetter, Color fallbackColor)
	{
		return getTabIcon(iconName, fallbackLetter, fallbackColor, TAB_ICON_SIZE);
	}

	private ImageIcon getTabIcon(String iconName, String fallbackLetter, Color fallbackColor, int size)
	{
		ImageIcon icon = IconManager.getIcon(iconName, size, size);
		if (icon != null)
		{
			return icon;
		}
		return createLetterIcon(fallbackLetter, fallbackColor, size);
	}

	/**
	 * Creates a simple letter icon in a colored circle.
	 */
	private ImageIcon createLetterIcon(String letter, Color color, int size)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g.setColor(color);
		g.fillOval(0, 0, size - 1, size - 1);

		g.setColor(Color.WHITE);
		int fontSize = Math.round(size * 0.6f);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
		FontMetrics fm = g.getFontMetrics();
		int x = (size - fm.stringWidth(letter)) / 2;
		int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
		g.drawString(letter, x, y);

		g.dispose();
		return new ImageIcon(img);
	}

	// ========== HANDLER SETTERS ==========

	public void setManualSyncHandler(Runnable handler)
	{
		this.manualSyncHandler = handler;
		targetsTab.setManualSyncHandler(handler);
		settingsPanel.setManualSyncHandler(handler);
	}

	public void setForceSyncHandler(Runnable handler)
	{
		this.forceSyncHandler = handler;
	}

	public void setTargetRefreshHandler(Runnable handler)
	{
		this.targetRefreshHandler = handler;
		targetsTab.setTargetRefreshHandler(handler);
	}

	public void setTargetToggleHandler(BiConsumer<com.dmmtracker.TargetPoint, Boolean> handler)
	{
		this.targetToggleHandler = handler;
		targetsTab.setTargetToggleHandler(handler);
	}

	public void setTargetNavigateHandler(java.util.function.Consumer<com.dmmtracker.TargetPoint> handler)
	{
		this.targetNavigateHandler = handler;
		targetsTab.setTargetNavigateHandler(handler);
	}

	public void setOpenWebHandler(Runnable handler)
	{
		this.openWebHandler = handler;
		settingsPanel.setOpenWebHandler(handler);
		targetsTab.setOpenWebHandler(handler);
	}

	public void setUnsyncHandler(Runnable handler)
	{
		this.unsyncHandler = handler;
		settingsPanel.setUnsyncHandler(handler);
	}

	public void setUnlinkHandler(Runnable handler)
	{
		this.unlinkHandler = handler;
		settingsPanel.setUnlinkHandler(handler);
	}

	public void setLinkAccountHandler(Runnable handler)
	{
		settingsPanel.setLinkHandler(handler);
		targetsTab.setLinkAccountHandler(handler);
	}

	public void setGuestAccountHandler(Runnable handler)
	{
		targetsTab.setGuestAccountHandler(handler);
	}

	public void setBackToPlanHandler(Runnable handler)
	{
		settingsPanel.setBackToPlanHandler(() ->
		{
			// Switch to Plan/Targets tab
			tabs.setSelectedComponent(targetsTab);
			if (handler != null)
			{
				handler.run();
			}
		});
	}

	/**
	 * Sets the handler for adding a boss to the plan via API.
	 */
	public void setBossAddToPlanHandler(Consumer<BossData> handler)
	{
		bossesTab.setAddToPlanHandler(handler);
	}

	public void setDiaryLocationHandler(DiariesTab.LocationNavigateHandler handler)
	{
		diariesTab.setLocationNavigateHandler(handler);
	}

	public void setDiaryLocationFocusChecker(DiariesTab.LocationFocusChecker checker)
	{
		diariesTab.setLocationFocusChecker(checker);
	}

	public void setClearNavigationHandler(Runnable handler)
	{
		diariesTab.setClearNavigationHandler(handler);
	}

	public void refreshDiaryTab()
	{
		diariesTab.refresh();
	}

	public boolean isPlanTabActive()
	{
		return VIEW_MAIN.equals(currentView) && tabs.getSelectedComponent() == targetsTab;
	}

	// ========== REFRESH ==========

	public void refresh(
		SyncService syncService,
		TargetService targetService,
		DMMTrackerConfig config,
		DeviceAuthService deviceAuthService,
		RuneliteTokenService tokenService
	)
	{
		lastConfig = config;
		lastAuthService = deviceAuthService;
		caTab.initializeFilters(config);

		boolean loggedIn = client != null && client.getGameState() == GameState.LOGGED_IN;
		targetsTab.refresh(syncService, targetService, config, deviceAuthService, tokenService, loggedIn);
		sigilsTab.refresh();
		diariesTab.refresh();
		caTab.refresh();
		bossesTab.refresh();
		searchTab.refresh();
		updateRecentMenu(syncService, targetService);
		updateSyncStatusPill(syncService);
		updateSettingsPanel(config, deviceAuthService);
	}

	/**
	 * Lightweight refresh - updates status indicators without rebuilding tabs.
	 */
	public void refreshStatusOnly(SyncService syncService, TargetService targetService)
	{
		updateSyncStatusPill(syncService);
		updateRecentMenu(syncService, targetService);
		if (lastConfig != null)
		{
			updateSettingsPanel(lastConfig, lastAuthService);
		}
	}

	private void updateSettingsPanel(DMMTrackerConfig config, DeviceAuthService authService)
	{
		if (config == null)
		{
			return;
		}

		boolean hasApiKey = config.apiKey() != null && !config.apiKey().isEmpty();
		boolean isLinked = authService != null && authService.isLinked();
		boolean isGuest = authService != null && authService.isGuestLinked();
		boolean connected = isLinked || hasApiKey;

		String statusText;
		String detailText;

		if (authService != null)
		{
			DeviceAuthService.LinkingState state = authService.getLinkingState();

			if (state == DeviceAuthService.LinkingState.ERROR)
			{
				statusText = "Status: Error";
				String error = authService.getLinkingError();
				detailText = error != null ? error : "Linking failed. Check network and try again.";
				settingsPanel.setConnectionState(false, statusText, detailText, false);
				settingsPanel.setSyncActionState(false, false);
				settingsPanel.setOverlaySettings(config);
				return;
			}

			if (state == DeviceAuthService.LinkingState.AWAITING_AUTH
				|| state == DeviceAuthService.LinkingState.POLLING)
			{
				statusText = "Status: Linking...";
				String code = authService.getCurrentUserCode();
				if (code != null && !code.isEmpty())
				{
					detailText = "Code: " + code + " - Enter at " + ApiConfig.webappHost() + "/link";
				}
				else
				{
					detailText = "Waiting for response...";
				}
				settingsPanel.setConnectionState(false, statusText, detailText, false);
				settingsPanel.setSyncActionState(false, false);
				settingsPanel.setOverlaySettings(config);
				return;
			}

			if (state == DeviceAuthService.LinkingState.EXPIRED)
			{
				statusText = "Status: Expired";
				detailText = "Code expired. Click 'Link / Upgrade Account' to try again.";
				settingsPanel.setConnectionState(false, statusText, detailText, false);
				settingsPanel.setSyncActionState(false, false);
				settingsPanel.setOverlaySettings(config);
				return;
			}
		}

		if (isLinked)
		{
			String username = authService.getLinkedUsername();
			if (isGuest)
			{
				statusText = "Status: Guest linked";
				detailText = username != null && !username.isEmpty()
					? "Guest: " + username
					: "Guest account linked";
			}
			else
			{
				statusText = "Status: Linked";
				detailText = username != null && !username.isEmpty()
					? "Account: " + username
					: "Account linked";
			}
		}
		else if (hasApiKey)
		{
			statusText = "Status: API key";
			detailText = "Advanced mode (no guest merge).";
		}
		else
		{
			statusText = "Status: Not connected";
			detailText = "Click 'Link / Upgrade Account' in Settings or Plan tab.";
		}

		settingsPanel.setConnectionState(connected, statusText, detailText, isGuest);
		settingsPanel.setSyncActionState(connected, isLinked);
		settingsPanel.setOverlaySettings(config);
	}

	private void updateSyncStatusPill(SyncService syncService)
	{
		if (syncService == null)
		{
			syncStatusPill.setVisible(false);
			return;
		}

		long now = System.currentTimeMillis();
		String lastError = syncService.getLastSyncError();
		long lastAttempt = syncService.getLastSyncAttemptMs();
		long lastSuccess = syncService.getLastSyncSuccessMs();

		if (lastError != null)
		{
			// Error state
			syncStatusPill.setText("Error");
			syncStatusPill.setBackground(new Color(0xE74C3C));
			syncStatusPill.setForeground(Color.WHITE);
			syncStatusPill.setToolTipText("Sync error: " + lastError);
			syncStatusPill.setVisible(true);
		}
		else if (lastAttempt > lastSuccess && now - lastAttempt < 30_000)
		{
			// Syncing state
			syncStatusPill.setText("Syncing...");
			syncStatusPill.setBackground(new Color(0xF39C12));
			syncStatusPill.setForeground(Color.WHITE);
			syncStatusPill.setToolTipText("Sync in progress");
			syncStatusPill.setVisible(true);
		}
		else if (lastSuccess > 0)
		{
			// Synced state - show briefly then hide
			long timeSinceSync = now - lastSuccess;
			if (timeSinceSync < 5000)
			{
				// Show "Synced" for 5 seconds after successful sync
				syncStatusPill.setText("Synced");
				syncStatusPill.setBackground(new Color(0x27AE60));
				syncStatusPill.setForeground(Color.WHITE);
				syncStatusPill.setToolTipText("Last synced: just now");
				syncStatusPill.setVisible(true);
			}
			else
			{
				// Hide after 5 seconds of being synced
				syncStatusPill.setVisible(false);
			}
		}
		else
		{
			// Idle/never synced - hide
			syncStatusPill.setVisible(false);
		}
	}

	private void updateRecentMenu(SyncService syncService, TargetService targetService)
	{
		recentMenu.removeAll();

		List<MenuSyncEvent> outgoing = new ArrayList<>();
		if (syncService != null)
		{
			List<SyncService.SyncEvent> events = syncService.getRecentEvents();
			if (events != null)
			{
				for (SyncService.SyncEvent event : events)
				{
					if (event != null)
					{
						outgoing.add(new MenuSyncEvent(event.getLabel(), event.getStatus()));
					}
				}
			}
		}
		JMenuItem forceItem = new JMenuItem("Force sync (compare & merge)");
		forceItem.setIcon(UiIconFactory.createRefreshIcon(new Color(0x3498DB), 12));
		forceItem.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		forceItem.addActionListener(e ->
		{
			if (forceSyncHandler != null)
			{
				forceSyncHandler.run();
			}
		});
		recentMenu.add(forceItem);
		recentMenu.addSeparator();

		addRecentSection("RuneLite -> Webapp", outgoing);

		recentMenu.addSeparator();

		List<MenuSyncEvent> incoming = new ArrayList<>();
		if (targetService != null)
		{
			incoming.add(buildIncomingEvent("Plan targets", targetService.getLastSuccessTimestamp(),
				targetService.getLastErrorMessage(), targetService.getLastErrorTimestamp(), targetService.isFetching()));
		}
		if (syncService != null)
		{
			incoming.add(buildIncomingEvent("Progress import", syncService.getLastProgressFetchSuccessMs(),
				syncService.getLastProgressFetchError(), syncService.getLastProgressFetchAttemptMs(), syncService.isProgressFetchInFlight()));
		}

		addRecentSection("Webapp -> RuneLite", incoming);
	}

	private void addRecentSection(String title, List<MenuSyncEvent> events)
	{
		JMenuItem header = new JMenuItem(title);
		header.setEnabled(false);
		header.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		recentMenu.add(header);

		if (events == null || events.isEmpty())
		{
			JMenuItem empty = new JMenuItem("No recent syncs");
			empty.setEnabled(false);
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			recentMenu.add(empty);
			return;
		}

		for (MenuSyncEvent event : events)
		{
			if (event == null)
			{
				continue;
			}

			JMenuItem item = new JMenuItem(event.label);
			item.setEnabled(false);
			item.setIcon(buildStatusIcon(event.status));
			item.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			recentMenu.add(item);
		}
	}

	private ImageIcon buildStatusIcon(SyncService.SyncStatus status)
	{
		int size = 10;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		Color color;
		if (status == SyncService.SyncStatus.SYNCED)
		{
			color = new Color(0x2ECC71);
		}
		else if (status == SyncService.SyncStatus.FAILED)
		{
			color = new Color(0xE74C3C);
		}
		else
		{
			color = ColorScheme.LIGHT_GRAY_COLOR;
		}

		g.setColor(color);
		g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		if (status == SyncService.SyncStatus.SYNCED)
		{
			g.drawLine(2, size / 2, size / 2 - 1, size - 3);
			g.drawLine(size / 2 - 1, size - 3, size - 2, 2);
		}
		else if (status == SyncService.SyncStatus.FAILED)
		{
			g.drawLine(2, 2, size - 2, size - 2);
			g.drawLine(size - 2, 2, 2, size - 2);
		}
		else
		{
			g.fillOval(size / 2 - 1, size / 2 - 1, 2, 2);
		}

		g.dispose();
		return new ImageIcon(img);
	}

	private MenuSyncEvent buildIncomingEvent(String label, long successMs, String error, long attemptMs, boolean pending)
	{
		SyncService.SyncStatus status = SyncService.SyncStatus.PENDING;
		if (error != null && !error.isEmpty() && attemptMs >= successMs)
		{
			status = SyncService.SyncStatus.FAILED;
		}
		else if (successMs > 0)
		{
			status = SyncService.SyncStatus.SYNCED;
		}
		else if (pending)
		{
			status = SyncService.SyncStatus.PENDING;
		}
		return new MenuSyncEvent(label, status);
	}

	private static final class MenuSyncEvent
	{
		private final String label;
		private final SyncService.SyncStatus status;

		private MenuSyncEvent(String label, SyncService.SyncStatus status)
		{
			this.label = label;
			this.status = status;
		}
	}
}
