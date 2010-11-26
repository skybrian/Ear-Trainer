// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * Provides a method to construct the score page UI.
 */
class ScorePage {
  static JComponent create(final ScoreKeeper scoreKeeper) {

    JTable table = new JTable(new PhraseScoreTableModel(scoreKeeper));

    Box page = Box.createVerticalBox();
    page.add(new JScrollPane(table));
    return page;
  }

  private static class PhraseScoreTableModel extends AbstractTableModel {
    private List<ScoreKeeper.PhraseScore> scores;

    PhraseScoreTableModel(final ScoreKeeper scoreKeeper) {
      scores = scoreKeeper.getPhraseScores();
      scoreKeeper.addScoreChangeListener(new Runnable() {
        public void run() {
          scores = scoreKeeper.getPhraseScores();
          fireTableDataChanged();
        }
      });
    }

    public int getColumnCount() {
      return 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
      switch (columnIndex) {
        case 0:
          return "Phrase";
        case 1:
          return "Right";
        case 2:
          return "Wrong";
        default:
          return "";
      }
    }

    public int getRowCount() {
      return scores.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      ScoreKeeper.PhraseScore row = this.scores.get(rowIndex);
      switch (columnIndex) {
        case 0:
          return renderPhrase(row.getPhrase());
        case 1:
          return row.getNumRight();
        case 2:
          return row.getNumWrong();
        default:
          return "";
      }
    }

    private String renderPhrase(Phrase phrase) {
      StringBuilder result = new StringBuilder();
      for (Interval interval : phrase.getIntervals()) {
        if (result.length() > 0) {
          result.append(" ");
        }
        result.append(interval.getShortName());
      }
      return result.toString();
    }
  }
}
