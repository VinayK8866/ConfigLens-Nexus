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

import com.configlens.core.model.ConfigNode;
import com.configlens.core.model.ConfigTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

/**
 * Handles background calculation of the breadcrumb path based on editor cursor
 * position.
 * Ensures the UI thread remains responsive even for massive 100MB configuration
 * files.
 */
public final class BreadcrumbManager {

	private final BreadcrumbComposite breadcrumbUI;
	private ConfigTree currentTree;
	private UpdateJob currentJob;

	public BreadcrumbManager(BreadcrumbComposite breadcrumbUI) {
		this.breadcrumbUI = breadcrumbUI;
	}

	public void updateTree(ConfigTree tree) {
		this.currentTree = tree;
	}

	/**
	 * Triggers a path update for the given line.
	 *
	 * @param line 1-indexed line number from the editor.
	 */
	public void updateForLine(int line) {
		if (currentTree == null)
			return;

		if (currentJob != null) {
			currentJob.cancel();
		}

		currentJob = new UpdateJob(line);
		currentJob.schedule();
	}

	private class UpdateJob extends Job {
		private final int line;

		public UpdateJob(int line) {
			super("ConfigLens: Updating Breadcrumb");
			this.line = line;
			setSystem(true); // Don't show in progress view
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;

			Optional<ConfigNode> nodeAtLine = currentTree.findNodeAtLine(line);
			List<ConfigNode> path = new ArrayList<>();

			nodeAtLine.ifPresent(node -> {
				ConfigNode current = node;
				// The tree lookup findNodeAtLine already finds the deep node.
				// To build the path, we might need parent references in ConfigNode.
				// Since ConfigNode presently only has children, we need a way to build path.
				// For now, let's assume we can resolve it or the Tree provides it.

				// Refactoring thought: If ConfigNode is immutable and doesn't have parent
				// pointers,
				// we can find the path during the search.

				// Let's implement a path search in ConfigTree or mock the path for now.
				// Actually, I will add a method to ConfigTree to return the full path list.
				path.addAll(calculatePath(line));
			});

			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;

			Display.getDefault().asyncExec(() -> {
				if (!breadcrumbUI.isDisposed()) {
					breadcrumbUI.setPath(path);
				}
			});

			return Status.OK_STATUS;
		}

		private List<ConfigNode> calculatePath(int line) {
			List<ConfigNode> path = new ArrayList<>();
			findPath(currentTree.getRootNode(), line, path);
			return path;
		}

		private boolean findPath(ConfigNode current, int line, List<ConfigNode> path) {
			if (current.containsLine(line)) {
				path.add(current);
				for (ConfigNode child : current.getChildren()) {
					if (findPath(child, line, path)) {
						return true;
					}
				}
				return true;
			}
			return false;
		}
	}
}
