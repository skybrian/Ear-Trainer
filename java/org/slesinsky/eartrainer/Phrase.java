// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * A sequence of notes that may be played relative to any starting note.
 */
class Phrase implements Comparable<Phrase> {
  private final int[] intervals;

  Phrase(Collection<Interval> intervals) {
    this.intervals = new int[intervals.size()];
    int i = 0;
    for (Interval interval : intervals) {
      this.intervals[i++] = interval.getHalfSteps(); 
    }
  }

  List<Interval> getIntervals() {
    List<Interval> result = new ArrayList<Interval>();
    for (int interval : intervals) {
      result.add(new Interval(interval));
    }
    return result;
  }

  List<Integer> getNotes(int startNote) {
    List<Integer> result = new ArrayList<Integer>();
    result.add(startNote);
    for (int interval : intervals) {
      startNote += interval;
      result.add(startNote);
    }
    return result;
  }

  Scale getScale() {
    return new Scale(0, getNotes(0));
  }  

  boolean canTransposeToScale(Scale candidate) {
    return candidate.containsAnywhere(getScale());
  }

  int getRange() {
    return getMaxNote(0) - getMinNote(0);
  }

  /**
   * Returns a start note such that the all the notes in the phrase will be between lowestNote
   * and highestNote (inclusive). If the phrase cannot be played within this range, returns null.
   */
  Integer chooseRandomStartNote(Random randomness, int lowestNote, int highestNote) {
    int minStartNote = lowestNote - getMinNote(0);
    int maxStartNote = highestNote - getMaxNote(0);

    if (minStartNote > maxStartNote) {
      return null;
    } else {
      return minStartNote + randomness.nextInt(maxStartNote - minStartNote + 1);
    }
  }

  boolean containsIntervalsInOrder(List<Interval> ascendingIntervals) {
    List<Interval> intervals = getIntervals();
    if (ascendingIntervals.size() != intervals.size()) {
      return false;
    }
    for (int i = 0 ; i < intervals.size(); i++) {
      if (!intervals.get(i).toAscending().equals(ascendingIntervals.get(i))) {
        return false;        
      }      
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(intervals);
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof Phrase)) {
      return false;
    }
    Phrase other = (Phrase) object;
    return compareTo(other) == 0;
  }

  public int compareTo(Phrase other) {
    int size = intervals.length;
    int otherSize = other.intervals.length;
    if (size != otherSize) {
      return size < otherSize ? -1 : 1;
    }

    for (int i = 0; i < size; i++) {
      int here = intervals[i];
      int there = other.intervals[i];
      if (here != there) {
        return here < there ? -1 : 1;
      }
    }
    
    return 0;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("Phrase(");
    boolean first = true;
    for (Interval interval : getIntervals()) {
      if (!first) {
        result.append(" ");
      }
      result.append(interval);
      first = false;
    }
    result.append(")");
    return result.toString();
  }

  // === private methods ===
  
  private int getMinNote(int startNote) {
    int result = startNote;
    for (int note : getNotes(startNote)) {
      result = Math.min(result, note);
    }
    return result;
  }

  private int getMaxNote(int startNote) {
    int result = startNote;
    for (int note : getNotes(startNote)) {
      result = Math.max(result, note);
    }
    return result;
  }

  /**
   * Mutable variant of a phrase.
   */
  static class Builder {
    private final List<Interval> intervals = new ArrayList<Interval>();
    
    void add(Interval interval) {
      intervals.add(interval);    
    }
    
    void pop() {
      intervals.remove(intervals.size() - 1);
    }
  
    Phrase build() {
      return new Phrase(intervals);
    }

    int range() {
      int min = 0;
      int max = 0;
      int current = 0;
      for (Interval interval : intervals) {
        current += interval.getHalfSteps();
        min = Math.min(current, min);
        max = Math.max(current, max);
      }
      return max - min;
    }
  }
}
