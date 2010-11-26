// Copyright 2010 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

/**
 * An action with a name.
 */
abstract class SimpleAction extends AbstractAction {
  SimpleAction(String name) {
    putValue(Action.NAME, name);
  }

  public void actionPerformed(ActionEvent actionEvent) {
    try {
      act();
    } catch (UnavailableException e) {
      Toolkit.getDefaultToolkit().beep();
      e.printStackTrace(System.err);
    }
  }

  abstract void act() throws UnavailableException;
}
