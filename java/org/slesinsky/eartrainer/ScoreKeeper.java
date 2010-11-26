// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Keeps track of which questions the user answered correctly.
 */
class ScoreKeeper {
  private int numRight = 0;
  private int numWrong = 0;
  private Map<Phrase, PhraseScore> phraseScores = new TreeMap<Phrase, PhraseScore>();
  private List<Runnable> scoreChangeListeners = new ArrayList<Runnable>();

  void reset() {
    numRight = 0;
    numWrong = 0;
    phraseScores.clear();
    fireChange();
  }

  void addResult(Phrase phrase, List<Interval> answer) {
    phrase = phrase.normalize();
    boolean isRight = phrase.getIntervals().equals(answer);
    if (isRight) {
      numRight++;
    } else {
      numWrong++;
    }
    PhraseScore score;
    if (!phraseScores.containsKey(phrase)) {
      score = new PhraseScore(phrase);
      phraseScores.put(phrase, score);
    } else {
      score = phraseScores.get(phrase);
    }
    score.addResult(isRight);
    fireChange();
  }

  int getTotal() {
    return numRight + numWrong;
  }

  String getScore() {
    int total = getTotal();
    if (total == 0) {
      return "";
    }
    double percent = (numRight * 100.0) / total;

    StringBuilder out = new StringBuilder();
    Formatter formatter = new Formatter(out);
    formatter.format("Score: %.0f%% (%d of %d)", percent, numRight, total);
    return out.toString();
  }

  List<PhraseScore> getPhraseScores() {
    return new ArrayList<PhraseScore>(phraseScores.values());    
  }
  
  void addScoreChangeListener(Runnable listener) {
    this.scoreChangeListeners.add(listener);
  }
  
  private void fireChange() {
    for (Runnable listener : scoreChangeListeners) {
      listener.run();
    }  
  }
  
  class PhraseScore {
    private final Phrase phrase;
    private int numRight;
    private int numWrong;

    PhraseScore(Phrase phrase) {
      this.phrase = phrase;
    }

    void addResult(boolean isRight) {
      if (isRight) {
        numRight++;
      } else {
        numWrong++;
      }
    }
    
    Phrase getPhrase() {
      return phrase;
    }
    
    int getNumRight() {
      return numRight;
    }
    
    int getNumWrong() {
      return numWrong;
    }
  }
}
