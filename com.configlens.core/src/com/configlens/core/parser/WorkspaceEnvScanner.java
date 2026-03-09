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
import com.configlens.core.model.ProjectEnvCache;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background scanner to discover and parse .env files within a project directory.
 * Operates using Java NIO to remain LSP-ready and independent of Eclipse APIs.
 */
public final class WorkspaceEnvScanner {

  private static final Logger LOGGER = Logger.getLogger(WorkspaceEnvScanner.class.getName());
  private final EnvStreamingParser parser = new EnvStreamingParser();

  /**
   * Scans a directory for .env files and populates the global ProjectEnvCache.
   *
   * @param projectId Unique identifier for the project scope (e.g., project name).
   * @param rootPath The base directory to scan.
   */
  public void scanProject(String projectId, Path rootPath) {
    if (rootPath == null || !Files.isDirectory(rootPath)) {
      return;
    }

    Map<String, String> aggregateEnv = new HashMap<>();

    try {
      Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (file.getFileName().toString().equals(".env")) {
            parseAndCollect(file, aggregateEnv);
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
          // Skip hidden directories (like .git, .metadata)
          if (dir.getFileName() != null && dir.getFileName().toString().startsWith(".")) {
            return FileVisitResult.SKIP_SUBTREE;
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to scan workspace for .env files in " + rootPath, e);
    }

    ProjectEnvCache.getInstance().updateEnv(projectId, aggregateEnv);
  }

  /**
   * Parses a single .env file and adds its keys to the collected map.
   */
  public void parseAndCollect(Path envFile, Map<String, String> targetMap) {
    try {
      ConfigTree tree = parser.parse(Files.newBufferedReader(envFile));
      for (ConfigNode node : tree.getRootNode().getChildren()) {
        node.getValue().ifPresent(val -> targetMap.put(node.getKey(), val.toString()));
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to parse .env file: " + envFile, e);
    }
  }
}
