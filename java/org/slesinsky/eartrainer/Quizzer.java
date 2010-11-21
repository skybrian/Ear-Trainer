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

  private final AnswerChoices choices;
  private final List<Interval> chosenAnswers;
  private Question currentQuestion;
  private boolean correctSoFar = true;

  private final List<Runnable> answerChosenListeners;

  Quizzer(QuestionChooser chooser, AnswerChoices choices, SequencePlayer player,
      ScoreKeeper scoreKeeper) {
    this.chooser = chooser;
    this.choices = choices;
    this.scoreKeeper = scoreKeeper;
    this.player = player;
    this.chosenAnswers = new ArrayList<Interval>();
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
    correctSoFar = true;
    choices.reset(currentQuestion.getChoices());
    chosenAnswers.clear();
    repeatQuestion();
  }

  void repeatQuestion() throws UnavailableException {
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
    if (currentQuestion.isCorrect(candidate, chosenAnswers.size())) {
      choices.reset(currentQuestion.getChoices());
      chosenAnswers.add(candidate);
      if (chosenAnswers.size() >= currentQuestion.getAnswerCount()) {
        scoreKeeper.addResult(correctSoFar);
        startQuestion();
      }
    } else {
      correctSoFar = false;
      choices.removeChoice(candidate);
      repeatQuestion();
    }
    for (Runnable listener : answerChosenListeners) {
      listener.run();
    }
  }

  // queries
  
  boolean isStarted() {
    return currentQuestion != null;
  }

  String getQuestionText() {
    int intervalStartNote = chosenAnswers.size() + 1;
    return "What's the difference in pitch between notes " + intervalStartNote +
        " and " + (intervalStartNote + 1) + "?";
  }

  String getChosenAnswers() {
    StringBuilder result = new StringBuilder();
    for (Interval answer : chosenAnswers) {
      result.append(answer.getName());
      result.append(", ");
    }
    return result.toString();
  }
}
