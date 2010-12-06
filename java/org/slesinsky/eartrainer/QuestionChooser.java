// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.ArrayList;
import java.util.Collection;
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

  static final int DEFAULT_NOTES_IN_PHRASE = 2;

  private static final int MIN_CHOICES = 3;

  private final Random randomness;
  private final ScoreKeeper scoreKeeper;

  private Scale scale;
  private IntervalFilter intervalFilter;
  private DirectionFilter directionFilter;
  private int noteCount;

  QuestionChooser(Random randomness, ScoreKeeper scoreKeeper) {
    this.randomness = randomness;
    this.scoreKeeper = scoreKeeper;
    this.scale = Scale.DEFAULT;
    this.intervalFilter = IntervalFilter.DEFAULT;
    this.directionFilter = DirectionFilter.DEFAULT;
    this.noteCount = DEFAULT_NOTES_IN_PHRASE;
  }
  
  void setIntervalAllowed(Interval choice, boolean newValue) {
    if (newValue) {
      intervalFilter = intervalFilter.enable(choice);
    } else {
      intervalFilter = intervalFilter.disable(choice);
    }
  }

  public void setScale(Scale scale) {
    this.scale = scale;
  }  
  
  void setNoteCount(int newValue) {
    this.noteCount = newValue;
  }

  void setDirectionFilter(DirectionFilter newValue) {
    this.directionFilter = newValue;
  }

  Question chooseQuestion() throws UnavailableException {
    int maxPhraseRange = Math.min(HIGHEST_NOTE - LOWEST_NOTE, getLargestPhraseRange());
    while (true) {
      // repeat recently wrong answers, if still valid
      List<Phrase> choices = new ArrayList<Phrase>();
      Set<Phrase> candidates = scoreKeeper.getWrongPhrases();
      for (Phrase candidate : candidates) {
        if (candidate != scoreKeeper.getLastPhrase() && 
            candidate.getIntervals().size() + 1 == noteCount &&
            intervalFilter.allows(candidate) &&
            directionFilter.allows(candidate) &&
            candidate.canTransposeToScale(scale)) {
          choices.add(candidate);            
        }
      }
      for (int tries = 0; tries < 100 && choices.size() < MIN_CHOICES; tries++) {
        Phrase candidate = chooseRandomPhrase();
        if (!candidate.equals(scoreKeeper.getLastPhrase()) && 
            !choices.contains(candidate) &&
            candidate.canTransposeToScale(scale)) {
          choices.add(candidate);  
        }
      }
      Phrase phrase = Util.choose(randomness, choices);
      if (phrase.getRange() > maxPhraseRange) {
        continue;
      }
      Integer startNote = phrase.chooseRandomStartNote(randomness, LOWEST_NOTE, HIGHEST_NOTE);
      if (startNote == null) {
        continue;        
      }
      phrase = new Phrase(phrase.getIntervals());
      return new Question(phrase, startNote, intervalFilter.intersectScale(scale));
    }
  }

  /**
   * Returns the range of a phrase including the largest interval, the second smallest
   * interval, and padded out with the smallest interval.
   */
  private int getLargestPhraseRange() {
    Interval smallest = intervalFilter.getSmallest();
    Interval secondSmallest = intervalFilter.getSecondSmallest();
    Interval largest = intervalFilter.getLargest();
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
    List<Interval> intervals = new ArrayList<Interval>();
    
    IntervalFilter filter = intervalFilter.intersectScale(scale);
    for (int i = 0; i < noteCount - 1; i++) {
      Collection<Interval> choices = filter.generate(directionFilter);
      intervals.add(Util.choose(randomness, choices));
    }

    return new Phrase(intervals);
  }
}
