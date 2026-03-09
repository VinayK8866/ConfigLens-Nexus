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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Utility to safely inject the Breadcrumb composite into an Eclipse Text
 * Editor's layout.
 * Ensures that the editor's main control is shifted to accommodate the header.
 */
public final class BreadcrumbInjector {

	/**
	 * Injects a BreadcrumbComposite into the given editor.
	 *
	 * @param editor The target text editor.
	 * @return The created BreadcrumbComposite or null if injection failed.
	 */
	public static BreadcrumbComposite inject(ITextEditor editor) {
		Control control = (Control) editor.getAdapter(Control.class);
		if (!(control instanceof Composite editorComposite)) {
			return null;
		}

		Composite parent = editorComposite.getParent();

		// Check if we already injected a breadcrumb to avoid duplicates
		for (Control child : parent.getChildren()) {
			if (child instanceof BreadcrumbComposite) {
				return (BreadcrumbComposite) child;
			}
		}

		// Standardize parent layout to GridLayout to support header/body separation
		if (!(parent.getLayout() instanceof GridLayout)) {
			GridLayout layout = new GridLayout(1, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.verticalSpacing = 0;
			parent.setLayout(layout);
		}

		// Create breadcrumb at the top
		BreadcrumbComposite breadcrumb = new BreadcrumbComposite(parent, SWT.NONE);
		breadcrumb.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		// Ensure it's visually at the top
		breadcrumb.moveAbove(editorComposite);

		// Editor body must fill the remaining space
		editorComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		parent.layout(true);
		return breadcrumb;
	}
}
