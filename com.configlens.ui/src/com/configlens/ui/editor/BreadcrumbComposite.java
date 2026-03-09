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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * A theme-aware, interactive breadcrumb navigation bar for configuration files.
 * Renders the hierarchical path of ConfigNodes and allows jumping to specific
 * lines.
 */
public final class BreadcrumbComposite extends Composite {

	private final Canvas canvas;
	private List<ConfigNode> pathNodes = Collections.emptyList();
	private final List<Rectangle> segmentBounds = new ArrayList<>();
	private int hoveredIndex = -1;

	private IBreadcrumbListener listener;

	public interface IBreadcrumbListener {
		void nodeSelected(ConfigNode node);
	}

	public BreadcrumbComposite(Composite parent, int style) {
		super(parent, style);
		setLayout(new FillLayout());

		canvas = new Canvas(this, SWT.DOUBLE_BUFFERED);
		canvas.setBackground(getBackground());

		canvas.addPaintListener(this::onPaint);
		canvas.addMouseMoveListener(this::onMouseMove);
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				onMouseDown(e);
			}
		});

		canvas.addMouseTrackListener(new org.eclipse.swt.events.MouseTrackAdapter() {
			@Override
			public void mouseExit(MouseEvent e) {
				hoveredIndex = -1;
				canvas.redraw();
			}
		});

		final Cursor handCursor = new Cursor(getDisplay(), SWT.CURSOR_HAND);
		canvas.setCursor(handCursor);
		addDisposeListener(e -> handCursor.dispose());
	}

	public void setBreadcrumbListener(IBreadcrumbListener listener) {
		this.listener = listener;
	}

	/**
	 * Updates the displayed breadcrumb path. Must be called from the UI thread.
	 */
	public void setPath(List<ConfigNode> nodes) {
		if (isDisposed())
			return;
		this.pathNodes = nodes != null ? new ArrayList<>(nodes) : Collections.emptyList();
		this.hoveredIndex = -1;
		canvas.redraw();
	}

	private void onPaint(PaintEvent e) {
		GC gc = e.gc;
		gc.setAntialias(SWT.ON);
		gc.setTextAntialias(SWT.ON);

		ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		// Using standard Eclipse theme colors
		Color fg = getForeground();
		Color linkColor = colorRegistry.get("org.eclipse.ui.workbench.ACTIVE_TAB_TEXT_COLOR");
		if (linkColor == null)
			linkColor = Display.getDefault().getSystemColor(SWT.COLOR_LINK_FOREGROUND);

		int x = 5;
		int y = (getBounds().height - gc.getFontMetrics().getHeight()) / 2;
		segmentBounds.clear();

		for (int i = 0; i < pathNodes.size(); i++) {
			ConfigNode node = pathNodes.get(i);
			String label = node.getKey();
			if ("root".equals(label))
				continue;

			Point extent = gc.stringExtent(label);
			Rectangle bounds = new Rectangle(x, y, extent.x, extent.y);
			segmentBounds.add(bounds);

			if (i == hoveredIndex) {
				gc.setForeground(linkColor);
				gc.drawLine(x, y + extent.y, x + extent.x, y + extent.y);
			} else {
				gc.setForeground(fg);
			}

			gc.drawString(label, x, y, true);
			x += extent.x + 5;

			if (i < pathNodes.size() - 1) {
				gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
				gc.drawString(">", x, y, true);
				x += gc.stringExtent(">").x + 5;
			}
		}
	}

	private void onMouseMove(MouseEvent e) {
		int oldHovered = hoveredIndex;
		hoveredIndex = -1;
		for (int i = 0; i < segmentBounds.size(); i++) {
			if (segmentBounds.get(i).contains(e.x, e.y)) {
				hoveredIndex = i;
				break;
			}
		}
		if (oldHovered != hoveredIndex) {
			canvas.redraw();
		}
	}

	private void onMouseDown(MouseEvent e) {
		if (e.button == 1 && hoveredIndex != -1 && listener != null) {
			// Adjustment because we skip 'root' in rendering but pathNodes includes it
			int actualIndex = hoveredIndex;
			if (!pathNodes.isEmpty() && "root".equals(pathNodes.get(0).getKey())) {
				actualIndex++;
			}
			if (actualIndex < pathNodes.size()) {
				listener.nodeSelected(pathNodes.get(actualIndex));
			}
		}
	}
}
