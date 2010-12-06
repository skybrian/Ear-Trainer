// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A set of intervals.
 */
class IntervalSet {
  private final SortedSet<Interval> choices;

  IntervalSet(Interval... choices) {
    this(new TreeSet<Interval>(Arrays.asList(choices)));
  }

  private IntervalSet(SortedSet<Interval> choices) {
    this.choices = choices;
  }  
  
  IntervalSet with(Interval choice) {
    SortedSet<Interval> newSet = new TreeSet<Interval>(choices);
    newSet.add(choice);
    return new IntervalSet(newSet);
  }

  IntervalSet without(Interval choice) {
    SortedSet<Interval> newSet = new TreeSet<Interval>(choices);
    newSet.remove(choice);
    return new IntervalSet(newSet);
  }

  IntervalSet intersectScale(Scale scale) {
    SortedSet<Interval> newSet = new TreeSet<Interval>();
    for (Interval choice : choices) {
      if (scale.containsAnywhere(choice)) {
        newSet.add(choice);
      }
    }        
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

  Collection<Interval> items() {
    return Collections.unmodifiableSet(choices);
  }

  Interval getSmallest() {
    return choices.first();
  }
  
  Interval getSecondSmallest() {
    Iterator<Interval> it = choices.iterator();
    it.next();
    return it.next();
  }

  Interval getLargest() {
    return choices.last();
  }
}
