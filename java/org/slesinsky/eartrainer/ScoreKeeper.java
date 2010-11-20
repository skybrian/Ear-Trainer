// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.Formatter;

/**
 * Keeps track of how many questions the user answered correctly.
 */
class ScoreKeeper {
  private int numRight = 0;
  private int numWrong = 0;
  private Runnable scoreChangeListener;

  void reset() {
    numRight = 0;
    numWrong = 0;
    scoreChangeListener.run();
  }

  void addResult(boolean correct) {
    if (correct) {
      numRight++;
    } else {
      numWrong++;
    }
    scoreChangeListener.run();
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

  void addScoreChangeListener(Runnable listener) {
    this.scoreChangeListener = listener;
  }
}
