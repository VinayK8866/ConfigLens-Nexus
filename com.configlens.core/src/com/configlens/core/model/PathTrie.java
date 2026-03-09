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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A memory-efficient Trie implementation designed to map dot-notation
 * hierarchical paths to their
 * corresponding ConfigNode representation in O(log n) time relative to the path
 * depth.
 */
public final class PathTrie {

	private final TrieNode root;

	public PathTrie() {
		this.root = new TrieNode();
	}

	/**
	 * Inserts a regular ConfigNode into the Trie based on its pre-calculated dot
	 * notation path.
	 *
	 * @param node the ConfigNode to index.
	 */
	public void insert(ConfigNode node) {
		if (node == null || node.getPath() == null || node.getPath().isEmpty()) {
			return;
		}

		String[] pathSegments = splitPath(node.getPath());
		TrieNode current = root;

		for (String segment : pathSegments) {
			current = current.children.computeIfAbsent(segment, k -> new TrieNode());
		}
		current.node = node;
	}

	/**
	 * Attempts to retrieve a node using an exact dot-notation path, e.g.,
	 * `spec.containers[0].name`.
	 *
	 * @param path The dot-notation path
	 * @return Optional containing the node if it exists
	 */
	public Optional<ConfigNode> findByPath(String path) {
		if (path == null || path.isEmpty()) {
			return Optional.empty();
		}

		String[] pathSegments = splitPath(path);
		TrieNode current = root;

		for (String segment : pathSegments) {
			current = current.children.get(segment);
			if (current == null) {
				return Optional.empty();
			}
		}

		return Optional.ofNullable(current.node);
	}

	/**
	 * Performs an efficient split handling dot notation separating indices
	 * correctly.
	 * Note: Doesn't handle complex escaped dots in keys for this proof of concept
	 * but
	 * handles typical array brackets like `containers[0]`.
	 */
	private String[] splitPath(String path) {
		// Basic dot separation. If keys have dots, they'd need quoting support here
		return path.split("\\.");
	}

	/** Internal Trie Node structure. */
	private static final class TrieNode {
		final Map<String, TrieNode> children = new HashMap<>(); // Adjust sizes for memory optimization
		ConfigNode node;
	}
}
