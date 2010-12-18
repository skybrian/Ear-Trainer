// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.sound.midi.Instrument;

/**
 * A sound that can be used to play the phrase.
 */
class Sound {
  final String name;
  final int bank;
  final int program;

  public Sound(Instrument instrument) {
    this.name = instrument.getName();
    this.bank = instrument.getPatch().getBank();
    this.program = instrument.getPatch().getProgram();
  }

  @Override
  public String toString() {
    return name;
  }
}
