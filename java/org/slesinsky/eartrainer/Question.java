// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

/**
 * A question that the ear trainer can ask the user. The question consists of a
 * musical phrase and a set of possible intervals to choose from.
 */
class Question {
  private final Phrase phrase;
  private final IntervalSet choices;

  Question(Phrase phrase, IntervalSet choices) {
    this.phrase = phrase;
    this.choices = choices;
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

  Phrase getPhrase() {
    return phrase;
  }
}
