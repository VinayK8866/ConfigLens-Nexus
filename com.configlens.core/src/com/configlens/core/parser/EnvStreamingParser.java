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
import com.configlens.core.model.PathTrie;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A streaming parser for .env files.
 * Processes files line-by-line to maintain minimal memory footprint (100MB
 * rule).
 */
public final class EnvStreamingParser {

	/**
	 * Parses a .env source into a ConfigTree.
	 *
	 * @param reader The raw .env reader
	 * @return The populated ConfigTree.
	 */
	public ConfigTree parse(Reader reader) throws IOException {
		return parse(reader, null);
	}

	public ConfigTree parse(String content) throws IOException {
		return parse(content, null);
	}

	public ConfigTree parse(Reader reader, ParserControl control) throws IOException {
		PathTrie indexerTrie = new PathTrie();
		ConfigNode root = buildTreeStreaming(reader, indexerTrie, control);
		return new ConfigTree(root, indexerTrie);
	}

	public ConfigTree parse(String content, ParserControl control) throws IOException {
		return parse(new StringReader(content == null ? "" : content), control);
	}

	private ConfigNode buildTreeStreaming(Reader reader, PathTrie indexTrie, ParserControl control) throws IOException {
		List<ConfigNode> children = new ArrayList<>();
		BufferedReader bufferedReader = new BufferedReader(reader);
		String line;
		int lineNumber = 0;

		while ((line = bufferedReader.readLine()) != null) {
			// Interruptible Parser: check for cancellation
			if (control != null && control.isCancelled()) {
				return null;
			}
			lineNumber++;
			String trimmed = line.trim();

			// Skip comments and empty lines
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}

			int eqIndex = trimmed.indexOf('=');
			if (eqIndex > 0) {
				String key = trimmed.substring(0, eqIndex).trim();
				String value = trimmed.substring(eqIndex + 1).trim();

				// Handle quotes if present
				if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
					value = value.substring(1, value.length() - 1);
				} else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
					value = value.substring(1, value.length() - 1);
				}

				ConfigNode node = new ConfigNode.Builder(key, key)
						.value(value)
						.startLine(lineNumber)
						.startColumn(line.indexOf(key))
						.endLine(lineNumber)
						.endColumn(line.length())
						.build();

				indexTrie.insert(node);
				children.add(node);
			}
		}

		ConfigNode root = new ConfigNode.Builder("root", "")
				.startLine(0)
				.startColumn(0)
				.endLine(lineNumber)
				.children(children)
				.build();

		indexTrie.insert(root);
		return root;
	}
}
