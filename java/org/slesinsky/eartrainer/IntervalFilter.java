// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Determines which intervals are allowed in generated phrases.
 */
class IntervalFilter {
  static final IntervalFilter DEFAULT =
      new IntervalFilter(Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH);

  // contains ascending intervals
  private final SortedSet<Interval> enabled;

  IntervalFilter(Interval... intervalsToEnable) {
    this.enabled = new TreeSet<Interval>();
    for (Interval interval : intervalsToEnable) {
      this.enabled.add(interval.toAscending());
    }
  }

  private IntervalFilter(SortedSet<Interval> enabled) {
    this.enabled = enabled;
  }

  IntervalFilter enable(Interval choice) {
    SortedSet<Interval> newSet = new TreeSet<Interval>(enabled);
    newSet.add(choice.toAscending());
    return new IntervalFilter(newSet);
  }

  IntervalFilter disable(Interval choice) {
    SortedSet<Interval> newSet = new TreeSet<Interval>(enabled);
    newSet.remove(choice.toAscending());
    return new IntervalFilter(newSet);
  }

  IntervalFilter intersectScale(Scale scale) {
    SortedSet<Interval> newSet = new TreeSet<Interval>();
    for (Interval item : enabled) {
      if (scale.containsAnywhere(item)) {
        newSet.add(item);
      }
    }        
    return new IntervalFilter(newSet);
  }
   
  boolean allows(Interval interval) {
    return enabled.contains(interval.toAscending());
  }

  boolean allows(Phrase phrase) {
    for (Interval interval : phrase.getIntervals()) {
      if (!allows(interval)) {
        return false;
      }
    }
    return true;
  }

  Collection<Interval> generate(DirectionFilter filter) {
    SortedSet<Interval> intervals = new TreeSet<Interval>();
    for (Interval interval : enabled) {        
      if (filter.allows(interval)) {
        intervals.add(interval);
      }
      if (filter.allows(interval.reverse())) {
        intervals.add(interval.reverse());
      }
    }
    return intervals;
  }

  Interval getSmallest() {
    return enabled.first();
  }
  
  Interval getSecondSmallest() {
    Iterator<Interval> it = enabled.iterator();
    it.next();
    return it.next();
  }

  Interval getLargest() {
    return enabled.last();
  }
}
