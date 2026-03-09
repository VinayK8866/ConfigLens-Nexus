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
import com.configlens.core.model.ConfigTree;
import java.util.Optional;

/**
 * Calculation engine for resolving absolute paths from document offsets.
 * Part of the 'com.configlens.core' bundle to remain LSP-ready.
 */
public final class AbsoluteResolver {

  /**
   * Resolves the dot-notation path at a specific line.
   *
   * @param tree The configuration tree to search
   * @param line 1-indexed line number
   * @return Optional dot-notation path
   */
  public Optional<String> resolvePathAtLine(ConfigTree tree, int line) {
    if (tree == null) {
      return Optional.empty();
    }
    return tree.findNodeAtLine(line).map(ConfigNode::getPath);
  }

  /**
   * Formats a raw dot-notation path into a specific format (e.g., Spring-compatible).
   * Note: The current ConfigNode path generation in parsers is already Spring-compatible.
   */
  public String formatPath(String rawPath, String mode) {
    if (rawPath == null || rawPath.isEmpty()) {
      return "";
    }
    
    if ("jsonpath".equalsIgnoreCase(mode)) {
      return "$." + rawPath;
    }
    
    // Default is absolute dot-notation
    return rawPath;
  }
}
