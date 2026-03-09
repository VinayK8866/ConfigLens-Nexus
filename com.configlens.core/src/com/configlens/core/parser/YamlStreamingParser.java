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
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.events.Event;
import org.snakeyaml.engine.v2.events.MappingEndEvent;
import org.snakeyaml.engine.v2.events.MappingStartEvent;
import org.snakeyaml.engine.v2.events.ScalarEvent;
import org.snakeyaml.engine.v2.events.SequenceEndEvent;
import org.snakeyaml.engine.v2.events.SequenceStartEvent;
import org.snakeyaml.engine.v2.scanner.StreamReader;
import org.snakeyaml.engine.v2.parser.Parser;
import org.snakeyaml.engine.v2.parser.ParserImpl;

/**
 * A purely event-driven streaming parser for YAML files using SnakeYAML-Engine.
 * Designed to strictly adhere to the "100MB Rule" by processing nodes
 * iteratively
 * and preventing any internal DOM aggregation that consumes excess memory.
 */
public final class YamlStreamingParser {

	// Settings are reused, preventing recreating parsers configuration for speed.
	private static final LoadSettings SETTINGS = LoadSettings.builder().build();

	/**
	 * Main entry point to stream parse a YAML source without heavy DOM-loading.
	 *
	 * @param reader The raw YAML reader
	 * @return The populated, immutable ConfigTree linked to a PathTrie.
	 */
	public ConfigTree parse(Reader reader) {
		return parse(reader, null);
	}

	public ConfigTree parse(String yamlContent) {
		return parse(yamlContent, null);
	}

	public ConfigTree parse(Reader reader, ParserControl control) {
		Reader tabFreeReader = new TabToSpaceReader(reader);
		Parser parser = new ParserImpl(SETTINGS, new StreamReader(SETTINGS, tabFreeReader));
		PathTrie indexerTrie = new PathTrie();

		ConfigNode root = buildTreeStreaming(parser, indexerTrie, control);

		return new ConfigTree(root, indexerTrie);
	}

	public ConfigTree parse(String yamlContent, ParserControl control) {
		return parse(new StringReader(yamlContent == null ? "" : yamlContent), control);
	}

	private ConfigNode buildTreeStreaming(Parser parser, PathTrie indexTrie, ParserControl control) {
		Stack<Frame> nestingStack = new Stack<>();
		// Abstract root wrapper container to handle multiple documents or base keys
		// uniformly.
		Frame rootFrame = new Frame("root", "", 0, 0);
		nestingStack.push(rootFrame);

		String pendingKey = null;
		int pendingKeyStartLine = -1;
		int pendingKeyStartCol = -1;

		while (parser.hasNext()) {
			// Interruptible Parser: check for cancellation
			if (control != null && control.isCancelled()) {
				return null;
			}

			Event event = parser.next();
			int currentDepth = nestingStack.size();

			// Sampled Indexing: depth limiting
			if (control != null && currentDepth > control.getMaxDepth()) {
				// For YAML we just continue until we pop back up, 
				// though skipping is harder without a dedicated 'skip' method.
				if (event instanceof MappingEndEvent || event instanceof SequenceEndEvent) {
					nestingStack.pop();
				} else if (event instanceof MappingStartEvent || event instanceof SequenceStartEvent) {
					// Dummy push to keep track of depth if we were to support deep skipping
					// For now we just don't index.
				}
				continue;
			}

			if (event instanceof ScalarEvent) {
				ScalarEvent scalar = (ScalarEvent) event;
				Frame currentFrame = nestingStack.peek();

				if (currentFrame.isMapping) {
					if (pendingKey == null) {
						// It's a Key
						pendingKey = scalar.getValue();
						pendingKeyStartLine = scalar.getStartMark().map(m -> m.getLine() + 1).orElse(0);
						pendingKeyStartCol = scalar.getStartMark().map(m -> m.getColumn()).orElse(0);
					} else {
						// It's a Value mapped to the prior Key
						String nodePath = generatePath(currentFrame.path, pendingKey);
						ConfigNode scalarNode = new ConfigNode.Builder(pendingKey, nodePath)
								.value(scalar.getValue())
								.startLine(scalar.getStartMark().map(m -> m.getLine() + 1).orElse(0))
								.startColumn(scalar.getStartMark().map(m -> m.getColumn()).orElse(0))
								.endLine(scalar.getEndMark().map(m -> m.getLine() + 1).orElse(0))
								.endColumn(scalar.getEndMark().map(m -> m.getColumn()).orElse(0))
								.build();

						indexTrie.insert(scalarNode);
						currentFrame.children.add(scalarNode);
						pendingKey = null;
					}
				} else if (currentFrame.isSequence) {
					// List Item logic - e.g. path like `containers[0]`
					String arrKey = "[" + currentFrame.listIndex + "]";
					String nodePath = generatePath(currentFrame.path, arrKey);
					ConfigNode listItem = new ConfigNode.Builder(arrKey, nodePath)
							.value(scalar.getValue())
							.startLine(scalar.getStartMark().map(m -> m.getLine() + 1).orElse(0))
							.startColumn(scalar.getStartMark().map(m -> m.getColumn()).orElse(0))
							.endLine(scalar.getEndMark().map(m -> m.getLine() + 1).orElse(0))
							.endColumn(scalar.getEndMark().map(m -> m.getColumn()).orElse(0))
							.build();

					currentFrame.listIndex++;
					indexTrie.insert(listItem);
					currentFrame.children.add(listItem);
				}

			} else if (event instanceof MappingStartEvent) {
				Frame currentFrame = nestingStack.peek();
				String blockKey = pendingKey != null ? pendingKey
						: (currentFrame.isSequence ? "[" + currentFrame.listIndex + "]" : "root");
				String blockPath = pendingKey != null ? generatePath(currentFrame.path, blockKey)
						: (currentFrame.isSequence ? generatePath(currentFrame.path, blockKey) : "");

				Frame mapFrame = new Frame(blockKey, blockPath,
						event.getStartMark().map(m -> m.getLine() + 1).orElse(0),
						event.getStartMark().map(m -> m.getColumn()).orElse(0));

				mapFrame.isMapping = true;
				nestingStack.push(mapFrame);

				if (currentFrame.isSequence && pendingKey == null) {
					currentFrame.listIndex++;
				}
				pendingKey = null;

			} else if (event instanceof SequenceStartEvent) {
				Frame currentFrame = nestingStack.peek();
				String blockKey = pendingKey != null ? pendingKey
						: (currentFrame.isSequence ? "[" + currentFrame.listIndex + "]" : "root");
				String blockPath = pendingKey != null ? generatePath(currentFrame.path, blockKey)
						: (currentFrame.isSequence ? generatePath(currentFrame.path, blockKey) : "");

				Frame seqFrame = new Frame(blockKey, blockPath,
						event.getStartMark().map(m -> m.getLine() + 1).orElse(0),
						event.getStartMark().map(m -> m.getColumn()).orElse(0));

				seqFrame.isSequence = true;
				nestingStack.push(seqFrame);

				if (currentFrame.isSequence && pendingKey == null) {
					currentFrame.listIndex++;
				}
				pendingKey = null;

			} else if (event instanceof MappingEndEvent || event instanceof SequenceEndEvent) {
				Frame closingFrame = nestingStack.pop();
				closingFrame.endLine = event.getEndMark().map(m -> m.getLine() + 1).orElse(0);
				closingFrame.endCol = event.getEndMark().map(m -> m.getColumn()).orElse(0);

				ConfigNode objectNode = new ConfigNode.Builder(closingFrame.key, closingFrame.path)
						.startLine(closingFrame.startLine)
						.startColumn(closingFrame.startCol)
						.endLine(closingFrame.endLine)
						.endColumn(closingFrame.endCol)
						.children(closingFrame.children)
						.build();

				indexTrie.insert(objectNode);

				if (!nestingStack.isEmpty()) {
					nestingStack.peek().children.add(objectNode);
				}
			}
		}

		Frame finalRoot = nestingStack.isEmpty() ? rootFrame : nestingStack.pop();
		ConfigNode generatedRoot = new ConfigNode.Builder("root", "")
				.startLine(0)
				.startColumn(0)
				.endLine(Integer.MAX_VALUE)
				.children(finalRoot.children)
				.build();

		indexTrie.insert(generatedRoot);
		return generatedRoot;
	}

	private String generatePath(String parentPath, String currentKey) {
		if (parentPath == null || parentPath.isEmpty()) {
			return currentKey;
		}
		// If currentKey is an array index like [0], we might want to avoid the dot
		// however the Trie split logic currently uses dots.
		// For standard dot-notation we use dots even for indices if needed or just
		// append.
		if (currentKey.startsWith("[")) {
			return parentPath + currentKey;
		}
		return parentPath + "." + currentKey;
	}

	/** Internal structure used purely to maintain context within iteration */
	private static class Frame {
		String key;
		String path;
		int startLine;
		int startCol;
		int endLine;
		int endCol;
		boolean isMapping = false;
		boolean isSequence = false;
		int listIndex = 0;
		List<ConfigNode> children = new ArrayList<>();

		Frame(String key, String path, int startLine, int startCol) {
			this.key = key;
			this.path = path;
			this.startLine = startLine;
			this.startCol = startCol;
		}
	}
}
