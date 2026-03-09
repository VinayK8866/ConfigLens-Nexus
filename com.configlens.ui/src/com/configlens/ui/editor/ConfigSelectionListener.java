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
package com.configlens.ui.editor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Listens for selection changes in the active editor and triggers breadcrumb
 * updates.
 * Connected only to supported configuration editors.
 */
public final class ConfigSelectionListener implements ISelectionListener {

	private final BreadcrumbManager breadcrumbManager;
	private final ITextEditor editor;

	public ConfigSelectionListener(ITextEditor editor, BreadcrumbManager manager) {
		this.editor = editor;
		this.breadcrumbManager = manager;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// Only process if the selection happened in our assigned editor
		if (part == editor && selection instanceof ITextSelection textSelection) {
			int line = textSelection.getStartLine() + 1; // 1-indexed
			breadcrumbManager.updateForLine(line);
		}
	}
}
