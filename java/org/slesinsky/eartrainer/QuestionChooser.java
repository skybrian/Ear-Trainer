// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.sound.midi.Sequence;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A configurable source of randomly generated musical questions.
 */
class QuestionChooser {
  private static final int MIDDLE_C = 60;
  private static final int LOWEST_NOTE =
      MIDDLE_C - Interval.OCTAVE.getHalfSteps() - Interval.PERFECT_FIFTH.getHalfSteps();
  private static final int HIGHEST_NOTE =
      MIDDLE_C + Interval.OCTAVE.getHalfSteps() + Interval.PERFECT_FIFTH.getHalfSteps();

  private static final IntervalSet DEFAULT_INTERVALS_IN_PHRASE =
      new IntervalSet(Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH);
  private static final int DEFAULT_NOTES_IN_PHRASE = 2;
  private static final Direction DEFAULT_DIRECTION = Direction.ASCENDING;

  private final Random randomness;

  private IntervalSet intervalChoices;
  private Direction direction;
  private int noteCount;

  QuestionChooser(Random randomness) {
    this.randomness = randomness;
    this.intervalChoices = DEFAULT_INTERVALS_IN_PHRASE;
    this.direction = DEFAULT_DIRECTION;
    this.noteCount = DEFAULT_NOTES_IN_PHRASE;
  }

  void setEnabled(Interval choice, boolean newValue) {
    if (newValue) {
      intervalChoices = intervalChoices.with(choice);
    } else {
      intervalChoices = intervalChoices.without(choice);
    }
  }

  boolean isEnabled(Interval interval) {
    return intervalChoices.contains(interval);
  }

  void setNoteCount(int newValue) {
    this.noteCount = newValue;
  }

  Direction getDirection() {
    return direction;
  }

  void setDirection(Direction newValue) {
    this.direction = newValue;
  }

  Question chooseQuestion() throws UnavailableException {
    int playRange = HIGHEST_NOTE - LOWEST_NOTE;
    int maxPhraseRange = Math.min(playRange, getLargestPhraseRange());
    while (true) {
      PhraseBuilder phrase = chooseRandomPhrase();
      if (phrase.getRange() <= maxPhraseRange) {
        int remainingRange = playRange - phrase.getRange();
        int lowNote = LOWEST_NOTE + randomness.nextInt(remainingRange + 1);
        int startNote = lowNote - phrase.getMinNote(0);
        Sequence prompt = makeSequence(phrase.getNotes(startNote));
        return new Question(prompt, intervalChoices, phrase.getIntervals());
      }
    }
  }

  /**
   * Returns the range of a phrase including the largest interval, the second smallest
   * interval, and padded out with the smallest interval.
   */
  private int getLargestPhraseRange() {
    Interval smallest = intervalChoices.getSmallest(1).toInterval();
    Interval secondSmallest = intervalChoices.getSmallest(2).getLargest(1).toInterval();
    Interval largest = intervalChoices.getLargest(1).toInterval();
    int largestPhraseRange = largest.getHalfSteps();
    if (noteCount > 2) {
      largestPhraseRange += secondSmallest.getHalfSteps();
    }
    for (int i = 3; i < noteCount; i++) {
      largestPhraseRange += smallest.getHalfSteps();
    }
    return largestPhraseRange;
  }

  private PhraseBuilder chooseRandomPhrase() {
    PhraseBuilder phrase = new PhraseBuilder(randomness);
    for (int i = 0; i < noteCount - 1; i++) {
      phrase.addRandomInterval(intervalChoices, direction);
    }
    return phrase;
  }

  private Sequence makeSequence(List<Integer> notes) throws UnavailableException {
    SequenceBuilder builder = new SequenceBuilder();
    for (int note : notes) {
      builder.addNote(note);
    }
    return builder.getSequence();
  }

  /**
   * The possible directions in which to create intervals to be added to a
   * question.
   */
  static enum Direction {
    ASCENDING("Up", true),
    DESCENDING("Down", false),
    BOTH("Both", true, false);

    private final String label;
    private final List<Boolean> choices;

    Direction(String label, Boolean... choices) {
      this.label = label;
      this.choices = Collections.unmodifiableList(Arrays.asList(choices));
    }

    String getLabel() {
      return label;
    }

    /**
     * Randomly chooses between ascending and descending, if available.
     * @return true if ascending
     */
    private boolean choose(Random randomness) {
      return Util.choose(randomness, choices);
    }
  }

  private static class PhraseBuilder {
    private final Random randomness;
    private final List<Interval> intervals = new ArrayList<Interval>();
    private final List<Boolean> isAscendingList = new ArrayList<Boolean>();

    PhraseBuilder(Random randomness) {
      this.randomness = randomness;
    }

    void addRandomInterval(
        IntervalSet intervalChoices,
        Direction direction) {
      intervals.add(intervalChoices.choose(randomness));
      isAscendingList.add(direction.choose(randomness));
    }

    public List<Interval> getIntervals() {
      return new ArrayList<Interval>(intervals);
    }

    List<Integer> getNotes(int startNote) {
      int note = startNote;
      List<Integer> result = new ArrayList<Integer>();
      result.add(note);
      for (int i = 0; i < intervals.size(); i++) {
        Interval interval = intervals.get(i);
        if (isAscendingList.get(i)) {
          note += interval.getHalfSteps();
        } else {
          note -= interval.getHalfSteps();
        }
        result.add(note);
      }
      return result;
    }

    int getMinNote(int startNote) {
      int lowest = startNote;
      for (int note : getNotes(startNote)) {
        lowest = Math.min(lowest, note);
      }
      return lowest;
    }

    int getMaxNote(int startNote) {
      int highest = 0;
      for (int note : getNotes(startNote)) {
        highest = Math.max(highest, note);
      }
      return highest;
    }

    int getRange() {
      return getMaxNote(0) - getMinNote(0);
    }
  }
}
