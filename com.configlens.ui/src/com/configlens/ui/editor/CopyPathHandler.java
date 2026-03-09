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
import com.configlens.core.parser.AbsoluteResolver;
import java.util.Optional;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Handles the "Copy Path" commands. Resolves the hierarchical path based on current
 * cursor selection and puts it into the system clipboard.
 */
public final class CopyPathHandler extends AbstractHandler {

  private final AbsoluteResolver resolver = new AbsoluteResolver();

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IEditorPart editor = HandlerUtil.getActiveEditor(event);
    if (!(editor instanceof ITextEditor textEditor)) {
      return null;
    }

    ISelection selection = textEditor.getSelectionProvider().getSelection();
    if (!(selection instanceof ITextSelection textSelection)) {
      return null;
    }

    int line = textSelection.getStartLine() + 1; // 0-based to 1-based
    String mode = event.getParameter("com.configlens.ui.param.mode");

    // Adhere to the '100MB Rule': offload potentially heavy Trie resolution to a Job
    Job copyJob = new Job("ConfigLens: Copying Path") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        ConfigTree tree = DocumentModelManager.getInstance().getTree(textEditor.getEditorInput());
        if (tree == null) {
          return Status.CANCEL_STATUS;
        }

        Optional<String> path = resolver.resolvePathAtLine(tree, line);
        if (path.isPresent()) {
          String formattedPath = resolver.formatPath(path.get(), mode);
          
          // Clipboard access must happen on UI thread
          Display.getDefault().asyncExec(() -> copyToClipboard(formattedPath));
        }
        
        return Status.OK_STATUS;
      }
    };
    
    copyJob.setSystem(true);
    copyJob.schedule();

    return null;
  }

  private void copyToClipboard(String text) {
    if (text == null || text.isEmpty()) return;
    
    Display display = Display.getDefault();
    Clipboard clipboard = new Clipboard(display);
    try {
      clipboard.setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
    } finally {
      clipboard.dispose();
    }
  }
}
