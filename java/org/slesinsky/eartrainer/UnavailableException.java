// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

/**
 * Generic exception indicating that the return value is unavailable.
 */
public class UnavailableException extends Exception {
  UnavailableException(Throwable throwable) {
    super(throwable);
  }
}
