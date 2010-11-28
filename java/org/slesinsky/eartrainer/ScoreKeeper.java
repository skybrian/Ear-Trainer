// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Keeps track of which questions the user answered correctly.
 */
class ScoreKeeper {
  private int numRight = 0;
  private int numWrong = 0;
  private Map<Phrase, PhraseRow> phraseScores = new TreeMap<Phrase, PhraseRow>();
  private List<Runnable> scoreChangeListeners = new ArrayList<Runnable>();
  // normalized
  private Set<Phrase> lastWasWrong = new HashSet<Phrase>();
  // normalized
  private Phrase lastPhrase;

  void reset() {
    numRight = 0;
    numWrong = 0;
    phraseScores.clear();
    lastWasWrong.clear();
    lastPhrase = null;
    fireChange();
  }

  void addResult(Phrase phrase, List<Interval> answer) {
    Phrase key = phrase.normalize();
    lastPhrase = key;
    boolean isRight = phrase.containsIntervalsInOrder(answer);
    if (isRight) {
      numRight++;
      lastWasWrong.remove(key);
    } else {
      numWrong++;
      lastWasWrong.add(key);
    }
    
    PhraseRow row;
    if (!phraseScores.containsKey(key)) {
      row = new PhraseRow();
      phraseScores.put(key, row);
    } else {
      row = phraseScores.get(key);
    }
    row.addResult(phrase, isRight);
    fireChange();
  }

  int getTotal() {
    return numRight + numWrong;
  }

  // returns normalized phrases
  Set<Phrase> getWrongPhrases() {
    return lastWasWrong;
  }

  // returns normalized phrase
  public Phrase getLastPhrase() {
    return lastPhrase;
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

  List<PhraseRow> getPhraseRows() {
    return new ArrayList<PhraseRow>(phraseScores.values());    
  }
  
  void addScoreChangeListener(Runnable listener) {
    this.scoreChangeListeners.add(listener);
  }
  
  private void fireChange() {
    for (Runnable listener : scoreChangeListeners) {
      listener.run();
    }  
  }

  class PhraseRow {
    private List<Phrase> phrases = new ArrayList<Phrase>();
    private int numRight;
    private int numWrong;

    void addResult(Phrase phrase, boolean isRight) {
      phrases.add(phrase);
      if (isRight) {
        numRight++;
      } else {
        numWrong++;
      }
    }
    
    List<Phrase> getPhrases() {
      return phrases;
    }
    
    int getNumRight() {
      return numRight;
    }
    
    int getNumWrong() {
      return numWrong;
    }
  }
}
