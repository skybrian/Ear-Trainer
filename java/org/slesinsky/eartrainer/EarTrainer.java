// Copyright 2009 Brian Slesinsky
package org.slesinsky.eartrainer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * A Swing app that quizzes the user to identify each interval in a random phrase.
 * The user can choose the length of the phrase and the intervals that might appear 
 * in it.
 */
public class EarTrainer {
  static final int MIDDLE_C = 60;

  static final Interval UNISON = new Interval(0);
  static final Interval PERFECT_FOURTH = new Interval(5);
  static final Interval PERFECT_FIFTH = new Interval(7);
  static final Interval OCTAVE = new Interval(12);

  private static final int LOWEST_NOTE = PERFECT_FIFTH.downFrom(OCTAVE.downFrom(MIDDLE_C));
  private static final int HIGHEST_NOTE = PERFECT_FIFTH.upFrom(OCTAVE.upFrom(MIDDLE_C));

  private static final IntervalSet CHOOSABLE_INTERVALS = IntervalSet.forRange(UNISON, OCTAVE);
  private static final IntervalSet BLACK_KEYS = IntervalSet.forHalfSteps(1,3,6,8,10);

  private static final IntervalSet DEFAULT_INTERVALS_IN_PHRASE =
      new IntervalSet(PERFECT_FOURTH, PERFECT_FIFTH);
  private static final int DEFAULT_NOTES_IN_PHRASE = 2;
  private static final Direction DEFAULT_DIRECTION = Direction.ASCENDING;

  private static final int BEATS_PER_MINUTE = 80;

  private static final Color BACKGROUND_COLOR = Color.WHITE;

  public static void main(String[] args) throws UnavailableException {
    App app = makeApp();
    JFrame frame = makeWindow(app.getPage());
    frame.setVisible(true);
    app.start();
  }

  // ============= Swing UI ===============

  public static App makeApp() throws UnavailableException {
    AnswerChoices choices = new AnswerChoices();
    QuestionChooser chooser = new QuestionChooser(new Random());
    SequencePlayer player = new SequencePlayer();
    ScoreKeeper scoreKeeper = new ScoreKeeper();
    Quizzer quizzer = new Quizzer(chooser, choices, player, scoreKeeper);
    JComponent page = makePage(
      makeHeader(quizzer),
      makeAnswerBar(quizzer),
      makeAnswerButtonGrid(chooser, quizzer, choices),
      makeFooter(chooser, scoreKeeper));
    return new App(page, quizzer, player);
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
        quizzer.startNewQuestion();
      }
    }

    public void shutdown() {
      player.shutdown();
    }
  }

  private static JFrame makeWindow(JComponent content) {
    JFrame frame = new JFrame("Ear Trainer");
    frame.getContentPane().setBackground(BACKGROUND_COLOR);
    frame.getContentPane().add(content);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocation(100, 10);
    return frame;
  }

  private static JComponent makePage(JComponent... sections) {
    Box page = Box.createVerticalBox();
    page.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    page.setBackground(BACKGROUND_COLOR);
    for (int i = 0; i < sections.length; i++) {
      page.add(sections[i]);
      if (i < sections.length - 1) {
        page.add(Box.createVerticalStrut(16));
      }
    }
    return page;
  }

  private static JComponent makeHeader(final Quizzer quizzer) {
    SimpleAction play = new SimpleAction("Play it again") {
      @Override
      void act() throws UnavailableException {
        quizzer.playQuestionNotes();
      }
    };

    SimpleAction skip = new SimpleAction("Skip") {
      @Override
      void act() throws UnavailableException {
        quizzer.startNewQuestion();
      }
    };

    Box header = Box.createHorizontalBox();
    
    final JLabel questionLabel = new JLabel(quizzer.getQuestionText());
    header.add(questionLabel);
    header.add(makeSpacer());
    final JButton playButton = new JButton(play);
    header.add(playButton);
    header.add(new JButton(skip));
    header.add(Box.createHorizontalGlue());

    quizzer.addAnswerChosenListener(new Runnable() {
      public void run() {
        questionLabel.setText(quizzer.getQuestionText());
        playButton.requestFocusInWindow();
      }
    });

    return header;
  }

  private static JComponent makeAnswerBar(final Quizzer quizzer) {
    Box box = Box.createHorizontalBox();
    final JLabel answerLabel = new JLabel();
    quizzer.addAnswerChosenListener(new Runnable() {
      public void run() {
        answerLabel.setText(quizzer.getChosenAnswers());
      }
    });
    box.add(answerLabel);
    box.add(Box.createHorizontalGlue());
    box.add(Box.createRigidArea(new Dimension(0, 20)));
    return box;
  }

  /**
   * Creates a grid with an answer button for each interval,
   * with buttons corresponding to black keys (ascending from C)
   * on the left.
   */
  private static JPanel makeAnswerButtonGrid(QuestionChooser chooser, Quizzer quizzer,
      AnswerChoices choices) {
    JPanel intervals = new JPanel();
    intervals.setBackground(BACKGROUND_COLOR);
    intervals.setLayout(new GridLayout(8, 2));

    boolean isLeftColumn = true;
    for (Interval interval : CHOOSABLE_INTERVALS.each()) {
      if (isLeftColumn && !BLACK_KEYS.contains(interval)) {
        intervals.add(Box.createGlue());        
      } else {
        isLeftColumn = !isLeftColumn;
      }
      intervals.add(makeAnswerCell(interval, chooser, quizzer, choices));
    }
    return intervals;
  }

  private static JComponent makeAnswerCell(final Interval interval, final QuestionChooser chooser,
      final Quizzer quizzer, final AnswerChoices choices) {

    JCheckBox checkBox = new JCheckBox(new AbstractAction() {
      public void actionPerformed(ActionEvent actionEvent) {
        JCheckBox box = (JCheckBox) actionEvent.getSource();
        chooser.setEnabled(interval, box.isSelected());
      }
    });
    checkBox.setSelected(chooser.isEnabled(interval));

    final JButton chooseButton = new JButton(new SimpleAction(interval.getName()) {
      @Override
      void act() throws UnavailableException {
        quizzer.chooseAnswer(interval);
      }
    });
    chooseButton.setPreferredSize(new Dimension(150, 40));

    choices.putAnswerChangeListener(interval, new Runnable() {
      public void run() {
        boolean enabled = choices.canChooseAnswer(interval);
        chooseButton.setEnabled(enabled);
      }
    });

    JPanel panel = new JPanel();
    panel.setBackground(BACKGROUND_COLOR);
    panel.setLayout(new BorderLayout());
    panel.add(checkBox, BorderLayout.WEST);
    panel.add(chooseButton, BorderLayout.CENTER);

    return panel;
  }

  private static JComponent makeFooter(QuestionChooser chooser, ScoreKeeper scoreKeeper) {
    Box footer = Box.createHorizontalBox();

    Box leftSide = Box.createVerticalBox();
    leftSide.add(makeNoteCountWidget(chooser));
    leftSide.add(makeNoteDirectionWidget(chooser));
    leftSide.setAlignmentY(Component.BOTTOM_ALIGNMENT);
    footer.add(leftSide);

    footer.add(Box.createHorizontalGlue());
    footer.add(makeScoreWidget(scoreKeeper));
    return footer;
  }

  static class DirectionToggleButton {
    private int choice = 0;
    private final JButton button;

    DirectionToggleButton(final QuestionChooser chooser) {
      choice = chooser.getDirection().ordinal();
      button = new JButton(Direction.values()[choice].getLabel());
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent actionEvent) {
          choice++;
          if (choice >= Direction.values().length) {
            choice = 0;
          }
          Direction newDir = Direction.values()[choice];
          chooser.setDirection(newDir);
          button.setText(newDir.getLabel());
        }
      });
    }
  }

  private static JComponent makeNoteCountWidget(final QuestionChooser chooser) {
    Box result = Box.createHorizontalBox();
    result.add(new JLabel("Notes in next phrase:"));
    result.add(makeSpacer());

    final SpinnerNumberModel model = new SpinnerNumberModel(2, 2, 99, 1);
    model.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent changeEvent) {
        chooser.setNoteCount(model.getNumber().intValue());
      }
    });
    JSpinner spinner = new JSpinner(model) {
      @Override
      public Dimension getMaximumSize() {
        return getPreferredSize();
      }
    };
    result.add(spinner);
    result.setAlignmentX(Component.LEFT_ALIGNMENT);
    return result;
  }

  private static JComponent makeNoteDirectionWidget(QuestionChooser chooser) {
    Box result = Box.createHorizontalBox();
    result.add(new JLabel("Direction: "));
    result.add(new DirectionToggleButton(chooser).button);
    result.setAlignmentX(Component.LEFT_ALIGNMENT);
    return result;
  }

  private static JComponent makeScoreWidget(final ScoreKeeper scoreKeeper) {
    final JButton resetButton = new JButton(new SimpleAction("Reset") {
      @Override
      void act() throws UnavailableException {
        scoreKeeper.reset();
      }
    });
    resetButton.setVisible(false);

    final JLabel scoreBox = new JLabel("");
    scoreKeeper.addScoreChangeListener(new Runnable() {
      public void run() {
        scoreBox.setText(scoreKeeper.getScore());
        resetButton.setVisible(scoreKeeper.getTotal() > 0);
      }
    });

    Box result = Box.createHorizontalBox();
    result.add(scoreBox);
    result.add(resetButton);
    result.setAlignmentY(Component.BOTTOM_ALIGNMENT);

    return result;
  }

  private static Component makeSpacer() {
    return Box.createRigidArea(new Dimension(4, 0));
  }

  static abstract class SimpleAction extends AbstractAction {
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

  // =========== Music Theory ===========

  static final class Interval implements Comparable<Interval> {
    private int halfSteps;

    Interval(int halfSteps) {
      if (halfSteps < 0) {
        throw new IllegalArgumentException("invalid value for halfSteps: " + halfSteps);
      }
      this.halfSteps = halfSteps;
    }

    String getName() {
      switch (halfSteps) {
        case 0: return "Unison";
        case 1: return "Minor Second";
        case 2: return "Major Second";
        case 3: return "Minor Third";
        case 4: return "Major Third";
        case 5: return "Perfect Fourth";
        case 6: return "Tritone";
        case 7: return "Perfect Fifth";
        case 8: return "Minor Sixth";
        case 9: return "Major Sixth";
        case 10: return "Minor Seventh";
        case 11: return "Major Seventh";
        case 12: return "Octave";
        default:
          return "Interval<" + halfSteps + ">";
      }
    }

    public Interval add(int halfSteps) {
      return new Interval(this.halfSteps + halfSteps);
    }

    int upFrom(int note) {
      return note + halfSteps;
    }

    int downFrom(int note) {
      return note - halfSteps;
    }

    @Override
    public boolean equals(Object o) {
      return o.getClass() == Interval.class && halfSteps == ((Interval) o).halfSteps;
    }

    @Override
    public int hashCode() {
      return halfSteps;
    }

    public int compareTo(Interval other) {
      if (halfSteps < other.halfSteps) {
        return -1;
      } else if (halfSteps == other.halfSteps) {
        return 0;
      } else {
        return 1;
      }
    }
  }

  enum Direction {
    ASCENDING("Up", true),
    DESCENDING("Down", false),
    BOTH("Both", true, false);

    private final String label;
    private final List<Boolean> choices;

    Direction(String label, Boolean... choices) {
      this.label = label;
      this.choices = Collections.unmodifiableList(Arrays.asList(choices));
    }

    public String getLabel() {
      return label;
    }

    public List<Boolean> getChoices() {
      return choices;
    }
  }

  static class IntervalSet {
    private final Set<Interval> choices;

    IntervalSet(Collection<Interval> choices) {
      this.choices = new TreeSet<Interval>(choices);
    }

    IntervalSet(Interval... choices) {
      this(Arrays.asList(choices));
    }

    Interval choose(Random randomness) {
      return EarTrainer.choose(randomness, choices);
    }

    IntervalSet with(Interval choice) {
      Set<Interval> newSet = new TreeSet<Interval>(choices);
      newSet.add(choice);
      return new IntervalSet(newSet);
    }

    IntervalSet without(Interval choice) {
      Set<Interval> newSet = new TreeSet<Interval>(choices);
      newSet.remove(choice);
      return new IntervalSet(newSet);
    }

    boolean contains(Interval interval) {
      return choices.contains(interval);
    }

    boolean isEmpty() {
      return choices.isEmpty();
    }

    int size() {
      return choices.size();
    }

    public Collection<Interval> each() {
      return Collections.unmodifiableSet(choices);
    }

    Interval getLargest() {
      if (isEmpty()) {
        return null;
      } else {
        return new ArrayList<Interval>(choices).get(size() - 1);
      }
    }

    static IntervalSet forRange(Interval lowest, Interval highest) {
      List<Interval> choices = new ArrayList<Interval>();
      Interval candidate = lowest;
      while (candidate.compareTo(highest) <= 0) {
        choices.add(candidate);
        candidate = candidate.add(1);
      }
      return new IntervalSet(choices);
    }

    public static IntervalSet forHalfSteps(int... halfStep) {
      List<Interval> choices = new ArrayList<Interval>();
      for (int step : halfStep) {
        choices.add(new Interval(step));
      }
      return new IntervalSet(choices);
    }
  }

  static class PhraseBuilder {
    private final Random randomness;
    private final List<Interval> intervals = new ArrayList<Interval>();
    private final List<Boolean> isAscendingList = new ArrayList<Boolean>();

    PhraseBuilder(Random randomness) {
      this.randomness = randomness;
    }

    void addRandomInterval(
        IntervalSet intervalChoices,
        Direction direction) {
      intervals.add(intervalChoices.choose(this.randomness));
      isAscendingList.add(choose(this.randomness, direction.getChoices()));
    }

    public List<Interval> getIntervals() {
      return new ArrayList<Interval>(intervals);
    }

    List<Integer> getNotes(int startNote) {
      int note = startNote;
      List<Integer> result = new ArrayList<Integer>();
      result.add(note);
      for (int i = 0; i < intervals.size(); i++) {
        Interval interval = intervals.get(i);
        if (isAscendingList.get(i)) {
          note = interval.upFrom(note);
        } else {
          note = interval.downFrom(note);
        }
        result.add(note);
      }
      return result;
    }

    int getMinNote(int startNote) {
      int lowest = startNote;
      for (int note : getNotes(startNote)) {
        lowest = Math.min(lowest, note);
      }
      return lowest;
    }

    int getMaxNote(int startNote) {
      int highest = 0;
      for (int note : getNotes(startNote)) {
        highest = Math.max(highest, note);
      }
      return highest;
    }

    int getRange() {
      return getMaxNote(0) - getMinNote(0);
    }
  }

  // ============= Rules of the Game ==============

  static class Question {
    private final Sequence prompt;
    private final IntervalSet choices;
    private final List<Interval> answer;

    Question(Sequence prompt, IntervalSet choices, List<Interval> answer) {
      this.prompt = prompt;
      this.choices = choices;
      this.answer = answer;
    }

    void play(SequencePlayer player) throws UnavailableException {
      player.play(prompt);
    }

    boolean isCorrect(Interval answer, int position) {
      return this.answer.get(position).equals(answer);
    }

   int getAnswerCount() {
      return answer.size();
    }

    IntervalSet getChoices() {
      return choices;
    }
  }

  static class AnswerChoices {
    private final Set<Interval> enabledAnswers;
    private final Set<Interval> wrongAnswers;
    private final Map<Interval, Runnable> answerListeners;

    AnswerChoices() {
      this.enabledAnswers = new TreeSet<Interval>();
      this.wrongAnswers = new TreeSet<Interval>();
      this.answerListeners = new HashMap<Interval, Runnable>();
    }

    void putAnswerChangeListener(Interval interval, Runnable listener) {
      this.answerListeners.put(interval, listener);
    }

    void startNewInterval(IntervalSet choices) {
      wrongAnswers.clear();
      enabledAnswers.clear();
      enabledAnswers.addAll(choices.each());
      for (Interval answer : CHOOSABLE_INTERVALS.each()) {
        answerListeners.get(answer).run();
      }
    }

    void addWrongAnswer(Interval answer) {
      wrongAnswers.add(answer);
      answerListeners.get(answer).run();
    }

    boolean canChooseAnswer(Interval answer) {
      return enabledAnswers.contains(answer) && !wrongAnswers.contains(answer);
    }
  }

  static class QuestionChooser {
    private final Random randomness;

    private IntervalSet intervalChoices;
    private Direction direction;
    private int noteCount;

    QuestionChooser(Random randomness) {
      this.randomness = randomness;
      this.intervalChoices = DEFAULT_INTERVALS_IN_PHRASE;
      this.direction = DEFAULT_DIRECTION;
      this.noteCount = DEFAULT_NOTES_IN_PHRASE;
    }

    void setEnabled(Interval choice, boolean newValue) {
      if (newValue) {
        intervalChoices = intervalChoices.with(choice);
      } else {
        intervalChoices = intervalChoices.without(choice);
      }
    }

    boolean isEnabled(Interval interval) {
      return intervalChoices.contains(interval);
    }

    void setNoteCount(int newValue) {
      this.noteCount = newValue;
    }

    Direction getDirection() {
      return direction;
    }

    void setDirection(Direction newValue) {
      this.direction = newValue;
    }

    Question chooseQuestion() throws UnavailableException {
      while (true) {
        PhraseBuilder phrase = chooseRandomPhrase();
        int remainingRange = (HIGHEST_NOTE - LOWEST_NOTE) - phrase.getRange();
        if (remainingRange >= 0) {
          int lowNote = LOWEST_NOTE + randomness.nextInt(remainingRange + 1);
          int startNote = lowNote - phrase.getMinNote(0);
          Sequence prompt = makeSequence(phrase.getNotes(startNote));
          return new Question(prompt, intervalChoices, phrase.getIntervals());
        }
      }
    }

    private PhraseBuilder chooseRandomPhrase() {
      PhraseBuilder phrase = new PhraseBuilder(randomness);
      for (int i = 0; i < noteCount - 1; i++) {
        phrase.addRandomInterval(intervalChoices, direction);
      }
      return phrase;
    }

    private Sequence makeSequence(List<Integer> notes) throws UnavailableException {
      SequenceBuilder builder = new SequenceBuilder();
      for (int note : notes) {
        builder.addNote(note);
      }
      return builder.getSequence();
    }
  }

  static class ScoreKeeper {
    private int numRight = 0;
    private int numWrong = 0;
    private Runnable scoreChangeListener;

    public void addResult(boolean correct) {
      if (correct) {
        numRight++;
      } else {
        numWrong++;
      }
      scoreChangeListener.run();
    }

    public void reset() {
      numRight = 0;
      numWrong = 0;
      scoreChangeListener.run();
    }

    private int getTotal() {
      return numRight + numWrong;
    }

    public String getScore() {
      int total = getTotal();
      if (total == 0) {
        return "";
      }
      double percent = (numRight * 100.0) / total;

      StringBuilder out = new StringBuilder();
      Formatter formatter = new Formatter(out);
      formatter.format("Score: %.0f%% (%d of %d)", percent, numRight, total);
      return out.toString();
    }

    public void addScoreChangeListener(Runnable listener) {
      this.scoreChangeListener = listener;
    }
  }

  static class Quizzer {
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

    public boolean isStarted() {
      return currentQuestion != null;
    }

    void startNewQuestion() throws UnavailableException {
      currentQuestion = chooser.chooseQuestion();
      correctSoFar = true;
      choices.startNewInterval(currentQuestion.getChoices());
      chosenAnswers.clear();
      playQuestionNotes();
    }

    void addAnswerChosenListener(Runnable callback) {
      this.answerChosenListeners.add(callback);
    }

    String getQuestionText() {
      int intervalStartNote = chosenAnswers.size() + 1;
      return "What's the difference in pitch between notes " + intervalStartNote +
          " and " + (intervalStartNote + 1) + "?";
    }

    void playQuestionNotes() throws UnavailableException {
      currentQuestion.play(player);
    }

    void chooseAnswer(Interval answer) throws UnavailableException {
      if (currentQuestion.isCorrect(answer, chosenAnswers.size())) {
        choices.startNewInterval(currentQuestion.getChoices());
        chosenAnswers.add(answer);
        if (chosenAnswers.size() >= currentQuestion.getAnswerCount()) {
          scoreKeeper.addResult(correctSoFar);
          startNewQuestion();
        }
      } else {
        correctSoFar = false;
        choices.addWrongAnswer(answer);
        playQuestionNotes();
      }
      for (Runnable listener : answerChosenListeners) {
        listener.run();
      }
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

  // ================== Midi Mechanics ====================

  static class SequenceBuilder {
    private static final int CHANNEL = 4;
    private static final int VELOCITY = 90;

    private final Sequence sequence;
    private Track track;
    private int currentBeat = 0;

    SequenceBuilder() throws UnavailableException {
      try {
        this.sequence = new Sequence(Sequence.PPQ, 1);
        this.track = sequence.createTrack();
      } catch (InvalidMidiDataException e) {
        throw new UnavailableException(e);
      }
    }

    void addNote(int note) throws UnavailableException {

      ShortMessage noteOn;
      ShortMessage noteOff;
      try {
        noteOn = new ShortMessage();
        noteOn.setMessage(ShortMessage.NOTE_ON, CHANNEL, note, VELOCITY);

        noteOff = new ShortMessage();
        noteOff.setMessage(ShortMessage.NOTE_OFF, CHANNEL, note, VELOCITY);
      } catch (InvalidMidiDataException e) {
        throw new UnavailableException(e);
      }

      track.add(new MidiEvent(noteOn, currentBeat));
      track.add(new MidiEvent(noteOff, currentBeat + 1));
      currentBeat += 1;
    }

    Sequence getSequence() {
      return sequence;
    }
  }

  static class SequencePlayer {
    private final Sequencer sequencer;

    SequencePlayer() throws UnavailableException {
      try {
        sequencer = MidiSystem.getSequencer();
        sequencer.setTempoInBPM(BEATS_PER_MINUTE);
        sequencer.open();
      } catch (MidiUnavailableException e) {
        throw new UnavailableException(e);
      }
    }

    void play(Sequence sequence) throws UnavailableException {
      try {
        Profiler p = new Profiler();
        sequencer.stop();
        p.log("stopped sequencer");
        sequencer.setSequence(sequence);
        p.log("set sequence");
        sequencer.setMicrosecondPosition(0);
        p.log("set position");
        sequencer.start();
        p.log("started sequencer");
      } catch (InvalidMidiDataException e) {
        throw new UnavailableException(e);
      }
    }

    public void shutdown() {
      sequencer.stop();
      sequencer.close(); // kills background thread
    }
  }

  // ====== Generic Utilities =======

  private static <T> T choose(Random randomness, Collection<T> choices) {
    List<T> answers = new ArrayList<T>(choices);
    return answers.get(randomness.nextInt(answers.size()));
  }

  public static class UnavailableException extends Exception {
    UnavailableException(Throwable throwable) {
      super(throwable);
    }
  }

  static class Profiler {
    private final long startTime;
    Profiler() {
      startTime = System.currentTimeMillis();
    }
    void log(String message) {
      long elapsed = System.currentTimeMillis() - startTime;
      if (elapsed >= 10) {
        System.out.println(elapsed + ": " + message);
      }
    }
  }
}
