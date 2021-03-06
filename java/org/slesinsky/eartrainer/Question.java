// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

/**
 * A question that the ear trainer can ask the user. The question consists of a
 * musical phrase and a set of possible intervals to choose from.
 */
class Question {
  private final Phrase phrase;
  private final int startNote;
  private final IntervalFilter choices;

  Question(Phrase phrase, int startNote, IntervalFilter choices) {
    this.phrase = phrase;
    this.startNote = startNote;
    this.choices = choices;
  }

  void play(SequencePlayer player) throws UnavailableException {
    player.play(phrase, startNote);
  }

  boolean isCorrect(Interval candidate, int position) {
    Interval answer = phrase.getIntervals().get(position).toAscending();
    return answer.equals(candidate);
  }

  int getAnswerCount() {
    return phrase.getIntervals().size();
  }

  IntervalFilter getChoices() {
    return choices;
  }

  Phrase getPhrase() {
    return phrase;
  }

  int getStartNote() {
    return startNote;
  }
}
