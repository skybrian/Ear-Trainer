// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.ArrayList;
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
    int maxPhraseRange = Math.min(HIGHEST_NOTE - LOWEST_NOTE, getLargestPhraseRange());
    while (true) {
      Phrase phrase = chooseRandomPhrase();
      if (phrase.getRange() > maxPhraseRange) {
        continue;
      }
      phrase = phrase.transposeRandomly(randomness, LOWEST_NOTE, HIGHEST_NOTE);
      if (phrase == null) {
        continue;        
      }
      return new Question(phrase, intervalChoices);
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

  private Phrase chooseRandomPhrase() {
    PhraseBuilder phrase = new PhraseBuilder(randomness);
    for (int i = 0; i < noteCount - 1; i++) {
      phrase.addRandomInterval(intervalChoices, direction);
    }
    return phrase.build(0);
  }

  /**
   * The possible directions in which to create intervals to be added to a
   * question.
   */
  static enum Direction {
    ASCENDING("Up"),
    DESCENDING("Down"),
    BOTH("Both");

    private final String label;

    Direction(String label) {
      this.label = label;
    }

    String getLabel() {
      return label;
    }
  }

  private static class PhraseBuilder {
    private final Random randomness;
    private final List<Interval> intervals = new ArrayList<Interval>();

    PhraseBuilder(Random randomness) {
      this.randomness = randomness;
    }

    void addRandomInterval(
        IntervalSet intervalChoices,
        Direction direction) {
      Interval interval = intervalChoices.choose(randomness);
      if (direction == Direction.DESCENDING ||
          (direction == Direction.BOTH && randomness.nextBoolean())) {
        interval = interval.invert();        
      }
      intervals.add(interval);
    }

    Phrase build(int startNote) {
      return new Phrase(startNote, intervals);
    }
  }
}
