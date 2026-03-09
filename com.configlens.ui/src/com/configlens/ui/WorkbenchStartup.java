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
    // Schedule startup logic in a non-blocking Job to prevent Eclipse hangs
    Job startupJob = new Job("ConfigLens: Startup Onboarding") {
      @Override
      protected IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
        Display.getDefault().asyncExec(() -> {
          IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
          if (window != null) {
            window.getPartService().addPartListener(new EditorLifecycleManager());
            
            // Show Welcome Page on first launch with a generous delay
            org.eclipse.jface.preference.IPreferenceStore store = Activator.getDefault().getPreferenceStore();
            if (!store.getBoolean("WELCOME_PAGE_SHOWN")) {
              showWelcomePageAfterDelay();
              store.setValue("WELCOME_PAGE_SHOWN", true);
            }
          }
        });
        return Status.OK_STATUS;
      }
    };
    startupJob.setSystem(true);
    startupJob.schedule(2000); // 2 second delay to let Eclipse initialize
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
        org.eclipse.ui.PlatformUI.getWorkbench().getBrowserSupport().createBrowser("configlens.welcome").openURL(
            org.eclipse.core.runtime.FileLocator.toFileURL(url));
      }
    } catch (Exception e) {
      // Ignore if browser cannot be opened
    }
  }
}
