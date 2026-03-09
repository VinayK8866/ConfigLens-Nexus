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
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for interacting with Google Gemini Pro API.
 * Performs async analysis of configuration structures.
 */
public final class GeminiAiService {

  private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final String apiKey;
  private final HttpClient httpClient;

  public GeminiAiService(String apiKey) {
    this.apiKey = apiKey;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }

  /**
   * Analyzes masked configuration content and returns AI insights.
   */
  public CompletableFuture<String> analyzeConfig(String maskedContent) {
    if (apiKey == null || apiKey.isBlank()) {
      return CompletableFuture.failedFuture(new IllegalStateException("API Key not provided"));
    }

    String prompt = "You are a cloud security expert. Analyze this configuration structure (sensitive values are [MASKED]). " +
                    "Identify potential architectural risks, missing security settings, or misconfigurations. " +
                    "Provide 3 concise, actionable suggestions.\n\n" + maskedContent;

    String jsonPayload = String.format(
        "{\"contents\": [{\"parts\": [{\"text\": \"%s\"}]}]}", 
        prompt.replace("\"", "\\\"").replace("\n", "\\n")
    );

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(API_URL + "?key=" + apiKey))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
        .build();

    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
          if (response.statusCode() == 200) {
            return extractText(response.body());
          } else {
            return "Error from Gemini: " + response.statusCode();
          }
        });
  }

  private String extractText(String body) {
    try {
      JsonNode root = MAPPER.readTree(body);
      JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
      if (!parts.isMissingNode() && parts.size() > 0) {
        JsonNode textNode = parts.path(0).path("text");
        if (!textNode.isMissingNode()) {
          return textNode.asText();
        }
      }
    } catch (Exception e) {
       // Fallback
    }
    return "Could not parse AI response.";
  }
}
