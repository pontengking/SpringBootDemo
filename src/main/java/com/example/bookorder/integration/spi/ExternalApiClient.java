package com.example.bookorder.integration.spi;

import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Outbound integration contract (SPI) for external API calls. Every client
 * implementation registers itself into this SPI registry under its own key, and
 * each service looks up the client it needs by that key.
 */
public interface ExternalApiClient {

	Map<String, ExternalApiClient> REGISTRY = new ConcurrentHashMap<>();

	/**
	 * Register a client implementation with a provider keyword.
	 */
	static void register(String key, ExternalApiClient client) {
		if (key != null && client != null) {
			REGISTRY.put(normalize(key), client);
		}
	}

	/**
	 * Remove a client from the registry (only if it is still the registered
	 * instance).
	 */
	static void unregister(String key, ExternalApiClient client) {
		if (key != null && client != null) {
			REGISTRY.remove(normalize(key), client);
		}
	}

	/**
	 * Retrieve a registered client implementation by provider keyword.
	 */
	static ExternalApiClient get(String key) {
		if (key == null) {
			return null;
		}
		return REGISTRY.get(normalize(key));
	}

	private static String normalize(String key) {
		return key.trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Fetch once. The implementation owns connect/request timeouts and reports
	 * transport failures. The returned response is the raw external HTTP response
	 * (status, headers, and body as a String), not the application's SUCCESS/FAIL
	 * envelope. Callers must invoke this outside database transactions; no
	 * automatic retry is implied.
	 */
	HttpResponse<String> fetch();
}