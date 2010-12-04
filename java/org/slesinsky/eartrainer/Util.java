// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Generic functions.
 */
class Util {
  
  static <T> T choose(Random randomness, Collection<T> choices) {
    List<T> answers = new ArrayList<T>(choices);
    return answers.get(randomness.nextInt(answers.size()));
  }

  static int modulus(int x, int n) {
    int result = x % n;
    return result < 0 ? result + n : result;
  }
}
