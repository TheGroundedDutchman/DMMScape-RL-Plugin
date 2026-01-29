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
 * Device Code Flow authentication service
 * Implements RFC 8628 Device Authorization Grant for linking to DMMScape webapp
 */
package com.dmmtracker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.LinkBrowser;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
public class DeviceAuthService
{
	private static final Logger log = LoggerFactory.getLogger(DeviceAuthService.class);
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final String DEFAULT_API_BASE_URL = ApiConfig.apiBase();
	private static final int TIMEOUT_SECONDS = 10;

	// Config keys for storing auth state
	private static final String CONFIG_GROUP = "dmmtracker";
	private static final String CONFIG_ACCESS_TOKEN = "accessToken";
	private static final String CONFIG_USERNAME = "linkedUsername";
	private static final String CONFIG_IS_GUEST = "linkedIsGuest";

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final ConfigManager configManager;
	private final ScheduledExecutorService executor;

	// Current linking state
	private volatile String currentUserCode = null;
	private volatile String currentDeviceCode = null;
	private volatile String verificationUrl = null;
	private volatile int pollInterval = 5;
	private volatile int expiresIn = 0;
	private volatile long expiresAt = 0;
	private volatile LinkingState linkingState = LinkingState.IDLE;
	private volatile String linkingError = null;

	private ScheduledFuture<?> pollFuture = null;
	private AuthCallback currentCallback = null;

	public enum LinkingState
	{
		IDLE,               // Not linking
		AWAITING_AUTH,      // Waiting for user to authorize in browser
		POLLING,            // Actively polling for authorization
		SUCCESS,            // Successfully linked
		ERROR,              // Error occurred
		EXPIRED             // Code expired
	}

	public interface AuthCallback
	{
		void onStateChange(LinkingState state, String message);
		void onSuccess(String username);
		void onError(String error);
	}

	@Inject
	public DeviceAuthService(OkHttpClient httpClient, ConfigManager configManager, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient.newBuilder()
			.connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.build();
		this.gson = new Gson();
		this.configManager = configManager;
		this.executor = executor;
	}

	/**
	 * Check if we have a stored access token
	 */
	public boolean isLinked()
	{
		String token = getAccessToken();
		return token != null && !token.isEmpty();
	}

	/**
	 * Get the stored access token
	 */
	public String getAccessToken()
	{
		return configManager.getConfiguration(CONFIG_GROUP, CONFIG_ACCESS_TOKEN);
	}

	/**
	 * Get the linked username
	 */
	public String getLinkedUsername()
	{
		return configManager.getConfiguration(CONFIG_GROUP, CONFIG_USERNAME);
	}

	public boolean isGuestLinked()
	{
		return "true".equalsIgnoreCase(configManager.getConfiguration(CONFIG_GROUP, CONFIG_IS_GUEST));
	}

	public String getCurrentUserCode()
	{
		return currentUserCode;
	}

	public String getCurrentDeviceCode()
	{
		return currentDeviceCode;
	}

	public String getVerificationUrl()
	{
		return verificationUrl;
	}

	public int getPollInterval()
	{
		return pollInterval;
	}

	public int getExpiresIn()
	{
		return expiresIn;
	}

	public long getExpiresAt()
	{
		return expiresAt;
	}

	public LinkingState getLinkingState()
	{
		return linkingState;
	}

	public String getLinkingError()
	{
		return linkingError;
	}

	/**
	 * Clear stored authentication
	 */
	public void unlink()
	{
		configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_ACCESS_TOKEN);
		configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_USERNAME);
		configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_IS_GUEST);
		cancelLinking();
		log.info("Unlinked from DMMScape account");
	}

	/**
	 * Start the device code linking flow
	 */
	public void startLinking(AuthCallback callback)
	{
		if (linkingState == LinkingState.AWAITING_AUTH || linkingState == LinkingState.POLLING)
		{
			log.debug("Already linking, ignoring duplicate request");
			return;
		}

		cancelLinking();
		currentCallback = callback;
		linkingState = LinkingState.IDLE;
		linkingError = null;

		// Request a new device code
		String apiBaseUrl = resolveApiBaseUrl();
		Request.Builder builder = new Request.Builder()
			.url(apiBaseUrl + "/auth/device/code")
			.header("Content-Type", "application/json")
			.header("User-Agent", "DMMScape-Companion/1.0")
			.post(RequestBody.create(JSON, "{}"));

		if (isGuestLinked())
		{
			String token = getAccessToken();
			if (token != null && !token.isEmpty())
			{
				builder.header("Authorization", "Bearer " + token);
			}
		}

		Request request = builder.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to request device code", e);
				setError("Network error: " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						String error = body != null ? body.string() : "Unknown error";
						log.error("Device code request failed: {}", error);
						setError("Failed to get device code");
						return;
					}

					String json = body.string();
					JsonObject result = new JsonParser().parse(json).getAsJsonObject();

					currentDeviceCode = result.get("device_code").getAsString();
					currentUserCode = result.get("user_code").getAsString();
					verificationUrl = result.get("verification_uri_complete").getAsString();
					pollInterval = result.get("interval").getAsInt();
					expiresIn = result.get("expires_in").getAsInt();
					expiresAt = System.currentTimeMillis() + (expiresIn * 1000L);

					log.info("Device code obtained. User code: {}", currentUserCode);

					linkingState = LinkingState.AWAITING_AUTH;
					if (currentCallback != null)
					{
						currentCallback.onStateChange(linkingState, "Enter code: " + currentUserCode);
					}

					// Open browser automatically
					javax.swing.SwingUtilities.invokeLater(() -> LinkBrowser.browse(verificationUrl));

					// Start polling
					startPolling();
				}
			}
		});
	}

	/**
	 * Create a guest account without browser login.
	 */
	public void startGuestLinking(AuthCallback callback)
	{
		if (linkingState == LinkingState.AWAITING_AUTH || linkingState == LinkingState.POLLING)
		{
			log.debug("Already linking, ignoring duplicate request");
			return;
		}

		cancelLinking();
		currentCallback = callback;
		linkingState = LinkingState.IDLE;
		linkingError = null;

		String apiBaseUrl = resolveApiBaseUrl();
		Request request = new Request.Builder()
			.url(apiBaseUrl + "/auth/guest")
			.header("Content-Type", "application/json")
			.header("User-Agent", "DMMScape-Companion/1.0")
			.post(RequestBody.create(JSON, "{}"))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to create guest account", e);
				setError("Network error: " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						String error = body != null ? body.string() : "Unknown error";
						log.error("Guest account request failed: {}", error);
						setError("Failed to create guest account");
						return;
					}

					String json = body.string();
					JsonObject result = new JsonParser().parse(json).getAsJsonObject();

					String accessToken = result.get("access_token").getAsString();
					JsonObject user = result.getAsJsonObject("user");
					String username = extractDisplayName(user);
					boolean isGuest = user.has("isGuest") && user.get("isGuest").getAsBoolean();

					storeLinkedAccount(accessToken, username, isGuest);

					linkingState = LinkingState.SUCCESS;
					if (currentCallback != null)
					{
						currentCallback.onSuccess(username);
					}
					clearLinkingSession();
				}
			}
		});
	}

	/**
	 * Request a one-time guest login link for the webapp.
	 */
	public void requestGuestLoginLink(Consumer<String> onSuccess, Consumer<String> onError)
	{
		if (!isGuestLinked())
		{
			if (onError != null)
			{
				onError.accept("Guest account not linked");
			}
			return;
		}

		String accessToken = getAccessToken();
		if (accessToken == null || accessToken.isEmpty())
		{
			if (onError != null)
			{
				onError.accept("Missing guest token");
			}
			return;
		}

		String apiBaseUrl = resolveApiBaseUrl();
		Request request = new Request.Builder()
			.url(apiBaseUrl + "/auth/guest/login-link")
			.header("Content-Type", "application/json")
			.header("Authorization", "Bearer " + accessToken)
			.header("User-Agent", "DMMScape-Companion/1.0")
			.post(RequestBody.create(JSON, "{}"))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to request guest login link", e);
				if (onError != null)
				{
					onError.accept("Network error");
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						String error = body != null ? body.string() : "Unknown error";
						log.error("Guest login link request failed: {}", error);
						if (onError != null)
						{
							onError.accept("Failed to create login link");
						}
						return;
					}

					String json = body.string();
					JsonObject result = new JsonParser().parse(json).getAsJsonObject();
					String loginUrl = result.get("login_url").getAsString();

					if (onSuccess != null)
					{
						onSuccess.accept(loginUrl);
					}
				}
			}
		});
	}

	/**
	 * Cancel the current linking flow
	 */
	public void cancelLinking()
	{
		clearLinkingSession();
		linkingState = LinkingState.IDLE;
		linkingError = null;
	}

	private void clearLinkingSession()
	{
		if (pollFuture != null)
		{
			pollFuture.cancel(false);
			pollFuture = null;
		}
		currentDeviceCode = null;
		currentUserCode = null;
		verificationUrl = null;
		currentCallback = null;
		expiresIn = 0;
		expiresAt = 0;
	}

	private void startPolling()
	{
		if (pollFuture != null)
		{
			pollFuture.cancel(false);
		}

		linkingState = LinkingState.POLLING;

		pollFuture = executor.scheduleAtFixedRate(
			this::pollForAuthorization,
			pollInterval,
			pollInterval,
			TimeUnit.SECONDS
		);
	}

	private void pollForAuthorization()
	{
		if (currentDeviceCode == null)
		{
			cancelLinking();
			return;
		}

		// Check expiration
		if (System.currentTimeMillis() > expiresAt)
		{
			log.info("Device code expired");
			linkingState = LinkingState.EXPIRED;
			linkingError = "Code expired. Please try again.";
			if (currentCallback != null)
			{
				currentCallback.onError(linkingError);
			}
			clearLinkingSession();
			return;
		}

		String apiBaseUrl = resolveApiBaseUrl();
		Request request = new Request.Builder()
			.url(apiBaseUrl + "/auth/device/status?device_code=" + currentDeviceCode)
			.header("User-Agent", "DMMScape-Companion/1.0")
			.build();

		try
		{
			Response response = httpClient.newCall(request).execute();
			try (ResponseBody body = response.body())
			{
				if (body == null)
				{
					return;
				}

				String json = body.string();
				JsonObject result = new JsonParser().parse(json).getAsJsonObject();

				if (response.isSuccessful())
				{
					// Success - we got the access token!
					String accessToken = result.get("access_token").getAsString();
					JsonObject user = result.getAsJsonObject("user");
					String username = extractDisplayName(user);
					boolean isGuest = user.has("isGuest") && user.get("isGuest").getAsBoolean();

					storeLinkedAccount(accessToken, username, isGuest);

					log.info("Successfully linked to DMMScape account: {}", username);

					linkingState = LinkingState.SUCCESS;
					if (currentCallback != null)
					{
						currentCallback.onSuccess(username);
					}
					clearLinkingSession();
					return;
				}

				// Handle error responses
				if (result.has("error"))
				{
					String error = result.get("error").getAsString();
					switch (error)
					{
						case "authorization_pending":
							// User hasn't authorized yet, keep polling
							log.debug("Authorization pending...");
							break;

						case "slow_down":
							// Polling too fast, increase interval
							pollInterval = result.has("interval")
								? result.get("interval").getAsInt()
								: pollInterval * 2;
							log.debug("Slowing down polling to {}s", pollInterval);
							// Restart polling with new interval
							if (pollFuture != null)
							{
								pollFuture.cancel(false);
							}
							pollFuture = executor.scheduleAtFixedRate(
								this::pollForAuthorization,
								pollInterval,
								pollInterval,
								TimeUnit.SECONDS
							);
							break;

						case "expired_token":
							log.info("Device code expired");
							linkingState = LinkingState.EXPIRED;
							linkingError = "Code expired. Please try again.";
							if (currentCallback != null)
							{
								currentCallback.onError(linkingError);
							}
							clearLinkingSession();
							break;

						case "access_denied":
							log.info("User denied authorization");
							setError("Authorization denied");
							break;

						default:
							String desc = result.has("error_description")
								? result.get("error_description").getAsString()
								: error;
							log.warn("Authorization error: {}", desc);
							setError(desc);
							break;
					}
				}
			}
		}
		catch (IOException e)
		{
			log.error("Poll request failed", e);
			// Don't stop polling on transient network errors
		}
	}

	private void setError(String error)
	{
		linkingState = LinkingState.ERROR;
		linkingError = error;
		if (currentCallback != null)
		{
			currentCallback.onError(error);
		}
		clearLinkingSession();
	}

	private void storeLinkedAccount(String accessToken, String username, boolean isGuest)
	{
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_ACCESS_TOKEN, accessToken);
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_IS_GUEST, String.valueOf(isGuest));
		if (username != null)
		{
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_USERNAME, username);
		}
		else
		{
			configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_USERNAME);
		}
	}

	private String extractDisplayName(JsonObject user)
	{
		if (user == null)
		{
			return null;
		}
		if (user.has("username") && !user.get("username").isJsonNull())
		{
			String value = user.get("username").getAsString();
			if (!value.isEmpty())
			{
				return value;
			}
		}
		if (user.has("displayName") && !user.get("displayName").isJsonNull())
		{
			String value = user.get("displayName").getAsString();
			if (!value.isEmpty())
			{
				return value;
			}
		}
		if (user.has("isGuest") && user.get("isGuest").getAsBoolean())
		{
			return "Guest";
		}
		return null;
	}

	/**
	 * Get authentication header value.
	 * Returns the access token if linked, otherwise returns null.
	 * Falls back to API key from config if not linked via device code.
	 */
	public String getAuthHeader(String apiKeyFallback)
	{
		String token = getAccessToken();
		if (token != null && !token.isEmpty())
		{
			return "Bearer " + token;
		}
		// Fall back to API key if available
		if (apiKeyFallback != null && !apiKeyFallback.isEmpty())
		{
			return apiKeyFallback; // Will be used with X-Api-Key header
		}
		return null;
	}

	private String resolveApiBaseUrl()
	{
		String webhookUrl = configManager.getConfiguration(CONFIG_GROUP, "webhookUrl");
		String targetUrl = configManager.getConfiguration(CONFIG_GROUP, "targetSyncUrl");

		String resolved = parseApiBaseFromUrl(webhookUrl);
		if (resolved != null)
		{
			return resolved;
		}

		resolved = parseApiBaseFromUrl(targetUrl);
		if (resolved != null)
		{
			return resolved;
		}

		return DEFAULT_API_BASE_URL;
	}

	private String parseApiBaseFromUrl(String url)
	{
		if (url == null || url.isEmpty())
		{
			return null;
		}

		try
		{
			java.net.URI uri = new java.net.URI(url);
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

			if (basePath.isEmpty())
			{
				return null;
			}

			return new java.net.URI(uri.getScheme(), uri.getAuthority(), basePath, null, null).toString();
		}
		catch (Exception e)
		{
			log.debug("Failed to parse API base URL: {}", e.getMessage());
			return null;
		}
	}
}
