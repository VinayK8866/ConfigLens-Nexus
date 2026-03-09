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

import static org.junit.Assert.assertEquals;

import com.configlens.core.model.ProjectEnvCache;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for WorkspaceEnvScanner using JUnit 4.
 */
public class WorkspaceEnvScannerTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testScannerDiscoversEnvFiles() throws IOException {
    // Setup nested structure
    Path tempDir = tempFolder.getRoot().toPath();
    Path projectRoot = tempDir.resolve("my-project");
    Files.createDirectory(projectRoot);
    
    Path subDir = projectRoot.resolve("src");
    Files.createDirectory(subDir);
    
    // Create .env in root
    Files.writeString(projectRoot.resolve(".env"), "ROOT_VAR=root\nSHARED=root-version");
    
    // Create .env in sub-directory
    Files.writeString(subDir.resolve(".env"), "SUB_VAR=sub\nSHARED=sub-version");

    WorkspaceEnvScanner scanner = new WorkspaceEnvScanner();
    scanner.scanProject("my-project", projectRoot);

    ProjectEnvCache cache = ProjectEnvCache.getInstance();
    assertEquals("root", cache.resolve("my-project", "ROOT_VAR"));
    assertEquals("sub", cache.resolve("my-project", "SUB_VAR"));
    
    // The aggregate behavior in scanner.visitFile depends on visitor order, 
    // but usually walking will visit root then sub. 
    // If it's a map.put, the last one wins. 
    // The current scanner visits root .env and sub .env.
  }
}
