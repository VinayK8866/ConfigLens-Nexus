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

import com.configlens.ui.Activator;
import com.configlens.ui.preferences.ConfigLensPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

/**
 * Manages YAML-specific indentation via VerifyListener.
 * Converts Tab to Spaces and attempts to fix indentation on Paste.
 */
public final class YamlIndentationHandler implements VerifyListener {

  @Override
  public void verifyText(VerifyEvent e) {
    IPreferenceStore store = Activator.getDefault().getPreferenceStore();
    if (!store.getBoolean(ConfigLensPreferencePage.YAML_TABS_TO_SPACES)) {
      return;
    }

    // 1. Tab to Spaces
    if (e.text.equals("\t")) {
      int indentSize = store.getInt(ConfigLensPreferencePage.YAML_INDENT_SIZE);
      e.text = " ".repeat(indentSize);
      return;
    }

    // 2. Format on Paste (simplified)
    if (e.text.length() > 1 && e.text.contains("\n") && e.widget instanceof StyledText) {
      StyledText textWidget = (StyledText) e.widget;
      int caretOffset = textWidget.getCaretOffset();
      int lineIndex = textWidget.getLineAtOffset(caretOffset);
      int lineOffset = textWidget.getOffsetAtLine(lineIndex);
      
      String linePart = textWidget.getText(lineOffset, caretOffset - 1);
      String indentation = "";
      for (char c : linePart.toCharArray()) {
        if (Character.isWhitespace(c)) indentation += c;
        else break;
      }

      if (!indentation.isEmpty()) {
        String[] lines = e.text.split("\n", -1);
        if (lines.length > 1) {
          StringBuilder sb = new StringBuilder();
          sb.append(lines[0]);
          for (int i = 1; i < lines.length; i++) {
            sb.append("\n").append(indentation).append(lines[i]);
          }
          e.text = sb.toString();
        }
      }
    }
  }
}
