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

/**
 * Centralized API configuration for switching between local and production environments.
 *
 * To switch environments, change USE_LOCAL:
 *   true  = local Supabase (localhost:54321) + local webapp (localhost:5173)
 *   false = production (dmmscape.com)
 */
public final class ApiConfig
{
	// ===== TOGGLE THIS FOR LOCAL DEVELOPMENT =====
	public static final boolean USE_LOCAL = false;
	// ==============================================
	public static final boolean FORCE_SYNC_DEFAULTS = USE_LOCAL;

	// Production URLs
	private static final String PROD_API_BASE = "https://dmmscape.com/api";
	private static final String PROD_WEBAPP = "https://dmmscape.com";

	// Local development URLs (Supabase local)
	private static final String LOCAL_API_BASE = "http://localhost:54321/functions/v1/api";
	private static final String LOCAL_WEBAPP = "http://localhost:5173";

	private ApiConfig()
	{
	}

	/** API base URL (no trailing slash) */
	public static String apiBase()
	{
		return USE_LOCAL ? LOCAL_API_BASE : PROD_API_BASE;
	}

	/** Webapp base URL (no trailing slash) */
	public static String webappBase()
	{
		return USE_LOCAL ? LOCAL_WEBAPP : PROD_WEBAPP;
	}

	/** Webapp display name for UI text */
	public static String webappHost()
	{
		return USE_LOCAL ? "localhost:5173" : "dmmscape.com";
	}

	public static String versionTag(String version)
	{
		String ver = version != null ? version.toUpperCase() : "";
		if (USE_LOCAL)
		{
			return "[local " + ver + "]";
		}
		return "";
	}

	// ============ Convenience endpoint methods ============

	public static String syncUrl()
	{
		return apiBase() + "/me/sync/runelite";
	}

	public static String targetSyncUrl()
	{
		return apiBase() + "/me/plan/targets";
	}

	public static String pluginDataUrl()
	{
		return apiBase() + "/plugin-data";
	}

	public static String envLabel()
	{
		return USE_LOCAL ? "[LOCAL] " : "";
	}
}
