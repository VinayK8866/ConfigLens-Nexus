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

import com.configlens.core.model.ProjectEnvCache;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Background Job to scan document content for placeholders (${VAR}, {{ VAR }})
 * and update the annotation model with resolved values.
 */
public final class GhostValueScanner extends Job {

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
      "(\\$\\{([A-Za-z0-9_.]+)\\}|\\{\\{\\s*([A-Za-z0-9_.]+)\\s*\\}\\})");

  private final ITextEditor editor;
  private final String projectId;

  public GhostValueScanner(ITextEditor editor, String projectId) {
    super("ConfigLens: Resolving Placeholders");
    this.editor = editor;
    this.projectId = projectId;
    setSystem(true);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
    IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());

    if (model == null || document == null || monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }

    String content = document.get();
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
    Map<GhostValueAnnotation, Position> newAnnotations = new HashMap<>();

    while (matcher.find()) {
      if (monitor.isCanceled()) return Status.CANCEL_STATUS;

      String varName = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
      String resolved = ProjectEnvCache.getInstance().resolve(projectId, varName);

      if (resolved != null) {
        GhostValueAnnotation annotation = new GhostValueAnnotation(resolved);
        Position position = new Position(matcher.start(), matcher.end() - matcher.start());
        newAnnotations.put(annotation, position);
      }
    }

    // Batch update the annotation model
    if (model instanceof IAnnotationModelExtension extension) {
      // Find old ghost annotations to remove
      extension.replaceAnnotations(getOldAnnotations(model), newAnnotations);
    }

    return Status.OK_STATUS;
  }

  private org.eclipse.jface.text.source.Annotation[] getOldAnnotations(IAnnotationModel model) {
    java.util.List<org.eclipse.jface.text.source.Annotation> old = new java.util.ArrayList<>();
    java.util.Iterator<?> it = model.getAnnotationIterator();
    while (it.hasNext()) {
      Object a = it.next();
      if (a instanceof GhostValueAnnotation) {
        old.add((org.eclipse.jface.text.source.Annotation) a);
      }
    }
    return old.toArray(new org.eclipse.jface.text.source.Annotation[0]);
  }
}
