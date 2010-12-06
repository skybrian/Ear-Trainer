// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of the current state of the quizz and handles transitions between states.
 */
class Quizzer {
  private final QuestionChooser chooser;
  private final SequencePlayer player;
  private final ScoreKeeper scoreKeeper;

  private Question currentQuestion;
  private int currentInterval;
  private final IntervalChoices choices;
  private final List<Interval> answers;

  private final List<Runnable> answerChosenListeners;

  Quizzer(QuestionChooser chooser, IntervalChoices choices, SequencePlayer player,
      ScoreKeeper scoreKeeper) {
    this.chooser = chooser;
    this.choices = choices;
    this.scoreKeeper = scoreKeeper;
    this.player = player;
    this.answers = new ArrayList<Interval>();
    this.answerChosenListeners = new ArrayList<Runnable>();
  }

  void addAnswerChosenListener(Runnable callback) {
    this.answerChosenListeners.add(callback);
  }

  // state transition handlers

  /**
   * Starts the next question. (If any question is in progress, skip it.)
   */
  void startQuestion() throws UnavailableException {
    currentQuestion = chooser.chooseQuestion();
    currentInterval = 0;
    choices.reset(currentQuestion.getChoices());
    answers.clear();
    playQuestion();
  }

  void playQuestion() throws UnavailableException {
    currentQuestion.play(player);
  }  
  
  /**
   * Checks an answer to the current question. If correct, asks the next question.
   * Updates the score if needed.
   * @param candidate  the answer that the user wants to try (must be ascending)
   */
  void checkAnswer(Interval candidate) throws UnavailableException {
    if (!candidate.isAscending()) {
      throw new IllegalArgumentException("got non-ascending interval");
    }
    if (!hasPlayerAnsweredForThisInterval()) {
      answers.add(candidate);  
    }
    if (currentQuestion.isCorrect(candidate, currentInterval)) {
      currentInterval++;
      if (currentInterval >= currentQuestion.getAnswerCount()) {
        scoreKeeper.addResult(currentQuestion, answers);
        startQuestion();
      } else {
        choices.reset(currentQuestion.getChoices());        
      }
    } else {
      choices.removeChoice(candidate);
      playQuestion();
    }
    for (Runnable listener : answerChosenListeners) {
      listener.run();
    }
  }

  // queries
  
  boolean isStarted() {
    return currentQuestion != null;
  }

  private boolean hasPlayerAnsweredForThisInterval() {
    return answers.size() > currentInterval;
  }  
  
  String getQuestionText() {
    int intervalStartNote = currentInterval + 1;
    return "What's the difference in pitch between notes " + intervalStartNote +
        " and " + (intervalStartNote + 1) + "?";
  }

  String getPhraseSoFar() {
    List<Interval> phrase = 
        currentQuestion.getPhrase().getIntervals().subList(0, currentInterval);
    StringBuilder result = new StringBuilder();
    for (Interval interval : phrase) {
      result.append(interval.getName());
      result.append(", ");
    }
    return result.toString();
  }
}
