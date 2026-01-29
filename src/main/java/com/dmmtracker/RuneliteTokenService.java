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
 * Manages short-lived sync tokens for RuneLite API calls.
 */
package com.dmmtracker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
public class RuneliteTokenService
{
	private static final Logger log = LoggerFactory.getLogger(RuneliteTokenService.class);
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final long REFRESH_WINDOW_MS = 2 * 60 * 1000;
	private static final long MIN_REFRESH_INTERVAL_MS = 10_000;

	private final OkHttpClient httpClient;
	private final Gson gson = new Gson();
	private final DMMTrackerConfig config;
	private final DeviceAuthService deviceAuthService;

	private volatile String authHeader = null;

	private volatile long expiresAtMs = 0;

	private volatile long lastRefreshAttemptMs = 0;

	private volatile String lastError = null;

	private final List<Runnable> pendingSuccess = new ArrayList<>();
	private final List<Consumer<String>> pendingError = new ArrayList<>();
	private volatile boolean refreshInProgress = false;

	@Inject
	public RuneliteTokenService(OkHttpClient httpClient, DMMTrackerConfig config, DeviceAuthService deviceAuthService)
	{
		this.httpClient = httpClient.newBuilder()
			.connectTimeout(10, TimeUnit.SECONDS)
			.readTimeout(10, TimeUnit.SECONDS)
			.writeTimeout(10, TimeUnit.SECONDS)
			.build();
		this.config = config;
		this.deviceAuthService = deviceAuthService;
	}

	public String getAuthHeader()
	{
		return authHeader;
	}

	public long getExpiresAtMs()
	{
		return expiresAtMs;
	}

	public long getLastRefreshAttemptMs()
	{
		return lastRefreshAttemptMs;
	}

	public String getLastError()
	{
		return lastError;
	}

	public synchronized void ensureToken(Runnable onReady, Consumer<String> onError)
	{
		if (isTokenValid())
		{
			onReady.run();
			return;
		}

		pendingSuccess.add(onReady);
		if (onError != null)
		{
			pendingError.add(onError);
		}

		if (refreshInProgress)
		{
			return;
		}

		refreshInProgress = true;
		refreshToken();
	}

	public synchronized void refreshIfNeeded()
	{
		if (isTokenValid())
		{
			return;
		}

		if (refreshInProgress)
		{
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastRefreshAttemptMs < MIN_REFRESH_INTERVAL_MS)
		{
			return;
		}

		refreshInProgress = true;
		refreshToken();
	}

	public synchronized void clearToken()
	{
		authHeader = null;
		expiresAtMs = 0;
	}

	private boolean isTokenValid()
	{
		if (authHeader == null || authHeader.isEmpty())
		{
			return false;
		}
		long now = System.currentTimeMillis();
		return expiresAtMs > 0 && now < (expiresAtMs - REFRESH_WINDOW_MS);
	}

	private void refreshToken()
	{
		lastRefreshAttemptMs = System.currentTimeMillis();

		String initialAuthHeader = deviceAuthService != null
			? deviceAuthService.getAuthHeader(config.apiKey())
			: config.apiKey();
		if (initialAuthHeader == null || initialAuthHeader.isEmpty())
		{
			failRefresh("Missing credentials");
			return;
		}

		String baseUrl = getApiBaseUrl();
		String tokenUrl = baseUrl.endsWith("/") ? baseUrl + "me/sync-token" : baseUrl + "/me/sync-token";

		RequestBody body = RequestBody.create(JSON, "{}");
		Request.Builder builder = new Request.Builder()
			.url(tokenUrl)
			.post(body)
			.header("Content-Type", "application/json")
			.header("User-Agent", "DMMScape-Companion/1.0");

		if (initialAuthHeader.startsWith("Bearer "))
		{
			builder.header("Authorization", initialAuthHeader);
		}
		else
		{
			builder.header("X-Api-Key", initialAuthHeader);
		}

		Request request = builder.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				failRefresh("Token request failed");
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					if (!response.isSuccessful() || responseBody == null)
					{
						failRefresh("Token request failed (" + response.code() + ")");
						return;
					}

					String bodyText = responseBody.string();
					JsonObject payload = gson.fromJson(bodyText, JsonObject.class);
					if (payload == null || !payload.has("token") || !payload.has("expiresAt"))
					{
						failRefresh("Invalid token payload");
						return;
					}

					String token = payload.get("token").getAsString();
					long expiresAt = payload.get("expiresAt").getAsLong();

					synchronized (RuneliteTokenService.this)
					{
						authHeader = "Bearer " + token;
						expiresAtMs = expiresAt;
						lastError = null;
						refreshInProgress = false;
						flushSuccess();
					}
				}
			}
		});
	}

	private String getApiBaseUrl()
	{
		String source = config.webhookUrl();
		if (source == null || source.isEmpty())
		{
			source = config.targetSyncUrl();
		}
		if (source == null || source.isEmpty())
		{
			return ApiConfig.apiBase();
		}

		try
		{
			URI uri = new URI(source);
			String path = uri.getPath();
			String basePath = "";

			int functionsIdx = path.indexOf("/functions/v1/api");
			if (functionsIdx >= 0)
			{
				basePath = path.substring(0, functionsIdx + "/functions/v1/api".length());
			}
			else
			{
				int apiIdx = path.indexOf("/api");
				if (apiIdx >= 0)
				{
					basePath = path.substring(0, apiIdx + 4);
				}
			}

			return new URI(uri.getScheme(), uri.getAuthority(), basePath, null, null).toString();
		}
		catch (Exception e)
		{
			log.debug("Failed to parse API base URL: {}", e.getMessage());
			return ApiConfig.apiBase();
		}
	}

	private synchronized void failRefresh(String message)
	{
		clearToken();
		lastError = message;
		refreshInProgress = false;
		flushError(message);
	}

	private void flushSuccess()
	{
		for (Runnable callback : pendingSuccess)
		{
			callback.run();
		}
		pendingSuccess.clear();
		pendingError.clear();
	}

	private void flushError(String message)
	{
		for (Consumer<String> callback : pendingError)
		{
			callback.accept(message);
		}
		pendingSuccess.clear();
		pendingError.clear();
	}
}
