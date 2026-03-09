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
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * A high-performance JSON streaming parser utilizing Jackson's JsonParser API.
 * Adheres to the 100MB rule by processing tokens iteratively.
 */
public final class JsonStreamingParser {

	private static final JsonFactory FACTORY = new JsonFactory();

	/**
	 * Main entry point to stream parse JSON content.
	 *
	 * @param reader The raw JSON reader
	 * @return The populated ConfigTree.
	 */
	/**
	 * Main entry point to stream parse JSON content with controls.
	 */
	public ConfigTree parse(Reader reader) throws IOException {
		return parse(reader, null);
	}

	public ConfigTree parse(String jsonContent) throws IOException {
		return parse(jsonContent, null);
	}

	public ConfigTree parse(Reader reader, ParserControl control) throws IOException {
		PathTrie indexerTrie = new PathTrie();
		ConfigNode root = buildTreeStreaming(reader, indexerTrie, control);
		return new ConfigTree(root, indexerTrie);
	}

	public ConfigTree parse(String jsonContent, ParserControl control) throws IOException {
		return parse(new StringReader(jsonContent == null ? "" : jsonContent), control);
	}

	private ConfigNode buildTreeStreaming(Reader reader, PathTrie indexTrie, ParserControl control) throws IOException {
		try (JsonParser parser = FACTORY.createParser(reader)) {
			Stack<Frame> nestingStack = new Stack<>();
			Frame rootFrame = new Frame("root", "", 1, 0);
			nestingStack.push(rootFrame);

			String pendingKey = null;
			int pendingKeyLine = -1;
			int pendingKeyCol = -1;

			while (!parser.isClosed()) {
				// Interruptible Parser: check for cancellation
				if (control != null && control.isCancelled()) {
					return null; 
				}

				JsonToken token = parser.nextToken();
				if (token == null) {
					break;
				}

				Frame currentFrame = nestingStack.peek();
				int currentDepth = nestingStack.size();

				switch (token) {
					case FIELD_NAME -> {
						pendingKey = parser.getCurrentName();
						pendingKeyLine = (int) parser.getTokenLocation().getLineNr();
						pendingKeyCol = (int) parser.getTokenLocation().getColumnNr();
					}
					case START_OBJECT -> {
						String blockKey = getBlockKey(currentFrame, pendingKey);
						String blockPath = getBlockPath(currentFrame, blockKey);

						// Sampled Indexing: depth limiting
						if (control != null && currentDepth > control.getMaxDepth()) {
							parser.skipChildren(); // Skip deeper nodes
							pendingKey = null;
							continue;
						}

						Frame mapFrame = new Frame(blockKey, blockPath,
								(int) parser.getTokenLocation().getLineNr(),
								(int) parser.getTokenLocation().getColumnNr());
						mapFrame.isObject = true;
						nestingStack.push(mapFrame);

						if (currentFrame.isArray && pendingKey == null) {
							currentFrame.listIndex++;
						}
						pendingKey = null;
					}
					case START_ARRAY -> {
						String blockKey = getBlockKey(currentFrame, pendingKey);
						String blockPath = getBlockPath(currentFrame, blockKey);

						// Sampled Indexing: depth limiting
						if (control != null && currentDepth > control.getMaxDepth()) {
							parser.skipChildren(); // Skip deeper nodes
							pendingKey = null;
							continue;
						}

						Frame seqFrame = new Frame(blockKey, blockPath,
								(int) parser.getTokenLocation().getLineNr(),
								(int) parser.getTokenLocation().getColumnNr());
						seqFrame.isArray = true;
						nestingStack.push(seqFrame);

						if (currentFrame.isArray && pendingKey == null) {
							currentFrame.listIndex++;
						}
						pendingKey = null;
					}
					case VALUE_STRING, VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT, VALUE_TRUE, VALUE_FALSE, VALUE_NULL -> {
						if (currentFrame.isObject && pendingKey != null) {
							String nodePath = generatePath(currentFrame.path, pendingKey);
							ConfigNode scalarNode = new ConfigNode.Builder(pendingKey, nodePath)
									.value(parser.getText())
									.startLine(pendingKeyLine)
									.startColumn(pendingKeyCol)
									.endLine((int) parser.getTokenLocation().getLineNr())
									.endColumn((int) parser.getTokenLocation().getColumnNr() + (parser.getText() != null ? parser.getTextLength() : 0))
									.build();

							indexTrie.insert(scalarNode);
							currentFrame.children.add(scalarNode);
							pendingKey = null;
						} else if (currentFrame.isArray) {
							String arrKey = "[" + currentFrame.listIndex + "]";
							String nodePath = generatePath(currentFrame.path, arrKey);
							ConfigNode listItem = new ConfigNode.Builder(arrKey, nodePath)
									.value(parser.getText())
									.startLine((int) parser.getTokenLocation().getLineNr())
									.startColumn((int) parser.getTokenLocation().getColumnNr())
									.endLine((int) parser.getTokenLocation().getLineNr())
									.endColumn((int) parser.getTokenLocation().getColumnNr() + (parser.getText() != null ? parser.getTextLength() : 0))
									.build();

							currentFrame.listIndex++;
							indexTrie.insert(listItem);
							currentFrame.children.add(listItem);
						}
					}
					case END_OBJECT, END_ARRAY -> {
						Frame closingFrame = nestingStack.pop();
						closingFrame.endLine = (int) parser.getTokenLocation().getLineNr();
						closingFrame.endCol = (int) parser.getTokenLocation().getColumnNr();

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
					default -> {
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
	}

	private String getBlockKey(Frame currentFrame, String pendingKey) {
		return pendingKey != null ? pendingKey : (currentFrame.isArray ? "[" + currentFrame.listIndex + "]" : "root");
	}

	private String getBlockPath(Frame currentFrame, String blockKey) {
		if (currentFrame.path.isEmpty()) {
			return blockKey.equals("root") ? "" : blockKey;
		}
		return generatePath(currentFrame.path, blockKey);
	}

	private String generatePath(String parentPath, String currentKey) {
		if (parentPath == null || parentPath.isEmpty()) {
			return currentKey;
		}
		if (currentKey.startsWith("[")) {
			return parentPath + currentKey;
		}
		return parentPath + "." + currentKey;
	}

	private static class Frame {
		String key;
		String path;
		int startLine;
		int startCol;
		int endLine;
		int endCol;
		boolean isObject = false;
		boolean isArray = false;
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
