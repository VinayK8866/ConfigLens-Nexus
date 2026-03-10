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
import com.configlens.ui.editor.SecretHighlighter;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension2;

/**
 * Manages the lifecycle of ConfigLens enhancements within Eclipse editors.
 * Detects when YAML, JSON, or .env files are opened and installs breadcrumbs
 * and painters.
 */
public final class EditorLifecycleManager implements IPartListener2 {

	private final Map<IEditorPart, ConfigSelectionListener> selectionListeners = new HashMap<>();
	private final SecretHighlighter secretHighlighter = new SecretHighlighter();
	private final SecretMarkerManager markerManager = new SecretMarkerManager();

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		checkAndInject(partRef);
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		checkAndInject(partRef);
	}

	private void checkAndInject(IWorkbenchPartReference partRef) {
		if (partRef.getPart(false) instanceof ITextEditor editor) {
			if (isSupported(editor.getEditorInput()) && !selectionListeners.containsKey(editor)) {
				installEnhancements(editor);
			}
		}
	}

	private boolean isSupported(IEditorInput input) {
		if (input instanceof IFileEditorInput fileInput) {
			String ext = fileInput.getFile().getFileExtension();
			return ext != null && (ext.equalsIgnoreCase("yaml") || ext.equalsIgnoreCase("yml")
					|| ext.equalsIgnoreCase("json") || ext.equalsIgnoreCase("env"));
		}
		return false;
	}

	private void installEnhancements(ITextEditor editor) {
		// IMPORTANT: Mark editor as installed FIRST so we never try twice, even if parts fail
		selectionListeners.put(editor, null);

		// 1. Inject Breadcrumb UI (optional — features do NOT depend on this succeeding)
		BreadcrumbComposite breadcrumb = null;
		try {
			breadcrumb = BreadcrumbInjector.inject(editor);
		} catch (Exception e) {
			// Breadcrumb injection failed — all other features still work
		}

		final BreadcrumbManager bcManager;
		if (breadcrumb != null) {
			bcManager = new BreadcrumbManager(breadcrumb);
			activeBreadcrumbManagers.put(editor, bcManager);

			// 2. Install Selection Listener for breadcrumb updates
			ConfigSelectionListener selectionListener = new ConfigSelectionListener(editor, bcManager);
			editor.getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(selectionListener);
			selectionListeners.put(editor, selectionListener);
		} else {
			bcManager = null;
		}

		// 3. Install Ghost Painter (always, independent of breadcrumb)
		installGhostPainter(editor);

		// 4. Trigger Initial Ghost Scan
		String projectId = getProjectId(editor);
		new GhostValueScanner(editor, projectId).schedule();

		// 5. Trigger Initial Parse + Secret Scan in Background
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		if (doc != null) {
			// Listen for changes and re-scan
			doc.addDocumentListener(new org.eclipse.jface.text.IDocumentListener() {
				@Override
				public void documentChanged(org.eclipse.jface.text.DocumentEvent event) {
					new GhostValueScanner(editor, projectId).schedule(500);
					if (editor.getEditorInput() instanceof IFileEditorInput fileInput) {
						markerManager.scanAndMark(fileInput.getFile(), doc);
					}
					triggerParse(editor, doc.get(), bcManager);
				}
				@Override
				public void documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent event) {}
			});

			// Initial secret scan
			if (editor.getEditorInput() instanceof IFileEditorInput fileInput) {
				markerManager.scanAndMark(fileInput.getFile(), doc);
			}
			triggerParse(editor, doc.get(), bcManager);
		}
	}

	private final Map<IEditorPart, BreadcrumbManager> activeBreadcrumbManagers = new HashMap<>();
	private final Map<IEditorPart, Job> activeParseJobs = new HashMap<>();

	private void triggerParse(ITextEditor editor, String content, BreadcrumbManager bcManager) {
		Job existing = activeParseJobs.remove(editor);
		if (existing != null) {
			existing.cancel();
		}

		Job parseJob = new Job("ConfigLens: Parsing " + editor.getTitle()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				com.configlens.core.parser.ParserControl control =
				    new com.configlens.core.parser.ParserControl(content.length());
				control.setCancellationSupplier(() -> monitor.isCanceled());

				DocumentModelManager.getInstance().refresh(editor.getEditorInput(), content, control);

				if (monitor.isCanceled() || control.isCancelled()) {
					return Status.CANCEL_STATUS;
				}

				ConfigTree tree = DocumentModelManager.getInstance().getTree(editor.getEditorInput());
				if (bcManager != null && tree != null) {
					bcManager.updateTree(tree);
				}

				if (tree != null) {
					new AiAnalysisJob(editor, tree).schedule();
					com.configlens.ui.views.FlatView.notifyTreeUpdated(editor.getEditorInput());
				}
				
				// Always re-run secret highlighting after parse (on UI thread)
				Display.getDefault().asyncExec(() -> {
					try {
						secretHighlighter.highlightSecrets(editor);
					} catch (Exception ex) {
						// Editor may have been disposed
					}
				});
				
				return Status.OK_STATUS;
			}
		};
		parseJob.setSystem(true);
		activeParseJobs.put(editor, parseJob);
		parseJob.schedule(500);
	}

	private void installGhostPainter(ITextEditor editor) {
		String projectId = getProjectId(editor);

		if (editor instanceof ITextEditorExtension2) {
			Object target = editor.getAdapter(ITextOperationTarget.class);
			if (target instanceof ISourceViewer viewer && viewer instanceof ITextViewerExtension2 extension) {
				extension.addPainter(new GhostValuePainter(viewer, projectId));

				// Install YAML Indentation Handlers if applicable
				String fileName = editor.getEditorInput().getName().toLowerCase();
				if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
					YamlIndentationHandler handler = new YamlIndentationHandler();
					viewer.getTextWidget().addVerifyListener(handler);
				}

				// Disable Eclipse's projection (folding) for config files.
				// This removes the white-area artifact with line numbers and minus marks.
				// We use asyncExec with a delay to ensure the editor is fully rendered first.
				if (viewer instanceof org.eclipse.jface.text.source.projection.ProjectionViewer projViewer) {
					Display.getDefault().asyncExec(() -> {
						try {
							if (projViewer.isProjectionMode()) {
								projViewer.disableProjection();
							}
						} catch (Exception e) {
							// Ignore — projection may not be supported
						}
					});
				}
			}
		}
	}

	private String getProjectId(ITextEditor editor) {
		if (editor.getEditorInput() instanceof IFileEditorInput fileInput) {
			return fileInput.getFile().getProject().getName();
		}
		return "global";
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		if (partRef.getPart(false) instanceof IEditorPart editor) {
			ConfigSelectionListener listener = selectionListeners.remove(editor);
			if (listener != null) {
				editor.getSite().getWorkbenchWindow().getSelectionService().removePostSelectionListener(listener);
			}
			DocumentModelManager.getInstance().remove(editor.getEditorInput());
		}
	}

	// Implementation of other IPartListener2 methods (empty)
	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
	}
}
