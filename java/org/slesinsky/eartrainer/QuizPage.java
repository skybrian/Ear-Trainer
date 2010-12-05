// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Provides a method to construct the Quiz Page UI.
 */
class QuizPage {
  static final IntervalSet BLACK_KEYS = IntervalSet.forHalfSteps(1, 3, 6, 8, 10);

  static JComponent create(AnswerChoices choices, QuestionChooser chooser,
      ScoreKeeper scoreKeeper, Quizzer quizzer) {
    return makeVerticalPage(
        makeHeader(quizzer),
        makeAnswerBar(quizzer),
        makeAnswerButtonGrid(chooser, quizzer, choices),
        makeFooter(chooser, scoreKeeper));
  }

  private static JComponent makeVerticalPage(JComponent... sections) {
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
    leftSide.add(makeScaleChooserWidget(chooser));
    leftSide.add(makeNoteCountWidget(chooser));
    leftSide.add(makeNoteDirectionWidget(chooser));
    leftSide.setAlignmentX(Component.LEFT_ALIGNMENT);
    leftSide.setAlignmentY(Component.BOTTOM_ALIGNMENT);
    footer.add(leftSide);

    footer.add(Box.createHorizontalGlue());
    footer.add(makeScoreWidget(scoreKeeper));
    return footer;
  }

  private static JComponent makeScaleChooserWidget(final QuestionChooser chooser) {
    final DefaultComboBoxModel model = new DefaultComboBoxModel(ScaleMenuItem.values());
    model.setSelectedItem(ScaleMenuItem.find(chooser.getScale()));

    JComboBox combo = new JComboBox(model) {
      @Override
      public Dimension getMaximumSize() {
        return getPreferredSize();
      }
    };
    
    combo.addActionListener(new SimpleAction("change scale") {
      @Override
      void act() throws UnavailableException {
        chooser.setScale(((ScaleMenuItem)model.getSelectedItem()).getScale());  
      }
    });
    
    Box result = Box.createHorizontalBox();
    result.add(new JLabel("Scale:"));
    result.add(makeSpacer());    
    result.add(combo);
    result.setAlignmentX(Component.LEFT_ALIGNMENT);
    return result;
  }

  private static JComponent makeNoteCountWidget(final QuestionChooser chooser) {

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

    Box result = Box.createHorizontalBox();
    result.add(new JLabel("Notes in next phrase:"));
    result.add(makeSpacer());
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

  private static class DirectionToggleButton {
    private int choice = 0;
    private final JButton button;

    DirectionToggleButton(final QuestionChooser chooser) {
      choice = chooser.getDirectionSet().ordinal();
      button = new JButton(Interval.DirectionSet.values()[choice].getLabel());
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent actionEvent) {
          choice++;
          if (choice >= Interval.DirectionSet.values().length) {
            choice = 0;
          }
          Interval.DirectionSet newDir = Interval.DirectionSet.values()[choice];
          chooser.setDirectionSet(newDir);
          button.setText(newDir.getLabel());
        }
      });
    }
  }

  static enum ScaleMenuItem {
    PENTATONIC("Pentatonic", Scale.MAJOR_PENTATONIC),
    BLUES("Blues", Scale.BLUES),
    MAJOR("Major / Natural Minor", Scale.MAJOR),
    MINOR("Harmonic Minor", Scale.HARMONIC_MINOR),
    CHROMATIC("Chromatic", Scale.CHROMATIC);
  
    private final String label;
    private final Scale scale;
  
    ScaleMenuItem(String label, Scale scale) {
      this.label = label;
      this.scale = scale;
    }
  
    String getLabel() {
      return label;
    }
  
    Scale getScale() {
      return scale;
    }

    @Override
    public String toString() {
      return label;
    }

    static ScaleMenuItem find(Scale scale) {
      for (ScaleMenuItem item : values()) {
        if (item.scale.equals(scale)) {
          return item;
        }
      }
      return null;
    }
  }
}
