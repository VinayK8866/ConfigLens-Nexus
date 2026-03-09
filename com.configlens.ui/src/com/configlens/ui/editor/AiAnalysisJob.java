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

import com.configlens.core.model.ConfigTree;
import com.configlens.core.parser.GeminiAiService;
import com.configlens.core.parser.PrivacyFilter;
import com.configlens.ui.Activator;
import com.configlens.ui.preferences.ConfigLensPreferencePage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

/**
 * Background Job to perform AI analysis via Gemini.
 * Shows status indicator and updates UI with results.
 */
public final class AiAnalysisJob extends Job {

  private final ConfigTree tree;
  private final IEditorPart editor;

  public AiAnalysisJob(IEditorPart editor, ConfigTree tree) {
    super("ConfigLens: AI Analysis");
    this.editor = editor;
    this.tree = tree;
    setUser(true); // Show progress in UI
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IPreferenceStore store = Activator.getDefault().getPreferenceStore();
    boolean optIn = store.getBoolean(ConfigLensPreferencePage.AI_OPT_IN);
    String apiKey = store.getString(ConfigLensPreferencePage.GEMINI_API_KEY);

    if (!optIn || apiKey.isEmpty()) {
      return Status.OK_STATUS;
    }

    monitor.beginTask("ConfigLens: AI Analyzing structural risks...", IProgressMonitor.UNKNOWN);
    
    // 1. Mask content for privacy
    PrivacyFilter filter = new PrivacyFilter();
    String masked = filter.maskForAi(tree.getRootNode());

    // 2. Call Gemini
    GeminiAiService aiService = new GeminiAiService(apiKey);
    try {
      String response = aiService.analyzeConfig(masked).get();
      
      // 3. Update Status Line (or log finding)
      Display.getDefault().asyncExec(() -> {
        if (!editor.getEditorSite().getShell().isDisposed()) {
          editor.getEditorSite().getActionBars().getStatusLineManager().setMessage("AI Insight: " + truncate(response));
        }
      });
      
    } catch (Exception e) {
      return new Status(IStatus.WARNING, "com.configlens.ui", "AI Analysis failed", e);
    } finally {
      monitor.done();
    }

    return Status.OK_STATUS;
  }

  private String truncate(String s) {
     if (s.length() > 100) return s.substring(0, 97) + "...";
     return s;
  }
}
