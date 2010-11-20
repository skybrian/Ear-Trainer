// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * A set of intervals.
 */
class IntervalSet {
  private final Set<Interval> choices;

  IntervalSet(Collection<Interval> choices) {
    this.choices = new TreeSet<Interval>(choices);
  }

  IntervalSet(Interval... choices) {
    this(Arrays.asList(choices));
  }

  Interval choose(Random randomness) {
    return Util.choose(randomness, choices);
  }

  IntervalSet with(Interval choice) {
    Set<Interval> newSet = new TreeSet<Interval>(choices);
    newSet.add(choice);
    return new IntervalSet(newSet);
  }

  IntervalSet without(Interval choice) {
    Set<Interval> newSet = new TreeSet<Interval>(choices);
    newSet.remove(choice);
    return new IntervalSet(newSet);
  }

  boolean contains(Interval interval) {
    return choices.contains(interval);
  }

  boolean isEmpty() {
    return choices.isEmpty();
  }

  int size() {
    return choices.size();
  }

  Collection<Interval> each() {
    return Collections.unmodifiableSet(choices);
  }

  IntervalSet getSmallest(int count) {
    return new IntervalSet(new ArrayList<Interval>(choices).subList(0, count));
  }

  IntervalSet getLargest(int count) {
    return new IntervalSet(new ArrayList<Interval>(choices).subList(size() - count, size()));
  }

  Interval toInterval() {
    return each().iterator().next();
  }

  /** Returns the set of all intervals from lowest to highest, inclusive. */
  static IntervalSet forRange(Interval lowest, Interval highest) {
    List<Interval> choices = new ArrayList<Interval>();
    Interval candidate = lowest;
    while (candidate.compareTo(highest) <= 0) {
      choices.add(candidate);
      candidate = candidate.add(Interval.MINOR_SECOND);
    }
    return new IntervalSet(choices);
  }

  static IntervalSet forHalfSteps(int... halfStep) {
    List<Interval> choices = new ArrayList<Interval>();
    for (int step : halfStep) {
      choices.add(new Interval(step));
    }
    return new IntervalSet(choices);
  }
}
