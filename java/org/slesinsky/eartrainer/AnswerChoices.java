// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Keeps track of the set of answers remaining to be chosen for the current question.
 */
class AnswerChoices {
  static final Iterable<Interval> ALL = Interval.range(Interval.UNISON, Interval.OCTAVE);

  private final Set<Interval> enabledAnswers;
  private final Set<Interval> wrongAnswers;
  private final Map<Interval, Runnable> answerListeners;

  AnswerChoices() {
    this.enabledAnswers = new TreeSet<Interval>();
    this.wrongAnswers = new TreeSet<Interval>();
    this.answerListeners = new HashMap<Interval, Runnable>();
  }

  void putChangeListener(Interval intervalToWatch, Runnable listener) {
    this.answerListeners.put(intervalToWatch, listener);
  }

  void reset(IntervalSet choices) {
    wrongAnswers.clear();
    enabledAnswers.clear();
    enabledAnswers.addAll(choices.items());
    for (Interval answer : ALL) {
      Runnable listener = answerListeners.get(answer);
      if (listener != null) {
        listener.run();
      }
    }
  }

  void removeChoice(Interval answerToRemove) {
    wrongAnswers.add(answerToRemove);
    answerListeners.get(answerToRemove).run();
  }

  boolean canChoose(Interval candidate) {
    return enabledAnswers.contains(candidate) && !wrongAnswers.contains(candidate);
  }
}
