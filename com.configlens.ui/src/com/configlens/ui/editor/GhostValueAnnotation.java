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

import org.eclipse.jface.text.source.Annotation;

/**
 * Annotation representing a placeholder that has a resolved environment value.
 * Used by GhostValuePainter to render the overlay and by Eclipse hovers to show the full value.
 */
public final class GhostValueAnnotation extends Annotation {
  public static final String TYPE = "com.configlens.ui.ghostAnnotation";
  
  private final String resolvedValue;

  public GhostValueAnnotation(String resolvedValue) {
    super(TYPE, false, "Value: " + resolvedValue);
    this.resolvedValue = resolvedValue;
  }

  public String getResolvedValue() {
    return resolvedValue;
  }
}
