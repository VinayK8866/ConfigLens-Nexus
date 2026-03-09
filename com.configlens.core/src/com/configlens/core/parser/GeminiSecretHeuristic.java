/*******************************************************************************
 * Copyright (c) 2026 VinayK8866.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * VinayK8866 - initial API and implementation
 *******************************************************************************/
package com.configlens.core.parser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Context-aware secret validation using Google Gemini Pro API.
 * This is an optional enhancement to local regex patterns.
 */
public final class GeminiSecretHeuristic {

	private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
	private final String apiKey;
	private final HttpClient httpClient;

	public GeminiSecretHeuristic(String apiKey) {
		this.apiKey = apiKey;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.build();
	}

	/**
	 * Asks Gemini if a specific key-value pair looks like a sensitive credential.
	 */
	public boolean confirmSecret(String key, String value) {
		if (apiKey == null || apiKey.isBlank())
			return false;

		String prompt = String.format(
				"Analyze this configuration entry: Key='%s', Value='%s'. " +
						"Does this value appear to be a sensitive secret like an API key, password, or private token? "
						+
						"Answer with exactly 'YES' or 'NO'.",
				key, value);

		try {
			String jsonPayload = String.format(
					"{\"contents\": [{\"parts\": [{\"text\": \"%s\"}]}]}", prompt.replace("\"", "\\\""));

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(API_URL + "?key=" + apiKey))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				String body = response.body().toUpperCase();
				return body.contains("\"YES\"");
			}
		} catch (Exception e) {
			// Fail silent for AI calls
		}
		return false;
	}
}
