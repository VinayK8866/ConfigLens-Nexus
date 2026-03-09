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
import java.io.IOException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class JsonStreamingParserTest {

	private JsonStreamingParser parser;

	@Before
	public void setUp() {
		parser = new JsonStreamingParser();
	}

	@Test
	public void testJsonParsingAndOffsets() throws IOException {
		String json = "{\n" +
				"  \"server\": {\n" +
				"    \"port\": 8080,\n" +
				"    \"features\": [\n" +
				"      \"ssl\",\n" +
				"      \"compression\"\n" +
				"    ]\n" +
				"  }\n" +
				"}";

		ConfigTree tree = parser.parse(json);

		Optional<ConfigNode> portNode = tree.getNodeByPath("server.port");
		assertTrue("Port node should be captured in Trie", portNode.isPresent());
		assertEquals("8080", portNode.get().getValue().orElse(""));
		assertEquals("Line offsets should map perfectly", 3, portNode.get().getStartLine());

		Optional<ConfigNode> sslNode = tree.getNodeByPath("server.features[0]");
		assertTrue("List indexing [0] should map correctly", sslNode.isPresent());
		assertEquals("ssl", sslNode.get().getValue().orElse(""));
		assertEquals(5, sslNode.get().getStartLine());

		Optional<ConfigNode> compNode = tree.getNodeByPath("server.features[1]");
		assertTrue(compNode.isPresent());
		assertEquals("compression", compNode.get().getValue().orElse(""));
		assertEquals(6, compNode.get().getStartLine());
	}

	@Test
	public void testLargeJsonMemoryEfficiency() throws IOException {
		// Simulate a stream that would be large if loaded into DOM
		// We'll just check that it parses a repeated structure correctly
		StringBuilder sb = new StringBuilder();
		sb.append("{\"items\": [");
		for (int i = 0; i < 1000; i++) {
			sb.append("{\"id\": ").append(i).append(", \"val\": \"some large text repeating...\"}");
			if (i < 999)
				sb.append(",");
		}
		sb.append("]}");

		ConfigTree tree = parser.parse(sb.toString());
		Optional<ConfigNode> lastItemNode = tree.getNodeByPath("items[999].id");
		assertTrue(lastItemNode.isPresent());
		assertEquals("999", lastItemNode.get().getValue().orElse(""));
	}
}
