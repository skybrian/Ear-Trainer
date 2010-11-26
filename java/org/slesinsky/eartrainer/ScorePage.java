// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.util.List;

/**
 * Provides a method to construct the score page UI.
 */
class ScorePage {
  static JComponent create(ScoreKeeper scoreKeeper, SequencePlayer player) {

    JTable table = new JTable(new PhraseTableModel(scoreKeeper));
    table.setDefaultRenderer(Phrase.class, new PhraseRenderer());
    table.setDefaultEditor(Phrase.class, new PhraseEditor(player));
    table.getColumn("Right").setMaxWidth(50);
    table.getColumn("Wrong").setMaxWidth(50);
    
    Box page = Box.createVerticalBox();
    page.add(new JScrollPane(table));
    return page;
  }

  private static String renderPhrase(Phrase phrase) {
    StringBuilder result = new StringBuilder();
    for (Interval interval : phrase.getIntervals()) {
      if (result.length() > 0) {
        result.append(" ");
      }
      result.append(interval.getShortName());
    }
    return result.toString();
  }
  
  private static class PhraseTableModel extends AbstractTableModel {
    private List<ScoreKeeper.PhraseScore> scores;

    PhraseTableModel(final ScoreKeeper scoreKeeper) {
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
    public Class<?> getColumnClass(int columnIndex) {
      switch (columnIndex) {
        case 0:
          return Phrase.class;
        default:
          return Object.class;
      }
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

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 0;
    }

    public int getRowCount() {
      return scores.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      ScoreKeeper.PhraseScore row = this.scores.get(rowIndex);
      switch (columnIndex) {
        case 0:
          return row.getPhrase();
        case 1:
          return row.getNumRight();
        case 2:
          return row.getNumWrong();
        default:
          return "";
      }
    }
  }

  private static class PhraseRenderer implements TableCellRenderer {
    private final PhraseCell cell = new PhraseCell();
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
        boolean hasFocus, int row, int column) {
      cell.setPhrase((Phrase) value);
      return cell.getComponent();
    }
  }
  
  private static class PhraseEditor extends AbstractCellEditor implements TableCellEditor {
    private final PhraseCell cell;
    private Phrase phrase;
    
    PhraseEditor(SequencePlayer player) {
      cell = new PhraseCell();
      cell.setPlayer(player);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, 
        int row, int column) {
      phrase = (Phrase) value;
      cell.setPhrase(phrase);
      return cell.getComponent();
    }

    public Object getCellEditorValue() {
      return phrase;      
    }
  }

  private static class PhraseCell {
    private final JButton button;
    private Phrase phrase;

    PhraseCell() {
      button = new JButton();
      //button.setUI(new BasicButtonUI());
      button.setHorizontalAlignment(SwingConstants.LEFT);
    }

    void setPhrase(Phrase phrase) {
      this.phrase = phrase;
      button.setText(renderPhrase(phrase));
    }

    public void setPlayer(final SequencePlayer player) {
      button.setAction(new SimpleAction("Play") {
        @Override
        void act() throws UnavailableException {
          phrase.transpose(60).play(player);
        }
      });
    }
    
    JComponent getComponent() {
      return button;
    }
  }
}
