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
import com.configlens.core.parser.SecretDetector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Scans the configuration tree for secrets and places visual markers
 * (Annotations)
 * in the Eclipse editor gutter and text area.
 */
public final class SecretHighlighter {

	private static final String ANNOTATION_TYPE = "com.configlens.ui.secretAnnotation";
	private final SecretDetector detector = new SecretDetector();

	public void highlightSecrets(ITextEditor editor, ConfigNode root) {
		IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		if (model == null || document == null)
			return;

		// 1. Clear existing secret annotations
		synchronized (model) {
			List<Annotation> toRemove = new ArrayList<>();
			Iterator<?> it = model.getAnnotationIterator();
			while (it.hasNext()) {
				Annotation ann = (Annotation) it.next();
				if (ANNOTATION_TYPE.equals(ann.getType())) {
					toRemove.add(ann);
				}
			}
			for (Annotation ann : toRemove) {
				model.removeAnnotation(ann);
			}

			// 2. Scan tree and add new ones
			scan(root, model, document);
		}
	}

	private void scan(ConfigNode node, IAnnotationModel model, IDocument document) {
		if (node.getValue().isPresent()) {
			try {
				int line = node.getStartLine() - 1;
				if (line >= 0 && line < document.getNumberOfLines()) {
					int lineOffset = document.getLineOffset(line);
					int lineLength = document.getLineLength(line);
					String lineText = document.get(lineOffset, lineLength);
					
					// Respect ignore comment in real-time highlighter
					if (lineText.contains("configlens-ignore")) {
						return;
					}
				}
			} catch (Exception e) {}

			if (detector.isSecret(node.getKey(), node.getValue().get().toString())) {
				addAnnotation(node, model, document);
			}
		}
		for (ConfigNode child : node.getChildren()) {
			scan(child, model, document);
		}
	}

	private void addAnnotation(ConfigNode node, IAnnotationModel model, IDocument document) {
		try {
			int line = node.getStartLine() - 1; // 1-based to 0-based
			if (line < 0 || line >= document.getNumberOfLines())
				return;

			int lineOffset = document.getLineOffset(line);
			
			// Use precise start/end columns to only highlight the value, not the entire line/key
			int start = lineOffset + node.getStartColumn();
			int length = node.getEndColumn() - node.getStartColumn();
			
			if (length <= 0) {
				length = document.getLineLength(line) - node.getStartColumn();
			}

			Annotation ann = new Annotation(ANNOTATION_TYPE, false, "Potential secret detected: " + node.getKey());
			model.addAnnotation(ann, new Position(start, length));
		} catch (Exception e) {
		}
	}
}
