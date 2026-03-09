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
import static org.junit.Assert.assertTrue;

import com.configlens.core.model.ConfigNode;
import com.configlens.core.model.ConfigTree;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class YamlStreamingParserTest {

  private YamlStreamingParser parser;

  @Before
  public void setUp() {
    parser = new YamlStreamingParser();
  }

  @Test
  public void testYamlParsingAndOffsets() {
    String yaml = "server:\n" +
        "  port: 8080\n" +
        "  features:\n" +
        "    - ssl\n" +
        "    - compression\n";

    ConfigTree tree = parser.parse(yaml);

    Optional<ConfigNode> portNode = tree.getNodeByPath("server.port");
    assertTrue("Port node should be captured in Trie", portNode.isPresent());
    assertEquals("8080", portNode.get().getValue().orElse(""));
    assertEquals("Line offsets should map perfectly", 2, portNode.get().getStartLine());

    Optional<ConfigNode> sslNode = tree.getNodeByPath("server.features[0]");
    assertTrue("List indexing [0] should map correctly", sslNode.isPresent());
    assertEquals("ssl", sslNode.get().getValue().orElse(""));
    assertEquals("List indexing [1] should map correctly", 4, sslNode.get().getStartLine());

    Optional<ConfigNode> compNode = tree.getNodeByPath("server.features[1]");
    assertTrue(compNode.isPresent());
    assertEquals("compression", compNode.get().getValue().orElse(""));
    assertEquals(5, compNode.get().getStartLine());
  }

  @Test
  public void testTabHandling() {
    // Standard YAML doesn't allow tabs for indentation, but we must handle them
    // since users might have them.
    // Our TabToSpaceReader converts them to 2 spaces.
    String yamlWithTabs = "app:\n" +
        "\tname: my-app\n" +
        "\tversion: 1.0.0\n";

    ConfigTree tree = parser.parse(yamlWithTabs);
    Optional<ConfigNode> nameNode = tree.getNodeByPath("app.name");
    assertTrue(nameNode.isPresent());
    assertEquals("my-app", nameNode.get().getValue().orElse(""));
  }
}
