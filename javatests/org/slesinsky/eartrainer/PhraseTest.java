// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Verifies calculations with Phrases.
 */
public class PhraseTest extends TestCase {

  public void testGetScale() throws Exception {
    checkGetScale("100001000000", Interval.PERFECT_FOURTH);
    checkGetScale("100001010000", Interval.PERFECT_FOURTH, Interval.MAJOR_SECOND);
    checkGetScale("100000010000", Interval.PERFECT_FOURTH.reverse());
  }

  public void testCanTransposeToScale() throws Exception {
    checkCanTransposeToScale(true, new Scale("100001000000"), Interval.PERFECT_FOURTH);
    checkCanTransposeToScale(true, new Scale("010000100000"), Interval.PERFECT_FOURTH);
    checkCanTransposeToScale(true, new Scale("001000010000"), Interval.PERFECT_FOURTH);
    checkCanTransposeToScale(false, new Scale("110000000000"), Interval.PERFECT_FOURTH);
    checkCanTransposeToScale(true, Scale.MAJOR, Interval.PERFECT_FOURTH);
    checkCanTransposeToScale(true, Scale.MAJOR, Interval.PERFECT_FOURTH, Interval.MAJOR_SECOND);
    checkCanTransposeToScale(true, Scale.MAJOR, Interval.PERFECT_FOURTH, Interval.MINOR_SECOND);
    checkCanTransposeToScale(false, Scale.MAJOR, Interval.MINOR_SECOND, Interval.MINOR_SECOND);
  }
  
  private void checkGetScale(String expected, Interval... intervals) {
    Phrase phrase = new Phrase(Arrays.asList(intervals));
    assertEquals(expected, phrase.getScale().getBitString());
  }

  private void checkCanTransposeToScale(boolean expected, Scale scale, Interval... intervals) {
    Phrase phrase = new Phrase(Arrays.asList(intervals));
    assertEquals(expected, phrase.canTransposeToScale(scale));
  }

}
