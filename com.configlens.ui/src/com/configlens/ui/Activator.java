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

import com.configlens.ui.editor.EnvCacheProjectListener;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "com.configlens.ui";
  private static Activator plugin;
  private EnvCacheProjectListener envListener;

  public Activator() {}

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    
    // Set default preferences
    org.eclipse.jface.preference.IPreferenceStore store = getPreferenceStore();
    store.setDefault(com.configlens.ui.preferences.ConfigLensPreferencePage.YAML_TABS_TO_SPACES, true);
    store.setDefault(com.configlens.ui.preferences.ConfigLensPreferencePage.YAML_INDENT_SIZE, 2);

    // Start Workspace Environment Monitoring
    envListener = new EnvCacheProjectListener();
    envListener.start();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    if (envListener != null) {
      envListener.stop();
      envListener = null;
    }
    plugin = null;
    super.stop(context);
  }

  public static Activator getDefault() {
    return plugin;
  }
}
