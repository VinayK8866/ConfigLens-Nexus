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

import java.util.Iterator;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

/**
 * Renders "Ghost Text" overlays next to placeholders based on GhostValueAnnotations
 * in the editor's annotation model. Updated efficiently during paint events.
 */
public final class GhostValuePainter implements IPainter, PaintListener {

  private final ISourceViewer viewer;
  private final StyledText textWidget;
  private boolean active = false;
  private Color ghostColor;

  public GhostValuePainter(ISourceViewer viewer, String projectId) {
    this.viewer = viewer;
    this.textWidget = viewer.getTextWidget();
  }

  @Override
  public void paint(int reason) {
    if (!active) {
      textWidget.addPaintListener(this);
      active = true;
      ghostColor = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
    }
  }

  @Override
  public void paintControl(PaintEvent e) {
    if (textWidget.isDisposed()) return;

    IAnnotationModel model = viewer.getAnnotationModel();
    if (model == null) return;

    GC gc = e.gc;
    gc.setForeground(ghostColor);
    gc.setAlpha(128);

    Iterator<?> it = model.getAnnotationIterator();
    while (it.hasNext()) {
      Object a = it.next();
      if (a instanceof GhostValueAnnotation ghost) {
        Position pos = model.getPosition(ghost);
        if (pos == null || pos.isDeleted()) continue;

        try {
          // Calculate if current annotation is in visible area
          int startLine = textWidget.getLineAtOffset(pos.getOffset());
          int topIndex = textWidget.getTopIndex();
          int bottomIndex = topIndex + (textWidget.getClientArea().height / textWidget.getLineHeight()) + 1;

          if (startLine >= topIndex && startLine <= bottomIndex) {
            int endOffset = pos.getOffset() + pos.getLength();
            int x = textWidget.getLocationAtOffset(endOffset).x;
            int y = textWidget.getLinePixel(startLine);

            String text = " // " + truncate(ghost.getResolvedValue());
            gc.drawString(text, x + 10, y, true);
          }
        } catch (Exception ex) {
          // Ignore range/offset issues during rapid editing
        }
      }
    }
  }

  private String truncate(String s) {
    if (s == null) return "";
    return s.length() > 25 ? s.substring(0, 22) + "..." : s;
  }

  @Override
  public void deactivate(boolean redraw) {
    if (active) {
      textWidget.removePaintListener(this);
      active = false;
    }
  }

  @Override
  public void dispose() {
    deactivate(false);
  }

  @Override
  public void setPositionManager(IPaintPositionManager manager) {}
}
