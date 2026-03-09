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

import java.util.Map;
import java.util.regex.Pattern;

/**
 * A central library of 20+ regex patterns for detecting common secrets.
 */
public final class SecretPatternLibrary {

  public static final Map<String, Pattern> PATTERNS = Map.ofEntries(
      Map.entry("AWS Access Key", Pattern.compile("(A3T[A-Z0-9]|AKIA|AGPA|AIDA|AROA|AIPA|ANPA|ANVA|ASIA)[A-Z0-9]{16}")),
      Map.entry("AWS Secret Key", Pattern.compile("(?i)aws(.{0,20})?(?-i)['\"][0-9a-zA-Z\\/+]{40}['\"]")),
      Map.entry("Google API Key", Pattern.compile("AIza[0-9A-Za-z\\\\-_]{35}")),
      Map.entry("Google OAuth Token", Pattern.compile("ya29\\.[0-9A-Za-z\\-_]+")),
      Map.entry("Firebase API Key", Pattern.compile("AIza[0-9A-Za-z\\\\-_]{35}")),
      Map.entry("Generic API Key", Pattern.compile("(?i)(api_key|apikey|api-key)(.{0,20})?['\"][0-9a-zA-Z]{32,45}['\"]")),
      Map.entry("GitHub Token", Pattern.compile("(ghp|gho|ghu|ghs|ghr)_[a-zA-Z0-9]{36}")),
      Map.entry("GitHub App Token", Pattern.compile("(v1|v2)\\.[0-9a-f]{40}")),
      Map.entry("Slack Token", Pattern.compile("xox[baprs]-[0-9]{12}-[0-9]{12}-[a-zA-Z0-9]{24}")),
      Map.entry("Slack Webhook", Pattern.compile("https://hooks\\.slack\\.com/services/T[a-zA-Z0-9_]{8}/B[a-zA-Z0-9_]{8}/[a-zA-Z0-9_]{24}")),
      Map.entry("Stripe API Key", Pattern.compile("sk_live_[0-9a-zA-Z]{24}")),
      Map.entry("Stripe Restricted Key", Pattern.compile("rk_live_[0-9a-zA-Z]{24}")), // Corrected to Pattern.compile
      Map.entry("Heroku API Key", Pattern.compile("(?i)heroku(.{0,20})?['\"][0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}['\"]")),
      Map.entry("MailChimp API Key", Pattern.compile("[0-9a-f]{32}-us[0-9]{1,2}")),
      Map.entry("Mailgun API Key", Pattern.compile("key-[0-9a-zA-Z]{32}")),
      Map.entry("Twilio API Key", Pattern.compile("SK[0-9a-fA-F]{32}")),
      Map.entry("SendGrid API Key", Pattern.compile("SG\\.[0-9A-Za-z\\-_]{22}\\.[0-9A-Za-z\\-_]{43}")),
      Map.entry("Facebook Access Token", Pattern.compile("EAACEdEose0cBA[0-9A-Za-z]+")),
      Map.entry("Facebook OAuth", Pattern.compile("(?i)facebook(.{0,20})?['\"][0-9a-f]{32}['\"]")),
      Map.entry("Twitter OAuth", Pattern.compile("(?i)twitter(.{0,20})?['\"][0-9a-zA-Z]{35,44}['\"]")),
      Map.entry("Square Access Token", Pattern.compile("sqOatp-[0-9A-Za-z\\-_]{22}")),
      Map.entry("Square OAuth Secret", Pattern.compile("sq0csp-[0-9A-Za-z\\-_]{43}")),
      Map.entry("Private Key", Pattern.compile("-----BEGIN (RSA|EC|DSA|GPG|OPENSSH) PRIVATE KEY-----")),
      Map.entry("Generic Password", Pattern.compile("(?i)(password|passwd|pwd)(.{0,20})?['\"][^'\"\\s]{8,50}['\"]"))
  );
  
  private SecretPatternLibrary() {}
}
