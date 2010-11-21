// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.sound.midi.Sequence;
import java.util.ArrayList;
import java.util.List;

/**
 * A sequence of notes.
 */
public class Phrase {
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
}
