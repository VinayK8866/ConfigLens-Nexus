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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class ConfigTreeTest {

	private ConfigTree configTree;
	private PathTrie indexTrie;

	@Before
	public void setUp() {
		indexTrie = new PathTrie();

		ConfigNode nameNode = new ConfigNode.Builder("name", "metadata.name")
				.value("nginx")
				.startLine(3)
				.endLine(3)
				.build();

		ConfigNode metadataNode = new ConfigNode.Builder("metadata", "metadata")
				.startLine(2)
				.endLine(3)
				.children(Arrays.asList(nameNode))
				.build();

		ConfigNode rootNode = new ConfigNode.Builder("root", "root")
				.startLine(1)
				.endLine(4)
				.children(Arrays.asList(metadataNode))
				.build();

		indexTrie.insert(nameNode);
		indexTrie.insert(metadataNode);
		indexTrie.insert(rootNode);

		configTree = new ConfigTree(rootNode, indexTrie);
	}

	@Test
	public void testFindNodeByPath() {
		Optional<ConfigNode> node = configTree.getNodeByPath("metadata.name");
		assertTrue("Node should be found by exact path", node.isPresent());
		assertEquals("nginx", node.get().getValue().orElse(""));
	}

	@Test
	public void testFindNodeAtLine() {
		Optional<ConfigNode> foundLine3 = configTree.findNodeAtLine(3);
		assertTrue(foundLine3.isPresent());
		assertEquals("name", foundLine3.get().getKey());
		assertEquals("metadata.name", foundLine3.get().getPath());

		// Clicking exactly on parent map key 'metadata:' line
		Optional<ConfigNode> foundLine2 = configTree.findNodeAtLine(2);
		assertTrue(foundLine2.isPresent());
		assertEquals("metadata", foundLine2.get().getKey());
	}
}
