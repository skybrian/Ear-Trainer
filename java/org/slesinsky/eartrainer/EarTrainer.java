// Copyright 2009 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Random;

/**
 * A Swing app that quizzes the user to identify each interval in a random phrase.
 * The user can choose the length of the phrase and the intervals that might appear 
 * in it.
 */
public class EarTrainer {
  private static final IntervalSet BLACK_KEYS = IntervalSet.forHalfSteps(1,3,6,8,10);

  public static void main(String[] args) throws UnavailableException {
    App app = makeApp();
    JFrame frame = makeWindow(app.getPage());
    frame.setVisible(true);
    app.start();
  }

  public static App makeApp() throws UnavailableException {
    AnswerChoices choices = new AnswerChoices();
    QuestionChooser chooser = new QuestionChooser(new Random());
    SequencePlayer player = new SequencePlayer();
    ScoreKeeper scoreKeeper = new ScoreKeeper();
    Quizzer quizzer = new Quizzer(chooser, choices, player, scoreKeeper);
    JComponent quizPage = makeQuizPage(
      makeHeader(quizzer),
      makeAnswerBar(quizzer),
      makeAnswerButtonGrid(chooser, quizzer, choices),
      makeFooter(chooser, scoreKeeper));

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Quiz", quizPage);
    tabs.addTab("Scores", makeScoresPage(scoreKeeper));
    
    return new App(tabs, quizzer, player);
  }

  public static class App {
    private final JComponent page;
    private final Quizzer quizzer;
    private final SequencePlayer player;

    App(JComponent page, Quizzer quizzer, SequencePlayer player) {
      this.page = page;
      this.quizzer = quizzer;
      this.player = player;
    }

    public JComponent getPage() {
      return page;
    }

    public void start() throws UnavailableException {
      if (!quizzer.isStarted()) {
        quizzer.startQuestion();
      }
    }

    public void shutdown() {
      player.shutdown();
    }
  }

  private static JFrame makeWindow(JComponent content) {
    JFrame frame = new JFrame("Ear Trainer");
    frame.getContentPane().add(content);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocation(100, 10);
    return frame;
  }

  private static JComponent makeQuizPage(JComponent... sections) {
    Box page = Box.createVerticalBox();
    page.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    for (int i = 0; i < sections.length; i++) {
      page.add(sections[i]);
      if (i < sections.length - 1) {
        page.add(Box.createVerticalStrut(16));
      }
    }
    return page;
  }

  private static JComponent makeHeader(final Quizzer quizzer) {
    SimpleAction play = new SimpleAction("Play it again") {
      @Override
      void act() throws UnavailableException {
        quizzer.playQuestion();
      }
    };

    SimpleAction skip = new SimpleAction("Skip") {
      @Override
      void act() throws UnavailableException {
        quizzer.startQuestion();
      }
    };

    Box header = Box.createHorizontalBox();
    
    final JLabel questionLabel = new JLabel(quizzer.getQuestionText());
    header.add(questionLabel);
    header.add(makeSpacer());
    final JButton playButton = new JButton(play);
    header.add(playButton);
    header.add(new JButton(skip));
    header.add(Box.createHorizontalGlue());

    quizzer.addAnswerChosenListener(new Runnable() {
      public void run() {
        questionLabel.setText(quizzer.getQuestionText());
        playButton.requestFocusInWindow();
      }
    });

    return header;
  }

  private static JComponent makeAnswerBar(final Quizzer quizzer) {
    Box box = Box.createHorizontalBox();
    final JLabel answerLabel = new JLabel();
    quizzer.addAnswerChosenListener(new Runnable() {
      public void run() {
        answerLabel.setText(quizzer.getPhraseSoFar());
      }
    });
    box.add(answerLabel);
    box.add(Box.createHorizontalGlue());
    box.add(Box.createRigidArea(new Dimension(0, 20)));
    return box;
  }

  /**
   * Creates a grid with an answer button for each interval,
   * with buttons corresponding to black keys (ascending from C)
   * on the left.
   */
  private static JPanel makeAnswerButtonGrid(QuestionChooser chooser, Quizzer quizzer,
      AnswerChoices choices) {
    JPanel intervals = new JPanel();
    intervals.setOpaque(false);
    intervals.setLayout(new GridLayout(8, 2));

    boolean isLeftColumn = true;
    for (Interval interval : AnswerChoices.ALL.each()) {
      if (isLeftColumn && !BLACK_KEYS.contains(interval)) {
        intervals.add(Box.createGlue());        
      } else {
        isLeftColumn = !isLeftColumn;
      }
      intervals.add(makeAnswerCell(interval, chooser, quizzer, choices));
    }
    return intervals;
  }

  private static JComponent makeAnswerCell(final Interval interval, final QuestionChooser chooser,
      final Quizzer quizzer, final AnswerChoices choices) {

    JCheckBox checkBox = new JCheckBox(new AbstractAction() {
      public void actionPerformed(ActionEvent actionEvent) {
        JCheckBox box = (JCheckBox) actionEvent.getSource();
        chooser.setEnabled(interval, box.isSelected());
      }
    });
    checkBox.setSelected(chooser.isEnabled(interval));

    final JButton chooseButton = new JButton(new SimpleAction(interval.getName()) {
      @Override
      void act() throws UnavailableException {
        quizzer.checkAnswer(interval);
      }
    });
    chooseButton.setPreferredSize(new Dimension(150, 40));

    choices.putChangeListener(interval, new Runnable() {
      public void run() {
        boolean enabled = choices.canChoose(interval);
        chooseButton.setEnabled(enabled);
      }
    });

    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BorderLayout());
    panel.add(checkBox, BorderLayout.WEST);
    panel.add(chooseButton, BorderLayout.CENTER);

    return panel;
  }

  private static JComponent makeFooter(QuestionChooser chooser, ScoreKeeper scoreKeeper) {
    Box footer = Box.createHorizontalBox();

    Box leftSide = Box.createVerticalBox();
    leftSide.add(makeNoteCountWidget(chooser));
    leftSide.add(makeNoteDirectionWidget(chooser));
    leftSide.setAlignmentY(Component.BOTTOM_ALIGNMENT);
    footer.add(leftSide);

    footer.add(Box.createHorizontalGlue());
    footer.add(makeScoreWidget(scoreKeeper));
    return footer;
  }

  private static JComponent makeScoresPage(final ScoreKeeper scoreKeeper) {

    JTable table = new JTable(new PhraseScoreTableModel(scoreKeeper));

    Box page = Box.createVerticalBox();
    page.add(new JScrollPane(table));
    return page;
  }  
  
  static class DirectionToggleButton {
    private int choice = 0;
    private final JButton button;

    DirectionToggleButton(final QuestionChooser chooser) {
      choice = chooser.getDirection().ordinal();
      button = new JButton(QuestionChooser.Direction.values()[choice].getLabel());
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent actionEvent) {
          choice++;
          if (choice >= QuestionChooser.Direction.values().length) {
            choice = 0;
          }
          QuestionChooser.Direction newDir = QuestionChooser.Direction.values()[choice];
          chooser.setDirection(newDir);
          button.setText(newDir.getLabel());
        }
      });
    }
  }

  private static JComponent makeNoteCountWidget(final QuestionChooser chooser) {
    Box result = Box.createHorizontalBox();
    result.add(new JLabel("Notes in next phrase:"));
    result.add(makeSpacer());

    final SpinnerNumberModel model = new SpinnerNumberModel(2, 2, 99, 1);
    model.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent changeEvent) {
        chooser.setNoteCount(model.getNumber().intValue());
      }
    });
    JSpinner spinner = new JSpinner(model) {
      @Override
      public Dimension getMaximumSize() {
        return getPreferredSize();
      }
    };
    result.add(spinner);
    result.setAlignmentX(Component.LEFT_ALIGNMENT);
    return result;
  }

  private static JComponent makeNoteDirectionWidget(QuestionChooser chooser) {
    Box result = Box.createHorizontalBox();
    result.add(new JLabel("Direction: "));
    result.add(new DirectionToggleButton(chooser).button);
    result.setAlignmentX(Component.LEFT_ALIGNMENT);
    return result;
  }

  private static JComponent makeScoreWidget(final ScoreKeeper scoreKeeper) {
    final JButton resetButton = new JButton(new SimpleAction("Reset") {
      @Override
      void act() throws UnavailableException {
        scoreKeeper.reset();
      }
    });
    resetButton.setVisible(false);

    final JLabel scoreBox = new JLabel("");
    scoreKeeper.addScoreChangeListener(new Runnable() {
      public void run() {
        scoreBox.setText(scoreKeeper.getScore());
        resetButton.setVisible(scoreKeeper.getTotal() > 0);
      }
    });

    Box result = Box.createHorizontalBox();
    result.add(scoreBox);
    result.add(resetButton);
    result.setAlignmentY(Component.BOTTOM_ALIGNMENT);

    return result;
  }

  private static Component makeSpacer() {
    return Box.createRigidArea(new Dimension(4, 0));
  }

  static abstract class SimpleAction extends AbstractAction {
    SimpleAction(String name) {
      putValue(Action.NAME, name);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      try {
        act();
      } catch (UnavailableException e) {
        Toolkit.getDefaultToolkit().beep();
        e.printStackTrace(System.err);
      }
    }

    abstract void act() throws UnavailableException;
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
        case 0: return "Phrase";
        case 1: return "Right";
        case 2: return "Wrong";
        default: return "";
      }
    }

    public int getRowCount() {
      return scores.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      ScoreKeeper.PhraseScore row = this.scores.get(rowIndex);
      switch (columnIndex) {
        case 0: return renderPhrase(row.getPhrase());
        case 1: return row.getNumRight();
        case 2: return row.getNumWrong();
        default: return "";
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
