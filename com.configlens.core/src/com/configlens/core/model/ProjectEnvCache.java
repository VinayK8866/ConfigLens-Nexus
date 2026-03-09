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
package com.configlens.core.model;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A central, thread-safe cache for environment variables (.env), scoped by project.
 * Adheres to the '100MB Rule' by storing only necessary key-value pairs in memory.
 */
public final class ProjectEnvCache {

  private static final ProjectEnvCache INSTANCE = new ProjectEnvCache();

  // Maps project identifier (e.g., project name or path) to its environment variables.
  private final Map<String, Map<String, String>> projectMaps = new ConcurrentHashMap<>();

  private ProjectEnvCache() {}

  public static ProjectEnvCache getInstance() {
    return INSTANCE;
  }

  /**
   * Updates the environment variables for a specific project.
   *
   * @param projectId Unique identifier for the project.
   * @param envData The new key-value pairs.
   */
  public void updateEnv(String projectId, Map<String, String> envData) {
    if (projectId == null || envData == null) {
      return;
    }
    projectMaps.put(projectId, Collections.unmodifiableMap(new ConcurrentHashMap<>(envData)));
  }

  /**
   * Resolves a key for a specific project.
   *
   * @param projectId The project to look in.
   * @param key The variable key.
   * @return The value or null if not found.
   */
  public String resolve(String projectId, String key) {
    if (projectId == null || key == null) {
      return null;
    }
    Map<String, String> env = projectMaps.get(projectId);
    return (env != null) ? env.get(key) : null;
  }

  /**
   * Clears the cache for a specific project.
   */
  public void clearProject(String projectId) {
    if (projectId != null) {
      projectMaps.remove(projectId);
    }
  }

  /**
   * Returns all keys for a project, useful for suggestions.
   */
  public Map<String, String> getProjectEnv(String projectId) {
    if (projectId == null) {
      return Collections.emptyMap();
    }
    return projectMaps.getOrDefault(projectId, Collections.emptyMap());
  }
}
