// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

/**
 * Plays a midi sequence.
 */
class SequencePlayer {
  private static final int BEATS_PER_MINUTE = 80;

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
      Profiler p = new Profiler();
      sequencer.stop();
      p.log("stopped sequencer");
      sequencer.setSequence(sequence);
      p.log("set sequence");
      sequencer.setMicrosecondPosition(0);
      p.log("set position");
      sequencer.start();
      p.log("started sequencer");
    } catch (InvalidMidiDataException e) {
      throw new UnavailableException(e);
    }
  }

  public void shutdown() {
    sequencer.stop();
    sequencer.close(); // kills background thread
  }
}
