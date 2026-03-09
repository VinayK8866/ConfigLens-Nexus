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

import com.configlens.core.model.ConfigNode;
import java.util.Optional;

/**
 * Masks sensitive values in a configuration structure before sending to AI.
 * Ensures only keys and non-sensitive structure are exported.
 */
public final class PrivacyFilter {

  /**
   * Generates an AI-safe representation of a configuration node subtree.
   * Replaces potential secrets with "[MASKED]".
   */
  public String maskForAi(ConfigNode node) {
    StringBuilder sb = new StringBuilder();
    buildSafeString(node, sb, 0);
    return sb.toString();
  }

  private void buildSafeString(ConfigNode node, StringBuilder sb, int indent) {
    sb.append("  ".repeat(indent)).append(node.getKey()).append(": ");
    
    Optional<Object> value = node.getValue();
    if (value.isPresent()) {
      if (isLikelySensitive(node.getKey(), value.get().toString())) {
        sb.append("[MASKED]");
      } else {
        sb.append(value.get().toString());
      }
    }
    sb.append("\n");

    for (ConfigNode child : node.getChildren()) {
      buildSafeString(child, sb, indent + 1);
    }
  }

  private boolean isLikelySensitive(String key, String value) {
    // Simple heuristic: if SecretDetector flags it, mask it.
    // Also mask if value is high entropy or key looks like a credential.
    return new SecretDetector().isSecret(key, value);
  }
}
