// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.Optional;
import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public final class MainPanel extends JPanel {
  private MainPanel() {
    super(new BorderLayout());
    String[] columnNames = {"String", "Integer", "Boolean"};
    Object[][] data = {
      {"aaa", 12, true}, {"bbb", 5, false},
      {"CCC", 92, true}, {"DDD", 0, false}
    };
    TableModel model = new DefaultTableModel(data, columnNames) {
      @Override public Class<?> getColumnClass(int column) {
        return getValueAt(0, column).getClass();
      }
    };
    JTable table = new JTable(model) {
      private transient HighlightListener highlighter;
      @Override public void updateUI() {
        addMouseListener(highlighter);
        addMouseMotionListener(highlighter);
        setDefaultRenderer(Object.class, null);
        setDefaultRenderer(Number.class, null);
        setDefaultRenderer(Boolean.class, null);
        super.updateUI();
        highlighter = new HighlightListener();
        addMouseListener(highlighter);
        addMouseMotionListener(highlighter);
        setDefaultRenderer(Object.class, new RolloverDefaultTableCellRenderer(highlighter));
        setDefaultRenderer(Number.class, new RolloverNumberRenderer(highlighter));
        setDefaultRenderer(Boolean.class, new RolloverBooleanRenderer(highlighter));
      }

      @Override public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component c = super.prepareEditor(editor, row, column);
        if (c instanceof JCheckBox) {
          c.setBackground(getSelectionBackground());
        }
        return c;
      }
    };
    table.setAutoCreateRowSorter(true);

    JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    sp.setTopComponent(new JScrollPane(new JTable(model)));
    sp.setBottomComponent(new JScrollPane(table));
    sp.setResizeWeight(.5);

    add(sp);
    setPreferredSize(new Dimension(320, 240));
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
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.getContentPane().add(new MainPanel());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

class HighlightListener extends MouseAdapter {
  private int viewRowIndex = -1;
  private int viewColumnIndex = -1;

  public boolean isHighlightedCell(int row, int column) {
    return this.viewRowIndex == row && this.viewColumnIndex == column;
  }

  private static Optional<JTable> getTable(MouseEvent e) {
    Component c = e.getComponent();
    if (c instanceof JTable) {
      return Optional.of((JTable) c);
    }
    return Optional.empty();
  }

  @Override public void mouseMoved(MouseEvent e) {
    getTable(e).ifPresent(table -> {
      Point pt = e.getPoint();
      final int prevRow = viewRowIndex;
      final int prevCol = viewColumnIndex;
      viewRowIndex = table.rowAtPoint(pt);
      viewColumnIndex = table.columnAtPoint(pt);
      if (viewRowIndex < 0 || viewColumnIndex < 0) {
        viewRowIndex = -1;
        viewColumnIndex = -1;
      }
      // >>>> HyperlinkCellRenderer.java
      // @see http://java.net/projects/swingset3/sources/svn/content/trunk/SwingSet3/src/com/sun/swingset3/demos/table/HyperlinkCellRenderer.java
      if (viewRowIndex == prevRow && viewColumnIndex == prevCol) {
        return;
      }
      Rectangle repaintRect;
      if (viewRowIndex >= 0) {
        Rectangle r = table.getCellRect(viewRowIndex, viewColumnIndex, false);
        if (prevRow >= 0 && prevCol >= 0) {
          repaintRect = r.union(table.getCellRect(prevRow, prevCol, false));
        } else {
          repaintRect = r;
        }
      } else {
        repaintRect = table.getCellRect(prevRow, prevCol, false);
      }
      table.repaint(repaintRect);
      // <<<<
      // table.repaint();
    });
  }

  @Override public void mouseExited(MouseEvent e) {
    getTable(e).ifPresent(table -> {
      if (viewRowIndex >= 0 && viewColumnIndex >= 0) {
        table.repaint(table.getCellRect(viewRowIndex, viewColumnIndex, false));
      }
      viewRowIndex = -1;
      viewColumnIndex = -1;
    });
  }
}

class RolloverDefaultTableCellRenderer extends DefaultTableCellRenderer {
  private static final Color HIGHLIGHT = new Color(0xFF_96_32);
  private final transient HighlightListener highlighter;

  protected RolloverDefaultTableCellRenderer(HighlightListener highlighter) {
    super();
    this.highlighter = highlighter;
  }

  @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    String str = Objects.toString(value, "");
    if (highlighter.isHighlightedCell(row, column)) {
      setText("<html><u>" + str);
      setForeground(isSelected ? table.getSelectionForeground() : HIGHLIGHT);
      setBackground(isSelected ? table.getSelectionBackground().darker() : table.getBackground());
    } else {
      setText(str);
      setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
      setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
    }
    return this;
  }
}

class RolloverNumberRenderer extends RolloverDefaultTableCellRenderer {
  protected RolloverNumberRenderer(HighlightListener highlighter) {
    super(highlighter);
    setHorizontalAlignment(SwingConstants.RIGHT);
  }
}

class RolloverBooleanRenderer extends JCheckBox implements TableCellRenderer, UIResource {
  private final transient HighlightListener highlighter;

  protected RolloverBooleanRenderer(HighlightListener highlighter) {
    super();
    this.highlighter = highlighter;
    setHorizontalAlignment(SwingConstants.CENTER);
    setBorderPainted(true);
    setRolloverEnabled(true);
    setOpaque(true);
    setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
  }

  @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    getModel().setRollover(highlighter.isHighlightedCell(row, column));

    if (isSelected) {
      setForeground(table.getSelectionForeground());
      super.setBackground(table.getSelectionBackground());
    } else {
      setForeground(table.getForeground());
      setBackground(table.getBackground());
      // setBackground(row % 2 == 0 ? table.getBackground() : Color.WHITE); // Nimbus
    }
    setSelected(Objects.equals(value, Boolean.TRUE));
    return this;
  }

  // Overridden for performance reasons. ---->
  @Override public boolean isOpaque() {
    Color back = getBackground();
    Object o = SwingUtilities.getAncestorOfClass(JTable.class, this);
    if (o instanceof JTable) {
      JTable table = (JTable) o;
      boolean colorMatch = Objects.nonNull(back) && back.equals(table.getBackground()) && table.isOpaque();
      return !colorMatch && super.isOpaque();
    } else {
      return super.isOpaque();
    }
  }

  @Override protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    // System.out.println(propertyName);
    // if (propertyName == "border" ||
    //     ((propertyName == "font" || propertyName == "foreground") && oldValue != newValue)) {
    //   super.firePropertyChange(propertyName, oldValue, newValue);
    // }
  }

  @Override public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    /* Overridden for performance reasons. */
  }

  @Override public void repaint(long tm, int x, int y, int width, int height) {
    /* Overridden for performance reasons. */
  }

  @Override public void repaint(Rectangle r) {
    /* Overridden for performance reasons. */
  }

  @Override public void repaint() {
    /* Overridden for performance reasons. */
  }

  @Override public void invalidate() {
    /* Overridden for performance reasons. */
  }

  @Override public void validate() {
    /* Overridden for performance reasons. */
  }

  @Override public void revalidate() {
    /* Overridden for performance reasons. */
  }
  // <---- Overridden for performance reasons.
}
