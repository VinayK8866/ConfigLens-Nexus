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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParserControlTest {

  @Test
  public void testDefaultControl() {
    ParserControl control = new ParserControl(1024); // Small file
    assertFalse(control.isCancelled());
    assertEquals(Integer.MAX_VALUE, control.getMaxDepth());
    assertFalse(control.isLargeFile());
  }

  @Test
  public void testLargeFileControl() {
    ParserControl control = new ParserControl(ParserControl.LARGE_FILE_THRESHOLD + 1);
    assertTrue(control.isLargeFile());
    assertEquals(ParserControl.DEFAULT_MAX_DEPTH, control.getMaxDepth());
  }

  @Test
  public void testCancellation() {
    ParserControl control = new ParserControl(100);
    assertFalse(control.isCancelled());
    control.cancel();
    assertTrue(control.isCancelled());
  }

  @Test
  public void testCancellationSupplier() {
    ParserControl control = new ParserControl(100);
    boolean[] state = new boolean[] { false };
    control.setCancellationSupplier(() -> state[0]);
    assertFalse(control.isCancelled());
    state[0] = true;
    assertTrue(control.isCancelled());
  }
}
