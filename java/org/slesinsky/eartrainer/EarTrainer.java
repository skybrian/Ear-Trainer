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
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;

/**
 * A swing app that plays a random interval when the button is pressed.
 */
public class EarTrainer {
  private static final int MIDDLE_C = 60;
  private static final int BEATS_PER_MINUTE = 90;

  public static void main(String[] args) throws Exception {
    SequencePlayer player = new SequencePlayer();
    IntervalChooser chooser = new IntervalChooser(new Random());

    JFrame frame = new JFrame("Ear Trainer");
    frame.getContentPane().add(new JButton(new PlayAction(player, chooser)));

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocation(100, 10);
    frame.setVisible(true);
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
  }

  static class PlayAction extends AbstractAction {
    private final SequencePlayer player;
    private final IntervalChooser chooser;

    PlayAction(SequencePlayer player, IntervalChooser chooser) {
      this.player = player;
      this.chooser = chooser;
      putValue(Action.NAME, "Play");
    }

    public void actionPerformed(ActionEvent actionEvent) {
      try {
        PlayableInterval interval = chooser.choose();
        interval.play(player);
        System.out.println(interval);
      } catch (Exception e) {
        reportError(e);
      }
    }

    private static void reportError(Exception e) {
      Toolkit.getDefaultToolkit().beep();
      e.printStackTrace(System.err);
    }
  }

  static class PlayableInterval {
    private final Interval interval;
    private final Sequence sequence;

    PlayableInterval(Interval interval, Sequence sequence) {
      this.interval = interval;
      this.sequence = sequence;
    }

    void play(SequencePlayer player) throws InvalidMidiDataException {
      player.play(sequence);
    }

    @Override
    public String toString() {
      return interval.toString();
    }
  }

  static class IntervalChooser {
    private static int LOWEST_START = MIDDLE_C - Interval.Octave.semitones;
    private static int HIGHEST_START = MIDDLE_C + Interval.Octave.semitones;

    private Random random;

    IntervalChooser(Random random) throws InvalidMidiDataException {
      this.random = random;
    }

    PlayableInterval choose() throws InvalidMidiDataException {
      Interval interval = chooseRandomInterval();
      Sequence sequence = chooseRandomSequence(interval);
      return new PlayableInterval(interval, sequence);
    }

    private Interval chooseRandomInterval() {
      Interval[] choices = Interval.values();
      return choices[random.nextInt(choices.length)];
    }

    private Sequence chooseRandomSequence(Interval interval) throws InvalidMidiDataException {
      SequenceBuilder builder = new SequenceBuilder();
      builder.addNote(chooseRandomNote(), 1);
      builder.addNote(interval, chooseRandomDirection(), 1);
      return builder.getSequence();
    }

    private int chooseRandomNote() {
      return random.nextInt(HIGHEST_START - LOWEST_START + 1) + LOWEST_START;
    }

    private boolean chooseRandomDirection() {
      return random.nextBoolean();
    }
  }

  static class SequenceBuilder {
    private static final int CHANNEL = 4;
    private static final int VELOCITY = 90;

    private final Sequence sequence;
    private Track track;
    private int currentBeat = 0;
    private int lastNote = MIDDLE_C;

    SequenceBuilder() throws InvalidMidiDataException {
      this.sequence = new Sequence(Sequence.PPQ, 1);
      this.track = sequence.createTrack();
    }

    void addNote(int note, int beats) throws InvalidMidiDataException {
      ShortMessage noteOn = new ShortMessage();
      noteOn.setMessage(ShortMessage.NOTE_ON, CHANNEL, note, VELOCITY);
      track.add(new MidiEvent(noteOn, currentBeat));

      ShortMessage noteOff = new ShortMessage();
      noteOff.setMessage(ShortMessage.NOTE_OFF, CHANNEL, note, VELOCITY);
      track.add(new MidiEvent(noteOff, currentBeat + beats));

      currentBeat += beats;
      lastNote = note;
    }

    /** Adds a note relative to the previous one. */
    void addNote(Interval interval, boolean ascending, int beats) throws InvalidMidiDataException {
      int note = ascending ? lastNote + interval.semitones : lastNote - interval.semitones;
      addNote(note, beats);
    }

    Sequence getSequence() {
      return sequence;
    }
  }

  static class SequencePlayer {
    private final Sequencer sequencer;

    SequencePlayer() throws MidiUnavailableException {
      sequencer = MidiSystem.getSequencer();
      sequencer.open();
      sequencer.setTempoInBPM(BEATS_PER_MINUTE);
    }

    void play(Sequence sequence) throws InvalidMidiDataException {
      sequencer.stop();
      sequencer.setSequence(sequence);
      sequencer.setMicrosecondPosition(0);
      sequencer.start();
    }
  }
}
