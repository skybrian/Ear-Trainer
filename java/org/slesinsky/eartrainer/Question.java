// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

/**
 * A question that the ear trainer can ask the user. The question is a musical
 * phrase that was generated from a set of possible intervals.
 */
class Question {
  private final IntervalSet choices;
  private final Phrase phrase;

  Question(Phrase phrase, IntervalSet choices) {
    this.choices = choices;
    this.phrase = phrase;
  }

  void play(SequencePlayer player) throws UnavailableException {
    phrase.play(player);
  }

  boolean isCorrect(Interval candidate, int position) {
    Interval answer = phrase.getIntervals().get(position).toAscending();
    return answer.equals(candidate);
  }

  int getAnswerCount() {
    return phrase.getIntervals().size();
  }

  IntervalSet getChoices() {
    return choices;
  }
}
