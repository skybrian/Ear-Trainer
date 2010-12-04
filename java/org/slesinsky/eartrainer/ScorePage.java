// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
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

    PhraseTableModel model = new PhraseTableModel(scoreKeeper);

    final JTable table = new JTable(model);
    table.setDefaultRenderer(ScoreKeeper.PhraseRow.class, new PhraseRenderer());
    table.setDefaultEditor(ScoreKeeper.PhraseRow.class, new PhraseEditor(player));
    table.getColumn("Right").setMaxWidth(50);
    table.getColumn("Wrong").setMaxWidth(50);

    model.addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e) {
        TableCellEditor editor = table.getCellEditor();
        if (editor != null) {
          editor.cancelCellEditing();
        }
      }
    });
    
    Box page = Box.createVerticalBox();
    page.add(new JScrollPane(table));
    return page;
  }

  private static class PhraseTableModel extends AbstractTableModel {
    private List<ScoreKeeper.PhraseRow> rows;

    PhraseTableModel(final ScoreKeeper scoreKeeper) {
      rows = scoreKeeper.getPhraseRows();
      scoreKeeper.addScoreChangeListener(new Runnable() {
        public void run() {
          rows = scoreKeeper.getPhraseRows();
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
          return ScoreKeeper.PhraseRow.class;
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
      return rows.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      ScoreKeeper.PhraseRow row = this.rows.get(rowIndex);
      switch (columnIndex) {
        case 0:
          return row;
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
      cell.setPhrases((ScoreKeeper.PhraseRow)value);
      return cell.getComponent();
    }
  }
  
  private static class PhraseEditor extends AbstractCellEditor implements TableCellEditor {
    private final PhraseCell cell;
    private ScoreKeeper.PhraseRow currentRow;

    PhraseEditor(SequencePlayer player) {
      cell = new PhraseCell();
      cell.setPlayer(player);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, 
        int row, int column) {
      currentRow = (ScoreKeeper.PhraseRow) value;
      cell.setPhrases(currentRow);
      return cell.getComponent();
    }

    public Object getCellEditorValue() {
      return currentRow;      
    }
  }

  private static class PhraseCell {
    private final JButton button;
    private ScoreKeeper.PhraseRow row;

    PhraseCell() {
      button = new JButton();
      button.setHorizontalAlignment(SwingConstants.LEFT);
    }

    void setPhrases(ScoreKeeper.PhraseRow row) {
      this.row = row;
      button.setText(renderPhrase(row.getPhrase()));
    }

    public void setPlayer(final SequencePlayer player) {
      button.setAction(new SimpleAction("Play") {
        @Override
        void act() throws UnavailableException {
          row.play(player);
        }
      });
    }
    
    JComponent getComponent() {
      return button;
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
