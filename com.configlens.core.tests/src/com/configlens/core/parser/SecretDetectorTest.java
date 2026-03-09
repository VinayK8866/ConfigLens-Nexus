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

import com.configlens.core.model.SecretResult;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class SecretDetectorTest {

  private SecretDetector detector;

  @Before
  public void setUp() {
    detector = new SecretDetector();
  }

  @Test
  public void testIsSecret() {
    assertTrue(detector.isSecret("key", "AKIAIOSFODNN7EXAMPLE")); // AWS Key pattern
    assertFalse(detector.isSecret("key", "simple_value"));
    assertTrue(detector.isSecret("key", "thisIsAVeryLongStringWithHighEntropy1234567890!@#$%^&*()_+")); // Entropy
  }

  @Test
  public void testScanLinePatternMatching() {
    List<SecretResult> results = detector.scanLine("aws_access_key_id = AKIAIOSFODNN7EXAMPLE", 1);
    assertEquals(1, results.size());
    assertEquals("AWS Access Key", results.get(0).type());
    assertEquals("AKIAIOSFODNN7EXAMPLE", results.get(0).value());
    assertEquals(1, results.get(0).lineNumber());
  }

  @Test
  public void testScanLineIgnoreComment() {
    List<SecretResult> results = detector.scanLine("aws_access_key_id = AKIAIOSFODNN7EXAMPLE # configlens-ignore", 1);
    assertTrue(results.isEmpty()); // Should be ignored
  }

  @Test
  public void testScanLineEntropy() {
    List<SecretResult> results = detector.scanLine("token: thisIsAVeryLongStringWithHighEntropy1234567890!@#$%^&*()_+", 1);
    assertEquals(1, results.size());
    assertEquals("High-Entropy Token", results.get(0).type());
  }
}
