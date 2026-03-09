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
import com.configlens.core.model.ConfigTree;
import com.configlens.core.model.ProjectEnvCache;
import com.configlens.core.parser.EnvStreamingParser;
import com.configlens.core.parser.JsonStreamingParser;
import com.configlens.core.parser.YamlStreamingParser;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

/**
 * Singleton manager to cache and synchronize ConfigTree models with active
 * Eclipse editors.
 */
public final class DocumentModelManager {

	private static final DocumentModelManager INSTANCE = new DocumentModelManager();
	private final Map<IEditorInput, ConfigTree> treeMap = new HashMap<>();

	private final YamlStreamingParser yamlParser = new YamlStreamingParser();
	private final JsonStreamingParser jsonParser = new JsonStreamingParser();
	private final EnvStreamingParser envParser = new EnvStreamingParser();

	private DocumentModelManager() {
	}

	public static DocumentModelManager getInstance() {
		return INSTANCE;
	}

	public ConfigTree getTree(IEditorInput input) {
		return treeMap.get(input);
	}

	/**
	 * Refreshes the model for the given input. Usually triggered on document change
	 * or file save.
	 */
	public void refresh(IEditorInput input, String content, com.configlens.core.parser.ParserControl control) {
		if (!(input instanceof IFileEditorInput fileInput))
			return;
		IFile file = fileInput.getFile();
		String ext = file.getFileExtension();

		try {
			ConfigTree tree = null;
			if ("yaml".equalsIgnoreCase(ext) || "yml".equalsIgnoreCase(ext)) {
				tree = yamlParser.parse(content, control);
			} else if ("json".equalsIgnoreCase(ext)) {
				tree = jsonParser.parse(content, control);
			} else if ("env".equalsIgnoreCase(ext)) {
				tree = envParser.parse(content, control);
				if (tree != null) {
					updateEnvCache(file, tree);
				}
			}

			if (tree != null) {
				treeMap.put(input, tree);
			}
		} catch (Exception e) {
			org.osgi.framework.Bundle bundle = org.osgi.framework.FrameworkUtil.getBundle(DocumentModelManager.class);
			if (bundle != null) {
				org.eclipse.core.runtime.Platform.getLog(bundle).log(
						new org.eclipse.core.runtime.Status(
								org.eclipse.core.runtime.IStatus.ERROR,
								bundle.getSymbolicName(),
								"Error refreshing document model",
								e));
			}		}
	}

	public void remove(IEditorInput input) {
		treeMap.remove(input);
	}

	private void updateEnvCache(IFile file, ConfigTree tree) {
		Map<String, String> envData = tree.getRootNode().getChildren().stream()
				.filter(node -> node.getValue().isPresent())
				.collect(Collectors.toMap(ConfigNode::getKey, node -> node.getValue().get().toString()));

		ProjectEnvCache.getInstance().updateEnv(file.getProject().getName(), envData);
	}
}
