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

/**
 * Represents a detected secret in a configuration file.
 * Decoupled from UI markers to remain LSP-ready.
 */
public record SecretResult(
    String type,
    String value,
    int lineNumber,
    int startColumn,
    int endColumn,
    String message
) {}
