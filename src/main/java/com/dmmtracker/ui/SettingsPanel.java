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
 * DMMScape Companion - Settings Panel
 * Provides in-plugin settings and device linking UI
 */
package com.dmmtracker.ui;

import com.dmmtracker.ApiConfig;
import com.dmmtracker.DMMTrackerConfig;
import com.dmmtracker.ui.components.CollapsiblePanel;
import com.dmmtracker.ui.components.UiComponents;
import com.dmmtracker.ui.components.UiIconFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JCheckBox;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.SwingUtil;

/**
 * Settings panel with sync options.
 */
public class SettingsPanel extends JPanel
{
	private static final String CONFIG_GROUP = "dmmtracker";
	private static final int TEXT_WRAP_WIDTH = 180;

	private final ConfigManager configManager;
	private final JButton openWebButton;
	private final JButton unsyncButton;
	private final JButton closeButton;
	private final JButton linkButton;
	private final JButton backToPlanButton;
	private final JButton syncNowButton;
	private final JButton unlinkButton;
	private final JLabel statusLabel = new JLabel();
	private final JLabel statusDetailLabel = new JLabel();
	private final JLabel openWebHelpLabel = new JLabel();
	private final JLabel unsyncHelpLabel = new JLabel();
	private final JLabel linkHelpLabel = new JLabel();
	private final JLabel syncNowHelpLabel = new JLabel();
	private final JLabel unlinkHelpLabel = new JLabel();
	private final JCheckBox targetOverlayToggle = UiComponents.createCheckBox("Show Target Overlay");
	private final JCheckBox worldMapTargetsToggle = UiComponents.createCheckBox("Show World Map Markers");
	private final JCheckBox worldMapLegendToggle = UiComponents.createCheckBox("Show World Map Legend");
	private final JCheckBox routeEnabledToggle = UiComponents.createCheckBox("Enable Route Drawing");
	private final JCheckBox sceneRouteToggle = UiComponents.createCheckBox("Show Scene Route");
	private final JCheckBox minimapRouteToggle = UiComponents.createCheckBox("Show Minimap Route");
	private final JCheckBox worldMapRouteToggle = UiComponents.createCheckBox("Show World Map Route");
	private final JCheckBox routeArrowsToggle = UiComponents.createCheckBox("Show Direction Arrows");
	private final JCheckBox mapShowCompletedToggle = UiComponents.createCheckBox("Show Completed");
	private final JCheckBox mapShowDiaryToggle = UiComponents.createCheckBox("Show Diary Targets");
	private final JCheckBox mapShowQuestToggle = UiComponents.createCheckBox("Show Quest Targets");
	private final JCheckBox mapShowBossToggle = UiComponents.createCheckBox("Show Boss Targets");
	private final JCheckBox mapShowCaToggle = UiComponents.createCheckBox("Show CA Targets");
	private final JCheckBox mapShowSigilToggle = UiComponents.createCheckBox("Show Sigil Targets");
	private final JCheckBox mapShowLampToggle = UiComponents.createCheckBox("Show Lamp Targets");
	private final JCheckBox mapShowPrayerToggle = UiComponents.createCheckBox("Show Prayer Targets");
	private final JCheckBox mapShowTeleportToggle = UiComponents.createCheckBox("Show Teleport Targets");
	private final JCheckBox mapShowCustomToggle = UiComponents.createCheckBox("Show Custom Targets");
	private final CollapsiblePanel mapFilterPanel = new CollapsiblePanel("Map Filters");
	private boolean updatingToggles = false;
	private final JLabel apiKeyHelpLabel = new JLabel();

	private Runnable openWebHandler;
	private Runnable unsyncHandler;
	private Runnable closeHandler;
	private Runnable linkHandler;
	private Runnable backToPlanHandler;
	private Runnable manualSyncHandler;
	private Runnable unlinkHandler;

	public SettingsPanel(ConfigManager configManager)
	{
		this.configManager = configManager;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(new EmptyBorder(8, 12, 8, 12));

		JLabel title = new JLabel("Settings");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
		title.setForeground(Color.WHITE);

		JLabel gearIcon = new JLabel(UiIconFactory.createGearIcon(DmmColors.INFO, 14));
		JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		titleGroup.setOpaque(false);
		titleGroup.add(gearIcon);
		titleGroup.add(title);

		closeButton = new JButton("X");
		styleButton(closeButton);
		closeButton.setPreferredSize(new Dimension(24, 24));
		closeButton.setToolTipText("Close settings");
		closeButton.addActionListener(e ->
		{
			if (closeHandler != null) closeHandler.run();
		});

		header.add(titleGroup, BorderLayout.WEST);
		header.add(closeButton, BorderLayout.EAST);

		// Content
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(new EmptyBorder(16, 16, 16, 16));

		JLabel infoLabel = new JLabel();
		infoLabel.setFont(FontManager.getRunescapeSmallFont());
		infoLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		infoLabel.setAlignmentX(LEFT_ALIGNMENT);
		infoLabel.setText(wrapText("Setup happens in the Plan tab. Use the checklist there to link and sync, then return here for display toggles."));

		backToPlanButton = new JButton("Back to Plan Setup");
		styleActionButton(backToPlanButton, DmmColors.INFO);
		backToPlanButton.setAlignmentX(LEFT_ALIGNMENT);
		backToPlanButton.addActionListener(e ->
		{
			if (backToPlanHandler != null)
			{
				backToPlanHandler.run();
			}
		});

		// Sync section
		JLabel syncLabel = new JLabel("DMMScape Sync");
		syncLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		syncLabel.setForeground(Color.WHITE);
		syncLabel.setAlignmentX(LEFT_ALIGNMENT);

		JLabel syncDesc = new JLabel();
		syncDesc.setFont(FontManager.getRunescapeSmallFont());
		syncDesc.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		syncDesc.setAlignmentX(LEFT_ALIGNMENT);
		syncDesc.setText(wrapText("Sync your progress and plan with the DMMScape webapp."));

		statusLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setAlignmentX(LEFT_ALIGNMENT);
		statusLabel.setText("Status: Not connected");

		statusDetailLabel.setFont(FontManager.getRunescapeSmallFont());
		statusDetailLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusDetailLabel.setAlignmentX(LEFT_ALIGNMENT);
		statusDetailLabel.setText(wrapText("Click 'Link / Upgrade Account' to connect with " + ApiConfig.webappHost() + "."));

		syncNowButton = new JButton("Sync Now");
		styleActionButton(syncNowButton, DmmColors.SUCCESS);
		syncNowButton.setAlignmentX(LEFT_ALIGNMENT);
		syncNowButton.addActionListener(e ->
		{
			if (manualSyncHandler != null) manualSyncHandler.run();
		});

		syncNowHelpLabel.setFont(FontManager.getRunescapeSmallFont());
		syncNowHelpLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		syncNowHelpLabel.setAlignmentX(LEFT_ALIGNMENT);
		syncNowHelpLabel.setText(wrapText("Manually sync progress now (requires being logged into OSRS)."));

		linkButton = new JButton("Link / Upgrade Account");
		styleActionButton(linkButton, DmmColors.INFO);
		linkButton.setAlignmentX(LEFT_ALIGNMENT);
		linkButton.addActionListener(e ->
		{
			if (linkHandler != null) linkHandler.run();
		});

		linkHelpLabel.setFont(FontManager.getRunescapeSmallFont());
		linkHelpLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		linkHelpLabel.setAlignmentX(LEFT_ALIGNMENT);
		linkHelpLabel.setText(wrapText("Shows a code to enter on " + ApiConfig.webappHost() + "/link to connect your account."));

		apiKeyHelpLabel.setFont(FontManager.getRunescapeSmallFont());
		apiKeyHelpLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		apiKeyHelpLabel.setAlignmentX(LEFT_ALIGNMENT);
		apiKeyHelpLabel.setText(wrapText("API key (advanced fallback) can be found in webapp Settings if needed."));

		openWebButton = new JButton("Open DMMScape Webapp");
		styleActionButton(openWebButton, DmmColors.SUCCESS);
		openWebButton.setAlignmentX(LEFT_ALIGNMENT);
		openWebButton.addActionListener(e ->
		{
			if (openWebHandler != null) openWebHandler.run();
		});

		openWebHelpLabel.setFont(FontManager.getRunescapeSmallFont());
		openWebHelpLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		openWebHelpLabel.setAlignmentX(LEFT_ALIGNMENT);
		openWebHelpLabel.setText(wrapText("Opens " + ApiConfig.webappHost() + ". If you're linked as guest, it opens a one-time guest login link."));

		unlinkButton = new JButton("Unlink Account");
		styleActionButton(unlinkButton, DmmColors.WARNING);
		unlinkButton.setAlignmentX(LEFT_ALIGNMENT);
		unlinkButton.addActionListener(e ->
		{
			if (unlinkHandler != null) unlinkHandler.run();
		});

		unlinkHelpLabel.setFont(FontManager.getRunescapeSmallFont());
		unlinkHelpLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		unlinkHelpLabel.setAlignmentX(LEFT_ALIGNMENT);
		unlinkHelpLabel.setText(wrapText("Unlink this device from your DMMScape account."));

		unsyncButton = new JButton("Disconnect Sync");
		styleActionButton(unsyncButton, DmmColors.ERROR);
		unsyncButton.setAlignmentX(LEFT_ALIGNMENT);
		unsyncButton.setEnabled(false);
		unsyncButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		unsyncButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		unsyncButton.addActionListener(e ->
		{
			if (unsyncHandler != null)
			{
				int choice = JOptionPane.showConfirmDialog(
					this,
					"Disconnect DMMScape and clear sync settings?",
					"Unsync",
					JOptionPane.YES_NO_OPTION
				);
				if (choice == JOptionPane.YES_OPTION)
				{
					unsyncHandler.run();
				}
			}
		});

		unsyncHelpLabel.setFont(FontManager.getRunescapeSmallFont());
		unsyncHelpLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		unsyncHelpLabel.setAlignmentX(LEFT_ALIGNMENT);
		unsyncHelpLabel.setText(wrapText("Not connected. Click 'Link / Upgrade Account' above to connect."));

		content.add(infoLabel);
		content.add(Box.createVerticalStrut(8));
		content.add(backToPlanButton);
		content.add(Box.createVerticalStrut(16));

		content.add(syncLabel);
		content.add(Box.createVerticalStrut(4));
		content.add(syncDesc);
		content.add(Box.createVerticalStrut(6));
		content.add(statusLabel);
		content.add(Box.createVerticalStrut(2));
		content.add(statusDetailLabel);
		content.add(Box.createVerticalStrut(10));
		content.add(syncNowButton);
		content.add(Box.createVerticalStrut(4));
		content.add(syncNowHelpLabel);
		content.add(Box.createVerticalStrut(10));
		content.add(linkButton);
		content.add(Box.createVerticalStrut(4));
		content.add(linkHelpLabel);
		content.add(Box.createVerticalStrut(2));
		content.add(apiKeyHelpLabel);
		content.add(Box.createVerticalStrut(12));
		content.add(openWebButton);
		content.add(Box.createVerticalStrut(4));
		content.add(openWebHelpLabel);
		content.add(Box.createVerticalStrut(10));
		content.add(unlinkButton);
		content.add(Box.createVerticalStrut(4));
		content.add(unlinkHelpLabel);
		content.add(Box.createVerticalStrut(8));
		content.add(unsyncButton);
		content.add(Box.createVerticalStrut(4));
		content.add(unsyncHelpLabel);
		content.add(Box.createVerticalStrut(16));

		JLabel overlayLabel = new JLabel("Overlay & Route Toggles");
		overlayLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		overlayLabel.setForeground(Color.WHITE);
		overlayLabel.setAlignmentX(LEFT_ALIGNMENT);

		JLabel overlayDesc = new JLabel();
		overlayDesc.setFont(FontManager.getRunescapeSmallFont());
		overlayDesc.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		overlayDesc.setAlignmentX(LEFT_ALIGNMENT);
		overlayDesc.setText(wrapText("Quick access to overlay and route settings."));

		configureToggle(targetOverlayToggle, "Show minimap arrow and tile highlight for next target");
		targetOverlayToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "showTargetOverlay", targetOverlayToggle.isSelected());
		});

		configureToggle(worldMapTargetsToggle, "Show plan targets on the world map");
		worldMapTargetsToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "showWorldMapTargets", worldMapTargetsToggle.isSelected());
		});

		configureToggle(worldMapLegendToggle, "Show legend and layer toggles on the world map");
		worldMapLegendToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "showWorldMapLegend", worldMapLegendToggle.isSelected());
		});

		configureToggle(routeEnabledToggle, "Draw lines connecting plan targets in order");
		routeEnabledToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "enableRouteDrawing", routeEnabledToggle.isSelected());
		});

		configureToggle(sceneRouteToggle, "Draw route lines in the game scene (3D world)");
		sceneRouteToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "showSceneRoute", sceneRouteToggle.isSelected());
		});

		configureToggle(minimapRouteToggle, "Draw route lines on the minimap");
		minimapRouteToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "showMinimapRoute", minimapRouteToggle.isSelected());
		});

		configureToggle(worldMapRouteToggle, "Draw route lines on the world map");
		worldMapRouteToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "showWorldMapRoute", worldMapRouteToggle.isSelected());
		});

		configureToggle(routeArrowsToggle, "Show arrows indicating route direction");
		routeArrowsToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "showRouteArrows", routeArrowsToggle.isSelected());
		});

		configureToggle(mapShowCompletedToggle, "Include completed plan items in map overlays");
		mapShowCompletedToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "mapShowCompleted", mapShowCompletedToggle.isSelected());
		});

		configureToggle(mapShowDiaryToggle, "Show diary targets on map overlays");
		mapShowDiaryToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "mapShowDiaryTargets", mapShowDiaryToggle.isSelected());
		});

		configureToggle(mapShowQuestToggle, "Show quest targets on map overlays");
		mapShowQuestToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "mapShowQuestTargets", mapShowQuestToggle.isSelected());
		});

		configureToggle(mapShowBossToggle, "Show boss targets on map overlays");
		mapShowBossToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "mapShowBossTargets", mapShowBossToggle.isSelected());
		});

		configureToggle(mapShowCaToggle, "Show combat achievement targets on map overlays");
		mapShowCaToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "mapShowCaTargets", mapShowCaToggle.isSelected());
		});

		configureToggle(mapShowSigilToggle, "Show sigil targets on map overlays");
		mapShowSigilToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "mapShowSigilTargets", mapShowSigilToggle.isSelected());
		});

		configureToggle(mapShowLampToggle, "Show lamp targets on map overlays");
		mapShowLampToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "mapShowLampTargets", mapShowLampToggle.isSelected());
		});

		configureToggle(mapShowPrayerToggle, "Show prayer targets on map overlays");
		mapShowPrayerToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "mapShowPrayerTargets", mapShowPrayerToggle.isSelected());
		});

		configureToggle(mapShowTeleportToggle, "Show teleport targets on map overlays");
		mapShowTeleportToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "mapShowTeleportTargets", mapShowTeleportToggle.isSelected());
		});

		configureToggle(mapShowCustomToggle, "Show custom targets on map overlays");
		mapShowCustomToggle.addActionListener(e ->
		{
			if (updatingToggles) return;
			configManager.setConfiguration(CONFIG_GROUP, "mapShowCustomTargets", mapShowCustomToggle.isSelected());
		});

		JLabel mapFilterDesc = new JLabel();
		mapFilterDesc.setFont(FontManager.getRunescapeSmallFont());
		mapFilterDesc.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		mapFilterDesc.setAlignmentX(LEFT_ALIGNMENT);
		mapFilterDesc.setText(wrapText("Filters for plan items shown on map and overlays."));

		JPanel mapFilterContent = mapFilterPanel.getContentPanel();
		mapFilterContent.add(mapFilterDesc);
		mapFilterContent.add(Box.createVerticalStrut(6));
		mapFilterContent.add(mapShowCompletedToggle);
		mapFilterContent.add(Box.createVerticalStrut(2));
		mapFilterContent.add(mapShowDiaryToggle);
		mapFilterContent.add(Box.createVerticalStrut(2));
		mapFilterContent.add(mapShowQuestToggle);
		mapFilterContent.add(Box.createVerticalStrut(2));
		mapFilterContent.add(mapShowBossToggle);
		mapFilterContent.add(Box.createVerticalStrut(2));
		mapFilterContent.add(mapShowCaToggle);
		mapFilterContent.add(Box.createVerticalStrut(2));
		mapFilterContent.add(mapShowSigilToggle);
		mapFilterContent.add(Box.createVerticalStrut(2));
		mapFilterContent.add(mapShowLampToggle);
		mapFilterContent.add(Box.createVerticalStrut(2));
		mapFilterContent.add(mapShowPrayerToggle);
		mapFilterContent.add(Box.createVerticalStrut(2));
		mapFilterContent.add(mapShowTeleportToggle);
		mapFilterContent.add(Box.createVerticalStrut(2));
		mapFilterContent.add(mapShowCustomToggle);
		mapFilterPanel.setExpanded(true, false);

		content.add(overlayLabel);
		content.add(Box.createVerticalStrut(4));
		content.add(overlayDesc);
		content.add(Box.createVerticalStrut(6));
		content.add(targetOverlayToggle);
		content.add(Box.createVerticalStrut(2));
		content.add(worldMapTargetsToggle);
		content.add(Box.createVerticalStrut(2));
		content.add(worldMapLegendToggle);
		content.add(Box.createVerticalStrut(6));
		content.add(routeEnabledToggle);
		content.add(Box.createVerticalStrut(2));
		content.add(sceneRouteToggle);
		content.add(Box.createVerticalStrut(2));
		content.add(minimapRouteToggle);
		content.add(Box.createVerticalStrut(2));
		content.add(worldMapRouteToggle);
		content.add(Box.createVerticalStrut(2));
		content.add(routeArrowsToggle);
		content.add(Box.createVerticalStrut(12));
		content.add(mapFilterPanel);
		content.add(Box.createVerticalGlue());

		add(header, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);

		// Default to disconnected state until refresh provides real status
		setConnectionState(false, statusLabel.getText(), statusDetailLabel.getText(), false);
		setSyncActionState(false, false);
	}

	private void configureToggle(JCheckBox toggle, String tooltip)
	{
		toggle.setFont(FontManager.getRunescapeSmallFont());
		toggle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		toggle.setOpaque(false);
		toggle.setFocusPainted(false);
		toggle.setToolTipText(tooltip);
		toggle.setAlignmentX(LEFT_ALIGNMENT);
	}

	private void styleButton(JButton button)
	{
		SwingUtil.removeButtonDecorations(button);
		button.setFocusPainted(false);
		button.setOpaque(true);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		button.setBorder(new EmptyBorder(4, 8, 4, 8));
		button.setFont(FontManager.getRunescapeSmallFont());
		button.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				button.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
	}

	private void styleActionButton(JButton button, Color color)
	{
		button.setFocusPainted(false);
		button.setOpaque(true);
		button.setBackground(color.darker());
		button.setForeground(Color.WHITE);
		button.setBorder(new EmptyBorder(8, 12, 8, 12));
		button.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		button.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				button.setBackground(color);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				button.setBackground(color.darker());
			}
		});
	}

	public void setConnectionState(boolean connected, String statusText, String detailText, boolean isGuest)
	{
		statusLabel.setText(statusText);
		statusDetailLabel.setText(wrapText(detailText));
		if (isGuest)
		{
			openWebHelpLabel.setText(wrapText("Opens a one-time guest login link in your browser."));
		}
		else if (!connected)
		{
			openWebHelpLabel.setText(wrapText("Open " + ApiConfig.webappHost() + " to create a plan. Then click 'Link / Upgrade Account' above to connect."));
		}
		else
		{
			openWebHelpLabel.setText(wrapText("Open " + ApiConfig.webappHost() + " to manage your plans and profile."));
		}

		if (connected)
		{
			unsyncButton.setEnabled(true);
			unsyncButton.setToolTipText("Disconnect and clear sync settings");
			unsyncButton.setBackground(DmmColors.ERROR.darker());
			unsyncButton.setForeground(Color.WHITE);
			unsyncHelpLabel.setText(wrapText("Clears connection and disables auto-sync, target sync, and overlays."));
			unsyncHelpLabel.setToolTipText("Disconnect and clear sync settings");
		}
		else
		{
			unsyncButton.setEnabled(false);
			unsyncButton.setToolTipText("Not connected yet. Use 'Link / Upgrade Account' above.");
			unsyncButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			unsyncButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			unsyncHelpLabel.setText(wrapText("Not connected. Click 'Link / Upgrade Account' above to connect."));
			unsyncHelpLabel.setToolTipText("Use the link button above to connect");
		}
	}

	public void setSyncActionState(boolean canSyncNow, boolean canUnlink)
	{
		syncNowButton.setEnabled(canSyncNow);
		syncNowButton.setToolTipText(canSyncNow ? "Manual sync now" : "Link your account to enable sync");
		syncNowButton.setBackground(canSyncNow ? DmmColors.SUCCESS.darker() : ColorScheme.DARKER_GRAY_COLOR);
		syncNowButton.setForeground(canSyncNow ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		syncNowHelpLabel.setText(wrapText(canSyncNow
			? "Manually sync progress now (requires being logged into OSRS)."
			: "Link your account to enable manual sync."));

		unlinkButton.setEnabled(canUnlink);
		unlinkButton.setToolTipText(canUnlink ? "Unlink this device" : "No linked account");
		unlinkButton.setBackground(canUnlink ? DmmColors.WARNING.darker() : ColorScheme.DARKER_GRAY_COLOR);
		unlinkButton.setForeground(canUnlink ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		unlinkHelpLabel.setText(wrapText(canUnlink
			? "Unlink this device from your DMMScape account."
			: "No linked account to unlink."));
	}

	public void setOverlaySettings(DMMTrackerConfig config)
	{
		if (config == null)
		{
			return;
		}

		updatingToggles = true;
		targetOverlayToggle.setSelected(config.showTargetOverlay());
		worldMapTargetsToggle.setSelected(config.showWorldMapTargets());
		worldMapLegendToggle.setSelected(config.showWorldMapLegend());
		routeEnabledToggle.setSelected(config.enableRouteDrawing());
		sceneRouteToggle.setSelected(config.showSceneRoute());
		minimapRouteToggle.setSelected(config.showMinimapRoute());
		worldMapRouteToggle.setSelected(config.showWorldMapRoute());
		routeArrowsToggle.setSelected(config.showRouteArrows());
		mapShowCompletedToggle.setSelected(config.mapShowCompleted());
		mapShowDiaryToggle.setSelected(config.mapShowDiaryTargets());
		mapShowQuestToggle.setSelected(config.mapShowQuestTargets());
		mapShowBossToggle.setSelected(config.mapShowBossTargets());
		mapShowCaToggle.setSelected(config.mapShowCaTargets());
		mapShowSigilToggle.setSelected(config.mapShowSigilTargets());
		mapShowLampToggle.setSelected(config.mapShowLampTargets());
		mapShowPrayerToggle.setSelected(config.mapShowPrayerTargets());
		mapShowTeleportToggle.setSelected(config.mapShowTeleportTargets());
		mapShowCustomToggle.setSelected(config.mapShowCustomTargets());
		updatingToggles = false;
	}

	public void setOpenWebHandler(Runnable handler)
	{
		this.openWebHandler = handler;
	}

	public void setUnsyncHandler(Runnable handler)
	{
		this.unsyncHandler = handler;
	}

	public void setLinkHandler(Runnable handler)
	{
		this.linkHandler = handler;
	}

	public void setManualSyncHandler(Runnable handler)
	{
		this.manualSyncHandler = handler;
	}

	public void setUnlinkHandler(Runnable handler)
	{
		this.unlinkHandler = handler;
	}

	public void setCloseHandler(Runnable handler)
	{
		this.closeHandler = handler;
	}

	public void setBackToPlanHandler(Runnable handler)
	{
		this.backToPlanHandler = handler;
	}

	private static String wrapText(String text)
	{
		if (text == null || text.isEmpty())
		{
			return "";
		}
		String trimmed = text.trim();
		if (trimmed.startsWith("<html"))
		{
			return text;
		}
		return "<html><body style='width: " + TEXT_WRAP_WIDTH + "px'>" + escapeHtml(text) + "</body></html>";
	}

	private static String escapeHtml(String text)
	{
		return text
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}
}
