// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

/**
 * An object that can print the elapsed time from when it was created.
 */
class Profiler {
  private final long startTime;
  Profiler() {
    startTime = System.currentTimeMillis();
  }
  void log(String message) {
    long elapsed = System.currentTimeMillis() - startTime;
    if (elapsed >= 10) {
      System.out.println(elapsed + ": " + message);
    }
  }
}
