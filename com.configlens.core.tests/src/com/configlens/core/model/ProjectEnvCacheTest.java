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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the thread-safe ProjectEnvCache with project-specific scoping.
 */
public class ProjectEnvCacheTest {

  private ProjectEnvCache cache;

  @Before
  public void setUp() {
    cache = ProjectEnvCache.getInstance();
    cache.clearProject("proj1");
    cache.clearProject("proj2");
  }

  @Test
  public void testProjectScoping() {
    Map<String, String> env1 = new HashMap<>();
    env1.put("API_URL", "https://api.proj1.com");
    cache.updateEnv("proj1", env1);

    Map<String, String> env2 = new HashMap<>();
    env2.put("API_URL", "https://api.proj2.com");
    cache.updateEnv("proj2", env2);

    assertEquals("https://api.proj1.com", cache.resolve("proj1", "API_URL"));
    assertEquals("https://api.proj2.com", cache.resolve("proj2", "API_URL"));
  }

  @Test
  public void testCacheUpdate() {
    Map<String, String> env = new HashMap<>();
    env.put("DEBUG", "true");
    cache.updateEnv("proj1", env);
    assertEquals("true", cache.resolve("proj1", "DEBUG"));

    env.put("DEBUG", "false");
    cache.updateEnv("proj1", env);
    assertEquals("false", cache.resolve("proj1", "DEBUG"));
  }

  @Test
  public void testNonExistentKeys() {
    assertNull(cache.resolve("proj1", "UNKNOWN"));
    assertNull(cache.resolve("non-existent", "KEY"));
  }
}
