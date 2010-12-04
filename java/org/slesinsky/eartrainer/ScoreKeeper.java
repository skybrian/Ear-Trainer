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

  void addResult(Question question, List<Interval> answer) {
    Phrase key = question.getPhrase();
    lastPhrase = key;
    boolean isRight = key.containsIntervalsInOrder(answer);
    if (isRight) {
      numRight++;
      lastWasWrong.remove(key);
    } else {
      numWrong++;
      lastWasWrong.add(key);
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
    private final Phrase phrase;
    private final List<Integer> startNotes = new ArrayList<Integer>();
    private int numRight;
    private int numWrong;
    private int lastStartNote = -1;
    
    PhraseRow(Phrase phrase) {
      this.phrase = phrase;
    }
    
    void addResult(int startNote, boolean isRight) {
      startNotes.add(startNote);
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
