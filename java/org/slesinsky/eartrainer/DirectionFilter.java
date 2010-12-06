// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

/**
 * Filters intervals based on their direction.
 */
enum DirectionFilter {
  ASCENDING("Up"),
  DESCENDING("Down"),
  BOTH("Both");

  static final DirectionFilter DEFAULT = ASCENDING;
  
  private final String label;

  DirectionFilter(String label) {
    this.label = label;
  }

  String getLabel() {
    return label;
  }
  
  boolean allows(Interval interval) {
    return this == BOTH
        || (this == ASCENDING && interval.isAscending())
        || (this == DESCENDING && interval.isDescending());
  }

  boolean allows(Phrase phrase) {
    for (Interval interval : phrase.getIntervals()) {
      if (!allows(interval)) {
        return false;
      }
    }
    return true;
  }
}
