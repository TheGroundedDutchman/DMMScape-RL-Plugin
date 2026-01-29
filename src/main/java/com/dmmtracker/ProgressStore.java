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

package com.dmmtracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.client.RuneLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Local progress store for UI state (owned unlocks, mapped tasks, diary counts).
 */
@Singleton
public class ProgressStore
{
	private static final Logger log = LoggerFactory.getLogger(ProgressStore.class);
	private static final String PROGRESS_FILE_NAME = "dmmscape-progress.json";

	public enum UpdateField
	{
		OWNED_SIGILS,
		OWNED_LAMPS,
		OWNED_PRAYERS,
		COMPLETED_DIARY_TASKS,
		COMPLETED_CA_TASKS,
		DIARY_TASK_COUNTS,
		BOSS_KILL_COUNTS
	}

	public enum UpdateSource
	{
		LOCAL,
		REMOTE,
		GAME
	}

	public interface ProgressListener
	{
		void onProgressUpdate(ProgressUpdate update);
	}

	public static class ProgressUpdate
	{
		private final UpdateSource source;
		private final EnumSet<UpdateField> fields;

		private ProgressUpdate(UpdateSource source, EnumSet<UpdateField> fields)
		{
			this.source = source;
			this.fields = fields;
		}

		public boolean hasField(UpdateField field)
		{
			return fields.contains(field);
		}

		public UpdateSource getSource()
		{
			return source;
		}

		public EnumSet<UpdateField> getFields()
		{
			return fields;
		}
	}

	private static class ProgressState
	{
		List<String> completedDiaryTasks = new ArrayList<>();
		List<String> completedCATasks = new ArrayList<>();
		List<String> ownedSigils = new ArrayList<>();
		List<String> ownedLamps = new ArrayList<>();
		List<String> ownedPrayers = new ArrayList<>();
		Map<String, Map<String, Integer>> diaryTaskCounts = new HashMap<>();
		Map<String, Integer> bossKillCounts = new HashMap<>();
	}

	private final Gson gson;
	private final List<ProgressListener> listeners = new CopyOnWriteArrayList<>();

	private Set<String> completedDiaryTasks = new HashSet<>();
	private Set<String> completedCATasks = new HashSet<>();
	private Set<String> ownedSigils = new HashSet<>();
	private Set<String> ownedLamps = new HashSet<>();
	private Set<String> ownedPrayers = new HashSet<>();
	private Map<String, Map<String, Integer>> diaryTaskCounts = new HashMap<>();
	private Map<String, Integer> bossKillCounts = new HashMap<>();

	@Inject
	public ProgressStore()
	{
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		load();
	}

	public void addListener(ProgressListener listener)
	{
		if (listener != null)
		{
			listeners.add(listener);
		}
	}

	public void removeListener(ProgressListener listener)
	{
		listeners.remove(listener);
	}

	public synchronized Set<String> getCompletedDiaryTasks()
	{
		return new HashSet<>(completedDiaryTasks);
	}

	public synchronized Set<String> getCompletedCATasks()
	{
		return new HashSet<>(completedCATasks);
	}

	public synchronized Set<String> getOwnedSigils()
	{
		return new HashSet<>(ownedSigils);
	}

	public synchronized Set<String> getOwnedLamps()
	{
		return new HashSet<>(ownedLamps);
	}

	public synchronized Set<String> getOwnedPrayers()
	{
		return new HashSet<>(ownedPrayers);
	}

	public synchronized Map<String, Map<String, Integer>> getDiaryTaskCounts()
	{
		Map<String, Map<String, Integer>> copy = new HashMap<>();
		for (Map.Entry<String, Map<String, Integer>> entry : diaryTaskCounts.entrySet())
		{
			copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
		}
		return copy;
	}

	/**
	 * Gets the kill count for a specific boss.
	 * @param bossName The boss name (case-insensitive)
	 * @return The kill count, or -1 if unknown
	 */
	public synchronized int getBossKillCount(String bossName)
	{
		if (bossName == null)
		{
			return -1;
		}
		Integer kc = bossKillCounts.get(bossName.toLowerCase());
		return kc != null ? kc : -1;
	}

	/**
	 * Sets the kill count for a specific boss.
	 * @param bossName The boss name
	 * @param kc The kill count
	 */
	public synchronized void setBossKillCount(String bossName, int kc)
	{
		if (bossName == null)
		{
			return;
		}
		bossKillCounts.put(bossName.toLowerCase(), kc);
		notifyListeners(EnumSet.of(UpdateField.BOSS_KILL_COUNTS), UpdateSource.LOCAL);
		save();
	}

	/**
	 * Gets all boss kill counts.
	 * @return Map of boss name (lowercase) to kill count
	 */
	public synchronized Map<String, Integer> getBossKillCounts()
	{
		return new HashMap<>(bossKillCounts);
	}

	/**
	 * Sets all boss kill counts (typically from sync).
	 * @param counts Map of boss name to kill count
	 * @param source The update source
	 */
	public synchronized void setBossKillCounts(Map<String, Integer> counts, UpdateSource source)
	{
		if (counts == null)
		{
			return;
		}
		Map<String, Integer> updated = new HashMap<>();
		for (Map.Entry<String, Integer> entry : counts.entrySet())
		{
			updated.put(entry.getKey().toLowerCase(), entry.getValue());
		}
		if (!updated.equals(bossKillCounts))
		{
			bossKillCounts = updated;
			notifyListeners(EnumSet.of(UpdateField.BOSS_KILL_COUNTS), source);
			save();
		}
	}

	public synchronized boolean isSigilOwned(String id)
	{
		return ownedSigils.contains(id);
	}

	public synchronized boolean isLampOwned(String id)
	{
		return ownedLamps.contains(id);
	}

	public synchronized boolean isPrayerOwned(String id)
	{
		return ownedPrayers.contains(id);
	}

	public synchronized void toggleSigilOwned(String id, UpdateSource source)
	{
		if (id == null)
		{
			return;
		}
		Set<String> updated = new HashSet<>(ownedSigils);
		if (!updated.add(id))
		{
			updated.remove(id);
		}
		updateOwnedState(updated, ownedLamps, ownedPrayers, source);
	}

	public synchronized void toggleLampOwned(String id, UpdateSource source)
	{
		if (id == null)
		{
			return;
		}
		Set<String> updated = new HashSet<>(ownedLamps);
		if (!updated.add(id))
		{
			updated.remove(id);
		}
		updateOwnedState(ownedSigils, updated, ownedPrayers, source);
	}

	public synchronized void togglePrayerOwned(String id, UpdateSource source)
	{
		if (id == null)
		{
			return;
		}
		Set<String> updated = new HashSet<>(ownedPrayers);
		if (!updated.add(id))
		{
			updated.remove(id);
		}
		updateOwnedState(ownedSigils, ownedLamps, updated, source);
	}

	public synchronized void updateOwnedState(Set<String> sigils, Set<String> lamps, Set<String> prayers, UpdateSource source)
	{
		EnumSet<UpdateField> changed = EnumSet.noneOf(UpdateField.class);

		if (sigils != null && !sigils.equals(ownedSigils))
		{
			ownedSigils = new HashSet<>(sigils);
			changed.add(UpdateField.OWNED_SIGILS);
		}
		if (lamps != null && !lamps.equals(ownedLamps))
		{
			ownedLamps = new HashSet<>(lamps);
			changed.add(UpdateField.OWNED_LAMPS);
		}
		if (prayers != null && !prayers.equals(ownedPrayers))
		{
			ownedPrayers = new HashSet<>(prayers);
			changed.add(UpdateField.OWNED_PRAYERS);
		}

		if (!changed.isEmpty())
		{
			save();
			notifyListeners(changed, source);
		}
	}

	public synchronized void setCompletedCATasks(Set<String> taskIds, UpdateSource source)
	{
		Set<String> updated = taskIds != null ? new HashSet<>(taskIds) : new HashSet<>();
		if (!updated.equals(completedCATasks))
		{
			completedCATasks = updated;
			save();
			notifyListeners(EnumSet.of(UpdateField.COMPLETED_CA_TASKS), source);
		}
	}

	public synchronized void addCompletedDiaryTasks(Collection<String> taskIds, UpdateSource source)
	{
		if (taskIds == null || taskIds.isEmpty())
		{
			return;
		}

		boolean changed = completedDiaryTasks.addAll(taskIds);
		if (changed)
		{
			save();
			notifyListeners(EnumSet.of(UpdateField.COMPLETED_DIARY_TASKS), source);
		}
	}

	public synchronized void setCompletedDiaryTasks(Set<String> taskIds, UpdateSource source)
	{
		Set<String> updated = taskIds != null ? new HashSet<>(taskIds) : new HashSet<>();
		if (!updated.equals(completedDiaryTasks))
		{
			completedDiaryTasks = updated;
			save();
			notifyListeners(EnumSet.of(UpdateField.COMPLETED_DIARY_TASKS), source);
		}
	}

	public synchronized boolean isCATaskCompleted(String taskName)
	{
		return taskName != null && completedCATasks.contains(taskName);
	}

	public synchronized void setCATaskCompleted(String taskName, boolean completed)
	{
		if (taskName == null)
		{
			return;
		}

		boolean changed;
		if (completed)
		{
			changed = completedCATasks.add(taskName);
		}
		else
		{
			changed = completedCATasks.remove(taskName);
		}

		if (changed)
		{
			save();
			notifyListeners(EnumSet.of(UpdateField.COMPLETED_CA_TASKS), UpdateSource.LOCAL);
		}
	}

	public synchronized boolean isDiaryTaskCompleted(String taskName)
	{
		return taskName != null && completedDiaryTasks.contains(taskName);
	}

	public synchronized void setDiaryTaskCompleted(String taskName, boolean completed)
	{
		if (taskName == null)
		{
			return;
		}

		boolean changed;
		if (completed)
		{
			changed = completedDiaryTasks.add(taskName);
		}
		else
		{
			changed = completedDiaryTasks.remove(taskName);
		}

		if (changed)
		{
			save();
			notifyListeners(EnumSet.of(UpdateField.COMPLETED_DIARY_TASKS), UpdateSource.LOCAL);
		}
	}

	public synchronized void setDiaryTaskCounts(Map<String, Map<String, Integer>> counts, UpdateSource source)
	{
		Map<String, Map<String, Integer>> updated = new HashMap<>();
		if (counts != null)
		{
			for (Map.Entry<String, Map<String, Integer>> entry : counts.entrySet())
			{
				updated.put(entry.getKey(), new HashMap<>(entry.getValue()));
			}
		}

		if (!updated.equals(diaryTaskCounts))
		{
			diaryTaskCounts = updated;
			save();
			notifyListeners(EnumSet.of(UpdateField.DIARY_TASK_COUNTS), source);
		}
	}

	private void notifyListeners(EnumSet<UpdateField> fields, UpdateSource source)
	{
		if (fields == null || fields.isEmpty())
		{
			return;
		}

		ProgressUpdate update = new ProgressUpdate(source, fields);
		for (ProgressListener listener : listeners)
		{
			try
			{
				listener.onProgressUpdate(update);
			}
			catch (Exception e)
			{
				log.debug("Progress listener error: {}", e.getMessage());
			}
		}
	}

	private void load()
	{
		Path path = getProgressPath();
		if (path == null || !Files.exists(path))
		{
			return;
		}

		try
		{
			String json = Files.readString(path, StandardCharsets.UTF_8);
			ProgressState state = gson.fromJson(json, ProgressState.class);
			if (state == null)
			{
				return;
			}

			completedDiaryTasks = new HashSet<>(safeList(state.completedDiaryTasks));
			completedCATasks = new HashSet<>(safeList(state.completedCATasks));
			ownedSigils = new HashSet<>(safeList(state.ownedSigils));
			ownedLamps = new HashSet<>(safeList(state.ownedLamps));
			ownedPrayers = new HashSet<>(safeList(state.ownedPrayers));
			diaryTaskCounts = safeMap(state.diaryTaskCounts);
			bossKillCounts = state.bossKillCounts != null ? new HashMap<>(state.bossKillCounts) : new HashMap<>();
		}
		catch (Exception e)
		{
			log.debug("Failed to load progress store: {}", e.getMessage());
		}
	}

	private void save()
	{
		Path path = getProgressPath();
		if (path == null)
		{
			return;
		}

		try
		{
			Files.createDirectories(path.getParent());
			ProgressState state = new ProgressState();
			state.completedDiaryTasks = sortedList(completedDiaryTasks);
			state.completedCATasks = sortedList(completedCATasks);
			state.ownedSigils = sortedList(ownedSigils);
			state.ownedLamps = sortedList(ownedLamps);
			state.ownedPrayers = sortedList(ownedPrayers);
			state.diaryTaskCounts = diaryTaskCounts;
			state.bossKillCounts = bossKillCounts;

			Files.writeString(path, gson.toJson(state), StandardCharsets.UTF_8);
		}
		catch (Exception e)
		{
			log.debug("Failed to save progress store: {}", e.getMessage());
		}
	}

	private Path getProgressPath()
	{
		try
		{
			return RuneLite.RUNELITE_DIR.toPath().resolve("dmmtracker").resolve(PROGRESS_FILE_NAME);
		}
		catch (Exception e)
		{
			log.debug("Unable to access RuneLite directory: {}", e.getMessage());
			return null;
		}
	}

	private static List<String> safeList(List<String> list)
	{
		return list != null ? list : Collections.emptyList();
	}

	private static Map<String, Map<String, Integer>> safeMap(Map<String, Map<String, Integer>> map)
	{
		if (map == null)
		{
			return new HashMap<>();
		}

		Map<String, Map<String, Integer>> copy = new HashMap<>();
		for (Map.Entry<String, Map<String, Integer>> entry : map.entrySet())
		{
			copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
		}
		return copy;
	}

	private static List<String> sortedList(Set<String> set)
	{
		List<String> list = new ArrayList<>(set);
		Collections.sort(list);
		return list;
	}
}
