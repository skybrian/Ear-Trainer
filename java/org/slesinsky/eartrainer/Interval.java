// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

/**
 * A musical interval. (The distance between two notes on a piano.)
 */
final class Interval implements Comparable<Interval> {
  static final Interval UNISON = new Interval(0);
  static final Interval MINOR_SECOND = new Interval(1);
  static final Interval PERFECT_FOURTH = new Interval(5);
  static final Interval PERFECT_FIFTH = new Interval(7);
  static final Interval OCTAVE = new Interval(12);

  private final int halfSteps;

  Interval(int halfSteps) {
    if (halfSteps < 0) {
      throw new IllegalArgumentException("invalid value for halfSteps: " + halfSteps);
    }
    this.halfSteps = halfSteps;
  }

  String getName() {
    switch (halfSteps) {
      case 0: return "Unison";
      case 1: return "Minor Second";
      case 2: return "Major Second";
      case 3: return "Minor Third";
      case 4: return "Major Third";
      case 5: return "Perfect Fourth";
      case 6: return "Tritone";
      case 7: return "Perfect Fifth";
      case 8: return "Minor Sixth";
      case 9: return "Major Sixth";
      case 10: return "Minor Seventh";
      case 11: return "Major Seventh";
      case 12: return "Octave";
      default:
        return "Interval<" + halfSteps + ">";
    }
  }

  int getHalfSteps() {
    return halfSteps;
  }

  Interval add(Interval other) {
    return new Interval(this.halfSteps + other.halfSteps);
  }

  @Override
  public boolean equals(Object o) {
    return o.getClass() == Interval.class && halfSteps == ((Interval) o).halfSteps;
  }

  @Override
  public int hashCode() {
    return halfSteps;
  }

  public int compareTo(Interval other) {
    if (halfSteps < other.halfSteps) {
      return -1;
    } else if (halfSteps == other.halfSteps) {
      return 0;
    } else {
      return 1;
    }
  }
}
