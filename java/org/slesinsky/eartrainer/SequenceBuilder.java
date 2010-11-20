// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Builds a Midi sequence that plays some notes.
 */
class SequenceBuilder {
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
