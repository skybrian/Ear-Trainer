// Copyright 2009 Brian Slesinsky

import org.slesinsky.eartrainer.EarTrainer;
import org.slesinsky.eartrainer.UnavailableException;

import javax.swing.JApplet;
import javax.swing.JLabel;
import java.awt.Color;

/**
 * Runs Ear Trainer as an applet.
 */
public class EarTrainerApplet extends JApplet {
  private EarTrainer.App app;

  @Override
  public void init() {
    try {
      app = EarTrainer.makeApp();
      getContentPane().add(app.getPage());
      getContentPane().setBackground(Color.WHITE);      
    } catch (UnavailableException e) {
      add(new JLabel("Cannot start: " + e.getMessage()));
    }
  }

  @Override
  public void start() {
    try {
      app.start();
    } catch (UnavailableException e) {
      add(new JLabel("Cannot start: " + e.getMessage()));
    }
  }

  @Override
  public void destroy() {
    app.shutdown();
    app = null;
  }
}
