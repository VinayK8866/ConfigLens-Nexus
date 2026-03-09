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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents the root wrapper of an entire configuration document (YAML/JSON),
 * providing O(log n)
 * Path resolution and Line-Offset-to-Node tracking.
 */
public final class ConfigTree {

	private final ConfigNode rootNode;
	private final PathTrie indexTrie;

	/**
	 * Initializes the ConfigTree securely. Data structures are intended to be
	 * generated iteratively by
	 * streaming parsers (like SnakeYaml-Engine Event API).
	 *
	 * @param rootNode  the immutable top-level node encompassing the whole config
	 *                  file.
	 * @param indexTrie the parallel indexed Trie allowing absolute path lookups.
	 */
	public ConfigTree(ConfigNode rootNode, PathTrie indexTrie) {
		this.rootNode = rootNode;
		this.indexTrie = indexTrie;
	}

	public ConfigNode getRootNode() {
		return rootNode;
	}

	/**
	 * Retrieves a specific path instantly using the internal Trie index. Time
	 * complexity is governed
	 * by path size O(pathSegments).
	 *
	 * @param path dot-notation path like server.port
	 * @return The node, if indexed.
	 */
	public Optional<ConfigNode> getNodeByPath(String path) {
		if (indexTrie == null) {
			return Optional.empty();
		}
		return indexTrie.findByPath(path);
	}

	/**
	 * Performs a depth-first search to find the narrowest intersecting node
	 * touching the provided Line.
	 * Time complexity is worst-case O(N) where N is number of nodes, but
	 * practically heavily pruned by line bounds.
	 *
	 * @param line 1-indexed target line (used by Editors to resolve caret position
	 *             to Breadcrumb)
	 * @return Optional node enclosing the requested line.
	 */
	public Optional<ConfigNode> findNodeAtLine(int line) {
		if (rootNode == null || !rootNode.containsLine(line)) {
			return Optional.empty();
		}
		return Optional.of(searchLine(rootNode, line));
	}

	private ConfigNode searchLine(ConfigNode node, int line) {
		if (node.getChildren() == null || node.getChildren().isEmpty()) {
			return node;
		}

		for (ConfigNode child : node.getChildren()) {
			if (child.containsLine(line)) {
				return searchLine(child, line);
			}
		}
		// If no children contain it strictly but the parent does (e.g., clicking on a
		// map key line)
		return node;
	}

	/**
	 * Returns a flattened list of all leaf nodes (scalar values) in the tree.
	 * Decoupled from UI to remain LSP-ready.
	 */
	public List<ConfigNode> getFlattenedNodes() {
		List<ConfigNode> result = new ArrayList<>();
		collectScalars(rootNode, result);
		return result;
	}

	private void collectScalars(ConfigNode node, List<ConfigNode> result) {
		if (node == null)
			return;
		if (node.getValue().isPresent()) {
			result.add(node);
		}
		for (ConfigNode child : node.getChildren()) {
			collectScalars(child, result);
		}
	}
}
