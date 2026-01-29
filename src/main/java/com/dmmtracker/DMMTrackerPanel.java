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
 * DMMScape RuneLite Plugin Panel
 */
package com.dmmtracker;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import com.dmmtracker.ui.ScrollPaneUtils;
import com.dmmtracker.ui.components.UiComponents;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

public class DMMTrackerPanel extends PluginPanel
{
	private final JLabel syncStatusLabel = new JLabel();
	private final JLabel syncDetailLabel = new JLabel();
	private final JLabel targetFetchLabel = new JLabel();
	private final JLabel targetUpdateLabel = new JLabel();
	private final JLabel planLabel = new JLabel();
	private final JLabel apiKeyLabel = new JLabel();
	private final JCheckBox showCompletedToggle = UiComponents.createCheckBox("Show completed");
	private final JButton syncNowButton = new JButton("Sync Now");
	private final JButton refreshTargetsButton = new JButton("Refresh Plan");
	private final JButton openWebButton = new JButton("Open DMMScape");
	private final JPanel targetListPanel = new JPanel();
	private List<TargetPoint> lastTargets = new ArrayList<>();
	private long lastTargetsTimestamp = -1;
	private long lastLocalUpdateTimestamp = -1;
	private boolean allowEdits = true;

	private Runnable manualSyncHandler;
	private Runnable targetRefreshHandler;
	private Runnable openWebHandler;
	private BiConsumer<TargetPoint, Boolean> targetToggleHandler;

	public DMMTrackerPanel()
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(new EmptyBorder(0, 0, 8, 0));
		JLabel title = new JLabel("DMMScape");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
		header.add(title, BorderLayout.WEST);
		openWebButton.addActionListener(e ->
		{
			if (openWebHandler != null)
			{
				openWebHandler.run();
			}
		});
		header.add(openWebButton, BorderLayout.EAST);

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		top.add(header);
		top.add(buildSyncSection());
		top.add(Box.createRigidArea(new Dimension(0, 8)));
		top.add(buildTargetInfoSection());
		top.add(Box.createRigidArea(new Dimension(0, 8)));

		targetListPanel.setLayout(new BoxLayout(targetListPanel, BoxLayout.Y_AXIS));
		targetListPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(targetListPanel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		ScrollPaneUtils.applyRuneliteStyle(scrollPane);

		add(top, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		showCompletedToggle.addActionListener(e -> rebuildTargetList());
		syncNowButton.addActionListener(e ->
		{
			if (manualSyncHandler != null)
			{
				manualSyncHandler.run();
			}
		});
		refreshTargetsButton.addActionListener(e ->
		{
			if (targetRefreshHandler != null)
			{
				targetRefreshHandler.run();
			}
		});
	}

	public void setManualSyncHandler(Runnable handler)
	{
		this.manualSyncHandler = handler;
	}

	public void setTargetRefreshHandler(Runnable handler)
	{
		this.targetRefreshHandler = handler;
	}

	public void setOpenWebHandler(Runnable handler)
	{
		this.openWebHandler = handler;
	}

	public void setTargetToggleHandler(BiConsumer<TargetPoint, Boolean> handler)
	{
		this.targetToggleHandler = handler;
	}

	public void refresh(SyncService syncService, TargetService targetService, DMMTrackerConfig config)
	{
		updateSyncStatus(syncService);
		updateTargetStatus(syncService, targetService, config);
		boolean previousAllowEdits = allowEdits;
		allowEdits = config.allowTargetEdits();

		if (!config.enableTargetSync())
		{
			lastTargets = new ArrayList<>();
			lastTargetsTimestamp = -1;
			rebuildTargetList();
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
			return;
		}

		if (previousAllowEdits != allowEdits)
		{
			rebuildTargetList();
		}
	}

	private JPanel buildSyncSection()
	{
		JPanel panel = buildSection("Progress Sync");
		JPanel content = new JPanel(new GridLayout(0, 1, 0, 4));
		content.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		syncStatusLabel.setFont(syncStatusLabel.getFont().deriveFont(Font.BOLD, 12f));
		syncDetailLabel.setFont(syncDetailLabel.getFont().deriveFont(11f));

		content.add(syncStatusLabel);
		content.add(syncDetailLabel);
		content.add(syncNowButton);

		panel.add(content, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildTargetInfoSection()
	{
		JPanel panel = buildSection("Plan");
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		planLabel.setFont(planLabel.getFont().deriveFont(Font.BOLD, 12f));
		apiKeyLabel.setFont(apiKeyLabel.getFont().deriveFont(11f));
		targetFetchLabel.setFont(targetFetchLabel.getFont().deriveFont(11f));
		targetUpdateLabel.setFont(targetUpdateLabel.getFont().deriveFont(11f));

		content.add(planLabel);
		content.add(apiKeyLabel);
		content.add(targetFetchLabel);
		content.add(targetUpdateLabel);

		JPanel controls = new JPanel();
		controls.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
		controls.add(showCompletedToggle);
		controls.add(Box.createHorizontalStrut(6));
		controls.add(refreshTargetsButton);

		content.add(Box.createRigidArea(new Dimension(0, 6)));
		content.add(controls);

		panel.add(content, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildSection(String title)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));

		JLabel label = new JLabel(title);
		label.setBorder(new EmptyBorder(4, 6, 4, 6));
		label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
		panel.add(label, BorderLayout.NORTH);

		return panel;
	}

	private void updateSyncStatus(SyncService syncService)
	{
		long now = System.currentTimeMillis();
		String status;
		if (syncService.getLastSyncError() != null)
		{
			status = "Error";
		}
		else if (syncService.getLastSyncAttemptMs() > syncService.getLastSyncSuccessMs()
			&& now - syncService.getLastSyncAttemptMs() < 30_000)
		{
			status = "Syncing...";
		}
		else if (syncService.getLastSyncSuccessMs() > 0)
		{
			status = "Synced";
		}
		else
		{
			status = "Idle";
		}

		syncStatusLabel.setText("Status: " + status);
		if (syncService.getLastSyncError() != null)
		{
			syncDetailLabel.setText("Last error: " + syncService.getLastSyncError());
		}
		else if (syncService.getLastSyncSuccessMs() > 0)
		{
			syncDetailLabel.setText("Last sync: " + formatTimeAgo(syncService.getLastSyncSuccessMs()));
		}
		else
		{
			syncDetailLabel.setText("Last sync: never");
		}
	}

	private void updateTargetStatus(SyncService syncService, TargetService targetService, DMMTrackerConfig config)
	{
		if (!config.enableTargetSync())
		{
			planLabel.setText("Plan: target sync disabled");
			targetFetchLabel.setText("Plan sync: disabled");
			targetUpdateLabel.setText("Plan updates: disabled");
			return;
		}

		String planName = targetService.getPlanName();
		if (planName == null || planName.isEmpty())
		{
			planName = "No plan loaded";
		}
		int totalTargets = targetService.getTargetsSnapshot().size();
		planLabel.setText("Plan: " + planName + " (" + totalTargets + ")");

		if (config.apiKey() == null || config.apiKey().isEmpty())
		{
			apiKeyLabel.setText("API key: missing");
		}
		else
		{
			apiKeyLabel.setText("API key: configured");
		}

		if (targetService.getLastErrorMessage() != null)
		{
			targetFetchLabel.setText("Plan sync: " + targetService.getLastErrorMessage());
		}
		else if (targetService.getLastSuccessTimestamp() > 0)
		{
			targetFetchLabel.setText("Plan sync: " + formatTimeAgo(targetService.getLastSuccessTimestamp()));
		}
		else
		{
			targetFetchLabel.setText("Plan sync: never");
		}

		if (syncService.getLastTargetUpdateError() != null)
		{
			targetUpdateLabel.setText("Plan updates: " + syncService.getLastTargetUpdateError());
		}
		else if (syncService.getLastTargetUpdateSuccessMs() > 0)
		{
			targetUpdateLabel.setText("Plan updates: " + formatTimeAgo(syncService.getLastTargetUpdateSuccessMs()));
		}
		else
		{
			targetUpdateLabel.setText("Plan updates: none");
		}
	}

	private void rebuildTargetList()
	{
		targetListPanel.removeAll();
		if (lastTargets.isEmpty())
		{
			JLabel empty = new JLabel("No targets loaded.");
			empty.setBorder(new EmptyBorder(8, 6, 8, 6));
			empty.setHorizontalAlignment(SwingConstants.CENTER);
			targetListPanel.add(empty);
		}
		else
		{
			List<TargetPoint> sorted = new ArrayList<>(lastTargets);
			sorted.sort(Comparator.comparingInt(TargetPoint::getOrder));

			for (TargetPoint target : sorted)
			{
				if (!showCompletedToggle.isSelected() && target.isCompleted())
				{
					continue;
				}
				targetListPanel.add(buildTargetRow(target, allowEdits));
			}
		}

		targetListPanel.revalidate();
		targetListPanel.repaint();
	}

	private JPanel buildTargetRow(TargetPoint target, boolean allowEdits)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(4, 6, 4, 6));

		JCheckBox checkbox = UiComponents.createCheckBox();
		checkbox.setSelected(target.isCompleted());
		checkbox.setEnabled(allowEdits && target.isToggleable());
		checkbox.addActionListener(e ->
		{
			if (targetToggleHandler != null)
			{
				targetToggleHandler.accept(target, checkbox.isSelected());
			}
		});

		String completion = "";
		String source = target.getCompletionSource();
		if ("progress".equalsIgnoreCase(source))
		{
			completion = " (auto)";
		}
		else if ("custom".equalsIgnoreCase(source))
		{
			completion = " (plan)";
		}
		String title = (target.getOrder() + 1) + ". " + target.getName() + completion;
		JLabel label = new JLabel(title);
		label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
		if (target.isCompleted())
		{
			label.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
		}
		if (target.getCategory() != null && !target.getCategory().isEmpty())
		{
			label.setToolTipText(target.getCategory());
		}

		row.add(checkbox, BorderLayout.WEST);
		row.add(label, BorderLayout.CENTER);

		return row;
	}

	private String formatTimeAgo(long timestamp)
	{
		long delta = System.currentTimeMillis() - timestamp;
		if (delta < 0)
		{
			return "just now";
		}
		long seconds = delta / 1000;
		if (seconds < 60)
		{
			return seconds + "s ago";
		}
		long minutes = seconds / 60;
		if (minutes < 60)
		{
			return minutes + "m ago";
		}
		long hours = minutes / 60;
		return hours + "h ago";
	}
}
