// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.ArrayList;
import java.util.HashSet;
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
  static final int MAX_NOTES_IN_PHRASE = 5;
  
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
    
    if (choices.size() < MIN_CHOICES) {
    
      // add a few choices from newly generated answers  
      List<Phrase> generated = generatePhrases();
      if (generated.isEmpty()) {
        throw new UnavailableException("unable to generate any phrases with these settings");
      }
  
      while (choices.size() < MIN_CHOICES && !generated.isEmpty()) {
        Phrase candidate = Util.choose(randomness, generated);
        generated.remove(candidate);
        if (!choices.contains(candidate)) {
          choices.add(candidate);
        }
      }
    }
    Phrase phrase = Util.choose(randomness, choices);
    Integer startNote = phrase.chooseRandomStartNote(randomness, LOWEST_NOTE, HIGHEST_NOTE);
    if (startNote == null) {
      throw new RuntimeException("phrase should be in range: " + phrase);
    }
    return new Question(phrase, startNote, intervalFilter.intersectScale(scale));
  }

  /**
   * Returns the range of a phrase including the largest interval, the second smallest
   * interval, and padded out with the smallest interval.
   */
  private int getMaxPhraseRange() {
    Interval smallest = intervalFilter.getSmallest();
    Interval secondSmallest = intervalFilter.getSecondSmallest();
    Interval largest = intervalFilter.getLargest();
    int largestPhraseRange = largest.getHalfSteps();
    if (noteCount > 2 && secondSmallest != null) {
      largestPhraseRange += secondSmallest.getHalfSteps();
    }
    for (int i = MIN_CHOICES; i < noteCount; i++) {
      largestPhraseRange += smallest.getHalfSteps();
    }
    return Math.min(HIGHEST_NOTE - LOWEST_NOTE, largestPhraseRange);
  }

  private List<Phrase> generatePhrases() {
    if (intervalFilter.isEmpty()) {
      return new ArrayList<Phrase>();
    }

    Set<Phrase> result = new HashSet<Phrase>();

    IntervalFilter filter = intervalFilter.intersectScale(this.scale);
    for (Scale scale : this.scale.getRotations()) {
      generatePhrases(filter, getMaxPhraseRange(), scale.getTonic(), 
          new Phrase.Builder(), noteCount - 1, result);
    }
    
    System.err.println("phrase count: " + result.size());
    return new ArrayList<Phrase>(result);
  }

  private void generatePhrases(IntervalFilter filter, int maxPhraseRange, Scale.Note currentNote,
      Phrase.Builder currentPhrase, int remainingNotes, Set<Phrase> result) {

    if (currentPhrase.range() > maxPhraseRange) {
      return;  
    }

    if (remainingNotes == 0) {
      result.add(currentPhrase.build());
      return;
    }
    
    for (Interval interval : currentNote.generate(directionFilter, filter)) {
      currentPhrase.add(interval);
      generatePhrases(filter, maxPhraseRange, currentNote.add(interval), currentPhrase,
          remainingNotes - 1, result);
      currentPhrase.pop();
    }
  }
}
