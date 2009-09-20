// Copyright 2009 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A swing app that plays a random interval and waits for the user to press the button
 * with the interval's name.
 */
public class EarTrainer {
  private static final int MIDDLE_C = 60;
  private static final int LOWEST_STARTING_NOTE = Interval.Octave.subtractFromNote(MIDDLE_C);
  private static final int HIGHEST_STARTING_NOTE = Interval.Octave.addToNote(MIDDLE_C);
  private static final int BEATS_PER_MINUTE = 80;

  public static void main(String[] args) throws UnavailableException {

    // assembly
    AnswerChoices choices = new AnswerChoices();
    QuestionChooser chooser = new QuestionChooser(choices, new Random());
    SequencePlayer player = new SequencePlayer();
    Quizzer quizzer = new Quizzer(chooser, choices, player);

    JComponent page = makePage(
        makeHeader(quizzer),
        makeAnswerBar(quizzer),
        makeAnswerButtonGrid(quizzer, choices),
        makeFooter(chooser));
    JFrame frame = makeWindow(page);

    // startup
    frame.setVisible(true);
    quizzer.startNewQuestion();
  }

  // ============= Swing UI ===============

  private static JFrame makeWindow(JComponent content) {
    JFrame frame = new JFrame("Ear Trainer");
    frame.getContentPane().add(content);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocation(100, 10);
    return frame;
  }

  private static JComponent makePage(JComponent... sections) {
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
        quizzer.playQuestionNotes();
      }
    };

    Box header = Box.createHorizontalBox();
    
    final JLabel questionLabel = new JLabel(quizzer.getQuestionText());
    header.add(questionLabel);
    header.add(makeSpacer());
    final JButton playButton = new JButton(play);
    header.add(playButton);
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
        answerLabel.setText(quizzer.getChosenAnswers());
      }
    });
    box.add(answerLabel);
    box.add(Box.createHorizontalGlue());
    return box;
  }

  /**
   * Creates a grid with an answer button for each interval,
   * with buttons corresponding to black keys (ascending from C)
   * on the left.
   */
  private static JPanel makeAnswerButtonGrid(final Quizzer quizzer, final AnswerChoices choices) {
    JPanel intervals = new JPanel();
    intervals.setLayout(new GridLayout(8, 2));

    boolean isLeftColumn = true;
    for (final Interval interval : Interval.values()) {
      if (isLeftColumn && !interval.isBlackKey()) {
        intervals.add(Box.createGlue());        
      } else {
        isLeftColumn = !isLeftColumn;
      }
      intervals.add(makeAnswerButton(quizzer, choices, interval));
    }
    return intervals;
  }

  private static JButton makeAnswerButton(final Quizzer quizzer, final AnswerChoices choices,
      final Interval interval) {

    final JButton button = new JButton(new SimpleAction(interval.getName()) {
      @Override
      void act() throws UnavailableException {
        quizzer.chooseAnswer(interval);
      }
    });
    button.setPreferredSize(new Dimension(150, 40));

    choices.putEnabledAnswerListener(interval, new Runnable() {
      public void run() {
        boolean enabled = choices.isAnswerEnabled(interval);
        button.setEnabled(enabled);
      }
    });

    return button;
  }

  private static JComponent makeFooter(final QuestionChooser chooser) {
    Box footer = Box.createHorizontalBox();

    footer.add(new JLabel("Notes in next phrase:"));
    footer.add(makeSpacer());

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
    footer.add(spinner);

    footer.add(Box.createHorizontalGlue());

    return footer;
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

  // =========== Music Theory ===========

  enum Interval {
    Unison, Minor_Second, Major_Second, Minor_Third, Major_Third, Perfect_Fourth,
    Tritone, Perfect_Fifth, Minor_Sixth, Major_Sixth, Minor_Seventh, Major_Seventh,
    Octave;

    String getName() {
      return toString().replace("_", " ");
    }

    boolean isBlackKey() {
      return this==Tritone || name().startsWith("Minor");
    }

    private int addToNote(int note) {
      return note + ordinal();
    }

    private int subtractFromNote(int note) {
      return note - ordinal();
    }
  }

  // ============= Rules of the Game ==============

  static class Question {
    private final Sequence prompt;
    private final List<Interval> answers;

    Question(Sequence prompt, List<Interval> answers) {
      this.prompt = prompt;
      this.answers = answers;
    }

    void play(SequencePlayer player) throws UnavailableException {
      player.play(prompt);
    }

    boolean isCorrect(Interval answer, int position) {
      return this.answers.get(position) == answer;
    }

    public int getAnswerCount() {
      return answers.size();
    }
  }

  static class AnswerChoices {
    private final EnumSet<Interval> enabledAnswers;
    private final Map<Interval, Runnable> enabledAnswerListeners;

    AnswerChoices() {
      this.enabledAnswers = EnumSet.allOf(Interval.class);
      this.enabledAnswerListeners = new HashMap<Interval, Runnable>();
    }

    /**
     * Sets the listener to be called when an answer is enabled or disabled.
     */
    void putEnabledAnswerListener(Interval interval, Runnable newValue) {
      this.enabledAnswerListeners.put(interval, newValue);
    }

    public void disable(Interval answer) {
      enabledAnswers.remove(answer);
      enabledAnswerListeners.get(answer).run();
    }

    void resetAllToEnabled() {
      for (Interval answerToEnable : Interval.values()) {
        enabledAnswers.add(answerToEnable);
        enabledAnswerListeners.get(answerToEnable).run();
      }
    }

    public List<Interval> getAnswersToChooseFrom() {
      return new ArrayList<Interval>(enabledAnswers);
    }

    boolean isAnswerEnabled(Interval answer) {
      return enabledAnswers.contains(answer);
    }
  }

  static class QuestionChooser {
    private final AnswerChoices choices;
    private final Random randomness;
    private int noteCount;

    QuestionChooser(AnswerChoices choices, Random randomness) {
      this.choices = choices;
      this.randomness = randomness;
      this.noteCount = 2;
    }

    void setNoteCount(int newValue) {
      this.noteCount = newValue;
    }

    Question chooseQuestion() throws UnavailableException {
      List<Interval> answers = chooseRandomAnswers();
      Sequence prompt = chooseRandomSequence(answers);
      return new Question(prompt, answers);
    }

    private List<Interval> chooseRandomAnswers() {
      List<Interval> answers = choices.getAnswersToChooseFrom();

      List<Interval> result = new ArrayList<Interval>();
      for (int i = 0; i < noteCount - 1; i++) {
        result.add(answers.get(randomness.nextInt(answers.size())));
      }
      return result;
    }

    private Sequence chooseRandomSequence(List<Interval> intervals) throws UnavailableException {
      SequenceBuilder builder = new SequenceBuilder();
      int note = chooseRandomNote();
      builder.addNote(note);

      for (Interval interval : intervals) {
        int nextNote;
        if (chooseRandomDirection()) {
          nextNote = interval.addToNote(note);
        } else {
          nextNote = interval.subtractFromNote(note);
        }
        builder.addNote(nextNote);
        note = nextNote;
      }
      return builder.getSequence();
    }

    private int chooseRandomNote() {
      int noteCount = HIGHEST_STARTING_NOTE - LOWEST_STARTING_NOTE + 1;
      return randomness.nextInt(noteCount) + LOWEST_STARTING_NOTE;
    }

    private boolean chooseRandomDirection() {
      return randomness.nextBoolean();
    }
  }

  static class Quizzer {
    private final QuestionChooser chooser;
    private final SequencePlayer player;

    private final AnswerChoices choices;
    private final List<Interval> chosenAnswers;
    private Question currentQuestion;

    private final List<Runnable> answerChosenListeners;

    Quizzer(QuestionChooser chooser, AnswerChoices choices, SequencePlayer player) {
      this.chooser = chooser;
      this.choices = choices;
      this.player = player;
      this.chosenAnswers = new ArrayList<Interval>();
      this.answerChosenListeners = new ArrayList<Runnable>();
    }

    void startNewQuestion() throws UnavailableException {
      currentQuestion = chooser.chooseQuestion();
      choices.resetAllToEnabled();
      chosenAnswers.clear();
      playQuestionNotes();
    }

    void addAnswerChosenListener(Runnable callback) {
      this.answerChosenListeners.add(callback);
    }

    String getQuestionText() {
      int intervalStartNote = chosenAnswers.size() + 1;
      return "What's the difference in pitch between notes " + intervalStartNote +
          " and " + (intervalStartNote + 1) + "?";
    }

    void playQuestionNotes() throws UnavailableException {
      currentQuestion.play(player);
    }

    void chooseAnswer(Interval answer) throws UnavailableException {
      if (currentQuestion.isCorrect(answer, chosenAnswers.size())) {
        choices.resetAllToEnabled();
        chosenAnswers.add(answer);
        if (chosenAnswers.size() >= currentQuestion.getAnswerCount()) {
          startNewQuestion();
        }
      } else {
        choices.disable(answer);
        playQuestionNotes();
      }
      for (Runnable listener : answerChosenListeners) {
        listener.run();
      }
    }

    public String getChosenAnswers() {
      StringBuilder result = new StringBuilder();
      for (Interval answer : chosenAnswers) {
        result.append(answer.getName());
        result.append(", ");
      }
      return result.toString();
    }
  }

  // ================== Midi Mechanics ====================

  static class SequenceBuilder {
    private static final int CHANNEL = 4;
    private static final int VELOCITY = 90;

    private final Sequence sequence;
    private Track track;
    private int currentBeat = 0;

    SequenceBuilder() throws UnavailableException {
      try {
        this.sequence = new Sequence(Sequence.PPQ, 1);
        this.track = sequence.createTrack();
      } catch (InvalidMidiDataException e) {
        throw new UnavailableException(e);
      }
    }

    void addNote(int note) throws UnavailableException {

      ShortMessage noteOn;
      ShortMessage noteOff;
      try {
        noteOn = new ShortMessage();
        noteOn.setMessage(ShortMessage.NOTE_ON, CHANNEL, note, VELOCITY);

        noteOff = new ShortMessage();
        noteOff.setMessage(ShortMessage.NOTE_OFF, CHANNEL, note, VELOCITY);
      } catch (InvalidMidiDataException e) {
        throw new UnavailableException(e);
      }

      track.add(new MidiEvent(noteOn, currentBeat));
      track.add(new MidiEvent(noteOff, currentBeat + 1));
      currentBeat += 1;
    }

    Sequence getSequence() {
      return sequence;
    }
  }

  static class SequencePlayer {
    private final Sequencer sequencer;

    SequencePlayer() throws UnavailableException {
      try {
        sequencer = MidiSystem.getSequencer();
        sequencer.setTempoInBPM(BEATS_PER_MINUTE);
        sequencer.open();
      } catch (MidiUnavailableException e) {
        throw new UnavailableException(e);
      }
    }

    void play(Sequence sequence) throws UnavailableException {
      try {
        sequencer.stop();
        sequencer.setSequence(sequence);
        sequencer.setMicrosecondPosition(0);
        sequencer.start();
      } catch (InvalidMidiDataException e) {
        throw new UnavailableException(e);
      }
    }
  }

  // ====== Generic Utilities =======

  static class UnavailableException extends Exception {
    UnavailableException(Throwable throwable) {
      super(throwable);
    }
  }
}
