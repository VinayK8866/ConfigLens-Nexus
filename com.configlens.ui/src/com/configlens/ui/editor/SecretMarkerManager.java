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

import com.configlens.core.model.SecretResult;
import com.configlens.core.parser.SecretDetector;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.BadLocationException;

/**
 * Handles the creation and management of Eclipse Problem Markers for detected secrets.
 */
public final class SecretMarkerManager {

  public static final String MARKER_TYPE = "com.configlens.ui.secretMarker";
  private final SecretDetector detector = new SecretDetector();

  /**
   * Scans document content for secrets and updates file markers in a background Job.
   */
  public void scanAndMark(IFile file, IDocument document) {
    String content = document.get();
    
    Job scanJob = new Job("ConfigLens: Scanning for Secrets") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          // 1. Clear existing markers
          file.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_ZERO);

          // 2. Scan content line-by-line (streaming approach)
          List<SecretResult> allSecrets = new ArrayList<>();
          int lineCount = document.getNumberOfLines();
          
          for (int i = 0; i < lineCount; i++) {
            if (monitor.isCanceled()) return Status.CANCEL_STATUS;
            
            int offset = document.getLineOffset(i);
            int length = document.getLineLength(i);
            String lineText = document.get(offset, length);
            
            allSecrets.addAll(detector.scanLine(lineText, i + 1));
          }

          // 3. Create new markers
          for (SecretResult secret : allSecrets) {
            createMarker(file, document, secret);
          }

        } catch (Exception e) {
          return new Status(IStatus.ERROR, "com.configlens.ui", "Failed to scan for secrets", e);
        }
        return Status.OK_STATUS;
      }
    };
    scanJob.setSystem(true);
    scanJob.schedule();
  }

  private void createMarker(IFile file, IDocument document, SecretResult secret) throws Exception {
    IMarker marker = file.createMarker(MARKER_TYPE);
    marker.setAttribute(IMarker.MESSAGE, secret.message());
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
    marker.setAttribute(IMarker.LINE_NUMBER, secret.lineNumber());
    
    try {
      int lineOffset = document.getLineOffset(secret.lineNumber() - 1);
      marker.setAttribute(IMarker.CHAR_START, lineOffset + secret.startColumn());
      marker.setAttribute(IMarker.CHAR_END, lineOffset + secret.endColumn());
    } catch (BadLocationException e) {
      // Fallback to line-only if document changed
    }
  }
}
