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
package com.configlens.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.configlens.core.model.ConfigNode;
import com.configlens.core.model.ConfigTree;
import com.configlens.core.model.PathTrie;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class AbsoluteResolverTest {

  private AbsoluteResolver resolver;

  @Before
  public void setUp() {
    resolver = new AbsoluteResolver();
  }

  @Test
  public void testFormatPath() {
    assertEquals("my.node.path", resolver.formatPath("my.node.path", "plain"));
    assertEquals("$.my.node.path", resolver.formatPath("my.node.path", "jsonpath"));
    assertEquals("", resolver.formatPath("", "jsonpath"));
  }

  @Test
  public void testResolvePathAtLine() {
    PathTrie trie = new PathTrie();
    
    ConfigNode subNode = new ConfigNode.Builder("sub", "root.sub")
        .startLine(2).endLine(2).build();
        
    ConfigNode rootNode = new ConfigNode.Builder("root", "root")
        .startLine(1).endLine(3)
        .children(java.util.Collections.singletonList(subNode))
        .build();

    trie.insert(rootNode);
    trie.insert(subNode);

    ConfigTree tree = new ConfigTree(rootNode, trie);

    Optional<String> pathSub = resolver.resolvePathAtLine(tree, 2);
    assertTrue(pathSub.isPresent());
    assertEquals("root.sub", pathSub.get());

    Optional<String> pathRoot = resolver.resolvePathAtLine(tree, 1);
    assertTrue(pathRoot.isPresent());
    assertEquals("root", pathRoot.get());
  }
}
