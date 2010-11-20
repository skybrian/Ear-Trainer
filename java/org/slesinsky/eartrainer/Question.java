// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.sound.midi.Sequence;
import java.util.List;

/**
 * A question that the ear trainer can ask the user. The question is a musical
 * phrase that was generated from a set of possible intervals.
 */
class Question {
  private final Sequence prompt;
  private final IntervalSet choices;
  private final List<Interval> answer;

  Question(Sequence prompt, IntervalSet choices, List<Interval> answer) {
    this.prompt = prompt;
    this.choices = choices;
    this.answer = answer;
  }

  void play(SequencePlayer player) throws UnavailableException {
    player.play(prompt);
  }

  boolean isCorrect(Interval answer, int position) {
    return this.answer.get(position).equals(answer);
  }

  int getAnswerCount() {
    return answer.size();
  }

  IntervalSet getChoices() {
    return choices;
  }
}
