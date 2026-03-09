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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable hierarchical representation of a configuration element. Memory
 * efficient design to accommodate massive JSON/YAML files. Avoids holding raw
 * tree structure values entirely unless scalar.
 */
public final class ConfigNode {

	private final String key;
	private final Object value;
	private final String path; // Pre-calculated dot-notation path, e.g., "server.port"
	private final int startLine;
	private final int startColumn;
	private final int endLine;
	private final int endColumn;
	private final List<ConfigNode> children;

	private ConfigNode(Builder builder) {
		this.key = Objects.requireNonNull(builder.key, "Key cannot be null");
		this.value = builder.value;
		this.path = Objects.requireNonNull(builder.path, "Path cannot be null");
		this.startLine = builder.startLine;
		this.startColumn = builder.startColumn;
		this.endLine = builder.endLine;
		this.endColumn = builder.endColumn;
		this.children = builder.children == null
				? Collections.emptyList()
				: Collections.unmodifiableList(builder.children);
	}

	public String getKey() {
		return key;
	}

	public Optional<Object> getValue() {
		return Optional.ofNullable(value);
	}

	public String getPath() {
		return path;
	}

	public int getStartLine() {
		return startLine;
	}

	public int getStartColumn() {
		return startColumn;
	}

	public int getEndLine() {
		return endLine;
	}

	public int getEndColumn() {
		return endColumn;
	}

	public List<ConfigNode> getChildren() {
		return children;
	}

	public boolean containsLine(int line) {
		return line >= startLine && line <= endLine;
	}

	/** Builder for ConfigNode. */
	public static final class Builder {
		private String key;
		private Object value;
		private String path;
		private int startLine;
		private int startColumn;
		private int endLine;
		private int endColumn;
		private List<ConfigNode> children;

		public Builder(String key, String path) {
			this.key = key;
			this.path = path;
		}

		public Builder value(Object value) {
			this.value = value;
			return this;
		}

		public Builder startLine(int startLine) {
			this.startLine = startLine;
			return this;
		}

		public Builder startColumn(int startColumn) {
			this.startColumn = startColumn;
			return this;
		}

		public Builder endLine(int endLine) {
			this.endLine = endLine;
			return this;
		}

		public Builder endColumn(int endColumn) {
			this.endColumn = endColumn;
			return this;
		}

		public Builder children(List<ConfigNode> children) {
			this.children = children;
			return this;
		}

		public ConfigNode build() {
			return new ConfigNode(this);
		}
	}

	@Override
	public String toString() {
		return "ConfigNode{" + "key='" + key + '\'' + ", path='" + path + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ConfigNode that = (ConfigNode) o;
		return startLine == that.startLine
				&& startColumn == that.startColumn
				&& endLine == that.endLine
				&& endColumn == that.endColumn
				&& key.equals(that.key)
				&& Objects.equals(value, that.value)
				&& path.equals(that.path)
				&& children.equals(that.children);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value, path, startLine, startColumn, endLine, endColumn, children);
	}
}
