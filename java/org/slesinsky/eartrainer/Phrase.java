// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.sound.midi.Sequence;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * A sequence of notes that may be played relative to any starting note.
 */
class Phrase implements Comparable<Phrase> {
  private final List<Interval> intervals;

  Phrase(Iterable<Interval> intervals) {
    this.intervals = new ArrayList<Interval>();
    for (Interval interval : intervals) {
      this.intervals.add(interval);
    }
  }

  List<Interval> getIntervals() {
    return intervals;
  }

  List<Integer> getNotes(int startNote) {
    List<Integer> result = new ArrayList<Integer>();
    result.add(startNote);
    for (Interval interval : intervals) {
      startNote += interval.getHalfSteps();
      result.add(startNote);
    }
    return result;
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

  void play(SequencePlayer player, int startNote) throws UnavailableException {
    player.play(makeSequence(startNote));
  }

  boolean containsIntervalsInOrder(List<Interval> ascendingIntervals) {
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

  boolean chosenFrom(IntervalSet ascendingIntervalSet) {
    for (Interval interval : getIntervals()) {
      if (!ascendingIntervalSet.contains(interval.toAscending())) {
        return false;
      }
    }
    return true;
  }  

  boolean chosenFrom(Interval.DirectionSet direction) {
    for (Interval interval : intervals) {
      if (!direction.contains(interval)) {
        return false;
      }
    }
    return true;
  }  
  
  @Override
  public int hashCode() {
    return intervals.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof Phrase)) {
      return false;
    }
    Phrase other = (Phrase) object;
    return intervals.equals(other.intervals);
  }

  public int compareTo(Phrase other) {
    if (intervals.size() != other.intervals.size()) {
      return intervals.size() < other.intervals.size() ? -1 : 1;
    }

    Iterator<Interval> it = intervals.iterator();
    Iterator<Interval> otherIt = other.intervals.iterator();
    while (it.hasNext() && otherIt.hasNext()) {
      int result = it.next().compareTo(otherIt.next());
      if (result != 0) {
        return result;
      }
    }

    return 0;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("Phrase(");
    boolean first = true;
    for (Interval interval : intervals) {
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

  private Sequence makeSequence(int startNote) throws UnavailableException {
    SequenceBuilder builder = new SequenceBuilder();
    for (int note : getNotes(startNote)) {
      builder.addNote(note);
    }
    return builder.getSequence();
  }
}
