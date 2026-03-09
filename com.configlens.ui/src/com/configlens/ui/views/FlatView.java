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
package com.configlens.ui.views;

import com.configlens.core.model.ConfigNode;
import com.configlens.core.model.ConfigTree;
import com.configlens.ui.editor.DocumentModelManager;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * A specialized Eclipse ViewPart providing a flattened, filterable property list
 * of the active configuration editor. Adheres to the '100MB Rule' using a 
 * Virtual Table for 10,000+ entries.
 */
public final class FlatView extends ViewPart {

  public static final String ID = "com.configlens.ui.views.FlatView";

  private TableViewer viewer;
  private Text filterText;
  private IPartListener partListener;
  
  private List<ConfigNode> allNodes = new ArrayList<>();
  private List<ConfigNode> filteredNodes = new ArrayList<>();

  @Override
  public void createPartControl(Composite parent) {
    parent.setLayout(new GridLayout(1, false));

    // 1. Filter Text
    filterText = new Text(parent, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH);
    filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    filterText.setMessage("Filter properties (key or value)...");
    filterText.addModifyListener(e -> applyFilter());

    // 2. Virtual Table Viewer
    viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.VIRTUAL);
    createColumns();

    Table table = viewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    // 3. Lazy Content Provider for high performance
    viewer.setContentProvider(new ILazyContentProvider() {
      @Override
      public void updateElement(int index) {
        if (index < filteredNodes.size()) {
          viewer.replace(filteredNodes.get(index), index);
        }
      }

      @Override
      public void dispose() {}

      @Override
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    // 4. Double-click to Navigate
    viewer.addDoubleClickListener(event -> navigateToEditor());

    registerPartListener();
    refreshData();
  }

  private void createColumns() {
    TableViewerColumn colPath = new TableViewerColumn(viewer, SWT.NONE);
    colPath.getColumn().setWidth(400);
    colPath.getColumn().setText("Absolute Path");
    colPath.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return ((ConfigNode) element).getPath();
      }
    });

    TableViewerColumn colValue = new TableViewerColumn(viewer, SWT.NONE);
    colValue.getColumn().setWidth(200);
    colValue.getColumn().setText("Value");
    colValue.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return ((ConfigNode) element).getValue().orElse("").toString();
      }
    });
  }

  private void applyFilter() {
    String search = filterText.getText().toLowerCase();
    if (search.isEmpty()) {
      filteredNodes = new ArrayList<>(allNodes);
    } else {
      filteredNodes = allNodes.stream()
          .filter(n -> n.getPath().toLowerCase().contains(search) 
              || n.getValue().orElse("").toString().toLowerCase().contains(search))
          .toList();
    }
    viewer.setItemCount(filteredNodes.size());
    viewer.refresh();
  }

  private void navigateToEditor() {
    int index = viewer.getTable().getSelectionIndex();
    if (index < 0 || index >= filteredNodes.size()) return;

    ConfigNode node = filteredNodes.get(index);
    IEditorPart activeEditor = getSite().getPage().getActiveEditor();
    
    if (activeEditor instanceof ITextEditor textEditor) {
      try {
        IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
        if (document != null) {
          // ConfigNode uses 1-based lines, IDocument uses 0-based
          int lineOffset = document.getLineOffset(node.getStartLine() - 1);
          textEditor.selectAndReveal(lineOffset, 0);
        }
      } catch (Exception e) {
        // Fallback or log error
      }
    }
  }

  private void registerPartListener() {
    partListener = new IPartListener() {
      @Override
      public void partActivated(IWorkbenchPart part) {
        if (part instanceof IEditorPart) refreshData();
      }
      @Override public void partBroughtToTop(IWorkbenchPart part) {}
      @Override public void partClosed(IWorkbenchPart part) {
        if (part instanceof IEditorPart) refreshData();
      }
      @Override public void partDeactivated(IWorkbenchPart part) {}
      @Override public void partOpened(IWorkbenchPart part) {}
    };
    getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
  }

  private void refreshData() {
    IEditorPart activeEditor = getSite().getPage().getActiveEditor();
    if (activeEditor == null) {
      allNodes = new ArrayList<>();
    } else {
      ConfigTree tree = DocumentModelManager.getInstance().getTree(activeEditor.getEditorInput());
      if (tree != null) {
        allNodes = tree.getFlattenedNodes();
      } else {
        allNodes = new ArrayList<>();
      }
    }
    applyFilter();
  }

  @Override
  public void setFocus() {
    filterText.setFocus();
  }

  @Override
  public void dispose() {
    if (partListener != null) {
      getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);
    }
    super.dispose();
  }
}
