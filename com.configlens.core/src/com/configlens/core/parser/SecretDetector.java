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

import com.configlens.core.model.SecretResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance secret detection engine.
 * Scans content for sensitive patterns while respecting ignore comments.
 */
public final class SecretDetector {

  private static final String IGNORE_COMMENT = "configlens-ignore";

  /**
   * Quick check for a key-value pair. Used by UI highlighters.
   */
  public boolean isSecret(String key, String value) {
    if (value == null || value.isBlank()) return false;
    // Check if any pattern matches the value
    for (Pattern p : SecretPatternLibrary.PATTERNS.values()) {
        if (p.matcher(value).find()) return true;
    }
    // Check entropy - stricter threshold to avoid false positives on config keys/paths
    return calculateEntropy(value) > 5.5 && value.length() > 25;
  }

  /**
   * Scans a single line for secrets.
   * 
   * @param lineText The text of the line.
   * @param lineNumber 1-indexed line number.
   * @return List of detected secrets on this line.
   */
  public List<SecretResult> scanLine(String lineText, int lineNumber) {
    List<SecretResult> results = new ArrayList<>();

    // Skip blank lines and pure comment lines (no secrets there)
    String trimmed = lineText.trim();
    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
      return results;
    }

    // Skip lines that look like pure YAML/JSON structure (no value after colon)
    // e.g. "aws:" or "  database:" with nothing after
    if (trimmed.matches("^[\\w.\\-]+:\\s*$") || trimmed.equals("{") || trimmed.equals("}") 
        || trimmed.equals("[") || trimmed.equals("]")) {
      return results;
    }

    // Check for ignore comment
    if (lineText.contains(IGNORE_COMMENT)) {
      return results;
    }

    for (Map.Entry<String, Pattern> entry : SecretPatternLibrary.PATTERNS.entrySet()) {
      String type = entry.getKey();
      Pattern pattern = entry.getValue();
      Matcher matcher = pattern.matcher(lineText);

      while (matcher.find()) {
        results.add(new SecretResult(
            type,
            matcher.group(),
            lineNumber,
            matcher.start(),
            matcher.end(),
            "Potential " + type + " detected. If this is not a secret, append '# configlens-ignore' to the line."
        ));
      }
    }

    // Add generic high-entropy check for long strings if no specific pattern matched
    if (results.isEmpty()) {
      checkHighEntropy(lineText, lineNumber, results);
    }

    return results;
  }

  private void checkHighEntropy(String lineText, int lineNumber, List<SecretResult> results) {
    // Regex to find potential tokens in quotes or after colons
    Pattern tokenCandidate = Pattern.compile("(['\"])([\\w\\-/+]{20,})\\1|:\\s*([\\w\\-/+]{20,})");
    Matcher matcher = tokenCandidate.matcher(lineText);
    
    while (matcher.find()) {
      String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
      if (value != null && calculateEntropy(value) > 4.5) {
        results.add(new SecretResult(
            "High-Entropy Token",
            value,
            lineNumber,
            matcher.start(matcher.group(2) != null ? 2 : 3),
            matcher.end(matcher.group(2) != null ? 2 : 3),
            "High-entropy string detected (possible token). If this is not a secret, append '# configlens-ignore' to the line."
        ));
      }
    }
  }

  private double calculateEntropy(String s) {
    if (s == null || s.isEmpty()) return 0;
    int[] count = new int[256];
    for (char c : s.toCharArray()) {
      if (c < 256) count[c]++;
    }
    double entropy = 0;
    for (int i = 0; i < 256; i++) {
      if (count[i] > 0) {
        double p = (double) count[i] / s.length();
        entropy -= p * (Math.log(p) / Math.log(2));
      }
    }
    return entropy;
  }
}
