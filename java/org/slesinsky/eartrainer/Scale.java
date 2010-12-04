// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.Set;
import java.util.TreeSet;

/**
 * A set of up to 12 notes to choose from, relative to a starting note.
 * For example, major, minor, pentatonic, dorian, etc. 
 */
class Scale implements Comparable<Scale> {
  private static final int OCTAVE = 12;
  private static final int ALL_BITS = (1 << OCTAVE) - 1;

  static Scale MAJOR = new Scale("101011010101");
  static Scale MINOR = MAJOR.rotate(Interval.MINOR_THIRD.invert());
  static Scale MAJOR_PENTATONIC = new Scale("101010010100");
  static Scale MINOR_PENTATONIC = MAJOR.rotate(Interval.MINOR_THIRD.invert()); 
  
  // bits 0 to 11 may be set to indicate ascending intervals that may be played.
  // (0 is the tonic.  This is the reverse of the bitString.)
  private final int bits;

  private Scale(int bits) {
    this.bits = bits;
  }

  /**
   * Creates the corresponding Scale for a bit string.
   * @param bitString a string of length 12, containing a "1" for notes in the
   * scale and a "0" for notes not in the scale, from lowest note to highest
   * note. The lowest note is considered the tonic.
   */
  Scale(String bitString) {
    if (bitString.length() != OCTAVE) {
      throw new IllegalArgumentException("bad bit string: " + bitString);        
    }
    int bits = 0;
    for (int i = 0; i < OCTAVE; i++) {
      switch (bitString.charAt(i)) {
        case '1':
          bits |= (1 << i);
          break;
        case '0':
          break;
        default:
          throw new IllegalArgumentException("bad bit string: " + bitString);
      }
    }
    this.bits = bits;
  }

  Scale rotate(Interval interval) {
    int halfSteps = Util.modulus(interval.getHalfSteps(), OCTAVE);
    return new Scale((bits << halfSteps | bits >>> (OCTAVE - halfSteps)) & ALL_BITS);  
  }

  Set<Scale> getRotations() {
    Set<Scale> result = new TreeSet<Scale>();
    int bits = this.bits;
    while (result.add(new Scale(bits))) {
      bits = rotateLeft(bits);
    } 
    return result;
  }

  String getBitString() {
    StringBuilder result = new StringBuilder();
    int bits = this.bits;
    for (int i = 0; i < OCTAVE; i++) {
      boolean isSet = (bits & 1) == 1;
      result.append(isSet ? "1" : "0");  
      bits = bits >>> 1;
    }
    return result.toString();
  }

  @Override
  public int hashCode() {
    return bits;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof Scale) && bits == ((Scale)obj).bits;
  }

  @Override
  public String toString() {
    return "Scale(" + getBitString() + ")";
  }

  public int compareTo(Scale other) {
    if (bits == other.bits) {
      return 0;
    } else {
      return bits < other.bits ? -1 : 1;
    }
  }

  private static int rotateLeft(int bits) {
    return (bits << 1 | bits >>> 11) & ALL_BITS;
  }
}
