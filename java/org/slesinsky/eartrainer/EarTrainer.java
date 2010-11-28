// Copyright 2009 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.util.Random;

/**
 * A Swing app that quizzes the user to identify each interval in a random phrase.
 * The user can choose the length of the phrase and the intervals that might appear 
 * in it.
 */
public class EarTrainer {

  public static void main(String[] args) throws UnavailableException {
    App app = makeApp();
    JFrame frame = makeWindow(app.getPage());
    frame.setVisible(true);
    app.start();
  }

  public static App makeApp() throws UnavailableException {
    AnswerChoices choices = new AnswerChoices();
    ScoreKeeper scoreKeeper = new ScoreKeeper();
    QuestionChooser chooser = new QuestionChooser(new Random(), scoreKeeper);
    SequencePlayer player = new SequencePlayer();
    Quizzer quizzer = new Quizzer(chooser, choices, player, scoreKeeper);
    JComponent quizPage = QuizPage.create(choices, chooser, scoreKeeper, quizzer);

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Quiz", quizPage);
    tabs.addTab("Scores", ScorePage.create(scoreKeeper, player));
    
    return new App(tabs, quizzer, player);
  }

  public static class App {
    private final JComponent page;
    private final Quizzer quizzer;
    private final SequencePlayer player;

    App(JComponent page, Quizzer quizzer, SequencePlayer player) {
      this.page = page;
      this.quizzer = quizzer;
      this.player = player;
    }

    public JComponent getPage() {
      return page;
    }

    public void start() throws UnavailableException {
      if (!quizzer.isStarted()) {
        quizzer.startQuestion();
      }
    }

    public void shutdown() {
      player.shutdown();
    }
  }

  private static JFrame makeWindow(JComponent content) {
    JFrame frame = new JFrame("Ear Trainer");
    frame.getContentPane().add(content);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocation(100, 10);
    return frame;
  }
}
