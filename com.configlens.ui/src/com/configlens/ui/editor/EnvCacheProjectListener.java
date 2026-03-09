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

import com.configlens.core.parser.WorkspaceEnvScanner;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Monitors the Eclipse workspace for .env file changes and triggers background scans.
 * Bridges the Eclipse Resource API with the pure-Java WorkspaceEnvScanner.
 */
public final class EnvCacheProjectListener implements IResourceChangeListener {

  private final WorkspaceEnvScanner scanner = new WorkspaceEnvScanner();

  /**
   * Starts the listener and performs an initial workspace-wide scan.
   */
  public void start() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
    scanAllProjects();
  }

  public void stop() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }

  private void scanAllProjects() {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (project.isOpen()) {
        scheduleScan(project);
      }
    }
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    IResourceDelta delta = event.getDelta();
    if (delta == null) return;

    try {
      delta.accept(d -> {
        IResource resource = d.getResource();
        if (resource instanceof IProject project && (d.getKind() == IResourceDelta.ADDED || d.getFlags() == IResourceDelta.OPEN)) {
          scheduleScan(project);
        } else if (resource.getName().equals(".env")) {
          scheduleScan(resource.getProject());
        }
        return true;
      });
    } catch (Exception e) {
      org.osgi.framework.Bundle bundle = org.osgi.framework.FrameworkUtil.getBundle(EnvCacheProjectListener.class);
      if (bundle != null) {
        org.eclipse.core.runtime.Platform.getLog(bundle).log(
            new org.eclipse.core.runtime.Status(
                org.eclipse.core.runtime.IStatus.ERROR,
                bundle.getSymbolicName(),
                "Error processing resource change",
                e));
      }    }
  }

  private void scheduleScan(IProject project) {
    if (project == null || !project.isOpen()) return;

    Job scanJob = new Job("ConfigLens: Scanning " + project.getName() + " for .env") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        if (project.getLocation() != null) {
          scanner.scanProject(project.getName(), project.getLocation().toFile().toPath());
        }
        return Status.OK_STATUS;
      }
    };
    scanJob.setPriority(Job.DECORATE);
    scanJob.setSystem(true);
    scanJob.schedule();
  }
}
