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
package com.configlens.ui.preferences;

import com.configlens.ui.Activator;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Main preference page for ConfigLens.
 * Handles AI Opt-in and API key storage securely via Eclipse Preferences.
 */
public final class ConfigLensPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public static final String AI_OPT_IN = "aiOptIn";
  public static final String GEMINI_API_KEY = "geminiApiKey";
  public static final String YAML_TABS_TO_SPACES = "yamlTabsToSpaces";
  public static final String YAML_INDENT_SIZE = "yamlIndentSize";

  public ConfigLensPreferencePage() {
    super(GRID);
    setPreferenceStore(Activator.getDefault().getPreferenceStore());
    setDescription("Configure AI-assisted configuration auditing and indentation management.");
  }

  @Override
  public void createFieldEditors() {
    addField(new BooleanFieldEditor(AI_OPT_IN, "&Enable AI-assisted Analysis (Google Gemini)", getFieldEditorParent()));
    
    StringFieldEditor keyEditor = new StringFieldEditor(GEMINI_API_KEY, "Gemini &API Key:", getFieldEditorParent());
    keyEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
    addField(keyEditor);
    
    addField(new BooleanFieldEditor(YAML_TABS_TO_SPACES, "Convert &Tabs to Spaces in YAML", getFieldEditorParent()));
    org.eclipse.jface.preference.IntegerFieldEditor indentEditor = new org.eclipse.jface.preference.IntegerFieldEditor(YAML_INDENT_SIZE, "Indentation &Size:", getFieldEditorParent());
    indentEditor.setValidRange(1, 8);
    addField(indentEditor);
  }

  @Override
  public void init(IWorkbench workbench) {
  }
}
