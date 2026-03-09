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

import java.io.IOException;
import java.io.FilterReader;
import java.io.Reader;

/**
 * A specialized reader that replaces tab characters with spaces on the fly.
 * This ensures YAML structure is preserved without loading the entire 100MB
 * file
 * into a single String for replacement.
 */
public final class TabToSpaceReader extends FilterReader {

	private static final int SPACES_PER_TAB = 2;
	private int pendingSpaces = 0;

	public TabToSpaceReader(Reader in) {
		super(in);
	}

	@Override
	public int read() throws IOException {
		if (pendingSpaces > 0) {
			pendingSpaces--;
			return ' ';
		}

		int c = super.read();
		if (c == '\t') {
			pendingSpaces = SPACES_PER_TAB - 1;
			return ' ';
		}
		return c;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (len <= 0)
			return 0;

		int c = read();
		if (c == -1)
			return -1;

		cbuf[off] = (char) c;
		int count = 1;

		while (count < len) {
			// For performance, we could try to read more, but we must respect the
			// SPACES_PER_TAB logic
			int next = read();
			if (next == -1)
				break;
			cbuf[off + count] = (char) next;
			count++;
		}
		return count;
	}
}
