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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Control object for long-running parsing operations.
 * Allows for cancellation (interruption) and depth limiting (sampled indexing).
 */
public final class ParserControl {

  private java.util.function.Supplier<Boolean> cancellationSupplier = () -> false;
  private final int maxDepth;
  private final long fileSize;

  public static final int DEFAULT_MAX_DEPTH = 5;
  public static final long LARGE_FILE_THRESHOLD = 100 * 1024 * 1024; // 100MB

  public ParserControl(long fileSize) {
    this.fileSize = fileSize;
    this.maxDepth = (fileSize > LARGE_FILE_THRESHOLD) ? DEFAULT_MAX_DEPTH : Integer.MAX_VALUE;
  }

  public void setCancellationSupplier(java.util.function.Supplier<Boolean> supplier) {
    this.cancellationSupplier = supplier;
  }

  public void cancel() {
     // Still allow manual cancellation
     final java.util.function.Supplier<Boolean> old = cancellationSupplier;
     this.cancellationSupplier = () -> true || old.get();
  }

  public boolean isCancelled() {
    return cancellationSupplier.get();
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public boolean isLargeFile() {
    return fileSize > LARGE_FILE_THRESHOLD;
  }
}
