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

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.CursorLinePainter;

/**
 * Participates in the initial setup of a YAML document.
 * Note: While IDocumentSetupParticipant usually only sets up partitioning,
 * we can use it to ensure our YAML-specific handlers are ready.
 */
public final class YamlDocumentSetupParticipant implements IDocumentSetupParticipant {

  @Override
  public void setup(IDocument document) {
    // Partitioning and other document-level setup would go here if needed.
    // However, IAutoEditStrategy and VerifyListener are better registered 
    // in EditorLifecycleManager where we have access to the SourceViewer.
  }
}
