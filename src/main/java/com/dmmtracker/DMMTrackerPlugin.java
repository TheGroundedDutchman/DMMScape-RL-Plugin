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
 * DMMScape Companion RuneLite Plugin
 * Syncs Combat Achievements, Boss KC, and Diary progress to the DMMScape web app
 */
package com.dmmtracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import com.dmmtracker.data.DiaryTaskData;
import com.dmmtracker.data.LocationData;
import com.dmmtracker.data.TeleportData;
import com.dmmtracker.ui.IconManager;
import com.dmmtracker.ui.tabs.DiariesTab;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.config.FlashNotification;
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.config.RequestFocusType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.client.util.Text;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.TrayIcon;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(
	name = "DMMScape",
	description = "Syncs DMMScape progress and plan targets. Build routes on the map and track them in-game.",
	tags = {"dmm", "deadman", "combat achievements", "diary", "boss", "tracker"}
)
public class DMMTrackerPlugin extends Plugin
{
	public static final String PLUGIN_VERSION = "v30";
	private static final Logger log = LoggerFactory.getLogger(DMMTrackerPlugin.class);
	// CA varps: Each varp contains 32 bits, 20 varps total = 640 possible CAs
	private static final int[] CA_VARPS = {
		VarPlayerID.CA_TASK_COMPLETED_0,
		VarPlayerID.CA_TASK_COMPLETED_1,
		VarPlayerID.CA_TASK_COMPLETED_2,
		VarPlayerID.CA_TASK_COMPLETED_3,
		VarPlayerID.CA_TASK_COMPLETED_4,
		VarPlayerID.CA_TASK_COMPLETED_5,
		VarPlayerID.CA_TASK_COMPLETED_6,
		VarPlayerID.CA_TASK_COMPLETED_7,
		VarPlayerID.CA_TASK_COMPLETED_8,
		VarPlayerID.CA_TASK_COMPLETED_9,
		VarPlayerID.CA_TASK_COMPLETED_10,
		VarPlayerID.CA_TASK_COMPLETED_11,
		VarPlayerID.CA_TASK_COMPLETED_12,
		VarPlayerID.CA_TASK_COMPLETED_13,
		VarPlayerID.CA_TASK_COMPLETED_14,
		VarPlayerID.CA_TASK_COMPLETED_15,
		VarPlayerID.CA_TASK_COMPLETED_16,
		VarPlayerID.CA_TASK_COMPLETED_17,
		VarPlayerID.CA_TASK_COMPLETED_18,
		VarPlayerID.CA_TASK_COMPLETED_19,
	};

	// Quest points varp
	private static final int QUEST_POINTS_VARP = VarPlayerID.QP;

	// Collection log entry count varp
	private static final int COLLECTION_LOG_COUNT_VARP = VarPlayerID.COLLECTION_COUNT;

	// Boss KC chat pattern: "Your X kill/completion/chest/etc count is: Y"
	private static final Pattern BOSS_KC_PATTERN = Pattern.compile(
		"Your (.+?) (?:kill|completion|chest|success|harvest) count is: ([\\d,]+)"
	);

	// Clue scroll completion pattern: "You have completed X easy/medium/hard/elite/master Treasure Trails"
	private static final Pattern CLUE_COMPLETION_PATTERN = Pattern.compile(
		"You have completed (\\d+) (beginner|easy|medium|hard|elite|master) Treasure Trails?"
	);

	private static final Pattern DIARY_TIER_PATTERN = Pattern.compile(
		"^(easy|medium|hard|elite)(?:\\s+tasks?)?$",
		Pattern.CASE_INSENSITIVE
	);

	private static final int WEBAPP_PROGRESS_POLL_FAST_SECONDS = 30;
	private static final int WEBAPP_PROGRESS_POLL_IDLE_SECONDS = 180;
	private static final int WEBAPP_PROGRESS_POLL_MAX_SECONDS = 600;
	private static final int WEBAPP_PROGRESS_POLL_MAX_BACKOFF_MULTIPLIER = 8;
	private static final long WEBAPP_PROGRESS_FAST_GRACE_MS = 2 * 60 * 1000L;
	private static final long WEBAPP_PROGRESS_POLL_CHECK_INTERVAL_MS = 10_000L;

	private static final int TARGET_POLL_IDLE_MULTIPLIER = 6;
	private static final int TARGET_POLL_IDLE_MIN_SECONDS = 30;
	private static final int TARGET_POLL_IDLE_MAX_SECONDS = 300;
	private static final long TARGET_POLL_ACTIVE_GRACE_MS = 2 * 60 * 1000L;
	private static final long TARGET_POLL_CHECK_INTERVAL_MS = 5_000L;
	private static final double POLL_JITTER_RATIO = 0.15;
	private static final long REQUIREMENT_SNAPSHOT_INTERVAL_MS = 5_000L;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private DMMTrackerConfig config;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private Notifier notifier;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private SyncService syncService;

	@Inject
	private DeviceAuthService deviceAuthService;

	@Inject
	private RuneliteTokenService runeliteTokenService;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Inject
	private TargetService targetService;

	@Inject
	private DMMTargetOverlay targetOverlay;

	@Inject
	private DMMMinimapOverlay minimapOverlay;

	@Inject
	private RouteOverlay routeOverlay;

	@Inject
	private WorldMapRouteOverlay worldMapRouteOverlay;

	@Inject
	private WorldMapLegendOverlay worldMapLegendOverlay;

	@Inject
	private com.dmmtracker.ui.DMMTabbedPanel panel;

	@Inject
	private com.dmmtracker.data.DataLoader dataLoader;

	@Inject
	private DiaryCompletionService diaryCompletionService;

	@Inject
	private ProgressStore progressStore;

	@Inject
	private TaskLookup taskLookup;

	@Inject
	private RequirementChecker requirementChecker;

	private NavigationButton navButton;
	private final List<TargetMapPoint> worldMapPoints = new ArrayList<>();
	private final List<TeleportMapPoint> teleportMapPoints = new ArrayList<>();
	private final Map<String, BufferedImage> mapIconCache = new HashMap<>();
	private BufferedImage teleportMapIcon;
	private BufferedImage teleportLayerIcon;
	private Runnable teleportDataListener;
	private long lastTargetTimestamp = 0;
	private long lastPanelRefresh = 0;
	private long lastRequirementSnapshotMs = 0;
	private String lastAuthHeader = null;

	// CA tier enum IDs from game data
	private static final Map<Integer, String> CA_TIER_ENUMS = Map.of(
		3981, "easy",
		3982, "medium",
		3983, "hard",
		3984, "elite",
		3985, "master",
		3986, "grandmaster"
	);

	// Diary task count varbits: diary -> [easy, medium, hard, elite]
	private static final Map<String, int[]> DIARY_COUNT_VARBITS = Map.ofEntries(
		Map.entry("Ardougne", new int[]{VarbitID.ARDOUGNE_EASY_COUNT, VarbitID.ARDOUGNE_MED_COUNT, VarbitID.ARDOUGNE_HARD_COUNT, VarbitID.ARDOUGNE_ELITE_COUNT}),
		Map.entry("Desert", new int[]{VarbitID.DESERT_EASY_COUNT, VarbitID.DESERT_MED_COUNT, VarbitID.DESERT_HARD_COUNT, VarbitID.DESERT_ELITE_COUNT}),
		Map.entry("Falador", new int[]{VarbitID.FALADOR_EASY_COUNT, VarbitID.FALADOR_MED_COUNT, VarbitID.FALADOR_HARD_COUNT, VarbitID.FALADOR_ELITE_COUNT}),
		Map.entry("Fremennik", new int[]{VarbitID.FREMENNIK_EASY_COUNT, VarbitID.FREMENNIK_MED_COUNT, VarbitID.FREMENNIK_HARD_COUNT, VarbitID.FREMENNIK_ELITE_COUNT}),
		Map.entry("Kandarin", new int[]{VarbitID.KANDARIN_EASY_COUNT, VarbitID.KANDARIN_MED_COUNT, VarbitID.KANDARIN_HARD_COUNT, VarbitID.KANDARIN_ELITE_COUNT}),
		Map.entry("Karamja", new int[]{VarbitID.KARAMJA_EASY_COUNT, VarbitID.KARAMJA_MED_COUNT, VarbitID.KARAMJA_HARD_COUNT, VarbitID.KARAMJA_ELITE_COUNT}),
		Map.entry("Kourend", new int[]{VarbitID.KOUREND_EASY_COUNT, VarbitID.KOUREND_MED_COUNT, VarbitID.KOUREND_HARD_COUNT, VarbitID.KOUREND_ELITE_COUNT}),
		Map.entry("Lumbridge", new int[]{VarbitID.LUMBRIDGE_EASY_COUNT, VarbitID.LUMBRIDGE_MED_COUNT, VarbitID.LUMBRIDGE_HARD_COUNT, VarbitID.LUMBRIDGE_ELITE_COUNT}),
		Map.entry("Morytania", new int[]{VarbitID.MORYTANIA_EASY_COUNT, VarbitID.MORYTANIA_MED_COUNT, VarbitID.MORYTANIA_HARD_COUNT, VarbitID.MORYTANIA_ELITE_COUNT}),
		Map.entry("Varrock", new int[]{VarbitID.VARROCK_EASY_COUNT, VarbitID.VARROCK_MED_COUNT, VarbitID.VARROCK_HARD_COUNT, VarbitID.VARROCK_ELITE_COUNT}),
		Map.entry("Western", new int[]{VarbitID.WESTERN_EASY_COUNT, VarbitID.WESTERN_MED_COUNT, VarbitID.WESTERN_HARD_COUNT, VarbitID.WESTERN_ELITE_COUNT}),
		Map.entry("Wilderness", new int[]{VarbitID.WILDERNESS_EASY_COUNT, VarbitID.WILDERNESS_MED_COUNT, VarbitID.WILDERNESS_HARD_COUNT, VarbitID.WILDERNESS_ELITE_COUNT})
	);

	private static final Map<String, String> DIARY_NAME_ALIASES = Map.ofEntries(
		Map.entry("kourend & kebos", "Kourend"),
		Map.entry("lumbridge & draynor", "Lumbridge"),
		Map.entry("western provinces", "Western")
	);

	// Cached state for delta detection
	private Set<Integer> lastCompletedCAs = new HashSet<>();
	private Map<String, Integer> lastBossKCs = new HashMap<>();
	private Map<String, Integer> lastSkillLevels = new HashMap<>();
	private Map<String, Integer> lastClueCompletions = new HashMap<>();
	private int lastQuestPoints = -1;
	private int lastCollectionLogCount = -1;
	private Map<String, Boolean> lastDiaryTiers = new HashMap<>();
	private Map<String, Integer> lastDiaryTaskCounts = new HashMap<>();
	private Set<String> lastCompletedDiaryTasks = new HashSet<>();
	private Set<String> lastCompletedQuests = new HashSet<>();
	private final Set<String> pendingDiarySyncs = new HashSet<>();

	// Player alarm state (adapted from Wilderness Player Alarm by Alex, BSD 2-Clause).
	private final Set<String> visiblePlayers = new HashSet<>();
	private final Map<String, Long> lastAlarmTimes = new HashMap<>();
	private boolean playerAlarmReady = false;

	// Dynamic CA mapping: game ID -> CA name
	private Map<Integer, String> caNameMapping = new HashMap<>();
	private boolean caMapInitialized = false;

	// Flag to track if we've done initial sync
	private boolean initialSyncDone = false;
	private boolean manualSyncRequested = false;

	private boolean ownedSyncReady = false;
	private boolean pendingOwnedSync = false;

	// Webapp progress import state
	private JsonObject pendingWebappProgress = null;
	private boolean webappProgressFetched = false;
	private ScheduledFuture<?> webappProgressPollTask = null;
	private int webappProgressBackoffMultiplier = 1;
	private int currentWebappProgressPollIntervalSeconds = WEBAPP_PROGRESS_POLL_FAST_SECONDS;
	private long webappProgressFastUntilMs = 0;
	private long lastWebappPollCheckMs = 0;
	private int webappProgressNoChangeStreak = 0;

	private long lastTargetInteractionMs = 0;
	private long lastTargetPollCheckMs = 0;
	private int lastTargetPollIntervalSeconds = -1;

	@Override
	protected void startUp() throws Exception
	{
		log.info("DMMScape {} started", PLUGIN_VERSION);
		initialSyncDone = false;
		ownedSyncReady = false;
		pendingOwnedSync = false;
		webappProgressFetched = false;
		pendingWebappProgress = null;

		if (ApiConfig.FORCE_SYNC_DEFAULTS)
		{
			// Keep sync enabled for bidirectional sync in local dev builds
			if (!config.syncEnabled())
			{
				configManager.setConfiguration("dmmtracker", "syncEnabled", true);
			}
			if (!config.autoImportWebapp())
			{
				configManager.setConfiguration("dmmtracker", "autoImportWebapp", true);
			}
			if (!config.enableTargetSync())
			{
				configManager.setConfiguration("dmmtracker", "enableTargetSync", true);
			}
		}
		ensureLocalEndpoints();

		if (requirementChecker != null)
		{
			requirementChecker.clearSnapshot();
			lastRequirementSnapshotMs = 0;
			clientThread.invokeLater(requirementChecker::refreshSnapshot);
		}

		panel.setManualSyncHandler(this::triggerManualSync);
		panel.setTargetToggleHandler(this::handleTargetToggle);
		panel.setTargetNavigateHandler(this::handleTargetNavigate);
		panel.setTargetRefreshHandler(this::handleTargetRefresh);
		panel.setOpenWebHandler(this::handleOpenWeb);
		panel.setBossAddToPlanHandler(this::handleBossAddToPlan);
		panel.setUnsyncHandler(this::handleUnsync);
		panel.setUnlinkHandler(this::handleUnlink);
		panel.setLinkAccountHandler(this::handleLinkAccount);
		panel.setForceSyncHandler(this::forceSync);
		panel.setBackToPlanHandler(this::refreshPanel);
		panel.setDiaryLocationHandler(this::handleDiaryLocationNavigate);
		panel.setClearNavigationHandler(this::clearNavigationTarget);
		panel.setDiaryLocationFocusChecker(new DiariesTab.LocationFocusChecker()
		{
			@Override
			public boolean isFocused(DiaryTaskData task, String region, String tier)
			{
				return targetService.isFocusTarget(buildDiaryFocusKey(task, region, tier));
			}

			@Override
			public boolean hasFocus()
			{
				return targetService.getFocusTarget() != null;
			}
		});

		if (progressStore != null)
		{
			progressStore.addListener(update ->
			{
				if (update.getSource() == ProgressStore.UpdateSource.LOCAL
					&& (update.hasField(ProgressStore.UpdateField.OWNED_SIGILS)
						|| update.hasField(ProgressStore.UpdateField.OWNED_LAMPS)
						|| update.hasField(ProgressStore.UpdateField.OWNED_PRAYERS)))
				{
					if (ownedSyncReady && config.syncEnabled())
					{
						sendOwnedSync();
					}
					else
					{
						pendingOwnedSync = true;
					}
				}

				SwingUtilities.invokeLater(this::refreshPanel);
			});
		}

		navButton = NavigationButton.builder()
			.tooltip("DMMScape")
			.icon(createNavIcon())
			.priority(5)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

		// Start target overlay system if enabled
		if (config.enableTargetSync())
		{
			targetService.startPolling();
			overlayManager.add(targetOverlay);
			overlayManager.add(minimapOverlay);
			overlayManager.add(routeOverlay);
			overlayManager.add(worldMapRouteOverlay);
			overlayManager.add(worldMapLegendOverlay);
			mouseManager.registerMouseListener(worldMapLegendOverlay);
			log.info("Target overlay system started");
			updateTargetPollingInterval(true);
		}

		if (dataLoader != null)
		{
			teleportDataListener = () -> clientThread.invokeLater(this::refreshTeleportMapPoints);
			dataLoader.addUpdateListener(teleportDataListener);
			refreshTeleportMapPoints();
		}

		if (config.syncEnabled() || config.enableTargetSync())
		{
			if (runeliteTokenService != null)
			{
				runeliteTokenService.refreshIfNeeded();
			}
			syncAuthHeaderIfNeeded();
		}

		if (config.syncEnabled())
		{
			startWebappProgressPolling();
		}

		// Refresh panel on startup to show current state
		SwingUtilities.invokeLater(this::refreshPanel);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("DMMScape {} stopped", PLUGIN_VERSION);

		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}

		clearWorldMapPoints();
		clearTeleportMapPoints();
		if (dataLoader != null && teleportDataListener != null)
		{
			dataLoader.removeUpdateListener(teleportDataListener);
			teleportDataListener = null;
		}

		// Stop target overlay system
		targetService.stopPolling();
		overlayManager.remove(targetOverlay);
		overlayManager.remove(minimapOverlay);
		overlayManager.remove(routeOverlay);
		overlayManager.remove(worldMapRouteOverlay);
		overlayManager.remove(worldMapLegendOverlay);
		mouseManager.unregisterMouseListener(worldMapLegendOverlay);
		stopWebappProgressPolling();

		lastCompletedCAs.clear();
		lastBossKCs.clear();
		lastSkillLevels.clear();
		lastClueCompletions.clear();
		lastDiaryTiers.clear();
		lastDiaryTaskCounts.clear();
		lastCompletedDiaryTasks.clear();
		lastCompletedQuests.clear();
		pendingDiarySyncs.clear();
		visiblePlayers.clear();
		lastAlarmTimes.clear();
		playerAlarmReady = false;
		lastQuestPoints = -1;
		lastCollectionLogCount = -1;
		initialSyncDone = false;
		ownedSyncReady = false;
		pendingOwnedSync = false;
		webappProgressFetched = false;
		pendingWebappProgress = null;
	}

	@Provides
	DMMTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DMMTrackerConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			if (requirementChecker != null)
			{
				requirementChecker.clearSnapshot();
				lastRequirementSnapshotMs = 0;
			}

			// Schedule initial sync after a short delay to ensure varps are loaded
			executor.schedule(this::performInitialSync, 2, TimeUnit.SECONDS);
			executor.schedule(() -> clientThread.invokeLater(this::initializePlayerAlarmCache), 2, TimeUnit.SECONDS);
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			// Reset state on logout
			initialSyncDone = false;
			manualSyncRequested = false;
			ownedSyncReady = false;
			pendingOwnedSync = false;
			webappProgressFetched = false;
			pendingWebappProgress = null;
			lastCompletedCAs.clear();
			lastBossKCs.clear();
			lastSkillLevels.clear();
			lastClueCompletions.clear();
			lastDiaryTiers.clear();
			lastDiaryTaskCounts.clear();
			lastCompletedDiaryTasks.clear();
			lastCompletedQuests.clear();
			pendingDiarySyncs.clear();
			visiblePlayers.clear();
			lastAlarmTimes.clear();
			playerAlarmReady = false;
			lastQuestPoints = -1;
			lastCollectionLogCount = -1;

			if (requirementChecker != null)
			{
				requirementChecker.clearSnapshot();
				lastRequirementSnapshotMs = 0;
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (!initialSyncDone || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		// Check if it's a CA varp change
		int varpId = event.getVarpId();
		if (config.trackCAs() && isCACompletionVarp(varpId))
		{
			checkCAChanges();
		}

		// Check quest point changes - also triggers quest completion check
		if (config.trackQuests() && varpId == QUEST_POINTS_VARP)
		{
			checkQuestPointChanges();
			// Quest completion sync must run on client thread
			clientThread.invokeLater(this::checkQuestCompletionChanges);
		}

		if (config.trackCollectionLog() && varpId == COLLECTION_LOG_COUNT_VARP)
		{
			checkCollectionLogChanges();
		}

		// Check diary varbit changes
		if (config.trackDiaries())
		{
			checkDiaryChanges();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (!config.trackDiaries() || !initialSyncDone)
		{
			return;
		}

		if (event.getGroupId() != InterfaceID.JOURNALSCROLL)
		{
			return;
		}

		clientThread.invokeLater(this::syncDiaryTaskUpdatesFromJournal);
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (!initialSyncDone || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		String skillName = event.getSkill().getName().toLowerCase();
		int newLevel = event.getLevel();
		int lastLevel = lastSkillLevels.getOrDefault(skillName, 0);

		if (newLevel > lastLevel)
		{
			log.info("Skill level up: {} {} -> {}", skillName, lastLevel, newLevel);
			lastSkillLevels.put(skillName, newLevel);

			SyncData delta = new SyncData();
			delta.setUsername(client.getLocalPlayer().getName());
			delta.setProfileType(getProfileType());
			delta.setTimestamp(System.currentTimeMillis());
			delta.setFullSync(false);

			Map<String, Integer> skillLevels = new HashMap<>();
			skillLevels.put(skillName, newLevel);
			delta.setSkillLevels(skillLevels);

			if (config.syncEnabled())
			{
				sendSyncWithAuth(delta);
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE &&
			event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String message = event.getMessage();

		if (config.trackBossKC())
		{
			// Check for boss KC message
			Matcher kcMatcher = BOSS_KC_PATTERN.matcher(message);
			if (kcMatcher.find())
			{
				String bossName = kcMatcher.group(1).trim();
				int killCount = Integer.parseInt(kcMatcher.group(2).replace(",", ""));
				handleBossKCUpdate(bossName, killCount);
				return;
			}
		}

		// Check for clue scroll completion message
		Matcher clueMatcher = CLUE_COMPLETION_PATTERN.matcher(message);
		if (clueMatcher.find())
		{
			int count = Integer.parseInt(clueMatcher.group(1));
			String tier = clueMatcher.group(2).toLowerCase();
			handleClueCompletion(tier, count);
		}
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event)
	{
		if (!config.playerAlarmEnabled() || !playerAlarmReady || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Player local = client.getLocalPlayer();
		Player player = event.getPlayer();
		if (local == null || player == null || player == local || player.getName() == null)
		{
			return;
		}

		String name = Text.removeTags(player.getName());
		if (name == null || name.isBlank())
		{
			return;
		}

		if (!visiblePlayers.add(name))
		{
			return;
		}

		if (shouldIgnorePlayer(player))
		{
			return;
		}

		long now = System.currentTimeMillis();
		if (isAlarmOnCooldown(name, now))
		{
			return;
		}

		sendPlayerAlarm(player, name);
		lastAlarmTimes.put(name, now);
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned event)
	{
		Player player = event.getPlayer();
		if (player == null || player.getName() == null)
		{
			return;
		}

		String name = Text.removeTags(player.getName());
		if (name != null)
		{
			visiblePlayers.remove(name);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"dmmtracker".equals(event.getGroup()))
		{
			return;
		}

		if (ApiConfig.FORCE_SYNC_DEFAULTS)
		{
			if ("syncEnabled".equals(event.getKey()) && !config.syncEnabled())
			{
				// Keep sync enabled for bidirectional sync in local dev builds
				configManager.setConfiguration("dmmtracker", "syncEnabled", true);
				return;
			}

			if ("autoImportWebapp".equals(event.getKey()) && !config.autoImportWebapp())
			{
				configManager.setConfiguration("dmmtracker", "autoImportWebapp", true);
				return;
			}

			if ("enableTargetSync".equals(event.getKey()) && !config.enableTargetSync())
			{
				configManager.setConfiguration("dmmtracker", "enableTargetSync", true);
				return;
			}
		}

		if (ApiConfig.USE_LOCAL && ("webhookUrl".equals(event.getKey()) || "targetSyncUrl".equals(event.getKey())))
		{
			ensureLocalEndpoints();
			return;
		}

		if ("enableTargetSync".equals(event.getKey()))
		{
			if (config.enableTargetSync())
			{
				targetService.startPolling();
				overlayManager.add(targetOverlay);
				overlayManager.add(minimapOverlay);
				overlayManager.add(routeOverlay);
				overlayManager.add(worldMapRouteOverlay);
				overlayManager.add(worldMapLegendOverlay);
				mouseManager.registerMouseListener(worldMapLegendOverlay);
				refreshWorldMapPoints();
				log.info("Target overlay system enabled via config");
				updateTargetPollingInterval(true);
			}
			else
			{
				targetService.stopPolling();
				overlayManager.remove(targetOverlay);
				overlayManager.remove(minimapOverlay);
				overlayManager.remove(routeOverlay);
				overlayManager.remove(worldMapRouteOverlay);
				overlayManager.remove(worldMapLegendOverlay);
				mouseManager.unregisterMouseListener(worldMapLegendOverlay);
				clearWorldMapPoints();
				log.info("Target overlay system disabled via config");
			}
		}

		if ("targetPollInterval".equals(event.getKey()))
		{
			if (config.enableTargetSync())
			{
				updateTargetPollingInterval(true);
			}
		}

		if ("showTargetOverlay".equals(event.getKey()))
		{
			if (config.enableTargetSync() && config.showTargetOverlay())
			{
				overlayManager.add(targetOverlay);
				overlayManager.add(minimapOverlay);
			}
			else
			{
				overlayManager.remove(targetOverlay);
				overlayManager.remove(minimapOverlay);
			}
		}

		if ("enableRouteDrawing".equals(event.getKey()) ||
			"showSceneRoute".equals(event.getKey()) ||
			"showMinimapRoute".equals(event.getKey()) ||
			"showWorldMapRoute".equals(event.getKey()))
		{
			// Route overlays update automatically based on config in their render methods
			log.debug("Route display config changed");
		}

		if ("targetSyncUrl".equals(event.getKey())
			|| "apiKey".equals(event.getKey())
			|| "accessToken".equals(event.getKey()))
		{
			if (runeliteTokenService != null)
			{
				runeliteTokenService.clearToken();
			}
			lastAuthHeader = null;
			targetService.setAuthHeader(null);
			if (config.enableTargetSync())
			{
				targetService.refreshNow();
			}
		}

		if ("showWorldMapTargets".equals(event.getKey())
			|| "maxMapTargets".equals(event.getKey())
			|| "mapShowCompleted".equals(event.getKey())
			|| "mapShowDiaryTargets".equals(event.getKey())
			|| "mapShowQuestTargets".equals(event.getKey())
			|| "mapShowBossTargets".equals(event.getKey())
			|| "mapShowCaTargets".equals(event.getKey())
			|| "mapShowSigilTargets".equals(event.getKey())
			|| "mapShowLampTargets".equals(event.getKey())
			|| "mapShowPrayerTargets".equals(event.getKey())
			|| "mapShowTeleportTargets".equals(event.getKey())
			|| "mapShowCustomTargets".equals(event.getKey()))
		{
			refreshWorldMapPoints();
		}

		if ("showWorldMapTeleports".equals(event.getKey())
			|| "maxWorldMapTeleports".equals(event.getKey()))
		{
			refreshTeleportMapPoints();
		}

		if ("allowTargetEdits".equals(event.getKey()))
		{
			refreshPanel();
		}

		if ("syncEnabled".equals(event.getKey()) && config.syncEnabled())
		{
			triggerManualSync();
			startWebappProgressPolling();
		}
		else if ("syncEnabled".equals(event.getKey()))
		{
			stopWebappProgressPolling();
		}
	}

	private void ensureLocalEndpoints()
	{
		if (!ApiConfig.USE_LOCAL)
		{
			return;
		}

		if (!ApiConfig.syncUrl().equals(config.webhookUrl()))
		{
			configManager.setConfiguration("dmmtracker", "webhookUrl", ApiConfig.syncUrl());
		}

		if (!ApiConfig.targetSyncUrl().equals(config.targetSyncUrl()))
		{
			configManager.setConfiguration("dmmtracker", "targetSyncUrl", ApiConfig.targetSyncUrl());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		long now = System.currentTimeMillis();

		if (requirementChecker != null)
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				if (now - lastRequirementSnapshotMs >= REQUIREMENT_SNAPSHOT_INTERVAL_MS)
				{
					requirementChecker.refreshSnapshot();
					lastRequirementSnapshotMs = now;
				}
			}
			else if (lastRequirementSnapshotMs != 0)
			{
				requirementChecker.clearSnapshot();
				lastRequirementSnapshotMs = 0;
			}
		}

		if (config.syncEnabled() || config.enableTargetSync())
		{
			if (runeliteTokenService != null)
			{
				runeliteTokenService.refreshIfNeeded();
			}
			syncAuthHeaderIfNeeded();
		}

		updateTargetPollingInterval(false);
		updateWebappProgressPollingInterval(false);

		if (config.enableTargetSync())
		{
			long targetTimestamp = targetService.getLastFetchTimestamp();
			if (targetTimestamp != lastTargetTimestamp)
			{
				lastTargetTimestamp = targetTimestamp;
				refreshWorldMapPoints();
				refreshPanel();
			}
		}
		else if (!worldMapPoints.isEmpty())
		{
			if (targetService.getFocusTarget() == null || !config.showWorldMapTargets())
			{
				clearWorldMapPoints();
			}
		}

		if (now - lastPanelRefresh > 1000)
		{
			lastPanelRefresh = now;
			refreshPanelStatusOnly();
		}
	}

	/**
	 * Performs the initial full state sync when a player logs in.
	 * This ensures we capture all existing progress, not just live changes.
	 */
	private void performInitialSync()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		// Run on client thread since Quest.getState() requires it
		clientThread.invokeLater(() ->
		{
			log.debug("Performing initial DMM tracker sync");

			String username = client.getLocalPlayer() != null ?
				client.getLocalPlayer().getName() : null;

			if (username == null || username.isEmpty())
			{
				log.warn("Could not get player name for sync");
				return;
			}

			// Initialize CA name mapping from game data
			if (config.trackCAs() && !caMapInitialized)
			{
				initializeCANameMapping();
			}

			// Gather all current state
			SyncData syncData = new SyncData();
			syncData.setUsername(username);
			syncData.setProfileType(getProfileType());
			syncData.setTimestamp(System.currentTimeMillis());
			syncData.setFullSync(true);

			// Get all completed CAs
			Set<Integer> completedCAs = Collections.emptySet();
			if (config.trackCAs())
			{
				completedCAs = getAllCompletedCAs();
				syncData.setCompletedCAs(completedCAs);
				// Include the CA name mapping for the web app
				syncData.setCaNameMapping(new HashMap<>(caNameMapping));
			}
			lastCompletedCAs = new HashSet<>(completedCAs);
			updateCompletedCaTasks(completedCAs, ProgressStore.UpdateSource.GAME);

			// Get quest points
			int questPoints = client.getVarpValue(QUEST_POINTS_VARP);
			if (config.trackQuests())
			{
				syncData.setQuestPoints(questPoints);

				// Get completed quests
				Set<String> completedQuests = getAllCompletedQuests();
				syncData.setCompletedQuests(completedQuests);
				lastCompletedQuests = new HashSet<>(completedQuests);
			}
			lastQuestPoints = questPoints;

			if (config.trackCollectionLog())
			{
				int collectionLogCount = client.getVarpValue(COLLECTION_LOG_COUNT_VARP);
				syncData.setCollectionLogCount(collectionLogCount);
				lastCollectionLogCount = collectionLogCount;
			}

			// Get diary tier completions and task counts
			Map<String, Map<String, Boolean>> diaryProgress = Collections.emptyMap();
			Map<String, Map<String, Integer>> diaryTaskCounts = Collections.emptyMap();
			if (config.trackDiaries())
			{
				diaryProgress = getAllDiaryProgress();
				diaryTaskCounts = getAllDiaryTaskCounts();
				syncData.setDiaryProgress(diaryProgress);
				syncData.setDiaryTaskCounts(diaryTaskCounts);
				if (diaryCompletionService != null)
				{
					Set<String> completedDiaryTasks = diaryCompletionService.getCompletedTasks();
					syncData.setCompletedDiaryTasks(completedDiaryTasks);
					lastCompletedDiaryTasks = new HashSet<>(completedDiaryTasks);
					progressStore.setCompletedDiaryTasks(completedDiaryTasks, ProgressStore.UpdateSource.GAME);
				}
			}
			updateLastDiaryTiers(diaryProgress);
			updateLastDiaryTaskCounts(diaryTaskCounts);
			progressStore.setDiaryTaskCounts(diaryTaskCounts, ProgressStore.UpdateSource.GAME);

			// Get all skill levels
			Map<String, Integer> skillLevels = getAllSkillLevels();
			syncData.setSkillLevels(skillLevels);
			lastSkillLevels = new HashMap<>(skillLevels);

			// Send the sync
			boolean shouldSync = config.syncEnabled() || manualSyncRequested;
			if (shouldSync)
			{
				if (manualSyncRequested)
				{
					sendSyncWithAuth(syncData, new SyncService.SyncCallback()
					{
						@Override
						public void onSuccess()
						{
							notifyGameMessage("DMMScape sync complete.");
						}

						@Override
						public void onError(String message)
						{
							notifyGameMessage("DMMScape sync failed: " + message);
						}
					}, this::handleSyncResponse);
					manualSyncRequested = false;
				}
				else
				{
					sendSyncWithAuth(syncData, null, this::handleSyncResponse);
				}
			}

			// Also generate export data for manual copy
			String exportJson = syncService.generateExportJson(syncData);
			log.info("DMMScape initial sync complete. {} CAs, {} QP, {} quests completed",
				completedCAs.size(), questPoints, lastCompletedQuests.size());

			initialSyncDone = true;

			// Fetch webapp progress to check for data we might be missing (e.g., hiscores)
			executor.schedule(this::fetchWebappProgress, 2, TimeUnit.SECONDS);
		});
	}

	/**
	 * Gets all completed CA task IDs by reading the CA varps.
	 * Each varp contains 32 bits, where each bit represents a CA task.
	 */
	private Set<Integer> getAllCompletedCAs()
	{
		Set<Integer> completed = new HashSet<>();

		for (int varpIndex = 0; varpIndex < CA_VARPS.length; varpIndex++)
		{
			int varpValue = client.getVarpValue(CA_VARPS[varpIndex]);

			for (int bit = 0; bit < 32; bit++)
			{
				if ((varpValue & (1 << bit)) != 0)
				{
					// CA ID = varpIndex * 32 + bit
					int caId = varpIndex * 32 + bit;
					completed.add(caId);
				}
			}
		}

		return completed;
	}

	private boolean isCACompletionVarp(int varpId)
	{
		for (int id : CA_VARPS)
		{
			if (id == varpId)
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Gets diary progress for all diaries.
	 * Note: Per-task completion is NOT available via varps, only tier completion.
	 */
	private Map<String, Map<String, Boolean>> getAllDiaryProgress()
	{
		Map<String, Map<String, Boolean>> progress = new HashMap<>();

		// Diary varbits for tier completion
		// Format: [easy, medium, hard, elite]
		Map<String, int[]> diaryVarbits = getDiaryVarbits();

		for (Map.Entry<String, int[]> entry : diaryVarbits.entrySet())
		{
			String diaryName = entry.getKey();
			int[] varbits = entry.getValue();

			Map<String, Boolean> tierProgress = new HashMap<>();
			String[] tierNames = {"easy", "medium", "hard", "elite"};

			for (int i = 0; i < varbits.length && i < tierNames.length; i++)
			{
				int value = client.getVarbitValue(varbits[i]);
				boolean completed = "Karamja".equals(diaryName) ? value >= 2 : value > 0;
				tierProgress.put(tierNames[i], completed);
			}

			progress.put(diaryName, tierProgress);
		}

		return progress;
	}

	private void updateLastDiaryTiers(Map<String, Map<String, Boolean>> progress)
	{
		lastDiaryTiers.clear();

		for (Map.Entry<String, Map<String, Boolean>> diary : progress.entrySet())
		{
			for (Map.Entry<String, Boolean> tier : diary.getValue().entrySet())
			{
				String key = diary.getKey() + "_" + tier.getKey();
				lastDiaryTiers.put(key, tier.getValue());
			}
		}
	}

	/**
	 * Returns the diary varbit IDs for each diary.
	 * These track tier completion status.
	 */
	private Map<String, int[]> getDiaryVarbits()
	{
		Map<String, int[]> varbits = new HashMap<>();

		// Varbit IDs for diary tier completion (from wiki)
		varbits.put("Ardougne", new int[]{4458, 4459, 4460, 4461});
		varbits.put("Desert", new int[]{4483, 4484, 4485, 4486});
		varbits.put("Falador", new int[]{4462, 4463, 4464, 4465});
		varbits.put("Fremennik", new int[]{4491, 4492, 4493, 4494});
		varbits.put("Kandarin", new int[]{4475, 4476, 4477, 4478});
		varbits.put("Karamja", new int[]{3578, 3599, 3611, 4566});
		varbits.put("Kourend", new int[]{7925, 7926, 7927, 7928});
		varbits.put("Lumbridge", new int[]{4495, 4496, 4497, 4498});
		varbits.put("Morytania", new int[]{4487, 4488, 4489, 4490});
		varbits.put("Varrock", new int[]{4479, 4480, 4481, 4482});
		varbits.put("Western", new int[]{4471, 4472, 4473, 4474});
		varbits.put("Wilderness", new int[]{4466, 4467, 4468, 4469});

		return varbits;
	}

	/**
	 * Gets all current skill levels.
	 */
	private Map<String, Integer> getAllSkillLevels()
	{
		Map<String, Integer> levels = new HashMap<>();

		for (Skill skill : Skill.values())
		{
			// Skip overall (virtual level)
			if (skill == Skill.OVERALL)
			{
				continue;
			}

			int level = client.getRealSkillLevel(skill);
			if (level > 0)
			{
				levels.put(skill.getName().toLowerCase(), level);
			}
		}

		return levels;
	}

	/**
	 * Checks for CA changes and sends delta update if needed.
	 */
	private void checkCAChanges()
	{
		Set<Integer> currentCAs = getAllCompletedCAs();
		Set<Integer> newCAs = new HashSet<>(currentCAs);
		newCAs.removeAll(lastCompletedCAs);

		if (!newCAs.isEmpty())
		{
			log.info("New CAs completed: {}", newCAs);

			SyncData delta = new SyncData();
			delta.setUsername(client.getLocalPlayer().getName());
			delta.setProfileType(getProfileType());
			delta.setTimestamp(System.currentTimeMillis());
			delta.setFullSync(false);
			delta.setCompletedCAs(newCAs);

			if (config.syncEnabled())
			{
				sendSyncWithAuth(delta);
			}

			lastCompletedCAs = currentCAs;
			updateCompletedCaTasks(currentCAs, ProgressStore.UpdateSource.GAME);
		}
	}

	private void updateCompletedCaTasks(Set<Integer> completedCAs, ProgressStore.UpdateSource source)
	{
		if (taskLookup == null || completedCAs == null)
		{
			return;
		}

		Set<String> mapped = taskLookup.mapCaTaskIds(completedCAs, caNameMapping);
		progressStore.setCompletedCATasks(mapped, source);
	}

	/**
	 * Checks for diary tier changes and sends delta update if needed.
	 */
	private void checkDiaryChanges()
	{
		Map<String, Map<String, Boolean>> currentProgress = getAllDiaryProgress();
		Map<String, Map<String, Integer>> currentCounts = getAllDiaryTaskCounts();
		Set<String> currentCompletedTasks = Collections.emptySet();
		List<String> newCompletions = new ArrayList<>();
		List<String> countIncreases = new ArrayList<>();
		boolean countsChanged = false;
		boolean tasksChanged = false;

		for (Map.Entry<String, Map<String, Boolean>> diary : currentProgress.entrySet())
		{
			for (Map.Entry<String, Boolean> tier : diary.getValue().entrySet())
			{
				String key = diary.getKey() + "_" + tier.getKey();
				Boolean lastValue = lastDiaryTiers.get(key);

				if (tier.getValue() && (lastValue == null || !lastValue))
				{
					newCompletions.add(key);
				}
			}
		}

		for (Map.Entry<String, Map<String, Integer>> diary : currentCounts.entrySet())
		{
			for (Map.Entry<String, Integer> tier : diary.getValue().entrySet())
			{
				String key = diary.getKey() + "_" + tier.getKey();
				int current = tier.getValue();
				int last = lastDiaryTaskCounts.getOrDefault(key, 0);
				if (current != last)
				{
					countsChanged = true;
				}
				if (current > last)
				{
					if (config.syncEnabled())
					{
						pendingDiarySyncs.add(key);
						countIncreases.add(diary.getKey() + " " + tier.getKey());
					}
				}
			}
		}

		if (diaryCompletionService != null)
		{
			currentCompletedTasks = diaryCompletionService.getCompletedTasks();
			tasksChanged = !currentCompletedTasks.equals(lastCompletedDiaryTasks);
		}

		if (!newCompletions.isEmpty() || countsChanged || tasksChanged)
		{
			SyncData delta = new SyncData();
			delta.setUsername(client.getLocalPlayer().getName());
			delta.setProfileType(getProfileType());
			delta.setTimestamp(System.currentTimeMillis());
			delta.setFullSync(false);

			if (!newCompletions.isEmpty())
			{
				log.info("New diary tiers completed: {}", newCompletions);
				delta.setDiaryProgress(currentProgress);
			}
			if (countsChanged)
			{
				delta.setDiaryTaskCounts(currentCounts);
			}
			if (tasksChanged)
			{
				delta.setCompletedDiaryTasks(currentCompletedTasks);
			}

			if (config.syncEnabled())
			{
				sendSyncWithAuth(delta);
			}
		}

		if (!countIncreases.isEmpty())
		{
			queueDiarySyncPrompt(countIncreases);
		}

		updateLastDiaryTiers(currentProgress);
		updateLastDiaryTaskCounts(currentCounts);
		progressStore.setDiaryTaskCounts(currentCounts, ProgressStore.UpdateSource.GAME);
		if (tasksChanged)
		{
			lastCompletedDiaryTasks = new HashSet<>(currentCompletedTasks);
			progressStore.setCompletedDiaryTasks(currentCompletedTasks, ProgressStore.UpdateSource.GAME);
		}
	}

	private void syncDiaryTaskUpdatesFromJournal()
	{
		Widget titleWidget = client.getWidget(InterfaceID.Journalscroll.TITLE);
		if (titleWidget == null)
		{
			return;
		}

		String header = Text.removeTags(titleWidget.getText());
		if (header == null || !header.toLowerCase().contains("achievement diary"))
		{
			return;
		}

		Widget textLayer = client.getWidget(InterfaceID.Journalscroll.TEXTLAYER);
		if (textLayer == null)
		{
			return;
		}

		Widget[] children = textLayer.getStaticChildren();
		if (children == null || children.length == 0)
		{
			return;
		}

		String diaryName = null;
		for (Widget child : children)
		{
			String rawText = child.getText();
			if (rawText == null || rawText.isEmpty())
			{
				continue;
			}
			String cleanText = Text.removeTags(rawText).trim();
			if (cleanText.endsWith("Tasks") || cleanText.endsWith("Area Tasks"))
			{
				diaryName = extractDiaryName(cleanText);
				break;
			}
		}
		if (diaryName == null && children.length > 0)
		{
			diaryName = extractDiaryName(children[0].getText());
		}
		if (diaryName == null || diaryName.isEmpty())
		{
			return;
		}

		String tier = detectDiaryTier(children);
		if (config.syncEnabled() && !hasPendingDiarySync(diaryName, tier))
		{
			return;
		}

		List<DiaryTaskUpdate> updates = new ArrayList<>();
		for (Widget child : children)
		{
			String rawText = child.getText();
			if (rawText == null || rawText.isEmpty())
			{
				continue;
			}
			if (!rawText.contains("<str>"))
			{
				continue;
			}
			String cleanText = Text.removeTags(rawText).trim();
			if (cleanText.isEmpty())
			{
				continue;
			}

			DiaryTaskUpdate update = new DiaryTaskUpdate();
			update.setDiary(diaryName);
			update.setTier(tier);
			update.setText(cleanText);
			update.setCompleted(true);
			updates.add(update);
		}

		if (updates.isEmpty())
		{
			return;
		}

		List<String> matchedTaskIds = new ArrayList<>();
		for (DiaryTaskUpdate update : updates)
		{
			String taskId = taskLookup.findDiaryTaskId(update.getDiary(), update.getText());
			if (taskId != null)
			{
				matchedTaskIds.add(taskId);
			}
		}
		if (!matchedTaskIds.isEmpty())
		{
			progressStore.addCompletedDiaryTasks(matchedTaskIds, ProgressStore.UpdateSource.GAME);
		}

		SyncData delta = new SyncData();
		delta.setUsername(client.getLocalPlayer().getName());
		delta.setProfileType(getProfileType());
		delta.setTimestamp(System.currentTimeMillis());
		delta.setFullSync(false);
		delta.setDiaryTaskUpdates(updates);

		if (config.syncEnabled())
		{
			sendSyncWithAuth(delta);
			clearPendingDiarySync(diaryName, tier);
		}
	}

	private String extractDiaryName(String title)
	{
		if (title == null)
		{
			return null;
		}

		String cleaned = Text.removeTags(title)
			.replaceAll("(?i)area tasks", "")
			.replaceAll("(?i)tasks", "")
			.replaceAll("(?i)diary", "")
			.trim()
			.replaceAll("\\s+", " ");

		if (cleaned.isEmpty())
		{
			return null;
		}

		return normalizeDiaryName(cleaned);
	}

	private String normalizeDiaryName(String name)
	{
		String alias = DIARY_NAME_ALIASES.get(name.toLowerCase());
		if (alias != null)
		{
			return alias;
		}
		return name;
	}

	private String detectDiaryTier(Widget[] children)
	{
		for (Widget child : children)
		{
			String rawText = child.getText();
			if (rawText == null || rawText.isEmpty())
			{
				continue;
			}
			String cleanText = Text.removeTags(rawText).trim().toLowerCase();
			Matcher matcher = DIARY_TIER_PATTERN.matcher(cleanText);
			if (matcher.matches())
			{
				return matcher.group(1).toLowerCase();
			}
		}

		return null;
	}

	private boolean hasPendingDiarySync(String diaryName, String tier)
	{
		if (pendingDiarySyncs.isEmpty())
		{
			return false;
		}

		if (tier != null)
		{
			return pendingDiarySyncs.contains(diaryName + "_" + tier);
		}

		for (String key : pendingDiarySyncs)
		{
			if (key.startsWith(diaryName + "_"))
			{
				return true;
			}
		}

		return false;
	}

	private void clearPendingDiarySync(String diaryName, String tier)
	{
		if (tier != null)
		{
			pendingDiarySyncs.remove(diaryName + "_" + tier);
			return;
		}

		pendingDiarySyncs.removeIf(key -> key.startsWith(diaryName + "_"));
	}

	private void queueDiarySyncPrompt(List<String> countIncreases)
	{
		if (countIncreases.isEmpty())
		{
			return;
		}

		String details = String.join(", ", countIncreases);
		if (countIncreases.size() > 3)
		{
			details = String.join(", ", countIncreases.subList(0, 3)) + " +" + (countIncreases.size() - 3) + " more";
		}

		String message = "Diary task completed (" + details + "). Open the Achievement Diary to sync tasks.";
		String formatted = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append("DMMScape: ")
			.append(message)
			.build();

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(formatted)
			.build());
	}

	private void checkQuestPointChanges()
	{
		int currentPoints = client.getVarpValue(QUEST_POINTS_VARP);
		if (currentPoints == lastQuestPoints)
		{
			return;
		}

		lastQuestPoints = currentPoints;
		log.info("Quest points updated: {}", currentPoints);

		SyncData delta = new SyncData();
		delta.setUsername(client.getLocalPlayer().getName());
		delta.setProfileType(getProfileType());
		delta.setTimestamp(System.currentTimeMillis());
		delta.setFullSync(false);
		delta.setQuestPoints(currentPoints);

		if (config.syncEnabled())
		{
			sendSyncWithAuth(delta);
		}
	}

	private void checkCollectionLogChanges()
	{
		int currentCount = client.getVarpValue(COLLECTION_LOG_COUNT_VARP);
		if (currentCount == lastCollectionLogCount)
		{
			return;
		}

		lastCollectionLogCount = currentCount;
		log.info("Collection log count updated: {}", currentCount);

		SyncData delta = new SyncData();
		delta.setUsername(client.getLocalPlayer().getName());
		delta.setProfileType(getProfileType());
		delta.setTimestamp(System.currentTimeMillis());
		delta.setFullSync(false);
		delta.setCollectionLogCount(currentCount);

		if (config.syncEnabled())
		{
			sendSyncWithAuth(delta);
		}
	}

	/**
	 * Handles boss KC update from chat message.
	 */
	private void handleBossKCUpdate(String bossName, int killCount)
	{
		String normalizedName = normalizeBossName(bossName);
		int lastKC = lastBossKCs.getOrDefault(normalizedName, 0);

		if (killCount > lastKC)
		{
			log.info("Boss KC update: {} -> {}", normalizedName, killCount);
			lastBossKCs.put(normalizedName, killCount);

			// Store in ProgressStore for UI display
			progressStore.setBossKillCount(normalizedName, killCount);

			SyncData delta = new SyncData();
			delta.setUsername(client.getLocalPlayer().getName());
			delta.setProfileType(getProfileType());
			delta.setTimestamp(System.currentTimeMillis());
			delta.setFullSync(false);

			Map<String, Integer> bossKills = new HashMap<>();
			bossKills.put(normalizedName, killCount);
			delta.setBossKills(bossKills);

			if (config.syncEnabled())
			{
				sendSyncWithAuth(delta);
			}
		}
	}

	/**
	 * Handles clue scroll completion from chat message.
	 */
	private void handleClueCompletion(String tier, int count)
	{
		int lastCount = lastClueCompletions.getOrDefault(tier, 0);

		if (count > lastCount)
		{
			log.info("Clue completion update: {} {} -> {}", tier, lastCount, count);
			lastClueCompletions.put(tier, count);

			SyncData delta = new SyncData();
			delta.setUsername(client.getLocalPlayer().getName());
			delta.setProfileType(getProfileType());
			delta.setTimestamp(System.currentTimeMillis());
			delta.setFullSync(false);

			Map<String, Integer> clueCompletions = new HashMap<>();
			clueCompletions.put(tier, count);
			delta.setClueCompletions(clueCompletions);

			if (config.syncEnabled())
			{
				sendSyncWithAuth(delta);
			}
		}
	}

	/**
	 * Normalizes boss name for consistent storage using the BossNameMapper.
	 */
	private String normalizeBossName(String name)
	{
		return BossNameMapper.getBossId(name);
	}

	/**
	 * Gets the current profile type (DMM, League, etc.)
	 */
	private String getProfileType()
	{
		EnumSet<WorldType> worldTypes = client.getWorldType();
		if (worldTypes.contains(WorldType.DEADMAN))
		{
			return "DMM";
		}
		if (worldTypes.contains(WorldType.SEASONAL))
		{
			return "LEAGUES";
		}
		return "MAIN";
	}

	/**
	 * Gets all completed quests using Quest.getState().
	 * Must be called on the client thread.
	 */
	private Set<String> getAllCompletedQuests()
	{
		Set<String> completed = new HashSet<>();

		for (Quest quest : Quest.values())
		{
			try
			{
				QuestState state = quest.getState(client);
				if (state == QuestState.FINISHED)
				{
					completed.add(quest.getName());
				}
			}
			catch (Exception e)
			{
				log.debug("Error getting quest state for {}: {}", quest.getName(), e.getMessage());
			}
		}

		return completed;
	}

	/**
	 * Checks for newly completed quests and sends delta update.
	 * Must be called on the client thread.
	 */
	private void checkQuestCompletionChanges()
	{
		if (!config.trackQuests() || !initialSyncDone)
		{
			return;
		}

		Set<String> currentQuests = getAllCompletedQuests();
		Set<String> newQuests = new HashSet<>(currentQuests);
		newQuests.removeAll(lastCompletedQuests);

		if (!newQuests.isEmpty())
		{
			log.info("New quests completed: {}", newQuests);

			SyncData delta = new SyncData();
			delta.setUsername(client.getLocalPlayer().getName());
			delta.setProfileType(getProfileType());
			delta.setTimestamp(System.currentTimeMillis());
			delta.setFullSync(false);
			delta.setCompletedQuests(newQuests);

			if (config.syncEnabled())
			{
				sendSyncWithAuth(delta);
			}

			lastCompletedQuests = currentQuests;
		}
	}

	/**
	 * Gets diary task counts for all diaries.
	 * Returns the number of tasks completed per tier.
	 */
	private Map<String, Map<String, Integer>> getAllDiaryTaskCounts()
	{
		Map<String, Map<String, Integer>> counts = new HashMap<>();
		String[] tierNames = {"easy", "medium", "hard", "elite"};

		for (Map.Entry<String, int[]> entry : DIARY_COUNT_VARBITS.entrySet())
		{
			String diaryName = entry.getKey();
			int[] varbits = entry.getValue();

			Map<String, Integer> tierCounts = new HashMap<>();
			for (int i = 0; i < varbits.length && i < tierNames.length; i++)
			{
				int count = client.getVarbitValue(varbits[i]);
				tierCounts.put(tierNames[i], count);
			}

			counts.put(diaryName, tierCounts);
		}

		return counts;
	}

	/**
	 * Updates the cached diary task counts.
	 */
	private void updateLastDiaryTaskCounts(Map<String, Map<String, Integer>> counts)
	{
		lastDiaryTaskCounts.clear();

		for (Map.Entry<String, Map<String, Integer>> diary : counts.entrySet())
		{
			for (Map.Entry<String, Integer> tier : diary.getValue().entrySet())
			{
				String key = diary.getKey() + "_" + tier.getKey();
				lastDiaryTaskCounts.put(key, tier.getValue());
			}
		}
	}

	/**
	 * Initializes the CA name mapping by reading game structs.
	 * This loads CA names dynamically from the game data.
	 * Must be called on the client thread.
	 */
	private void initializeCANameMapping()
	{
		log.debug("Initializing CA name mapping from game data");
		caNameMapping.clear();

		for (Map.Entry<Integer, String> tierEntry : CA_TIER_ENUMS.entrySet())
		{
			int enumId = tierEntry.getKey();
			String tierName = tierEntry.getValue();

			try
			{
				var enumComp = client.getEnum(enumId);
				if (enumComp == null)
				{
					log.warn("Could not find enum for CA tier: {} ({})", tierName, enumId);
					continue;
				}

				int[] structIds = enumComp.getIntVals();
				log.debug("Processing {} CAs for tier {}", structIds.length, tierName);

				for (int structId : structIds)
				{
					try
					{
						var struct = client.getStructComposition(structId);
						if (struct == null)
						{
							continue;
						}

						// Struct param 1306 = game ID, 1308 = name
						int gameId = struct.getIntValue(1306);
						String name = struct.getStringValue(1308);

						if (name != null && !name.isEmpty())
						{
							caNameMapping.put(gameId, name);
							// Also update the CAIDMapper for reference
							CAIDMapper.addMapping(gameId, name, tierName);
						}
					}
					catch (Exception e)
					{
						log.debug("Error reading struct {}: {}", structId, e.getMessage());
					}
				}
			}
			catch (Exception e)
			{
				log.warn("Error loading CA tier {}: {}", tierName, e.getMessage());
			}
		}

		caMapInitialized = true;
		log.info("CA name mapping initialized with {} entries", caNameMapping.size());
	}

	private void initializePlayerAlarmCache()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		visiblePlayers.clear();
		Player local = client.getLocalPlayer();
		for (Player player : client.getPlayers())
		{
			if (player == null || player == local || player.getName() == null)
			{
				continue;
			}

			String name = Text.removeTags(player.getName());
			if (name != null && !name.isBlank())
			{
				visiblePlayers.add(name);
			}
		}

		playerAlarmReady = true;
	}

	private boolean shouldIgnorePlayer(Player player)
	{
		if (config.ignoreFriends() && player.isFriend())
		{
			return true;
		}

		if (config.ignoreClanMembers() && player.isClanMember())
		{
			return true;
		}

		return config.ignoreFriendsChat() && player.isFriendsChatMember();
	}

	private boolean isAlarmOnCooldown(String playerName, long now)
	{
		int cooldownSeconds = Math.max(0, config.alarmCooldown());
		if (cooldownSeconds == 0)
		{
			return false;
		}

		Long lastAlarm = lastAlarmTimes.get(playerName);
		return lastAlarm != null && now - lastAlarm < cooldownSeconds * 1000L;
	}

	private void sendPlayerAlarm(Player player, String playerName)
	{
		String message = buildPlayerAlarmMessage(player, playerName);

		if (config.alarmGameMessage())
		{
			String formattedMessage = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append("DMMScape: ")
				.append(message)
				.build();

			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage(formattedMessage)
				.build());
		}

		if (config.alarmSound() || config.alarmTrayNotification() || config.alarmFlash())
		{
			notifier.notify(buildPlayerAlarmNotification(), "DMMScape: " + message);
		}
	}

	private void triggerManualSync()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			notifyGameMessage("DMMScape sync skipped: not logged in.");
			return;
		}

		webappProgressFastUntilMs = System.currentTimeMillis() + WEBAPP_PROGRESS_FAST_GRACE_MS;
		updateWebappProgressPollingInterval(true);
		manualSyncRequested = true;
		executor.execute(this::performInitialSync);
		notifyGameMessage("DMMScape sync requested.");
	}

	private void forceSync()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			notifyGameMessage("DMMScape force sync skipped: not logged in.");
			return;
		}

		webappProgressFastUntilMs = System.currentTimeMillis() + WEBAPP_PROGRESS_FAST_GRACE_MS;
		updateWebappProgressPollingInterval(true);
		webappProgressFetched = false;
		pendingWebappProgress = null;
		manualSyncRequested = true;
		executor.execute(this::performInitialSync);
		targetService.refreshNow();
		pollWebappProgress(true);
		notifyGameMessage("DMMScape force sync requested.");
	}

	private void handleTargetRefresh()
	{
		markTargetInteraction();
		targetService.refreshNow();
	}

	private void markTargetInteraction()
	{
		lastTargetInteractionMs = System.currentTimeMillis();
		updateTargetPollingInterval(true);
	}

	private void handleTargetToggle(TargetPoint target, boolean completed)
	{
		markTargetInteraction();
		if (!config.allowTargetEdits())
		{
			notifyGameMessage("Target edits are disabled in plugin settings.");
			return;
		}

		if (config.targetSyncUrl() == null || config.targetSyncUrl().isEmpty())
		{
			notifyGameMessage("Target sync URL not configured.");
			return;
		}

		withAuthHeader(authHeader ->
		{
			targetService.setTargetCompletion(target.getId(), completed);

			String label = completed
				? "Plan: completed " + target.getName()
				: "Plan: uncompleted " + target.getName();
			SyncService.SyncEvent event = syncService.recordEvent(label);

			SyncService.TargetUpdate update = new SyncService.TargetUpdate(target.getId(), target.getType(), completed);
			syncService.sendTargetUpdates(
				List.of(update),
				config.targetSyncUrl(),
				authHeader,
				targetService.getActiveListId(),
				new SyncService.SyncCallback()
				{
					@Override
					public void onSuccess()
					{
						if (event != null)
						{
							event.setStatus(SyncService.SyncStatus.SYNCED);
						}
						notifyGameMessage("Target updated in DMMScape.");
					}

					@Override
					public void onError(String message)
					{
						if (event != null)
						{
							event.setStatus(SyncService.SyncStatus.FAILED);
						}
						notifyGameMessage("Target update failed: " + message);
					}
				}
			);
		}, error -> notifyGameMessage("Link your DMMScape account or set an API key first."));
	}

	private void handleTargetNavigate(TargetPoint target)
	{
		if (target == null || target.getWorldPoint() == null)
		{
			return;
		}

		markTargetInteraction();

		clientThread.invokeLater(() ->
		{
			Widget worldMapWidget = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
			if (worldMapWidget == null || worldMapWidget.isHidden())
			{
				return;
			}
			if (client.getWorldMap() != null)
			{
				client.getWorldMap().setWorldMapPositionTarget(target.getWorldPoint());
			}
		});
	}

	private void handleUnlink()
	{
		if (deviceAuthService == null)
		{
			return;
		}

		deviceAuthService.unlink();
		lastAuthHeader = null;
		targetService.setAuthHeader(null);
		refreshPanel();
	}

	private void handleBossAddToPlan(com.dmmtracker.data.BossData boss)
	{
		markTargetInteraction();
		withAuthHeader(authHeader ->
		{
			syncService.addToPlan(
				"boss",
				boss.getId(),
				boss.getName(),
				boss.getLocation(),
				boss.getPointsPerKill(),
				boss.getCategory(),
				null,
				authHeader,
				config.targetSyncUrl(),
				new SyncService.SyncCallback()
				{
					@Override
					public void onSuccess()
					{
						notifyGameMessage("Added " + boss.getName() + " to plan.");
						// Refresh targets to show new item
						targetService.refreshNow();
					}

					@Override
					public void onError(String message)
					{
						notifyGameMessage("Failed to add to plan: " + message);
					}
				}
			);
		}, error -> notifyGameMessage("Link your DMMScape account or set an API key to add targets."));
	}

	private void handleDiaryLocationNavigate(DiaryTaskData task, String region, String tier)
	{
		if (task == null)
		{
			return;
		}

		markTargetInteraction();

		WorldPoint worldPoint = task.getWorldPoint();
		if (worldPoint == null)
		{
			notifyGameMessage("No location available for this diary task.");
			return;
		}

		String key = buildDiaryFocusKey(task, region, tier);
		if (targetService.isFocusTarget(key))
		{
			clearNavigationTarget();
			return;
		}

		TargetPoint focus = new TargetPoint();
		focus.setId(task.getId() != null && !task.getId().isEmpty() ? task.getId() : key);
		focus.setType("diary");
		focus.setName(task.getDescription() != null ? task.getDescription() : "Diary task");
		focus.setCategory(region != null ? region : "");
		focus.setCompleted(false);
		focus.setCompletionSource("custom");
		focus.setToggleable(false);
		focus.setOrder(0);
		focus.setWorldPoint(worldPoint);

		targetService.setFocusTarget(key, focus);
		ensureFocusOverlaysEnabled();
		clientThread.invokeLater(() ->
		{
			if (client.getWorldMap() != null)
			{
				client.getWorldMap().setWorldMapPositionTarget(worldPoint);
			}
			refreshWorldMapPoints();
		});
		SwingUtilities.invokeLater(panel::refreshDiaryTab);
		notifyGameMessage("Navigation set: " + focus.getName());
	}

	private void clearNavigationTarget()
	{
		if (targetService.getFocusTarget() == null)
		{
			return;
		}

		targetService.clearFocusTarget();
		disableFocusOverlaysIfIdle();
		clientThread.invokeLater(this::refreshWorldMapPoints);
		SwingUtilities.invokeLater(panel::refreshDiaryTab);
		notifyGameMessage("Navigation cleared.");
	}

	private String buildDiaryFocusKey(DiaryTaskData task, String region, String tier)
	{
		String taskId = task.getId();
		if (taskId != null && !taskId.isEmpty())
		{
			return "diary:" + taskId;
		}

		String desc = task.getDescription() != null ? task.getDescription() : "unknown";
		String regionKey = region != null ? region : "";
		String tierKey = tier != null ? tier : "";
		return "diary:" + regionKey + ":" + tierKey + ":" + desc;
	}

	private void syncAuthHeaderIfNeeded()
	{
		String current = runeliteTokenService != null
			? runeliteTokenService.getAuthHeader()
			: getFallbackAuthHeader();
		if (!Objects.equals(current, lastAuthHeader))
		{
			lastAuthHeader = current;
			targetService.setAuthHeader(current);
			if (syncService != null)
			{
				syncService.clearProgressEtag();
			}
		}
	}

	private void updateTargetPollingInterval(boolean force)
	{
		if (!config.enableTargetSync() || targetService == null)
		{
			return;
		}

		long now = System.currentTimeMillis();
		if (!force && now - lastTargetPollCheckMs < TARGET_POLL_CHECK_INTERVAL_MS)
		{
			return;
		}
		lastTargetPollCheckMs = now;

		boolean planActive = panel != null && panel.isShowing() && panel.isPlanTabActive();
		boolean focusActive = targetService.getFocusTarget() != null;
		boolean recentInteraction = now - lastTargetInteractionMs < TARGET_POLL_ACTIVE_GRACE_MS;
		boolean fastMode = planActive || focusActive || recentInteraction;

		int baseInterval = clampTargetPollInterval(config.targetPollInterval());
		int idleInterval = clampTargetPollInterval(Math.max(
			TARGET_POLL_IDLE_MIN_SECONDS,
			baseInterval * TARGET_POLL_IDLE_MULTIPLIER
		));
		int desiredInterval = fastMode ? baseInterval : idleInterval;

		if (force || desiredInterval != lastTargetPollIntervalSeconds)
		{
			targetService.setDesiredPollIntervalSeconds(desiredInterval);
			lastTargetPollIntervalSeconds = desiredInterval;
		}
	}

	private int clampTargetPollInterval(int interval)
	{
		int value = interval <= 0 ? 5 : interval;
		value = Math.max(5, Math.min(TARGET_POLL_IDLE_MAX_SECONDS, value));
		return value;
	}

	private int getDesiredWebappProgressPollIntervalSeconds()
	{
		long now = System.currentTimeMillis();
		int baseInterval = now <= webappProgressFastUntilMs
			? WEBAPP_PROGRESS_POLL_FAST_SECONDS
			: WEBAPP_PROGRESS_POLL_IDLE_SECONDS;

		long effective = (long) baseInterval * Math.max(1, webappProgressBackoffMultiplier);
		if (effective > WEBAPP_PROGRESS_POLL_MAX_SECONDS)
		{
			effective = WEBAPP_PROGRESS_POLL_MAX_SECONDS;
		}
		if (effective < WEBAPP_PROGRESS_POLL_FAST_SECONDS)
		{
			effective = WEBAPP_PROGRESS_POLL_FAST_SECONDS;
		}
		return (int) effective;
	}

	private int applyPollJitter(int intervalSeconds)
	{
		if (intervalSeconds <= 1)
		{
			return intervalSeconds;
		}
		double jitter = (Math.random() * 2.0 - 1.0) * POLL_JITTER_RATIO;
		int jittered = (int) Math.round(intervalSeconds * (1.0 + jitter));
		return Math.max(1, jittered);
	}

	private void scheduleWebappProgressPolling(int intervalSeconds)
	{
		int jittered = applyPollJitter(intervalSeconds);
		currentWebappProgressPollIntervalSeconds = intervalSeconds;
		webappProgressPollTask = executor.scheduleAtFixedRate(
			() -> pollWebappProgress(false),
			jittered,
			jittered,
			TimeUnit.SECONDS
		);
	}

	private void rescheduleWebappProgressPollingIfNeeded(boolean force)
	{
		if (webappProgressPollTask == null || webappProgressPollTask.isCancelled())
		{
			return;
		}

		int desiredInterval = getDesiredWebappProgressPollIntervalSeconds();
		if (!force && desiredInterval == currentWebappProgressPollIntervalSeconds)
		{
			return;
		}

		webappProgressPollTask.cancel(false);
		webappProgressPollTask = null;
		scheduleWebappProgressPolling(desiredInterval);
	}

	private void registerWebappProgressSuccess(boolean hasChanges)
	{
		webappProgressBackoffMultiplier = 1;
		if (hasChanges)
		{
			webappProgressNoChangeStreak = 0;
			webappProgressFastUntilMs = System.currentTimeMillis() + WEBAPP_PROGRESS_FAST_GRACE_MS;
		}
		else
		{
			webappProgressNoChangeStreak++;
			if (webappProgressNoChangeStreak >= 3 && System.currentTimeMillis() > webappProgressFastUntilMs)
			{
				webappProgressFastUntilMs = 0;
			}
		}
		rescheduleWebappProgressPollingIfNeeded(false);
	}

	private void registerWebappProgressFailure()
	{
		webappProgressBackoffMultiplier = Math.min(
			webappProgressBackoffMultiplier * 2,
			WEBAPP_PROGRESS_POLL_MAX_BACKOFF_MULTIPLIER
		);
		rescheduleWebappProgressPollingIfNeeded(true);
	}

	private void updateWebappProgressPollingInterval(boolean force)
	{
		if (webappProgressPollTask == null || webappProgressPollTask.isCancelled())
		{
			return;
		}
		long now = System.currentTimeMillis();
		if (!force && now - lastWebappPollCheckMs < WEBAPP_PROGRESS_POLL_CHECK_INTERVAL_MS)
		{
			return;
		}
		lastWebappPollCheckMs = now;
		rescheduleWebappProgressPollingIfNeeded(false);
	}

	private String getFallbackAuthHeader()
	{
		if (deviceAuthService == null)
		{
			return config.apiKey();
		}

		return deviceAuthService.getAuthHeader(config.apiKey());
	}

	private void handleSyncResponse(JsonObject response)
	{
		if (response == null || progressStore == null)
		{
			return;
		}

		mergeWebappProgress(response, false);
		ownedSyncReady = true;

		if (pendingOwnedSync && config.syncEnabled())
		{
			pendingOwnedSync = false;
			sendOwnedSync();
		}
	}

	/**
	 * Merges skill levels from remote, keeping max values.
	 */
	private void mergeSkillLevelsFromRemote(Map<String, Integer> remoteSkills)
	{
		boolean changed = false;

		for (Map.Entry<String, Integer> entry : remoteSkills.entrySet())
		{
			String skill = entry.getKey().toLowerCase();
			int remoteLevel = entry.getValue();
			int localLevel = lastSkillLevels.getOrDefault(skill, 0);

			if (remoteLevel > localLevel)
			{
				lastSkillLevels.put(skill, remoteLevel);
				log.debug("Merged skill level from sync response: {} {} -> {}", skill, localLevel, remoteLevel);
				changed = true;
			}
		}

		if (changed)
		{
			log.info("Merged skill levels from sync response");
		}
	}

	/**
	 * Merges clue completions from remote, keeping max values.
	 */
	private void mergeClueCompletionsFromRemote(Map<String, Integer> remoteClues)
	{
		boolean changed = false;

		for (Map.Entry<String, Integer> entry : remoteClues.entrySet())
		{
			String tier = entry.getKey().toLowerCase();
			int remoteCount = entry.getValue();
			int localCount = lastClueCompletions.getOrDefault(tier, 0);

			if (remoteCount > localCount)
			{
				lastClueCompletions.put(tier, remoteCount);
				log.debug("Merged clue completion from sync response: {} {} -> {}", tier, localCount, remoteCount);
				changed = true;
			}
		}

		if (changed)
		{
			log.info("Merged clue completions from sync response");
		}
	}

	/**
	 * Parses a JSON object field as a Map<String, Integer>.
	 */
	private Map<String, Integer> parseIntegerMap(JsonObject response, String key)
	{
		if (response == null || key == null || !response.has(key) || response.get(key).isJsonNull())
		{
			return null;
		}

		JsonElement element = response.get(key);
		if (!element.isJsonObject())
		{
			return null;
		}

		Map<String, Integer> result = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet())
		{
			if (entry.getValue() != null && entry.getValue().isJsonPrimitive())
			{
				try
				{
					result.put(entry.getKey(), entry.getValue().getAsInt());
				}
				catch (NumberFormatException e)
				{
					// Skip invalid entries
				}
			}
		}
		return result;
	}

	/**
	 * Merges boss KC data from remote server into local store.
	 * Uses max(local, remote) strategy - highest KC wins.
	 */
	private void mergeBossKcFromRemote(Map<String, Integer> remoteBossKc)
	{
		if (remoteBossKc == null)
		{
			return;
		}

		for (Map.Entry<String, Integer> entry : remoteBossKc.entrySet())
		{
			String bossName = entry.getKey();
			int remoteKc = entry.getValue();
			int localKc = progressStore.getBossKillCount(bossName);

			// Update if local is unknown (-1) or remote is higher
			if (localKc < 0 || remoteKc > localKc)
			{
				progressStore.setBossKillCount(bossName, remoteKc);
				lastBossKCs.put(bossName.toLowerCase(), remoteKc);
				log.debug("Merged boss KC from remote: {} = {}", bossName, remoteKc);
			}
		}
	}

	private Set<String> parseStringSet(JsonObject response, String key)
	{
		if (response == null || key == null || !response.has(key) || response.get(key).isJsonNull())
		{
			return null;
		}

		JsonElement element = response.get(key);
		if (!element.isJsonArray())
		{
			return null;
		}

		Set<String> values = new HashSet<>();
		for (JsonElement item : element.getAsJsonArray())
		{
			if (item != null && item.isJsonPrimitive())
			{
				values.add(item.getAsString());
			}
		}
		return values;
	}

	// ============ Webapp Progress Import ============

	/**
	 * Fetches progress data from the webapp after initial sync.
	 */
	private void fetchWebappProgress()
	{
		if (webappProgressFetched || !config.syncEnabled())
		{
			return;
		}

		webappProgressFetched = true;
		webappProgressFastUntilMs = System.currentTimeMillis() + WEBAPP_PROGRESS_FAST_GRACE_MS;
		pollWebappProgress(true);
	}

	private void startWebappProgressPolling()
	{
		if (webappProgressPollTask != null && !webappProgressPollTask.isCancelled())
		{
			return;
		}

		webappProgressBackoffMultiplier = 1;
		webappProgressNoChangeStreak = 0;
		webappProgressFastUntilMs = System.currentTimeMillis() + WEBAPP_PROGRESS_FAST_GRACE_MS;
		scheduleWebappProgressPolling(getDesiredWebappProgressPollIntervalSeconds());
	}

	private void stopWebappProgressPolling()
	{
		if (webappProgressPollTask != null)
		{
			webappProgressPollTask.cancel(false);
			webappProgressPollTask = null;
		}
		webappProgressBackoffMultiplier = 1;
		webappProgressNoChangeStreak = 0;
		webappProgressFastUntilMs = 0;
	}

	private void pollWebappProgress(boolean notify)
	{
		if (!config.syncEnabled() || syncService == null)
		{
			return;
		}

		if (!hasAuthConfigured())
		{
			return;
		}

		if (syncService.isProgressFetchInFlight())
		{
			return;
		}

		withAuthHeader(authHeader -> syncService.fetchProgress(authHeader, config.webhookUrl(), new SyncService.ProgressFetchCallback()
		{
			@Override
			public void onSuccess(JsonObject progress)
			{
				clientThread.invokeLater(() ->
				{
					List<String> differences = compareWebappProgress(progress);
					boolean hasChanges = !differences.isEmpty();
					registerWebappProgressSuccess(hasChanges);

					if (config.autoImportWebapp())
					{
						if (notify && hasChanges)
						{
							notifyGameMessage("DMMScape webapp has " + differences.size() + " updates. Importing...");
						}
						mergeWebappProgress(progress, notify);
						return;
					}

					pendingWebappProgress = progress;
					if (hasChanges)
					{
						promptWebappImport(differences);
					}
					else
					{
						pendingWebappProgress = null;
					}
				});
			}

			@Override
			public void onError(String message)
			{
				log.debug("Could not fetch webapp progress: {}", message);
				registerWebappProgressFailure();
			}

			@Override
			public void onNotModified()
			{
				registerWebappProgressSuccess(false);
			}
		}), error -> log.debug("No auth for webapp progress fetch: {}", error));
	}

	private boolean hasAuthConfigured()
	{
		if (deviceAuthService != null && deviceAuthService.isLinked())
		{
			return true;
		}
		String apiKey = config.apiKey();
		return apiKey != null && !apiKey.isEmpty();
	}

	/**
	 * Compares webapp progress with local game state.
	 * Returns a list of human-readable differences where webapp has data we don't.
	 */
	private List<String> compareWebappProgress(JsonObject webappProgress)
	{
		List<String> differences = new ArrayList<>();

		if (webappProgress == null)
		{
			return differences;
		}

		// Compare boss KC - webapp might have hiscores data
		Map<String, Integer> webappBossKc = parseIntegerMap(webappProgress, "bossKc");
		if (webappBossKc != null)
		{
			for (Map.Entry<String, Integer> entry : webappBossKc.entrySet())
			{
				String boss = entry.getKey();
				int webappKc = entry.getValue();
				int localKc = progressStore.getBossKillCount(boss);

				if (webappKc > 0 && (localKc < 0 || webappKc > localKc))
				{
					differences.add(String.format("Boss KC: %s = %d (local: %s)",
						boss, webappKc, localKc < 0 ? "unknown" : String.valueOf(localKc)));
				}
			}
		}

		// Compare skill levels - webapp might have hiscores data
		Map<String, Integer> webappSkills = parseIntegerMap(webappProgress, "skillLevels");
		if (webappSkills != null)
		{
			for (Map.Entry<String, Integer> entry : webappSkills.entrySet())
			{
				String skill = entry.getKey();
				int webappLevel = entry.getValue();
				int localLevel = lastSkillLevels.getOrDefault(skill.toLowerCase(), 0);

				if (webappLevel > localLevel)
				{
					differences.add(String.format("Skill: %s = %d (local: %d)", skill, webappLevel, localLevel));
				}
			}
		}

		// Compare clue completions
		Map<String, Integer> webappClues = parseIntegerMap(webappProgress, "clueCompletions");
		if (webappClues != null)
		{
			for (Map.Entry<String, Integer> entry : webappClues.entrySet())
			{
				String tier = entry.getKey();
				int webappCount = entry.getValue();
				int localCount = lastClueCompletions.getOrDefault(tier.toLowerCase(), 0);

				if (webappCount > localCount)
				{
					differences.add(String.format("Clues (%s): %d (local: %d)", tier, webappCount, localCount));
				}
			}
		}

		// Compare collection log count
		if (webappProgress.has("collectionLogCount") && !webappProgress.get("collectionLogCount").isJsonNull())
		{
			int webappCollLog = webappProgress.get("collectionLogCount").getAsInt();
			if (webappCollLog > lastCollectionLogCount)
			{
				differences.add(String.format("Collection Log: %d slots (local: %d)",
					webappCollLog, lastCollectionLogCount < 0 ? 0 : lastCollectionLogCount));
			}
		}

		// Compare quest points
		if (webappProgress.has("questPoints") && !webappProgress.get("questPoints").isJsonNull())
		{
			int webappQP = webappProgress.get("questPoints").getAsInt();
			if (webappQP > lastQuestPoints)
			{
				differences.add(String.format("Quest Points: %d (local: %d)", webappQP, lastQuestPoints));
			}
		}

		return differences;
	}

	/**
	 * Shows a prompt asking the user if they want to import webapp data.
	 */
	private void promptWebappImport(List<String> differences)
	{
		if (differences.isEmpty() || pendingWebappProgress == null)
		{
			return;
		}

		// Build message
		StringBuilder message = new StringBuilder();
		message.append("DMMScape webapp has data not in your game:\n\n");

		int shown = 0;
		for (String diff : differences)
		{
			if (shown >= 5)
			{
				message.append(String.format("... and %d more\n", differences.size() - 5));
				break;
			}
			message.append("• ").append(diff).append("\n");
			shown++;
		}

		message.append("\nImport this data to the plugin?");

		SwingUtilities.invokeLater(() ->
		{
			int choice = JOptionPane.showConfirmDialog(
				panel,
				message.toString(),
				"DMMScape - Import Webapp Data",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE
			);

			if (choice == JOptionPane.YES_OPTION)
			{
				clientThread.invokeLater(() -> mergeWebappProgress());
			}
			else
			{
				pendingWebappProgress = null;
			}
		});
	}

	/**
	 * Merges webapp progress data into local ProgressStore.
	 * Uses max(local, remote) strategy for numeric values.
	 */
	private void mergeWebappProgress()
	{
		JsonObject progress = pendingWebappProgress;
		pendingWebappProgress = null;

		mergeWebappProgress(progress, true);
	}

	private void mergeWebappProgress(JsonObject progress, boolean notify)
	{
		if (progress == null || progressStore == null)
		{
			return;
		}

		int mergeCount = 0;

		String source = progress.has("_source") && progress.get("_source").isJsonPrimitive()
			? progress.get("_source").getAsString()
			: "";
		boolean allowTaskDeletions = "manual".equalsIgnoreCase(source);
		boolean allowNumericDecreases = allowTaskDeletions || "import".equalsIgnoreCase(source);

		// Completed tasks (diary/CA/owned)
		Set<String> completedTasks = parseStringSet(progress, "completedTasks");
		Set<String> diaryTaskIds = new HashSet<>();
		Set<String> caTaskIds = new HashSet<>();
		Set<String> ownedSigils = new HashSet<>();
		Set<String> ownedLamps = new HashSet<>();
		Set<String> ownedPrayers = new HashSet<>();

		if (completedTasks != null)
		{
			for (String task : completedTasks)
			{
				if (task == null)
				{
					continue;
				}
				if (task.startsWith("diary:"))
				{
					diaryTaskIds.add(task.substring("diary:".length()));
				}
				else if (task.startsWith("ca:"))
				{
					caTaskIds.add(task.substring("ca:".length()));
				}
				else if (task.startsWith("sigil:"))
				{
					ownedSigils.add(task.substring("sigil:".length()));
				}
				else if (task.startsWith("lamp:"))
				{
					ownedLamps.add(task.substring("lamp:".length()));
				}
				else if (task.startsWith("prayer:"))
				{
					ownedPrayers.add(task.substring("prayer:".length()));
				}
			}
		}

		Set<String> ownedSigilsOverride = parseStringSet(progress, "ownedSigils");
		if (ownedSigilsOverride != null)
		{
			ownedSigils.addAll(ownedSigilsOverride);
		}
		Set<String> ownedLampsOverride = parseStringSet(progress, "ownedLamps");
		if (ownedLampsOverride != null)
		{
			ownedLamps.addAll(ownedLampsOverride);
		}
		Set<String> ownedPrayersOverride = parseStringSet(progress, "ownedPrayers");
		if (ownedPrayersOverride != null)
		{
			ownedPrayers.addAll(ownedPrayersOverride);
		}

		boolean hasCompletedTasks = completedTasks != null;

		if (allowTaskDeletions && hasCompletedTasks)
		{
			Set<String> existingDiary = progressStore.getCompletedDiaryTasks();
			if (!diaryTaskIds.equals(existingDiary))
			{
				progressStore.setCompletedDiaryTasks(diaryTaskIds, ProgressStore.UpdateSource.REMOTE);
				mergeCount += countSetDelta(existingDiary, diaryTaskIds);
			}

			Set<String> existingCa = progressStore.getCompletedCATasks();
			if (!caTaskIds.equals(existingCa))
			{
				progressStore.setCompletedCATasks(caTaskIds, ProgressStore.UpdateSource.REMOTE);
				mergeCount += countSetDelta(existingCa, caTaskIds);
			}

			Set<String> existingSigils = progressStore.getOwnedSigils();
			Set<String> existingLamps = progressStore.getOwnedLamps();
			Set<String> existingPrayers = progressStore.getOwnedPrayers();
			if (!ownedSigils.equals(existingSigils) || !ownedLamps.equals(existingLamps) || !ownedPrayers.equals(existingPrayers))
			{
				progressStore.updateOwnedState(ownedSigils, ownedLamps, ownedPrayers, ProgressStore.UpdateSource.REMOTE);
				mergeCount += countSetDelta(existingSigils, ownedSigils);
				mergeCount += countSetDelta(existingLamps, ownedLamps);
				mergeCount += countSetDelta(existingPrayers, ownedPrayers);
			}
		}
		else
		{
			if (!diaryTaskIds.isEmpty())
			{
				Set<String> existing = progressStore.getCompletedDiaryTasks();
				Set<String> newTasks = new HashSet<>(diaryTaskIds);
				newTasks.removeAll(existing);
				if (!newTasks.isEmpty())
				{
					progressStore.addCompletedDiaryTasks(newTasks, ProgressStore.UpdateSource.REMOTE);
					mergeCount += newTasks.size();
				}
			}

			if (!caTaskIds.isEmpty())
			{
				Set<String> existing = progressStore.getCompletedCATasks();
				Set<String> merged = new HashSet<>(existing);
				merged.addAll(caTaskIds);
				if (!merged.equals(existing))
				{
					progressStore.setCompletedCATasks(merged, ProgressStore.UpdateSource.REMOTE);
					mergeCount += Math.max(0, merged.size() - existing.size());
				}
			}

			if (!ownedSigils.isEmpty() || !ownedLamps.isEmpty() || !ownedPrayers.isEmpty())
			{
				Set<String> mergedSigils = new HashSet<>(progressStore.getOwnedSigils());
				Set<String> mergedLamps = new HashSet<>(progressStore.getOwnedLamps());
				Set<String> mergedPrayers = new HashSet<>(progressStore.getOwnedPrayers());
				int beforeCount = mergedSigils.size() + mergedLamps.size() + mergedPrayers.size();

				mergedSigils.addAll(ownedSigils);
				mergedLamps.addAll(ownedLamps);
				mergedPrayers.addAll(ownedPrayers);

				int afterCount = mergedSigils.size() + mergedLamps.size() + mergedPrayers.size();
				if (afterCount != beforeCount)
				{
					progressStore.updateOwnedState(mergedSigils, mergedLamps, mergedPrayers, ProgressStore.UpdateSource.REMOTE);
					mergeCount += Math.max(0, afterCount - beforeCount);
				}
			}
		}

		// Merge boss KC
		Map<String, Integer> bossKc = parseIntegerMap(progress, "bossKc");
		if (bossKc != null)
		{
			Map<String, Integer> currentBoss = progressStore.getBossKillCounts();
			if (allowNumericDecreases)
			{
				Map<String, Integer> mergedBoss = new HashMap<>();
				for (Map.Entry<String, Integer> entry : bossKc.entrySet())
				{
					if (entry.getKey() == null)
					{
						continue;
					}
					mergedBoss.put(entry.getKey().toLowerCase(), entry.getValue());
				}

				int bossUpdates = countMapDelta(currentBoss, mergedBoss);
				if (bossUpdates > 0 || currentBoss.size() != mergedBoss.size())
				{
					progressStore.setBossKillCounts(mergedBoss, ProgressStore.UpdateSource.REMOTE);
					mergeCount += bossUpdates;
				}

				lastBossKCs = new HashMap<>(mergedBoss);
			}
			else if (!bossKc.isEmpty())
			{
				Map<String, Integer> mergedBoss = new HashMap<>(currentBoss);
				int bossUpdates = 0;

				for (Map.Entry<String, Integer> entry : bossKc.entrySet())
				{
					String boss = entry.getKey().toLowerCase();
					int webappKc = entry.getValue();
					int localKc = mergedBoss.getOrDefault(boss, -1);

					if (webappKc > localKc)
					{
						mergedBoss.put(boss, webappKc);
						bossUpdates++;
					}

					int lastKc = lastBossKCs.getOrDefault(boss, 0);
					if (webappKc > lastKc)
					{
						lastBossKCs.put(boss, webappKc);
					}
				}

				if (bossUpdates > 0)
				{
					progressStore.setBossKillCounts(mergedBoss, ProgressStore.UpdateSource.REMOTE);
					mergeCount += bossUpdates;
				}
			}
		}

		// Merge skill levels (update lastSkillLevels for delta detection)
		Map<String, Integer> skills = parseIntegerMap(progress, "skillLevels");
		if (skills != null)
		{
			if (allowNumericDecreases)
			{
				Map<String, Integer> normalized = new HashMap<>();
				for (Map.Entry<String, Integer> entry : skills.entrySet())
				{
					if (entry.getKey() == null)
					{
						continue;
					}
					normalized.put(entry.getKey().toLowerCase(), entry.getValue());
				}
				int updates = countMapDelta(lastSkillLevels, normalized);
				if (updates > 0 || lastSkillLevels.size() != normalized.size())
				{
					lastSkillLevels = new HashMap<>(normalized);
					mergeCount += updates;
				}
			}
			else
			{
				for (Map.Entry<String, Integer> entry : skills.entrySet())
				{
					String skill = entry.getKey().toLowerCase();
					int webappLevel = entry.getValue();
					int localLevel = lastSkillLevels.getOrDefault(skill, 0);

					if (webappLevel > localLevel)
					{
						lastSkillLevels.put(skill, webappLevel);
						mergeCount++;
					}
				}
			}
		}

		// Merge clue completions
		Map<String, Integer> clues = parseIntegerMap(progress, "clueCompletions");
		if (clues != null)
		{
			if (allowNumericDecreases)
			{
				Map<String, Integer> normalized = new HashMap<>();
				for (Map.Entry<String, Integer> entry : clues.entrySet())
				{
					if (entry.getKey() == null)
					{
						continue;
					}
					normalized.put(entry.getKey().toLowerCase(), entry.getValue());
				}
				int updates = countMapDelta(lastClueCompletions, normalized);
				if (updates > 0 || lastClueCompletions.size() != normalized.size())
				{
					lastClueCompletions = new HashMap<>(normalized);
					mergeCount += updates;
				}
			}
			else
			{
				for (Map.Entry<String, Integer> entry : clues.entrySet())
				{
					String tier = entry.getKey().toLowerCase();
					int webappCount = entry.getValue();
					int localCount = lastClueCompletions.getOrDefault(tier, 0);

					if (webappCount > localCount)
					{
						lastClueCompletions.put(tier, webappCount);
						mergeCount++;
					}
				}
			}
		}

		// Merge collection log count
		if (progress.has("collectionLogCount") && !progress.get("collectionLogCount").isJsonNull())
		{
			int webappCollLog = progress.get("collectionLogCount").getAsInt();
			if (webappCollLog > lastCollectionLogCount)
			{
				lastCollectionLogCount = webappCollLog;
				mergeCount++;
			}
		}

		// Merge quest points
		if (progress.has("questPoints") && !progress.get("questPoints").isJsonNull())
		{
			int webappQP = progress.get("questPoints").getAsInt();
			if (webappQP > lastQuestPoints)
			{
				lastQuestPoints = webappQP;
				mergeCount++;
			}
		}

		// Merge completed quests
		Set<String> completedQuests = parseStringSet(progress, "completedQuests");
		if (completedQuests != null && !completedQuests.isEmpty())
		{
			int before = lastCompletedQuests.size();
			lastCompletedQuests.addAll(completedQuests);
			mergeCount += Math.max(0, lastCompletedQuests.size() - before);
		}

		// Merge diary task counts
		Map<String, Map<String, Integer>> remoteCounts = parseNestedIntegerMap(progress, "diaryTaskCounts");
		if (remoteCounts != null && !remoteCounts.isEmpty())
		{
			Map<String, Map<String, Integer>> mergedCounts = progressStore.getDiaryTaskCounts();
			int countUpdates = 0;

			for (Map.Entry<String, Map<String, Integer>> entry : remoteCounts.entrySet())
			{
				String diary = entry.getKey();
				Map<String, Integer> remote = entry.getValue();
				if (remote == null)
				{
					continue;
				}

				Map<String, Integer> existing = mergedCounts.getOrDefault(diary, new HashMap<>());
				Map<String, Integer> updated = new HashMap<>(existing);

				for (Map.Entry<String, Integer> tierEntry : remote.entrySet())
				{
					String tier = tierEntry.getKey();
					int remoteValue = tierEntry.getValue();
					int localValue = updated.getOrDefault(tier, 0);
					if (remoteValue > localValue)
					{
						updated.put(tier, remoteValue);
						countUpdates++;
					}
				}

				mergedCounts.put(diary, updated);
			}

			if (countUpdates > 0)
			{
				progressStore.setDiaryTaskCounts(mergedCounts, ProgressStore.UpdateSource.REMOTE);
				updateLastDiaryTaskCounts(mergedCounts);
				mergeCount += countUpdates;
			}
		}

		if (mergeCount > 0)
		{
			if (notify)
			{
				notifyGameMessage("Synced " + mergeCount + " updates from DMMScape.");
			}
			SwingUtilities.invokeLater(this::refreshPanel);
		}
	}

	private Map<String, Map<String, Integer>> parseNestedIntegerMap(JsonObject response, String key)
	{
		if (response == null || key == null || !response.has(key) || response.get(key).isJsonNull())
		{
			return null;
		}

		JsonElement element = response.get(key);
		if (!element.isJsonObject())
		{
			return null;
		}

		Map<String, Map<String, Integer>> result = new HashMap<>();
		JsonObject outer = element.getAsJsonObject();

		for (Map.Entry<String, JsonElement> entry : outer.entrySet())
		{
			if (entry.getValue() == null || !entry.getValue().isJsonObject())
			{
				continue;
			}

			Map<String, Integer> inner = new HashMap<>();
			for (Map.Entry<String, JsonElement> tierEntry : entry.getValue().getAsJsonObject().entrySet())
			{
				if (tierEntry.getValue() != null && tierEntry.getValue().isJsonPrimitive())
				{
					try
					{
						inner.put(tierEntry.getKey(), tierEntry.getValue().getAsInt());
					}
					catch (NumberFormatException e)
					{
						// Skip invalid entries
					}
				}
			}

			result.put(entry.getKey(), inner);
		}

		return result;
	}

	private int countSetDelta(Set<String> before, Set<String> after)
	{
		if (before == null && after == null)
		{
			return 0;
		}
		if (before == null)
		{
			return after != null ? after.size() : 0;
		}
		if (after == null)
		{
			return before.size();
		}

		Set<String> removed = new HashSet<>(before);
		removed.removeAll(after);
		Set<String> added = new HashSet<>(after);
		added.removeAll(before);
		return removed.size() + added.size();
	}

	private int countMapDelta(Map<String, Integer> before, Map<String, Integer> after)
	{
		if (before == null && after == null)
		{
			return 0;
		}
		if (before == null)
		{
			return after != null ? after.size() : 0;
		}
		if (after == null)
		{
			return before.size();
		}

		int delta = 0;
		for (Map.Entry<String, Integer> entry : after.entrySet())
		{
			Integer beforeValue = before.get(entry.getKey());
			if (!Objects.equals(beforeValue, entry.getValue()))
			{
				delta++;
			}
		}
		for (String key : before.keySet())
		{
			if (!after.containsKey(key))
			{
				delta++;
			}
		}
		return delta;
	}

	private void withAuthHeader(java.util.function.Consumer<String> onReady, java.util.function.Consumer<String> onError)
	{
		if (runeliteTokenService == null)
		{
			String fallback = getFallbackAuthHeader();
			if (fallback == null || fallback.isEmpty())
			{
				if (onError != null)
				{
					onError.accept("Missing credentials");
				}
				return;
			}
			onReady.accept(fallback);
			return;
		}

		runeliteTokenService.ensureToken(() ->
		{
			String header = runeliteTokenService.getAuthHeader();
			if (header == null || header.isEmpty())
			{
				if (onError != null)
				{
					onError.accept("Sync token unavailable");
				}
				return;
			}
			onReady.accept(header);
		}, error ->
		{
			if (onError != null)
			{
				onError.accept(error);
			}
		});
	}

	private void sendSyncWithAuth(SyncData data)
	{
		sendSyncWithAuth(data, null, null);
	}

	private void sendSyncWithAuth(SyncData data, SyncService.SyncCallback callback)
	{
		sendSyncWithAuth(data, callback, null);
	}

	private void sendSyncWithAuth(SyncData data, SyncService.SyncCallback callback, SyncService.SyncResponseHandler responseHandler)
	{
		withAuthHeader(authHeader -> syncService.sendSync(data, config.webhookUrl(), authHeader, callback, responseHandler), error ->
		{
			if (callback != null)
			{
				callback.onError(error);
			}
			else
			{
				log.debug("Sync skipped: {}", error);
			}
		});
	}

	private void sendOwnedSync()
	{
		if (client.getGameState() != GameState.LOGGED_IN || progressStore == null)
		{
			return;
		}

		String username = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
		if (username == null || username.isEmpty())
		{
			return;
		}

		SyncData data = new SyncData();
		data.setUsername(username);
		data.setProfileType(getProfileType());
		data.setTimestamp(System.currentTimeMillis());
		data.setFullSync(false);
		data.setOwnedSigils(progressStore.getOwnedSigils());
		data.setOwnedLamps(progressStore.getOwnedLamps());
		data.setOwnedPrayers(progressStore.getOwnedPrayers());

		sendSyncWithAuth(data, null, this::handleSyncResponse);
	}

	private void handleOpenWeb()
	{
		if (deviceAuthService != null && deviceAuthService.isGuestLinked())
		{
			if (!confirmGuestWebOpen())
			{
				return;
			}

			deviceAuthService.requestGuestLoginLink(
				url -> SwingUtilities.invokeLater(() -> LinkBrowser.browse(url)),
				error -> SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
					panel,
					"Failed to open guest web login: " + error,
					"Guest Web Login",
					JOptionPane.ERROR_MESSAGE
				))
			);
			return;
		}

		LinkBrowser.browse(ApiConfig.webappBase());
	}

	private boolean confirmGuestWebOpen()
	{
		String message = "<html><b>Important:</b> If you've already used " + ApiConfig.webappHost() + " with a real account, " +
			"do NOT continue.<br>Use <b>Upgrade to Account</b> in the plugin instead so your data merges correctly." +
			"<br><br>This will log your browser into your guest account.<br>" +
			"If you've never used " + ApiConfig.webappHost() + ", it's safe to continue.</html>";

		int choice = JOptionPane.showOptionDialog(
			panel,
			message,
			"Open Guest Web",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE,
			null,
			new Object[]{ "Open Guest Web", "Cancel" },
			"Cancel"
		);

		return choice == JOptionPane.YES_OPTION;
	}

	private void handleUnsync()
	{
		if (configManager != null)
		{
			configManager.setConfiguration("dmmtracker", "syncEnabled", false);
			configManager.setConfiguration("dmmtracker", "enableTargetSync", false);
			configManager.setConfiguration("dmmtracker", "apiKey", "");
		}

		if (deviceAuthService != null)
		{
			deviceAuthService.unlink();
		}

		if (runeliteTokenService != null)
		{
			runeliteTokenService.clearToken();
		}

		lastAuthHeader = null;
		targetService.setAuthHeader(null);

		targetService.stopPolling();
		overlayManager.remove(targetOverlay);
		overlayManager.remove(minimapOverlay);
		overlayManager.remove(routeOverlay);
		overlayManager.remove(worldMapRouteOverlay);
		overlayManager.remove(worldMapLegendOverlay);
		mouseManager.unregisterMouseListener(worldMapLegendOverlay);
		clearWorldMapPoints();
		refreshPanel();
	}

	private void handleLinkAccount()
	{
		if (deviceAuthService == null)
		{
			return;
		}

		// Start the device code flow
		deviceAuthService.startLinking(new DeviceAuthService.AuthCallback()
		{
			@Override
			public void onStateChange(DeviceAuthService.LinkingState state, String message)
			{
				SwingUtilities.invokeLater(() -> refreshPanel());
			}

			@Override
			public void onSuccess(String username)
			{
				SwingUtilities.invokeLater(() ->
				{
					refreshPanel();
					// Re-sync auth header
					syncAuthHeaderIfNeeded();
				});
			}

			@Override
			public void onError(String error)
			{
				SwingUtilities.invokeLater(() -> refreshPanel());
			}
		});

		refreshPanel();
	}

	private void refreshPanel()
	{
		if (panel != null)
		{
			panel.refresh(syncService, targetService, config, deviceAuthService, runeliteTokenService);
		}
	}

	private void refreshPanelStatusOnly()
	{
		if (panel != null)
		{
			panel.refreshStatusOnly(syncService, targetService);
		}
	}

	private void refreshWorldMapPoints()
	{
		clearWorldMapPoints();

		boolean hasFocus = targetService.getFocusTarget() != null;
		if ((!config.enableTargetSync() && !hasFocus) || !config.showWorldMapTargets())
		{
			return;
		}

		List<TargetPoint> targets = targetService.getFilteredTargetsWithCoords(config);
		targets.sort(Comparator.comparingInt(TargetPoint::getOrder));
		int maxTargets = Math.max(1, config.maxMapTargets());

		for (int i = 0; i < Math.min(maxTargets, targets.size()); i++)
		{
			TargetPoint target = targets.get(i);
			if (target.getWorldPoint() == null)
			{
				continue;
			}

			int order = i + 1;
			boolean isTeleport = isTeleportTarget(target);
			String cacheKey = order + (isTeleport ? "-teleport" : "-default");
			BufferedImage icon = mapIconCache.computeIfAbsent(cacheKey, key -> createMapIcon(order, isTeleport));
			TargetMapPoint point = new TargetMapPoint(target, icon);
			worldMapPointManager.add(point);
			worldMapPoints.add(point);
		}
	}

	private void refreshTeleportMapPoints()
	{
		clearTeleportMapPoints();

		if (!config.showWorldMapTeleports())
		{
			return;
		}

		if (dataLoader == null)
		{
			return;
		}

		List<TeleportData> teleports = dataLoader.getTeleports();
		if (teleports.isEmpty())
		{
			return;
		}

		BufferedImage icon = getTeleportLayerIcon();
		int maxTeleports = Math.max(1, config.maxWorldMapTeleports());
		int count = 0;

		for (TeleportData teleport : teleports)
		{
			LocationData location = teleport.getLocation();
			if (location == null || location.getZ() != 0)
			{
				continue;
			}

			if (count >= maxTeleports)
			{
				break;
			}

			WorldPoint worldPoint = new WorldPoint(location.getX(), location.getY(), location.getZ());
			BufferedImage markerIcon = icon != null ? icon : createTeleportFallbackIcon();
			TeleportMapPoint point = new TeleportMapPoint(teleport, worldPoint, markerIcon, buildTeleportTooltip(teleport));
			worldMapPointManager.add(point);
			teleportMapPoints.add(point);
			count++;
		}
	}

	private void clearTeleportMapPoints()
	{
		for (TeleportMapPoint point : teleportMapPoints)
		{
			worldMapPointManager.remove(point);
		}
		teleportMapPoints.clear();
	}

	private BufferedImage getTeleportLayerIcon()
	{
		if (teleportLayerIcon != null)
		{
			return teleportLayerIcon;
		}

		BufferedImage base = IconManager.getImage(IconManager.LAW_RUNE_DETAIL);
		if (base == null)
		{
			return null;
		}

		teleportLayerIcon = ImageUtil.resizeImage(base, 14, 14);
		return teleportLayerIcon;
	}

	private BufferedImage createTeleportFallbackIcon()
	{
		BufferedImage icon = new BufferedImage(14, 14, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = icon.createGraphics();
		graphics.setColor(new Color(77, 163, 255, 200));
		graphics.fillOval(1, 1, 12, 12);
		graphics.setColor(Color.BLACK);
		graphics.drawOval(1, 1, 12, 12);
		graphics.dispose();
		return icon;
	}

	private String buildTeleportTooltip(TeleportData teleport)
	{
		if (teleport == null)
		{
			return "";
		}

		StringBuilder tooltip = new StringBuilder();
		if (teleport.getName() != null)
		{
			tooltip.append(teleport.getName());
		}

		String source = teleport.getSource();
		if (source != null && !source.isEmpty())
		{
			tooltip.append("\n").append(source);
		}

		String typeLabel = formatTeleportTypeLabel(teleport.getType());
		if (typeLabel != null && !typeLabel.isEmpty() && (source == null || !source.equalsIgnoreCase(typeLabel)))
		{
			tooltip.append("\nType: ").append(typeLabel);
		}

		Integer level = teleport.getLevel();
		if (level != null)
		{
			tooltip.append("\nMagic: ").append(level);
		}

		LocationData location = teleport.getLocation();
		if (location != null)
		{
			tooltip.append("\n(")
				.append(location.getX())
				.append(", ")
				.append(location.getY())
				.append(", ")
				.append(location.getZ())
				.append(")");
		}

		return tooltip.toString();
	}

	private String formatTeleportTypeLabel(String type)
	{
		if (type == null)
		{
			return null;
		}

		switch (type.toLowerCase())
		{
			case "standard":
				return "Standard Spellbook";
			case "ancient":
				return "Ancient Magicks";
			case "lunar":
				return "Lunar Spellbook";
			case "arceuus":
				return "Arceuus Spellbook";
			case "jewellery":
				return "Jewellery";
			case "scroll":
				return "Teleport Scrolls";
			case "other":
				return "Other";
			default:
				return type;
		}
	}

	private void ensureFocusOverlaysEnabled()
	{
		if (config.enableTargetSync())
		{
			return;
		}

		overlayManager.add(targetOverlay);
		overlayManager.add(minimapOverlay);
		overlayManager.add(routeOverlay);
		overlayManager.add(worldMapRouteOverlay);
		overlayManager.add(worldMapLegendOverlay);
		mouseManager.registerMouseListener(worldMapLegendOverlay);
	}

	private void disableFocusOverlaysIfIdle()
	{
		if (config.enableTargetSync())
		{
			return;
		}

		overlayManager.remove(targetOverlay);
		overlayManager.remove(minimapOverlay);
		overlayManager.remove(routeOverlay);
		overlayManager.remove(worldMapRouteOverlay);
		overlayManager.remove(worldMapLegendOverlay);
		mouseManager.unregisterMouseListener(worldMapLegendOverlay);
	}

	private void clearWorldMapPoints()
	{
		for (TargetMapPoint point : worldMapPoints)
		{
			worldMapPointManager.remove(point);
		}
		worldMapPoints.clear();
	}

	private BufferedImage createNavIcon()
	{
		BufferedImage logo = com.dmmtracker.ui.IconManager.getImage(com.dmmtracker.ui.IconManager.DMMSCAPE_LOGO);
		if (logo != null)
		{
			return ImageUtil.resizeImage(logo, 16, 16);
		}

		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = icon.createGraphics();
		graphics.setColor(new Color(255, 215, 0));
		graphics.fillOval(1, 1, 14, 14);
		graphics.setColor(Color.BLACK);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
		graphics.drawString("D", 4, 12);
		graphics.dispose();
		return icon;
	}

	private BufferedImage createMapIcon(int order, boolean isTeleport)
	{
		BufferedImage icon = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = icon.createGraphics();
		BufferedImage teleportIcon = isTeleport ? getTeleportMapIcon() : null;

		if (teleportIcon != null)
		{
			graphics.drawImage(teleportIcon, 1, 1, null);
			graphics.setColor(Color.BLACK);
			graphics.drawOval(1, 1, 18, 18);
		}
		else
		{
			graphics.setColor(new Color(0, 200, 255, 200));
			graphics.fillOval(1, 1, 18, 18);
			graphics.setColor(Color.BLACK);
			graphics.drawOval(1, 1, 18, 18);
		}

		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
		String text = String.valueOf(order);
		int x = text.length() > 1 ? 5 : 7;
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, 14);
		graphics.setColor(Color.WHITE);
		graphics.drawString(text, x, 13);
		graphics.dispose();
		return icon;
	}

	private BufferedImage getTeleportMapIcon()
	{
		if (teleportMapIcon != null)
		{
			return teleportMapIcon;
		}

		BufferedImage base = IconManager.getImage(IconManager.LAW_RUNE_DETAIL);
		if (base == null)
		{
			return null;
		}

		teleportMapIcon = ImageUtil.resizeImage(base, 18, 18);
		return teleportMapIcon;
	}

	private boolean isTeleportTarget(TargetPoint target)
	{
		return target != null && "teleport".equalsIgnoreCase(target.getType());
	}

	private void notifyGameMessage(String message)
	{
		ChatMessageBuilder builder = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append(message);
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(builder.build())
			.build());
	}

	private String buildPlayerAlarmMessage(Player player, String playerName)
	{
		StringBuilder message = new StringBuilder("Player detected: ").append(playerName);
		if (config.showCombatLevel())
		{
			int level = player.getCombatLevel();
			if (level > 0)
			{
				message.append(" (level ").append(level).append(")");
			}
		}

		return message.toString();
	}

	private Notification buildPlayerAlarmNotification()
	{
		NotificationSound sound = config.alarmSound() ? NotificationSound.NATIVE : NotificationSound.OFF;
		FlashNotification flash = config.alarmFlash()
			? FlashNotification.FLASH_UNTIL_CANCELLED
			: FlashNotification.DISABLED;
		return new Notification()
			.withEnabled(true)
			.withInitialized(true)
			.withOverride(true)
			.withTray(config.alarmTrayNotification())
			.withTrayIconType(TrayIcon.MessageType.WARNING)
			.withRequestFocus(RequestFocusType.OFF)
			.withSound(sound)
			.withVolume(100)
			.withTimeout(0)
			.withGameMessage(false)
			.withFlash(flash)
			.withFlashColor(config.alarmFlashColor())
			.withSendWhenFocused(true);
	}
}
