// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Keeps track of the set of intervals remaining to be chosen for the current question.
 */
class IntervalChoices {
  private IntervalFilter enabledAnswers;
  private final Set<Interval> wrongAnswers;
  private final Map<Interval, Runnable> answerListeners;

  IntervalChoices() {
    this.enabledAnswers = new IntervalFilter();
    this.wrongAnswers = new TreeSet<Interval>();
    this.answerListeners = new HashMap<Interval, Runnable>();
  }

  void putChangeListener(Interval intervalToWatch, Runnable listener) {
    this.answerListeners.put(intervalToWatch, listener);
  }

  void reset(IntervalFilter choices) {
    enabledAnswers = choices;
    wrongAnswers.clear();
    for (Runnable listener : answerListeners.values()) {
      listener.run();
    }
  }

  void removeChoice(Interval answerToRemove) {
    wrongAnswers.add(answerToRemove);
    Runnable listener = answerListeners.get(answerToRemove);
    if (listener != null) {
      listener.run();
    }
  }

  boolean allows(Interval candidate) {
    return enabledAnswers.allows(candidate) && !wrongAnswers.contains(candidate);
  }
}
