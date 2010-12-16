// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.ArrayList;
import java.util.BitSet;
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
  private Phrase lastPhrase;

  void reset() {
    numRight = 0;
    numWrong = 0;
    phraseScores.clear();
    lastPhrase = null;
    fireChange();
  }

  void addResult(Question question, List<Interval> answer) {
    Phrase key = question.getPhrase();
    lastPhrase = key;
    boolean isRight = key.containsIntervalsInOrder(answer);
    if (isRight) {
      numRight++;
    } else {
      numWrong++;
    }
    
    PhraseRow row;
    if (!phraseScores.containsKey(key)) {
      row = new PhraseRow(key);
      phraseScores.put(key, row);
    } else {
      row = phraseScores.get(key);
    }
    row.addResult(question.getStartNote(), isRight);
    fireChange();
  }

  int getTotal() {
    return numRight + numWrong;
  }

  // returns normalized phrases
  Set<Phrase> getPhrasesWithWinningStreakLessThan(int range) {
    Set<Phrase> result = new HashSet<Phrase>();
    for (PhraseRow row : phraseScores.values()) {
      if (row.getNumTries() < range || row.getNumWrong(range) > 0) {
        result.add(row.getPhrase());  
      }
    }
    return result;
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
    private final Phrase phrase;
    private final List<Integer> startNotes = new ArrayList<Integer>();
    private final BitSet wasAnswerCorrect = new BitSet();
    private int lastStartNote = -1;
    
    PhraseRow(Phrase phrase) {
      this.phrase = phrase;
    }
    
    void addResult(int startNote, boolean wasRight) {
      startNotes.add(startNote);
      if (wasRight) {
        wasAnswerCorrect.set(getNumTries() - 1);
      }
    }

    Phrase getPhrase() {
      return phrase;
    }

    public int getNumTries() {
      return startNotes.size();
    }
    
    int getNumRight() {
      return wasAnswerCorrect.cardinality();
    }
    
    int getNumWrong() {
      return startNotes.size() - getNumRight();
    }
    
    int getNumWrong(int range) {
      int result = 0;
      for (int i = getNumTries() - 1; i >= 0 && range > 0; i--, range--) {
        if (!wasAnswerCorrect.get(i)) {
          result++;
        }
      }
      return result;
    }
    
    public void play(SequencePlayer player) throws UnavailableException {
      if (startNotes.size() == 0) {
        phrase.play(player, 60);
        return;
      }
      
      lastStartNote++;
      if (lastStartNote >= startNotes.size()) {
        lastStartNote = 0;
      }
      phrase.play(player, startNotes.get(lastStartNote));
    }
  }
}
