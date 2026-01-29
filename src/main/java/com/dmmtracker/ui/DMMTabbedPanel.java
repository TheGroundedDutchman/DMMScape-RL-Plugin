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
import com.dmmtracker.DeviceAuthService;
import com.dmmtracker.RuneliteTokenService;
import com.dmmtracker.SyncService;
import com.dmmtracker.TargetPoint;
import com.dmmtracker.TargetService;
import com.dmmtracker.data.BossData;
import com.dmmtracker.ui.tabs.DiariesTab;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import java.awt.BorderLayout;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

public class DMMTabbedPanel extends PluginPanel
{
	public DMMTabbedPanel()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel placeholder = new JLabel("Tabs coming soon");
		placeholder.setBorder(new EmptyBorder(12, 8, 8, 8));
		add(placeholder, BorderLayout.NORTH);
	}

	public void setManualSyncHandler(Runnable handler)
	{
	}

	public void setTargetToggleHandler(BiConsumer<TargetPoint, Boolean> handler)
	{
	}

	public void setTargetNavigateHandler(Consumer<TargetPoint> handler)
	{
	}

	public void setTargetRefreshHandler(Runnable handler)
	{
	}

	public void setOpenWebHandler(Runnable handler)
	{
	}

	public void setBossAddToPlanHandler(Consumer<BossData> handler)
	{
	}

	public void setUnsyncHandler(Runnable handler)
	{
	}

	public void setUnlinkHandler(Runnable handler)
	{
	}

	public void setLinkAccountHandler(Runnable handler)
	{
	}

	public void setForceSyncHandler(Runnable handler)
	{
	}

	public void setBackToPlanHandler(Runnable handler)
	{
	}

	public void setDiaryLocationHandler(DiariesTab.LocationNavigateHandler handler)
	{
	}

	public void setClearNavigationHandler(Runnable handler)
	{
	}

	public void setDiaryLocationFocusChecker(DiariesTab.LocationFocusChecker handler)
	{
	}

	public void refresh(
		SyncService syncService,
		TargetService targetService,
		DMMTrackerConfig config,
		DeviceAuthService authService,
		RuneliteTokenService tokenService
	)
	{
	}

	public void refreshStatusOnly(SyncService syncService, TargetService targetService)
	{
	}

	public boolean isPlanTabActive()
	{
		return false;
	}
}
