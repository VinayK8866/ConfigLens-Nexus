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
package com.configlens.ui;

import com.configlens.ui.editor.EditorLifecycleManager;
import com.configlens.ui.editor.EnvCacheProjectListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Early startup class to register editor tracking and global listeners.
 */
public final class WorkbenchStartup implements IStartup {

  @Override
  public void earlyStartup() {
    // Schedule startup logic in a non-blocking Job to ensure Eclipse is responsive
    Job startupJob = new Job("ConfigLens: Global Initializer") {
      @Override
      protected IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
        Display.getDefault().asyncExec(() -> {
          try {
            EditorLifecycleManager manager = new EditorLifecycleManager();
            
            // 1. Hook into future editor events
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
              window.getPartService().addPartListener(manager);
            }

            // 2. ACTIVATE INSTANTLY: Inject into ALL editors already open
            // This is critical for post-installation UX so features work without tab-switching.
            manager.initializeExistingEditors();

            // 3. Show Welcome Page for this version
            org.eclipse.jface.preference.IPreferenceStore store = Activator.getDefault().getPreferenceStore();
            String versionKey = "WELCOME_PAGE_SHOWN_1.0.4";
            if (!store.getBoolean(versionKey)) {
              store.setValue(versionKey, true);
              showWelcomePageAfterDelay();
            }
          } catch (Exception e) {
            // Log if startup hooks failed, but don't crash Eclipse
          }
        });
        return Status.OK_STATUS;
      }
    };
    startupJob.setSystem(true);
    startupJob.schedule(1500); // 1.5s delay is optimal to avoid race conditions with workbench startup
  }

  private void showWelcomePageAfterDelay() {
    Job welcomeJob = new Job("ConfigLens: Showing Welcome Page") {
      @Override
      protected IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
        Display.getDefault().asyncExec(() -> {
          showWelcomePage();
        });
        return Status.OK_STATUS;
      }
    };
    welcomeJob.setSystem(true);
    welcomeJob.schedule(5000); // 5 second delay to ensure UI is ready
  }

  private void showWelcomePage() {
    try {
      java.net.URL url = org.eclipse.core.runtime.FileLocator.find(
          org.osgi.framework.FrameworkUtil.getBundle(WorkbenchStartup.class),
          new org.eclipse.core.runtime.Path("intro/WelcomePage.html"), null);
      if (url != null) {
        org.eclipse.ui.browser.IWorkbenchBrowserSupport support = org.eclipse.ui.PlatformUI.getWorkbench().getBrowserSupport();
        support.createBrowser(
            org.eclipse.ui.browser.IWorkbenchBrowserSupport.AS_EDITOR, 
            "configlens.welcome", 
            "ConfigLens Welcome", 
            "Getting started with ConfigLens"
        ).openURL(org.eclipse.core.runtime.FileLocator.toFileURL(url));
      }
    } catch (Exception e) {
      // Ignore if browser cannot be opened
    }
  }
}
