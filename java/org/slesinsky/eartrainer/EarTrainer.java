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
import java.util.EnumSet;
import java.util.Collections;
import java.util.Arrays;

/**
 * TODO: javadoc
 */
public class EarTrainer {
  private static final int MIDDLE_C = 60;

  public static void main(String[] args) throws Exception {
    SequencePlayer player = new SequencePlayer();
    SequenceChooser chooser = new SequenceChooser();

    JFrame frame = new JFrame("Ear Trainer");

    JButton playButton = new JButton();
    playButton.setAction(new PlayAction(player, chooser));
    frame.getContentPane().add(playButton);

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
    private final SequenceChooser chooser;

    public PlayAction(SequencePlayer player, SequenceChooser chooser) {
      this.player = player;
      this.chooser = chooser;
      putValue(Action.NAME, "Play");
    }

    public void actionPerformed(ActionEvent actionEvent) {
      try {
        player.play(chooser.makeSequence());
      } catch (Exception e) {
        reportError(e);
      }
    }

    private static void reportError(Exception e) {
      Toolkit.getDefaultToolkit().beep();
      e.printStackTrace(System.err);
    }
  }

  static class SequenceChooser {
    private static int LOWEST_START = MIDDLE_C - Interval.Octave.semitones;
    private static int HIGHEST_START = MIDDLE_C + Interval.Octave.semitones;

    private Random random;

    SequenceChooser() {
      this.random = new Random();
    }

    public Sequence makeSequence() throws InvalidMidiDataException {
      SequenceBuilder builder = new SequenceBuilder();

      builder.addNote(chooseRandomNote(), 1);
      builder.addInterval(chooseRandomInterval(), chooseRandomDirection());
      return builder.getSequence();
    }

    private int chooseRandomNote() {
      return random.nextInt(HIGHEST_START - LOWEST_START + 1) + LOWEST_START;
    }

    private Interval chooseRandomInterval() {
      Interval[] choices = Interval.values();
      return choices[random.nextInt(choices.length)];
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

    void addQuarterNotes(int... notes) throws InvalidMidiDataException {
      for (int note : notes) {
        addNote(note, 1);
      }
    }

    void addInterval(Interval interval, boolean ascending) throws InvalidMidiDataException {
      int note = ascending ? lastNote + interval.semitones : lastNote - interval.semitones;
      addNote(note, 1);
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

    Sequence getSequence() {
      return sequence;
    }
  }

  static class SequencePlayer {
    private final Sequencer sequencer;

    SequencePlayer() throws MidiUnavailableException {
      sequencer = MidiSystem.getSequencer();
      sequencer.open();
      sequencer.setTempoInBPM(90);
    }

    void play(Sequence sequence) throws InvalidMidiDataException {
      sequencer.stop();
      sequencer.setSequence(sequence);
      sequencer.setMicrosecondPosition(0);
      sequencer.start();
    }
  }
}
