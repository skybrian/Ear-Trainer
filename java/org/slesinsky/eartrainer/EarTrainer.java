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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.Random;

/**
 * A swing app that plays a random interval and waits for the user to press the button
 * with its name.
 */
public class EarTrainer {
  private static final int MIDDLE_C = 60;
  private static final int LOWEST_START = MIDDLE_C - Interval.Octave.semitones;
  private static final int HIGHEST_START = MIDDLE_C + Interval.Octave.semitones;
  private static final int BEATS_PER_MINUTE = 90;

  public static void main(String[] args) throws UnavailableException {
    SequencePlayer player = new SequencePlayer();
    QuestionChooser chooser = new QuestionChooser(new Random());
    Quizzer quizzer = new Quizzer(chooser, player);

    startUI(quizzer);
    quizzer.nextQuestion();
  }

  // ============= Swing UI ===============

  private static void startUI(Quizzer quizzer) {

    Box page = Box.createVerticalBox();
    page.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    Box header = Box.createHorizontalBox();
    header.add(new JLabel("Choose the interval matching the notes that you heard."));
    header.add(new JButton(new PlayAction(quizzer)));
    page.add(header);

    page.add(Box.createVerticalStrut(16));

    page.add(makeAnswerButtonGrid(quizzer));

    JFrame frame = new JFrame("Ear Trainer");
    frame.getContentPane().add(page);

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocation(100, 10);
    frame.setVisible(true);
  }

  /**
   * Creates a grid with an answer button for each interval,
   * with buttons corresponding to black keys (ascending from C)
   * on the left.
   */
  private static JPanel makeAnswerButtonGrid(Quizzer quizzer) {
    JPanel intervals = new JPanel();
    intervals.setLayout(new GridLayout(8, 2));

    boolean isLeftColumn = true;
    for (Interval interval : Interval.values()) {
      if (isLeftColumn && !interval.isBlackKey()) {
        intervals.add(Box.createGlue());        
      } else {
        isLeftColumn = !isLeftColumn;
      }
      intervals.add(new JButton(new ChooseAnswerAction(quizzer, interval)));
    }
    return intervals;
  }

  static class PlayAction extends AbstractAction {
    private final Quizzer quizzer;

    PlayAction(Quizzer quizzer) {
      this.quizzer = quizzer;
      putValue(Action.NAME, "Play It Again");
    }

    public void actionPerformed(ActionEvent actionEvent) {
      try {
        quizzer.playQuestion();
      } catch (UnavailableException e) {
        reportError(e);
      }
    }
  }

  static class ChooseAnswerAction extends AbstractAction {
    private final Quizzer quizzer;
    private final Interval interval;

    public ChooseAnswerAction(Quizzer quizzer, Interval interval) {
      this.quizzer = quizzer;
      this.interval = interval;
      putValue(Action.NAME, interval.toString());
    }

    public void actionPerformed(ActionEvent actionEvent) {
      try {
        quizzer.chooseAnswer(interval);
      } catch (UnavailableException e) {
        reportError(e);
      }
    }
  }

  private static void reportError(Exception e) {
    Toolkit.getDefaultToolkit().beep();
    e.printStackTrace(System.err);
  }

  // ============= Rules of the Game ==============

  static class Quizzer {
    private final QuestionChooser chooser;
    private final SequencePlayer player;
    private Question question;

    Quizzer(QuestionChooser chooser, SequencePlayer player) {
      this.chooser = chooser;
      this.player = player;
    }

    void nextQuestion() throws UnavailableException {
      this.question = chooser.choose();
      playQuestion();
    }

    void playQuestion() throws UnavailableException {
      question.play(player);
    }

    void chooseAnswer(Interval answer) throws UnavailableException {
      if (question.matches(answer)) {
        nextQuestion();
      } else {
        playQuestion();
      }
    }
  }

  enum Interval {
    Unison(0),
    MinorSecond(1),
    MajorSecond(2),
    MinorThird(3),
    MajorThird(4),
    PerfectFourth(5),
    Tritone(6),
    PerfectFifth(7),
    MinorSixth(8),
    MajorSixth(9),
    MinorSeventh(10),
    MajorSeventh(11),
    Octave(12)
    ;

    private final int semitones;

    Interval(int semitones) {
      this.semitones = semitones;
    }

    boolean isBlackKey() {
      return this==Tritone || name().startsWith("Minor");
    }
  }

  static class Question {
    private final Interval interval;
    private final Sequence sequence;

    Question(Interval interval, Sequence sequence) {
      this.interval = interval;
      this.sequence = sequence;
    }

    void play(SequencePlayer player) throws UnavailableException {
      player.play(sequence);
    }

    boolean matches(Interval otherInterval) {
      return interval == otherInterval;
    }

    @Override
    public String toString() {
      return interval.toString();
    }
  }

  static class QuestionChooser {
    private final Random random;

    QuestionChooser(Random random) {
      this.random = random;
    }

    Question choose() throws UnavailableException {
      Interval interval = chooseRandomInterval();
      Sequence sequence = chooseRandomSequence(interval);
      return new Question(interval, sequence);
    }

    private Interval chooseRandomInterval() {
      Interval[] choices = Interval.values();
      return choices[random.nextInt(choices.length)];
    }

    private Sequence chooseRandomSequence(Interval interval) throws UnavailableException {
      SequenceBuilder builder = new SequenceBuilder();
      builder.addNote(chooseRandomNote());
      builder.addNote(interval, chooseRandomDirection());
      return builder.getSequence();
    }

    private int chooseRandomNote() {
      return random.nextInt(HIGHEST_START - LOWEST_START + 1) + LOWEST_START;
    }

    private boolean chooseRandomDirection() {
      return random.nextBoolean();
    }
  }

  // ================== Midi Mechanics ====================

  static class SequenceBuilder {
    private static final int CHANNEL = 4;
    private static final int VELOCITY = 90;

    private final Sequence sequence;
    private Track track;
    private int currentBeat = 0;
    private int lastNote = MIDDLE_C;

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
      lastNote = note;
    }

    /** Adds a note relative to the previous one. */
    void addNote(Interval interval, boolean ascending) throws UnavailableException {
      int note = ascending ? lastNote + interval.semitones : lastNote - interval.semitones;
      addNote(note);
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
