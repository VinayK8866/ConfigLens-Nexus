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
import java.util.Iterator;
import java.util.List;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Scans the document content line-by-line for secrets and places visual markers
 * (yellow Annotations) in the Eclipse editor.
 *
 * <p>Uses the same line-based scanning as SecretMarkerManager, which guarantees
 * that {@code # configlens-ignore} is always respected — regardless of how the
 * YAML/JSON parser mapped line numbers onto nodes.</p>
 */
public final class SecretHighlighter {

	private static final String ANNOTATION_TYPE = "com.configlens.ui.secretAnnotation";
	private final SecretDetector detector = new SecretDetector();

	/**
	 * Rescans the entire document and refreshes yellow secret annotations.
	 * Must be called from the UI thread.
	 */
	public void highlightSecrets(ITextEditor editor) {
		IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		if (model == null || document == null)
			return;

		synchronized (model) {
			// 1. Clear existing secret annotations
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

			// 2. Re-scan line by line (same logic as SecretMarkerManager)
			try {
				int lineCount = document.getNumberOfLines();
				for (int i = 0; i < lineCount; i++) {
					int lineOffset = document.getLineOffset(i);
					int lineLength = document.getLineLength(i);
					String lineText = document.get(lineOffset, lineLength);

					// SecretDetector.scanLine() already checks for # configlens-ignore
					List<SecretResult> results = detector.scanLine(lineText, i + 1);

					for (SecretResult result : results) {
						// Highlight only the matched secret value, not the entire line
						int start = lineOffset + result.startColumn();
						int length = result.endColumn() - result.startColumn();
						if (length <= 0) length = lineLength; // fallback

						String msg = result.message();
						Annotation ann = new Annotation(ANNOTATION_TYPE, false, msg);
						model.addAnnotation(ann, new Position(start, length));
					}
				}
			} catch (BadLocationException e) {
				// Ignore — document may have changed concurrently
			}
		}
	}

	/**
	 * @deprecated Use {@link #highlightSecrets(ITextEditor)} which scans by line.
	 * This overload is kept only for backward compatibility with the parse-job call.
	 */
	public void highlightSecrets(ITextEditor editor,
			com.configlens.core.model.ConfigNode ignoredRoot) {
		highlightSecrets(editor);
	}
}
