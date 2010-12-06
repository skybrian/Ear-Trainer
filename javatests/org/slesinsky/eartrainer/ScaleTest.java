// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import junit.framework.TestCase;

import java.util.List;
import java.util.Set;

/**
 * Verifies calculations with scales.
 */
public class ScaleTest extends TestCase {

  public void testBitStringRoundTrip() throws Exception {
    checkRoundTrip("010000000000");
    checkRoundTrip("100000000000");
    checkRoundTrip("000000000001");
    checkRoundTrip("000000000000");
    checkRoundTrip("111111111111");
  }

  public void testRotate() throws Exception {
    checkRotations("100000000000", "100000000000", Interval.UNISON);
    checkRotations("100000000000", "010000000000", Interval.MINOR_SECOND);
    checkRotations("100000000000", "000000000001", Interval.MAJOR_SEVENTH);
    checkRotations("100000000000", "100000000000", Interval.OCTAVE);
    checkRotations("100000000000", "010000000000", Interval.OCTAVE.up());
    checkRotations("000000000001", "100000000000", Interval.MINOR_SECOND);
    checkRotations("000000000001", "000000000010", Interval.MINOR_SECOND.reverse());
    checkRotations("100000000000", "000000000001", Interval.MINOR_SECOND.reverse());
    checkRotations("000000000001", "100000000000", Interval.MAJOR_SEVENTH.reverse());
    checkRotations("000000000001", "000000000001", Interval.OCTAVE.reverse());
    checkRotations("000000000001", "000000000010", Interval.OCTAVE.up().reverse());
  }

  public void testGetRotations() throws Exception {
    checkScaleSet(new Scale("100000000000").getRotations(),
        "100000000000", 
        "010000000000",
        "001000000000",
        "000100000000", 
        "000010000000", 
        "000001000000",
        "000000100000", 
        "000000010000", 
        "000000001000", 
        "000000000100",
        "000000000010", 
        "000000000001" 
    );
    checkScaleSet(new Scale("101010101010").getRotations(),
        "101010101010", 
        "010101010101"
    );
    checkScaleSet(new Scale("111111111111").getRotations(),
        "111111111111" 
    );
  }

  public void testContainsPhrase() throws Exception {
    assertFalse(Scale.MAJOR_PENTATONIC.containsAnywhere(Interval.MINOR_SECOND));
    assertTrue(Scale.MAJOR_PENTATONIC.containsAnywhere(Interval.MAJOR_SECOND));
  }

  public void testGenerateIntervals() throws Exception {
    Scale.Note start = new Scale("100001010001").getTonic();
    IntervalFilter filter =
        new IntervalFilter(Interval.MAJOR_THIRD, Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH);

    List<Interval> result = start.generate(DirectionFilter.ASCENDING, filter);
    checkIntervalList(result, "P4^ P5^");

    result = start.generate(DirectionFilter.DESCENDING, filter);
    checkIntervalList(result, "P5v P4v");

    result = start.generate(DirectionFilter.BOTH, filter);
    checkIntervalList(result, "P5v P4v P4^ P5^");
  }

  // === end of tests ===
  
  private void checkRoundTrip(String bitString) {
    assertEquals(bitString, new Scale(bitString).getBitString());
  }

  private void checkRotations(String input, String expected, Interval interval) {
    assertEquals(expected, new Scale(input).rotate(interval).getBitString());
  }
   
  private void checkScaleSet(Set<Scale> candidate, String... expectedItems) {
    StringBuilder expected = new StringBuilder();
    for (String item : expectedItems) {
      expected.append(item + "\n");
    }
    StringBuilder actual = new StringBuilder();
    for (Scale item : candidate) {
      actual.append(item.getBitString() + "\n");
    }
    assertEquals(expected.toString(), actual.toString());    
  }

  private void checkIntervalList(List<Interval> candidate, String expected) {
    StringBuilder actual = new StringBuilder();
    for (Interval item : candidate) {
      if (actual.length() > 0) {
        actual.append(" ");
      }
      actual.append(item.getShortNameAscii());
    }
    
    assertEquals(expected, actual.toString());        
  }
}
