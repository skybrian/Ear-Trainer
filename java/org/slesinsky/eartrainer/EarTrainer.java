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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
 * with its name.
 */
public class EarTrainer {
  private static final int MIDDLE_C = 60;
  private static final int LOWEST_STARTING_NOTE = Interval.Octave.subtractFromNote(MIDDLE_C);
  private static final int HIGHEST_STARTING_NOTE = Interval.Octave.addToNote(MIDDLE_C);
  private static final int BEATS_PER_MINUTE = 90;

  public static void main(String[] args) throws UnavailableException {

    // assembly
    AnswerChoices choices = new AnswerChoices();
    QuestionChooser chooser = new QuestionChooser(choices, new Random());
    SequencePlayer player = new SequencePlayer();
    Quizzer quizzer = new Quizzer(chooser, choices, player);
    JFrame frame = makeWindow(makePage(quizzer, choices));

    // startup
    frame.setVisible(true);
    quizzer.nextQuestion();
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

  private static JComponent makePage(Quizzer quizzer, AnswerChoices choices) {
    Box page = Box.createVerticalBox();
    page.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    page.add(makeHeader(quizzer));
    page.add(Box.createVerticalStrut(16));
    page.add(makeAnswerButtonGrid(quizzer, choices));
    return page;
  }

  private static JComponent makeHeader(final Quizzer quizzer) {
    Box header = Box.createHorizontalBox();
    header.add(new JLabel("What's the difference in pitch between these two notes?"));
    header.add(Box.createHorizontalStrut(4));
    SimpleAction playIt = new SimpleAction("Play it again") {
      @Override
      void act() throws UnavailableException {
        quizzer.playQuestion();
      }
    };
    final JButton playItButton = new JButton(playIt);
    quizzer.setAnswerChosenListener(new ChangeListener() {
      public void stateChanged(ChangeEvent changeEvent) {
        playItButton.requestFocusInWindow();
      }
    });
    header.add(playItButton);
    return header;
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

    choices.putEnabledAnswerListener(interval, new ChangeListener() {
      public void stateChanged(ChangeEvent changeEvent) {
        button.setEnabled(choices.isAnswerEnabled(interval));
      }
    });

    return button;
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

    int addToNote(int note, boolean ascending) {
      if (ascending) {
        return addToNote(note);
      } else {
        return subtractFromNote(note);
      }
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
    private final Interval answer;

    Question(Sequence prompt, Interval answer) {
      this.prompt = prompt;
      this.answer = answer;
    }

    void play(SequencePlayer player) throws UnavailableException {
      player.play(prompt);
    }

    boolean isCorrect(Interval answer) {
      return this.answer == answer;
    }
  }

  static class AnswerChoices {
    private final EnumSet<Interval> enabledAnswers;
    private final Map<Interval, ChangeListener> enabledAnswerListeners;

    AnswerChoices() {
      this.enabledAnswers = EnumSet.allOf(Interval.class);
      this.enabledAnswerListeners = new HashMap<Interval, ChangeListener>();
    }

    /**
     * Sets the listener to be called when an answer is enabled or disabled.
     */
    void putEnabledAnswerListener(Interval interval, ChangeListener newValue) {
      this.enabledAnswerListeners.put(interval, newValue);
    }

    public void disable(Interval answer) {
      enabledAnswers.remove(answer);
      enabledAnswerListeners.get(answer).stateChanged(new ChangeEvent(this));
    }

    void resetAllToEnabled() {
      for (Interval answerToEnable : Interval.values()) {
        enabledAnswers.add(answerToEnable);
        enabledAnswerListeners.get(answerToEnable).stateChanged(new ChangeEvent(this));
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
    private AnswerChoices choices;
    private final Random randomness;

    QuestionChooser(AnswerChoices choices, Random randomness) {
      this.choices = choices;
      this.randomness = randomness;
    }

    Question chooseQuestion() throws UnavailableException {
      Interval answer = chooseRandomAnswer();
      Sequence prompt = chooseRandomSequence(answer);
      return new Question(prompt, answer);
    }

    private Interval chooseRandomAnswer() {
      List<Interval> answers = choices.getAnswersToChooseFrom();
      return answers.get(randomness.nextInt(answers.size()));
    }

    private Sequence chooseRandomSequence(Interval interval) throws UnavailableException {
      SequenceBuilder builder = new SequenceBuilder();
      int firstNote = chooseRandomNote();
      builder.addNote(firstNote);
      if (chooseRandomDirection()) {
        builder.addNote(interval.addToNote(firstNote));
      } else {
        builder.addNote(interval.subtractFromNote(firstNote));
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
    private final AnswerChoices choices;
    private final SequencePlayer player;
    private Question currentQuestion;
    private ChangeListener answerChosenListener;

    Quizzer(QuestionChooser chooser, AnswerChoices choices, SequencePlayer player) {
      this.chooser = chooser;
      this.choices = choices;
      this.player = player;
    }
  
    public void setAnswerChosenListener(ChangeListener listener) {
      this.answerChosenListener = listener;
    }

    void nextQuestion() throws UnavailableException {
      currentQuestion = chooser.chooseQuestion();
      choices.resetAllToEnabled();
      playQuestion();
    }

    void playQuestion() throws UnavailableException {
      currentQuestion.play(player);
    }

    void chooseAnswer(Interval answer) throws UnavailableException {
      if (currentQuestion.isCorrect(answer)) {
        nextQuestion();
      } else {
        choices.disable(answer);
        playQuestion();
      }
      answerChosenListener.stateChanged(new ChangeEvent(this));
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
        sequencer.open();
        sequencer.setTempoInBPM(BEATS_PER_MINUTE);
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
