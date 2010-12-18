// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Plays a midi sequence.
 */
class SequencePlayer {
  private static final int BEATS_PER_MINUTE = 80;

  private final Sequencer sequencer;
  private final Synthesizer synth;
  private Sound sound;

  SequencePlayer() throws UnavailableException {
    try {
      sequencer = MidiSystem.getSequencer();
      
//      MidiDevice.Info device = sequencer.getDeviceInfo();
//      System.out.println("Sequencer: " + device.getName());
//      System.out.println("Version: " + device.getVersion());
//      System.out.println("Vendor: " + device.getVendor());
//      System.out.println("Description: " + device.getDescription());
//      System.out.println();
      
      sequencer.setTempoInBPM(BEATS_PER_MINUTE);
      sequencer.open();

      synth = MidiSystem.getSynthesizer();
      
//      System.out.println("Instruments:");
//      Instrument[] instruments = synth.getDefaultSoundbank().getInstruments();
//      for (Instrument inst : instruments) {
//        Patch patch = inst.getPatch();
//        System.out.println(inst.getName() + ": " + patch.getBank() + "." + patch.getProgram());  
//      } 
//      System.out.println();

    } catch (MidiUnavailableException e) {
      throw new UnavailableException(e);
    }
  }
  
  Sound[] getSounds() {
    List<Sound> result = new ArrayList<Sound>();
    for (Instrument instrument : synth.getDefaultSoundbank().getInstruments()) {
      if (instrument.getPatch().getBank() == 0) {
        result.add(new Sound(instrument));
      }
    }
    return result.toArray(new Sound[result.size()]);
  }

  Sound getDefaultSound() {
    return new Sound(synth.getDefaultSoundbank().getInstrument(new Patch(0, 4)));
  }


  public void setSound(Sound sound) {
    this.sound = sound;  
  }  
  
  void play(Phrase phrase, int startNote) throws UnavailableException {

    Sequence sequence = makeSequence(phrase, startNote);        
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

  private Sequence makeSequence(Phrase phrase, int startNote) throws UnavailableException {
    if (sound == null) {
      sound = getDefaultSound();
    }

    SequenceBuilder builder = new SequenceBuilder();
    builder.addProgramChange(sound);
    for (int note : phrase.getNotes(startNote)) {
      builder.addNote(note);
    }
    return builder.getSequence();
  }
}
