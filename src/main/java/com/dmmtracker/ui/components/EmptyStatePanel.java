/*
 * Copyright (c) 2026, DMMScape
 * All rights reserved.
 */
package com.dmmtracker.ui.components;

import com.dmmtracker.ui.DmmColors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * A panel that displays an empty or error state with optional retry button.
 */
public class EmptyStatePanel extends JPanel
{
	private final JLabel iconLabel = new JLabel();
	private final LoadingSpinner loadingSpinner = new LoadingSpinner(32);
	private final JLabel titleLabel = new JLabel();
	private final JLabel messageLabel = new JLabel();
	private final JButton retryButton = new JButton("Retry");
	private final JPanel iconContainer = new JPanel(new BorderLayout());
	private Runnable retryHandler;
	private boolean isLoading = false;

	public EmptyStatePanel()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(40, 20, 40, 20));

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.setOpaque(false);

		// Icon container holds either the icon label or the loading spinner
		iconContainer.setOpaque(false);
		iconContainer.setAlignmentX(CENTER_ALIGNMENT);

		iconLabel.setFont(iconLabel.getFont().deriveFont(Font.PLAIN, 32f));
		iconLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

		loadingSpinner.setSpinnerColor(DmmColors.INFO);
		loadingSpinner.setAlignmentX(CENTER_ALIGNMENT);

		iconContainer.add(iconLabel, BorderLayout.CENTER);

		titleLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD, 14f));
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setAlignmentX(CENTER_ALIGNMENT);
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

		messageLabel.setFont(FontManager.getRunescapeSmallFont());
		messageLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		messageLabel.setAlignmentX(CENTER_ALIGNMENT);
		messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

		retryButton.setFont(FontManager.getRunescapeSmallFont());
		retryButton.setForeground(Color.WHITE);
		retryButton.setBackground(DmmColors.INFO);
		retryButton.setBorder(new EmptyBorder(6, 16, 6, 16));
		retryButton.setFocusPainted(false);
		retryButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		retryButton.setAlignmentX(CENTER_ALIGNMENT);
		retryButton.setVisible(false);
		retryButton.getAccessibleContext().setAccessibleName("Retry");
		retryButton.getAccessibleContext().setAccessibleDescription("Retry the failed operation");
		retryButton.addActionListener(e ->
		{
			if (retryHandler != null)
			{
				retryHandler.run();
			}
		});

		// Add hover effects to retry button
		retryButton.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				retryButton.setBackground(DmmColors.INFO.brighter());
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				retryButton.setBackground(DmmColors.INFO);
			}

			@Override
			public void mousePressed(java.awt.event.MouseEvent e)
			{
				retryButton.setBackground(DmmColors.INFO.darker());
			}

			@Override
			public void mouseReleased(java.awt.event.MouseEvent e)
			{
				if (retryButton.contains(e.getPoint()))
				{
					retryButton.setBackground(DmmColors.INFO.brighter());
				}
				else
				{
					retryButton.setBackground(DmmColors.INFO);
				}
			}
		});

		centerPanel.add(iconContainer);
		centerPanel.add(Box.createVerticalStrut(12));
		centerPanel.add(titleLabel);
		centerPanel.add(Box.createVerticalStrut(6));
		centerPanel.add(messageLabel);
		centerPanel.add(Box.createVerticalStrut(16));
		centerPanel.add(retryButton);

		add(centerPanel, BorderLayout.CENTER);
	}

	private void showIcon()
	{
		if (isLoading)
		{
			isLoading = false;
			loadingSpinner.stop();
			iconContainer.removeAll();
			iconContainer.add(iconLabel, BorderLayout.CENTER);
			iconContainer.revalidate();
			iconContainer.repaint();
		}
	}

	private void showSpinner()
	{
		if (!isLoading)
		{
			isLoading = true;
			iconContainer.removeAll();
			iconContainer.add(loadingSpinner, BorderLayout.CENTER);
			loadingSpinner.start();
			iconContainer.revalidate();
			iconContainer.repaint();
		}
	}

	/**
	 * Wraps message in HTML with width constraint to prevent horizontal scrolling.
	 */
	private String wrapMessage(String message)
	{
		if (message == null || message.isEmpty())
		{
			return "";
		}
		// Wrap in HTML with width constraint for text wrapping
		return "<html><body style='width: 140px; text-align: center'>" + message + "</body></html>";
	}

	/**
	 * Configures the panel for an empty state (no data).
	 */
	public void setEmptyState(String title, String message)
	{
		showIcon();
		iconLabel.setText("\u2205"); // Empty set symbol
		iconLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		titleLabel.setText(title);
		messageLabel.setText(wrapMessage(message));
		retryButton.setVisible(false);
	}

	/**
	 * Configures the panel for an error state with retry option.
	 */
	public void setErrorState(String title, String message, Runnable onRetry)
	{
		showIcon();
		iconLabel.setText("\u26A0"); // Warning symbol
		iconLabel.setForeground(DmmColors.ERROR);
		titleLabel.setText(title);
		messageLabel.setText(wrapMessage(message));
		retryHandler = onRetry;
		retryButton.setVisible(onRetry != null);
	}

	/**
	 * Configures the panel for a loading state with animated spinner.
	 */
	public void setLoadingState(String message)
	{
		showSpinner();
		titleLabel.setText("Loading...");
		messageLabel.setText(wrapMessage(message));
		retryButton.setVisible(false);
	}

	/**
	 * Configures the panel for a "no results" state (e.g., search with no matches).
	 */
	public void setNoResultsState(String message)
	{
		showIcon();
		iconLabel.setText("\uD83D\uDD0D"); // Magnifying glass
		iconLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		titleLabel.setText("No Results");
		messageLabel.setText(wrapMessage(message));
		retryButton.setVisible(false);
	}

	/**
	 * Sets a custom icon character.
	 */
	public void setIcon(String iconChar, Color color)
	{
		showIcon();
		iconLabel.setText(iconChar);
		iconLabel.setForeground(color);
	}
}
