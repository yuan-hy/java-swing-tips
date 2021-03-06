// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

public final class MainPanel extends JPanel {
  private MainPanel() {
    super(new BorderLayout(5, 5));

    JTextArea area = new JTextArea();
    area.setEditable(false);

    ProgressMonitor dmy = new ProgressMonitor(null, "message dummy", "note", 0, 100);
    SpinnerNumberModel millisToDecide = new SpinnerNumberModel(dmy.getMillisToDecideToPopup(), 0, 5 * 1000, 100);
    SpinnerNumberModel millisToPopup = new SpinnerNumberModel(dmy.getMillisToPopup(), 0, 5 * 1000, 100);

    JButton runButton = new JButton("run");
    runButton.addActionListener(e -> {
      Window w = SwingUtilities.getWindowAncestor(runButton);
      int toDecideToPopup = millisToDecide.getNumber().intValue();
      int toPopup = millisToPopup.getNumber().intValue();
      ProgressMonitor monitor = new ProgressMonitor(w, "message", "note", 0, 100);
      monitor.setMillisToDecideToPopup(toDecideToPopup);
      monitor.setMillisToPopup(toPopup);

      // System.out.println(monitor.getMillisToDecideToPopup());
      // System.out.println(monitor.getMillisToPopup());

      int lengthOfTask = Math.max(10_000, toDecideToPopup * 5);
      runButton.setEnabled(false);
      executeWorker(monitor, lengthOfTask, runButton, area);
    });

    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.insets = new Insets(5, 5, 5, 0);
    c.anchor = GridBagConstraints.LINE_END;

    JPanel p = new JPanel(new GridBagLayout());
    p.add(new JLabel("MillisToDecideToPopup:"), c);
    p.add(new JLabel("MillisToPopup:"), c);

    c.gridx = 1;
    c.weightx = 1d;
    c.fill = GridBagConstraints.HORIZONTAL;
    p.add(new JSpinner(millisToDecide), c);
    p.add(new JSpinner(millisToPopup), c);

    Box box = Box.createHorizontalBox();
    box.add(Box.createHorizontalGlue());
    box.add(runButton);

    add(new JScrollPane(area));
    add(p, BorderLayout.NORTH);
    add(box, BorderLayout.SOUTH);
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    setPreferredSize(new Dimension(320, 240));
  }

  private void executeWorker(ProgressMonitor monitor, int lengthOfTask, JButton button, JTextArea area) {
    SwingWorker<String, String> worker = new BackgroundTask(lengthOfTask) {
      @Override protected void process(List<String> chunks) {
        // if (isCancelled()) {
        //   return;
        // }
        if (!isDisplayable()) {
          System.out.println("process: DISPOSE_ON_CLOSE");
          cancel(true);
          return;
        }
        for (String message: chunks) {
          monitor.setNote(message);
        }
      }

      @Override public void done() {
        if (!isDisplayable()) {
          System.out.println("done: DISPOSE_ON_CLOSE");
          cancel(true);
          return;
        }
        button.setEnabled(true);
        monitor.close();
        if (isCancelled()) {
          area.append("Cancelled\n");
        } else {
          try {
            String text = get();
            area.append(text + "\n");
          } catch (InterruptedException ex) {
            area.append("Interrupted\n");
            Thread.currentThread().interrupt();
          } catch (ExecutionException ex) {
            ex.printStackTrace();
            area.append(String.format("Error: %s%n", ex.getMessage()));
          }
        }
        area.setCaretPosition(area.getDocument().getLength());
      }
    };
    worker.addPropertyChangeListener(new ProgressListener(monitor));
    worker.execute();
  }

  public static void main(String[] args) {
    EventQueue.invokeLater(MainPanel::createAndShowGui);
  }

  private static void createAndShowGui() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
      ex.printStackTrace();
      Toolkit.getDefaultToolkit().beep();
    }
    JFrame frame = new JFrame("@title@");
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    // frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.getContentPane().add(new MainPanel());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

class BackgroundTask extends SwingWorker<String, String> {
  private final int lengthOfTask;

  protected BackgroundTask(int lengthOfTask) {
    super();
    this.lengthOfTask = lengthOfTask;
  }

  @Override public String doInBackground() throws InterruptedException {
    int current = 0;
    while (current < lengthOfTask && !isCancelled()) {
      if (current % 10 == 0) {
        Thread.sleep(5);
      }
      int v = 100 * current / lengthOfTask;
      setProgress(v);
      publish(String.format("%d%%", v));
      current++;
    }
    return "Done";
  }
}

class ProgressListener implements PropertyChangeListener {
  private final ProgressMonitor monitor;

  protected ProgressListener(ProgressMonitor monitor) {
    this.monitor = monitor;
    this.monitor.setProgress(0);
  }

  @Override public void propertyChange(PropertyChangeEvent e) {
    String strPropertyName = e.getPropertyName();
    if ("progress".equals(strPropertyName)) {
      monitor.setProgress((Integer) e.getNewValue());
      Object o = e.getSource();
      if (o instanceof SwingWorker) {
        SwingWorker<?, ?> task = (SwingWorker<?, ?>) o;
        if (task.isDone() || monitor.isCanceled()) {
          task.cancel(true);
        }
      }
    }
  }
}
