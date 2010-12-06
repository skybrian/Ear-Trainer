// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.Iterator;

/**
 * A musical interval. (The distance between two notes on a piano.)
 * May be ascending or descending.
 */
final class Interval implements Comparable<Interval> {
  static final Interval UNISON = new Interval(0);
  static final Interval MINOR_SECOND = new Interval(1);
  static final Interval MAJOR_SECOND = new Interval(2);
  static final Interval MINOR_THIRD = new Interval(3);
  static final Interval MAJOR_THIRD = new Interval(4);
  static final Interval PERFECT_FOURTH = new Interval(5);
  static final Interval TRITONE = new Interval(6);
  static final Interval PERFECT_FIFTH = new Interval(7);
  static final Interval MAJOR_SEVENTH = new Interval(11);
  static final Interval OCTAVE = new Interval(12);

  private static final String UP_ARROW = "\u2191";
  private static final String DOWN_ARROW = "\u2193";

  private final int halfSteps;

  Interval(int halfSteps) {
    this.halfSteps = halfSteps;
  }

  String getName() {
    switch (Math.abs(halfSteps)) {
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

  String getShortName() {
    return getAbbreviation() + (isAscending() ? UP_ARROW : DOWN_ARROW);
  }

  String getShortNameAscii() {
    return getAbbreviation() + (isAscending() ? "^" : "v");
  }
    
  /** Returns a negative value if descending */
  int getHalfSteps() {
    return halfSteps;
  }

  Interval up() {
    return new Interval(this.halfSteps + 1);
  }
  
  boolean isAscending() {
    return halfSteps >= 0;
  }
  
  boolean isDescending() {
    return halfSteps <= 0;
  }

  Interval toAscending() {
    return new Interval(Math.abs(halfSteps));
  }

  Interval reverse() {
    return new Interval(-halfSteps);
  }
    
  Interval invert() {
    if (halfSteps >= 0 && halfSteps <= 12) {
      return new Interval(-12 + halfSteps);
    } else if (halfSteps >= -12 && halfSteps < 0) {
      return new Interval(halfSteps + 12);
    } else {
      throw new RuntimeException("not implemented");
    }
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
  
  private String getAbbreviation() {
    switch (Math.abs(halfSteps)) {
      case 0: return "U";
      case 1: return "m2";
      case 2: return "M2";
      case 3: return "m3";
      case 4: return "M3";
      case 5: return "P4";
      case 6: return "TT";
      case 7: return "P5";
      case 8: return "m6";
      case 9: return "M6";
      case 10: return "m7";
      case 11: return "M7";
      case 12: return "8va";
      default:
        return "?";
    }
  }

  static Iterable<Interval> range(final Interval start, final Interval end) {
    return new Iterable<Interval>() {
      public Iterator<Interval> iterator() {
        return new Iterator<Interval>() {
          int current = start.halfSteps - 1;

          public boolean hasNext() {
            return current < end.halfSteps;
          }

          public Interval next() {
            current++;
            return new Interval(current);
          }

          public void remove() {
            throw new UnsupportedOperationException("not implemented");
          }
        };
      }
    };
  }

}
