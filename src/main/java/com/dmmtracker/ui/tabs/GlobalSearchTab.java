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
import com.dmmtracker.data.BossData;
import com.dmmtracker.data.CATaskData;
import com.dmmtracker.data.DataLoader;
import com.dmmtracker.data.DiaryData;
import com.dmmtracker.data.DiaryTaskData;
import com.dmmtracker.data.DiaryTierData;
import com.dmmtracker.data.LampData;
import com.dmmtracker.data.PluginData;
import com.dmmtracker.data.PrayerData;
import com.dmmtracker.data.SigilData;
import com.dmmtracker.data.SkillData;
import com.dmmtracker.ui.DmmColors;
import com.dmmtracker.ui.ScrollPaneUtils;
import com.dmmtracker.ui.components.ListItemPanel;
import com.dmmtracker.ui.components.UiComponents;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.LinkBrowser;

/**
 * Global search tab that searches across all data types.
 */
public class GlobalSearchTab extends JPanel
{
	private final DataLoader dataLoader;
	private final ProgressStore progressStore;
	private final IconTextField searchField;
	private final JPanel resultsPanel;
	private final JScrollPane scrollPane;

	private static final String CA_WIKI_URL = "https://oldschool.runescape.wiki/w/Combat_Achievements";

	private static final Map<String, Color> TYPE_COLORS = Map.of(
		"Boss", DmmColors.CATEGORY_BOSS,
		"Sigil", DmmColors.TYPE_SIGIL,
		"Lamp", DmmColors.INFO,
		"Prayer", DmmColors.TYPE_CA,
		"Skill", DmmColors.TYPE_SKILL,
		"Diary", DmmColors.GOLD,
		"CA", DmmColors.SUCCESS_BRIGHT
	);

	// Navigation callback: (tabName, itemId) -> navigate to tab and optionally scroll to item
	// tabName: "bosses", "sigils", "diaries", "cas"
	// itemId: identifier for the item (e.g., boss name, diary name, etc.)
	private BiConsumer<String, String> navigationHandler;

	public GlobalSearchTab(DataLoader dataLoader, ProgressStore progressStore)
	{
		this.dataLoader = dataLoader;
		this.progressStore = progressStore;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Search input
		JPanel searchPanel = new JPanel(new BorderLayout(4, 0));
		searchPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

		searchField = UiComponents.createSearchField("Search all content...");
		searchField.getAccessibleContext().setAccessibleName("Global Search");
		searchField.getAccessibleContext().setAccessibleDescription("Search across all content including bosses, diaries, combat achievements, sigils, lamps, prayers, and skills");

		// Live search with debounce
		Timer debounceTimer = new Timer(200, e -> performSearch());
		debounceTimer.setRepeats(false);
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			public void insertUpdate(DocumentEvent e)
			{
				debounceTimer.restart();
			}

			public void removeUpdate(DocumentEvent e)
			{
				debounceTimer.restart();
			}

			public void changedUpdate(DocumentEvent e)
			{
				debounceTimer.restart();
			}
		});

		searchPanel.add(searchField, BorderLayout.CENTER);

		// Results panel
		resultsPanel = new JPanel();
		resultsPanel.setLayout(new javax.swing.BoxLayout(resultsPanel, javax.swing.BoxLayout.Y_AXIS));
		resultsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		scrollPane = new JScrollPane(resultsPanel);
		ScrollPaneUtils.applyRuneliteStyle(scrollPane);

		JSplitPane splitPane = UiComponents.createVerticalSplit(searchPanel, scrollPane);
		add(splitPane, BorderLayout.CENTER);

		// Initial state
		showPlaceholder();
	}

	public void refresh()
	{
		String query = searchField.getText().trim();
		if (query.length() < 2)
		{
			showPlaceholder();
			return;
		}
		performSearch();
	}

	public void focusSearch()
	{
		SwingUtilities.invokeLater(() -> searchField.requestFocusInWindow());
	}

	/**
	 * Sets a handler for navigating to tabs when clicking search results.
	 * @param handler BiConsumer receiving (tabName, itemId)
	 */
	public void setNavigationHandler(BiConsumer<String, String> handler)
	{
		this.navigationHandler = handler;
	}

	private void showPlaceholder()
	{
		resultsPanel.removeAll();

		JPanel placeholder = new JPanel();
		placeholder.setLayout(new javax.swing.BoxLayout(placeholder, javax.swing.BoxLayout.Y_AXIS));
		placeholder.setBackground(ColorScheme.DARK_GRAY_COLOR);
		placeholder.setBorder(new EmptyBorder(40, 20, 40, 20));

		JLabel icon = new JLabel("\uD83D\uDD0D"); // Magnifying glass emoji
		icon.setFont(icon.getFont().deriveFont(32f));
		icon.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel text = new JLabel("<html><body style='width: 140px; text-align: center'>Type to search across:<br>Bosses, Diaries, CAs, Sigils, Lamps, Prayers, Skills</body></html>");
		text.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		text.setAlignmentX(Component.CENTER_ALIGNMENT);
		text.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

		placeholder.add(javax.swing.Box.createVerticalGlue());
		placeholder.add(icon);
		placeholder.add(javax.swing.Box.createRigidArea(new Dimension(0, 12)));
		placeholder.add(text);
		placeholder.add(javax.swing.Box.createVerticalGlue());

		resultsPanel.add(placeholder);
		resultsPanel.revalidate();
		resultsPanel.repaint();
	}

	private void performSearch()
	{
		String query = searchField.getText().trim();
		if (query.length() < 2)
		{
			showPlaceholder();
			return;
		}

		resultsPanel.removeAll();
		int totalResults = 0;
		String lower = query.toLowerCase();
		Set<String> completedDiaryTasks = progressStore.getCompletedDiaryTasks();
		Set<String> completedCATasks = progressStore.getCompletedCATasks();

		// Search bosses
		List<BossData> bosses = dataLoader.searchBosses(query);
		if (!bosses.isEmpty())
		{
			totalResults += addResultSection("Boss", bosses.size(), TYPE_COLORS.get("Boss"));
			int count = 0;
			for (BossData boss : bosses)
			{
				if (count++ >= 8)
				{
					addMoreIndicator(bosses.size() - 8);
					break;
				}
				resultsPanel.add(buildResultRow(
					boss.getName(),
					boss.getPointsPerKill() + " pts/kill",
					boss.getWikiUrl(),
					count % 2 == 0,
					"bosses",
					boss.getName(),
					query
				));
			}
		}

		// Search sigils
		List<SigilData> sigils = dataLoader.searchSigils(query);
		if (!sigils.isEmpty())
		{
			totalResults += addResultSection("Sigil", sigils.size(), TYPE_COLORS.get("Sigil"));
			int count = 0;
			for (SigilData sigil : sigils)
			{
				if (count++ >= 8)
				{
					addMoreIndicator(sigils.size() - 8);
					break;
				}
				resultsPanel.add(buildResultRow(
					sigil.getName(),
					sigil.getPoints() + " pts",
					sigil.getWikiUrl(),
					count % 2 == 0,
					"sigils",
					sigil.getName(),
					query
				));
			}
		}

		// Search lamps
		List<LampData> lamps = dataLoader.getLamps().stream()
			.filter(l -> l.getName().toLowerCase().contains(lower))
			.collect(Collectors.toList());
		if (!lamps.isEmpty())
		{
			totalResults += addResultSection("Lamp", lamps.size(), TYPE_COLORS.get("Lamp"));
			for (int i = 0; i < Math.min(8, lamps.size()); i++)
			{
				LampData lamp = lamps.get(i);
				resultsPanel.add(buildResultRow(
					lamp.getName(),
					lamp.getPoints() + " pts",
					lamp.getWikiUrl(),
					i % 2 == 0,
					query
				));
			}
		}

		// Search prayers
		List<PrayerData> prayers = dataLoader.getPrayers().stream()
			.filter(p -> p.getName().toLowerCase().contains(lower))
			.collect(Collectors.toList());
		if (!prayers.isEmpty())
		{
			totalResults += addResultSection("Prayer", prayers.size(), TYPE_COLORS.get("Prayer"));
			for (int i = 0; i < Math.min(8, prayers.size()); i++)
			{
				PrayerData prayer = prayers.get(i);
				resultsPanel.add(buildResultRow(
					prayer.getName(),
					prayer.getPoints() + " pts",
					prayer.getWikiUrl(),
					i % 2 == 0,
					query
				));
			}
		}

		// Search diary tasks
		List<DiaryTaskHit> diaryHits = new ArrayList<>();
		for (DiaryData diary : dataLoader.getDiaries())
		{
			for (DiaryTierData tier : List.of(diary.getTiers().getEasy(), diary.getTiers().getMedium(), diary.getTiers().getHard(), diary.getTiers().getElite()))
			{
				if (tier == null)
				{
					continue;
				}
				String tierName = tier.getTier() != null ? tier.getTier() : "";
				for (DiaryTaskData task : tier.getTasks())
				{
					String description = task.getDescription() != null ? task.getDescription() : "";
					String haystack = (diary.getName() + " " + description).toLowerCase();
					if (haystack.contains(lower))
					{
						boolean completed = completedDiaryTasks.contains(task.getId());
						String detail = diary.getName() + " (" + tierName + ")" + (completed ? " - DONE" : "");
						diaryHits.add(new DiaryTaskHit(description, detail, diary.getName(), completed));
					}
				}
			}
		}

		if (!diaryHits.isEmpty())
		{
			totalResults += addResultSection("Diary", diaryHits.size(), TYPE_COLORS.get("Diary"));
			for (int i = 0; i < Math.min(8, diaryHits.size()); i++)
			{
				DiaryTaskHit hit = diaryHits.get(i);
				resultsPanel.add(buildResultRow(
					hit.name,
					hit.detail,
					null,
					i % 2 == 0,
					"diaries",
					hit.diaryName,
					query
				));
			}
			if (diaryHits.size() > 8)
			{
				addMoreIndicator(diaryHits.size() - 8);
			}
		}

		// Search combat achievement tasks
		List<CATaskHit> caHits = new ArrayList<>();
		Map<String, PluginData.CATierData> tiers = dataLoader.getCATiers();
		for (String tierName : List.of("easy", "medium", "hard", "elite", "master", "grandmaster"))
		{
			PluginData.CATierData tierData = tiers.get(tierName);
			if (tierData == null)
			{
				continue;
			}
			for (CATaskData task : tierData.getTasks())
			{
				String text = (task.getName() + " " + task.getDescription()).toLowerCase();
				if (text.contains(lower))
				{
					boolean completed = completedCATasks.contains(task.getId());
					String detail = capitalize(tierName) + " tier" + (completed ? " - DONE" : "");
					caHits.add(new CATaskHit(task.getName(), detail, tierName, completed));
				}
			}
		}

		if (!caHits.isEmpty())
		{
			totalResults += addResultSection("CA", caHits.size(), TYPE_COLORS.get("CA"));
			for (int i = 0; i < Math.min(8, caHits.size()); i++)
			{
				CATaskHit hit = caHits.get(i);
				resultsPanel.add(buildResultRow(
					hit.name,
					hit.detail,
					CA_WIKI_URL,
					i % 2 == 0,
					"cas",
					hit.tierName,
					query
				));
			}
			if (caHits.size() > 8)
			{
				addMoreIndicator(caHits.size() - 8);
			}
		}

		// Search skills
		List<SkillData> skills = dataLoader.getSkills().stream()
			.filter(s -> s.getName().toLowerCase().contains(lower))
			.collect(Collectors.toList());
		if (!skills.isEmpty())
		{
			totalResults += addResultSection("Skill", skills.size(), TYPE_COLORS.get("Skill"));
			for (int i = 0; i < Math.min(8, skills.size()); i++)
			{
				SkillData skill = skills.get(i);
				int maxPoints = dataLoader.calculateSkillPoints(99);
				resultsPanel.add(buildResultRow(
					skill.getName(),
					"Max " + maxPoints + " pts",
					"https://oldschool.runescape.wiki/w/" + skill.getName(),
					i % 2 == 0,
					query
				));
			}
		}

		if (totalResults == 0)
		{
			JLabel noResults = new JLabel("No results for \"" + query + "\"");
			noResults.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			noResults.setBorder(new EmptyBorder(20, 20, 20, 20));
			noResults.setAlignmentX(Component.CENTER_ALIGNMENT);
			resultsPanel.add(noResults);
		}

		resultsPanel.revalidate();
		resultsPanel.repaint();

		// Scroll to top
		SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
	}

	private int addResultSection(String typeName, int count, Color color)
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(new EmptyBorder(8, 8, 4, 8));
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

		JLabel badge = new JLabel(typeName + " (" + count + ")");
		badge.setForeground(color);
		badge.setFont(badge.getFont().deriveFont(Font.BOLD, 11f));
		header.add(badge, BorderLayout.WEST);

		resultsPanel.add(header);
		return count;
	}

	private void addMoreIndicator(int remaining)
	{
		JLabel more = new JLabel("... and " + remaining + " more");
		more.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		more.setFont(more.getFont().deriveFont(10f));
		more.setBorder(new EmptyBorder(4, 12, 8, 8));
		resultsPanel.add(more);
	}

	private JPanel buildResultRow(String name, String detail, String wikiUrl, boolean alternate, String navTab, String navItemId, String query)
	{
		// Use ListItemPanel with index for alternating colors (alternate=true means even index)
		ListItemPanel row = new ListItemPanel(alternate ? 0 : 1);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

		// Apply query highlighting to the name
		JLabel nameLabel = new JLabel(highlightMatch(name, query));
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(nameLabel.getFont().deriveFont(11f));

		// Navigation arrow indicator
		JLabel navArrow = new JLabel("\u2192"); // Right arrow
		navArrow.setForeground(DmmColors.INFO);
		navArrow.setFont(navArrow.getFont().deriveFont(Font.BOLD, 10f));
		navArrow.setVisible(navTab != null && navigationHandler != null);
		navArrow.setToolTipText("Go to " + (navTab != null ? navTab : ""));

		JLabel detailLabel = new JLabel(detail);
		detailLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		detailLabel.setFont(detailLabel.getFont().deriveFont(10f));

		JPanel rightPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
		rightPanel.setOpaque(false);
		rightPanel.add(detailLabel);
		rightPanel.add(navArrow);

		row.setCenterComponent(nameLabel);
		row.setRightComponent(rightPanel);

		// Click handler: navigate to tab, or open wiki as fallback
		row.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e)
			{
				if (navTab != null && navigationHandler != null)
				{
					navigationHandler.accept(navTab, navItemId);
				}
				else if (wikiUrl != null)
				{
					LinkBrowser.browse(wikiUrl);
				}
			}
		});

		return row;
	}

	// Overload for backward compatibility
	private JPanel buildResultRow(String name, String detail, String wikiUrl, boolean alternate, String query)
	{
		return buildResultRow(name, detail, wikiUrl, alternate, null, null, query);
	}

	private String capitalize(String s)
	{
		if (s == null || s.isEmpty())
		{
			return s;
		}
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	/**
	 * Highlights matching portions of text using HTML markup.
	 * @param text The text to highlight within
	 * @param query The search query to highlight
	 * @return HTML string with matches highlighted in a distinct color
	 */
	private String highlightMatch(String text, String query)
	{
		if (text == null || query == null || query.isEmpty())
		{
			return escapeHtml(text);
		}

		String lowerText = text.toLowerCase();
		String lowerQuery = query.toLowerCase();
		int index = lowerText.indexOf(lowerQuery);

		if (index < 0)
		{
			return escapeHtml(text);
		}

		StringBuilder sb = new StringBuilder("<html>");
		int lastEnd = 0;

		while (index >= 0)
		{
			// Add text before match
			sb.append(escapeHtml(text.substring(lastEnd, index)));
			// Add highlighted match (preserve original case)
			sb.append("<span style='color:#F1C40F;font-weight:bold;'>");
			sb.append(escapeHtml(text.substring(index, index + query.length())));
			sb.append("</span>");
			lastEnd = index + query.length();
			index = lowerText.indexOf(lowerQuery, lastEnd);
		}

		// Add remaining text
		sb.append(escapeHtml(text.substring(lastEnd)));
		sb.append("</html>");

		return sb.toString();
	}

	private String escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	private static class DiaryTaskHit
	{
		private final String name;
		private final String detail;
		private final String diaryName;
		private final boolean completed;

		DiaryTaskHit(String name, String detail, String diaryName, boolean completed)
		{
			this.name = name;
			this.detail = detail;
			this.diaryName = diaryName;
			this.completed = completed;
		}
	}

	private static class CATaskHit
	{
		private final String name;
		private final String detail;
		private final String tierName;
		private final boolean completed;

		CATaskHit(String name, String detail, String tierName, boolean completed)
		{
			this.name = name;
			this.detail = detail;
			this.tierName = tierName;
			this.completed = completed;
		}
	}
}
