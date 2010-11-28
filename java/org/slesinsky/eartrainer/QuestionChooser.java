// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
  private static final Interval.DirectionSet DEFAULT_DIRECTION = Interval.DirectionSet.ASCENDING;

  private static final int MIN_CHOICES = 3;

  private final Random randomness;
  private final ScoreKeeper scoreKeeper;

  private IntervalSet intervalChoices;
  private Interval.DirectionSet directionSet;
  private int noteCount;

  QuestionChooser(Random randomness, ScoreKeeper scoreKeeper) {
    this.randomness = randomness;
    this.scoreKeeper = scoreKeeper;
    this.intervalChoices = DEFAULT_INTERVALS_IN_PHRASE;
    this.directionSet = DEFAULT_DIRECTION;
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

  Interval.DirectionSet getDirectionSet() {
    return directionSet;
  }

  void setDirectionSet(Interval.DirectionSet newValue) {
    this.directionSet = newValue;
  }

  Question chooseQuestion() throws UnavailableException {
    int maxPhraseRange = Math.min(HIGHEST_NOTE - LOWEST_NOTE, getLargestPhraseRange());
    while (true) {
      // repeat recently wrong answers, if still valid
      List<Phrase> choices = new ArrayList<Phrase>();
      Set<Phrase> candidates = scoreKeeper.getWrongPhrases();
      System.err.println("candidates: " + candidates.size());
      for (Phrase candidate : candidates) {
        if (candidate != scoreKeeper.getLastPhrase() && 
            candidate.getIntervals().size() + 1 == noteCount &&
            candidate.chosenFrom(intervalChoices) &&
            candidate.chosenFrom(directionSet)) {
          choices.add(candidate);            
        }
      }
      System.err.println("repeats: " + choices.size());
      for (int tries = 0; tries < 10 && choices.size() < MIN_CHOICES; tries++) {
        Phrase candidate = chooseRandomPhrase();
        if (candidate != scoreKeeper.getLastPhrase() && !choices.contains(candidate)) {
          choices.add(candidate);  
        }
      }
      Phrase phrase = Util.choose(randomness, choices);
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
    for (int i = MIN_CHOICES; i < noteCount; i++) {
      largestPhraseRange += smallest.getHalfSteps();
    }
    return largestPhraseRange;
  }

  private Phrase chooseRandomPhrase() {
    PhraseBuilder phrase = new PhraseBuilder(randomness);
    for (int i = 0; i < noteCount - 1; i++) {
      phrase.addRandomInterval(intervalChoices, directionSet);
    }
    return phrase.build(0);
  }

  private static class PhraseBuilder {
    private final Random randomness;
    private final List<Interval> intervals = new ArrayList<Interval>();

    PhraseBuilder(Random randomness) {
      this.randomness = randomness;
    }

    void addRandomInterval(
        IntervalSet intervalChoices,
        Interval.DirectionSet directionChoices) {
      Interval interval = intervalChoices.choose(randomness);
      if (directionChoices == Interval.DirectionSet.DESCENDING ||
          (directionChoices == Interval.DirectionSet.BOTH && randomness.nextBoolean())) {
        interval = interval.invert();        
      }
      intervals.add(interval);
    }

    Phrase build(int startNote) {
      return new Phrase(startNote, intervals);
    }
  }
}
