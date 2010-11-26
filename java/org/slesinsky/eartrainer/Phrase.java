// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.sound.midi.Sequence;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * A sequence of notes.
 */
class Phrase implements Comparable<Phrase> {
  private final int startNote;
  private final List<Interval> intervals;

  Phrase(int startNote, Iterable<Interval> intervals) {
    this.startNote = startNote;
    this.intervals = new ArrayList<Interval>();
    for (Interval interval : intervals) {
      this.intervals.add(interval);
    }
  }

  List<Integer> getNotes() {
    int note = startNote;
    List<Integer> result = new ArrayList<Integer>();
    result.add(note);
    for (Interval interval : intervals) {
      note += interval.getHalfSteps();
      result.add(note);
    }
    return result;
  }

  int getMinNote() {
    int lowest = startNote;
    for (int note : getNotes()) {
      lowest = Math.min(lowest, note);
    }
    return lowest;
  }

  int getMaxNote() {
    int highest = startNote;
    for (int note : getNotes()) {
      highest = Math.max(highest, note);
    }
    return highest;
  }

  int getRange() {
    return getMaxNote() - getMinNote();
  }
  
  List<Interval> getIntervals() {
    return intervals;
  }

  /**
   * Randomly transposes this phrase to within the given range.
   * Returns null if the phrase doesn't fit within the range.
   */
  Phrase transposeRandomly(Random randomness, int lowestNote, int highestNote) {
    int maxPhraseRange = highestNote - lowestNote;
    if (getRange() > maxPhraseRange) {
      return null;
    }
    int remainingRange = maxPhraseRange - getRange();
    int newLowNote = lowestNote + randomness.nextInt(remainingRange + 1);
    int oldLowNote = getMinNote();
    return transpose(newLowNote - oldLowNote);
  }

  Phrase transpose(int halfSteps) {
    return new Phrase(startNote + halfSteps, intervals);
  }

  Phrase normalize() {
    return new Phrase(0, intervals);
  }

  void play(SequencePlayer player) throws UnavailableException {
    player.play(makeSequence());
  }
  
  private Sequence makeSequence() throws UnavailableException {
    SequenceBuilder builder = new SequenceBuilder();
    for (int note : getNotes()) {
      builder.addNote(note);
    }
    return builder.getSequence();
  }

  @Override
  public int hashCode() {
    return startNote ^ intervals.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof Phrase)) {
      return false;
    }
    Phrase other = (Phrase) object;
    return startNote == other.startNote && intervals.equals(other.intervals);
  }

  public int compareTo(Phrase other) {
    Iterator<Interval> it = intervals.iterator();
    Iterator<Interval> otherIt = other.intervals.iterator();
    while (it.hasNext() && otherIt.hasNext()) {
      int result = it.next().compareTo(otherIt.next());
      if (result != 0) {
        return result;
      }
    }
    if (it.hasNext() != otherIt.hasNext()) {
      return it.hasNext() ? 1 : -1;
    } 

    if (startNote != other.startNote) {
      return startNote < other.startNote ? -1 : 1;
    }
    return 0;
  }
}
