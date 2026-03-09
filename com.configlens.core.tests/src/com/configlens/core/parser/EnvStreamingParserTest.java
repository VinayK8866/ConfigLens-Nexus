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

public class EnvStreamingParserTest {

	private EnvStreamingParser parser;

	@Before
	public void setUp() {
		parser = new EnvStreamingParser();
	}

	@Test
	public void testEnvParsingAndOffsets() throws java.io.IOException {
		String env = "DEBUG=true\n" +
				"DATABASE_URL=\"postgres://user:pass@localhost:5432/db\"\n" +
				"# Comment line\n" +
				"API_KEY=1234567890\n";

		ConfigTree tree = parser.parse(env);

		Optional<ConfigNode> debugNode = tree.getNodeByPath("DEBUG");
		assertTrue("DEBUG node should be captured", debugNode.isPresent());
		assertEquals("true", debugNode.get().getValue().orElse(""));
		assertEquals(1, debugNode.get().getStartLine());

		Optional<ConfigNode> dbNode = tree.getNodeByPath("DATABASE_URL");
		assertTrue(dbNode.isPresent());
		assertEquals("postgres://user:pass@localhost:5432/db", dbNode.get().getValue().orElse(""));
		assertEquals(2, dbNode.get().getStartLine());

		Optional<ConfigNode> apiKeyNode = tree.getNodeByPath("API_KEY");
		assertTrue(apiKeyNode.isPresent());
		assertEquals("1234567890", apiKeyNode.get().getValue().orElse(""));
		assertEquals(4, apiKeyNode.get().getStartLine());
	}
}
