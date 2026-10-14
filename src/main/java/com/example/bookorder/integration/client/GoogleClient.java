package com.example.bookorder.integration.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.bookorder.integration.spi.ExternalApiClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component("googleClient")
public class GoogleClient implements ExternalApiClient {

	/**
	 * Registry key of this client. A new XXXClient defines its own key the same
	 * way.
	 */
	public static final String KEY = "google";

	private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
	private final URI uri;

	public GoogleClient(@Value("${external.page-url:https://www.google.com}") String url) {
		uri = URI.create(url);
	}

	/** Create the key -> this client mapping in the SPI registry. */
	@PostConstruct
	void registerSelf() {
		ExternalApiClient.register(KEY, this);
	}

	/** Remove the mapping when the application context closes. */
	@PreDestroy
	void unregisterSelf() {
		ExternalApiClient.unregister(KEY, this);
	}

	/**
	 * Perform one bounded HTTP GET without application retries and map transport
	 * failures. The HTTP status is left to the caller to interpret.
	 */
	@Override
	public HttpResponse<String> fetch() {
		try {
			HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build();
			return client.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ExternalUnavailableException(e);
		} catch (IOException e) { // includes connect/request timeouts
			throw new ExternalUnavailableException(e);
		}
	}

	public static class ExternalUnavailableException extends RuntimeException {
		public ExternalUnavailableException(Throwable cause) {
			super("External service unavailable", cause);
		}
	}
}