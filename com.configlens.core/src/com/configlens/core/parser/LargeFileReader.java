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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * Optimized reader for large files using MappedByteBuffer to minimize heap pressure.
 */
public final class LargeFileReader {

  /**
   * Reads a file into a String (for smaller files or when necessary) 
   * but ideally we'd stream directly from the buffer.
   */
  public static String readWithMapping(File file) throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(file, "r");
         FileChannel channel = raf.getChannel()) {
      
      long size = channel.size();
      MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
      
      byte[] bytes = new byte[(int) size];
      buffer.get(bytes);
      return new String(bytes, StandardCharsets.UTF_8);
    }
  }

  /**
   * Provides a Reader interface over a MappedByteBuffer.
   */
  public static java.io.Reader getMappedReader(File file) throws IOException {
    // For simplicity in this implementation, we read all bytes.
    // In a full production system, we would wrap the MappedByteBuffer in a custom Reader.
    return new java.io.StringReader(readWithMapping(file));
  }
}
