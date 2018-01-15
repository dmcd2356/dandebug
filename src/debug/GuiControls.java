/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;

/**
 *
 * @author dan
 */
public class GuiControls {
  
  final static private int GAPSIZE = 4; // gap size to place on each side of each widget
  
  public enum Orient { NONE, LEFT, RIGHT, CENTER }

  private static JFrame         mainFrame;
  private static GridBagLayout  mainLayout;
  private static HashMap<String, JComponent>  gComponents = new HashMap();

  /**
   * this places the specified component in the container & saves the entry referenced by name.
   * 
   * @param name      - the name to refer to the component by
   * @param component - the component to add
   * @param container - the container to place the component in
   */
  private static void addComponent(String name, JComponent component, Container container) {
      if (container != null && name != null && !name.isEmpty()) {
        if (gComponents.containsKey(name)) {
          System.err.println("'" + name + "' component already added to container!");
          System.exit(1);
        }

        container.add(component);
        gComponents.put(name, component);
      }
  }
  
  private static void addComponent(String name, JComponent extcomp, JComponent intcomp, Container container) {
      if (container != null && name != null && !name.isEmpty()) {
        if (gComponents.containsKey(name)) {
          System.err.println("'" + name + "' component already added to container!");
          System.exit(1);
        }

        container.add(intcomp);
        gComponents.put(name, extcomp);
      }
  }
  
  public static JComponent getComponent(String name) {
    if (!gComponents.containsKey(name)) {
      System.err.println("'" + name + "' component not found!");
      System.exit(1);
    }
    
    return gComponents.get(name);
  }
  
  public static void setMainFrame(JFrame frame, GridBagLayout gbag) {
    mainFrame  = frame;
    mainLayout = gbag;
  }
  
  /**
   * this sets up the gridbag constraints for a single panel to fill the container
   * 
   * @return the constraints
   */
  public static GridBagConstraints setGbagConstraintsPanel() {
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(GAPSIZE, GAPSIZE, GAPSIZE, GAPSIZE);

    c.fill = GridBagConstraints.BOTH;
    c.gridwidth  = GridBagConstraints.REMAINDER;
    // since only 1 component, these both have to be non-zero for grid bag to work
    c.weightx = 1.0;
    c.weighty = 1.0;
    return c;
  }

  /**
   * This sets up the gridbag constraints for a simple element
   * 
   * @param pos - the orientation on the line
   * @param end - true if this is the last (or only) entry on the line
   * @return the constraints
   */
  public static GridBagConstraints setGbagConstraints(Orient pos, boolean end) {
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(GAPSIZE, GAPSIZE, GAPSIZE, GAPSIZE);

    switch(pos) {
      case LEFT:
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        break;
      case RIGHT:
        c.anchor = GridBagConstraints.BASELINE_TRAILING;
        break;
      case CENTER:
        c.anchor = GridBagConstraints.CENTER;
        break;
      case NONE:
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth  = GridBagConstraints.REMAINDER;
        // since only 1 component, these both have to be non-zero for grid bag to work
        c.weightx = 1.0;
        c.weighty = 1.0;
        return c;
    }
    c.fill = GridBagConstraints.NONE;
    
    c.gridheight = 1;
    if (end) {
      c.gridwidth = GridBagConstraints.REMAINDER; //end row
    }
    return c;
  }

  /**
   * This sets up the gridbag constraints for an element on a line and places a label to the left
   * 
   * @param container - the container to place the element in
   * @param gridbag   - the gridbag layout
   * @param pos       - the orientation on the line
   * @param end       - true if this is the last (or only) entry on the line
   * @param fullline  - true if take up entire line with item
   * @param title     - name of label to add
   * @return the constraints
   */
  public static GridBagConstraints setGbagInsertLabel(Container container, GridBagLayout gridbag,
                             Orient pos, boolean end, boolean fullline, String title) {
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(GAPSIZE, GAPSIZE, GAPSIZE, GAPSIZE);
    
    switch(pos) {
      case LEFT:
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        break;
      case RIGHT:
        c.anchor = GridBagConstraints.BASELINE_TRAILING;
        break;
      case CENTER:
        c.anchor = GridBagConstraints.CENTER;
        break;
      case NONE:
        break;
    }
    c.fill = GridBagConstraints.NONE;
    JLabel label = new JLabel(title);
    gridbag.setConstraints(label, c);
    container.add(label);
    
    if (fullline) {
      c.weightx = 1.0;
      c.fill = GridBagConstraints.HORIZONTAL;
    } else {
      c.weightx = 50.0;
    }
    if (end) {
      c.gridwidth = GridBagConstraints.REMAINDER; //end row
    }
    return c;
  }

  /**
   * This creates a JLabel and places it in the container.
   * 
   * @param owner   - the name of the jPanel container to place the component in (null if use main frame)
   * @param name    - the name id of the component
   * @param title   - the name to display as a label preceeding the widget
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @return the button widget
   */
  public static void makeLabel(String owner, String name, String title, Orient pos, boolean end) {
    // set default container to main frame
    Container container   = mainFrame;
    GridBagLayout gridbag = mainLayout;
    if (owner != null && !owner.isEmpty()) {
      container = getComponent(owner);
      gridbag = (GridBagLayout) ((JPanel)container).getLayout();
    }

    JLabel label = new JLabel(title);
    gridbag.setConstraints(label, setGbagConstraints(pos, end));

    // place entry in container & add entry to components list
    addComponent(name, label, container);
  }

  /**
   * This creates a JButton and places it in the container.
   * 
   * @param owner   - the name of the jPanel container to place the component in (null if use main frame)
   * @param name    - the name id of the component
   * @param title   - the name to display as a label preceeding the widget
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @return the button widget
   */
  public static JButton makeButton(String owner, String name, String title, Orient pos, boolean end) {
    // set default container to main frame
    Container container   = mainFrame;
    GridBagLayout gridbag = mainLayout;
    if (owner != null && !owner.isEmpty()) {
      container = getComponent(owner);
      gridbag = (GridBagLayout) ((JPanel)container).getLayout();
    }

    JButton button = new JButton(title);
    gridbag.setConstraints(button, setGbagConstraints(pos, end));

    // place entry in container & add entry to components list
    addComponent(name, button, container);
    return button;
  }

  /**
   * This creates a JCheckBox and places it in the container.
   * 
   * @param owner   - the name of the jPanel container to place the component in (null if use main frame)
   * @param name    - the name id of the component
   * @param title   - the name to display as a label preceeding the widget
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @param value   - 0 to have checkbox initially unselected, any other value for selected
   * @return the checkbox widget
   */
  public static JCheckBox makeCheckbox(String owner, String name, String title, Orient pos,
              boolean end, int value) {
    // set default container to main frame
    Container container   = mainFrame;
    GridBagLayout gridbag = mainLayout;
    if (owner != null && !owner.isEmpty()) {
      container = getComponent(owner);
      gridbag = (GridBagLayout) ((JPanel)container).getLayout();
    }
    JCheckBox cbox = new JCheckBox(title);
    cbox.setSelected(value != 0);
    gridbag.setConstraints(cbox, setGbagConstraints(pos, end));

    // place entry in container & add entry to components list
    addComponent(name, cbox, container);
    return cbox;
  }

  /**
   * This creates a JTextField and places it in the container.
   * These are single line String displays.
   * 
   * @param owner   - the name of the jPanel container to place the component in (null if use main frame)
   * @param name    - the name id of the component
   * @param title   - the name to display as a label preceeding the widget
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @param value   - 0 to have checkbox initially unselected, any other value for selected
   * @param writable - true if field is writable by user, false if display only
   * @return the checkbox widget
   */
  public static JTextField makeTextField(String owner, String name, String title, Orient pos,
                boolean end, String value, boolean writable) {
    // set default container to main frame
    Container container   = mainFrame;
    GridBagLayout gridbag = mainLayout;
    if (owner != null && !owner.isEmpty()) {
      container = getComponent(owner);
      gridbag = (GridBagLayout) ((JPanel)container).getLayout();
    }

    // insert a label before the component
    GridBagConstraints c = setGbagInsertLabel(container, gridbag, pos, end, true, title);
    
    JTextField field = new JTextField();
    field.setText(value);
    field.setPreferredSize(new Dimension(80, 25));
    field.setMinimumSize(new Dimension(80, 25));
    field.setEditable(writable);
    gridbag.setConstraints(field, c);

    // place entry in container & add entry to components list
    addComponent(name, field, container);
    return field;
  }

  /**
   * This creates a JRadioButton and places it in the container.
   * 
   * @param owner   - the name of the jPanel container to place the component in (null if use main frame)
   * @param name    - the name id of the component
   * @param title   - the name to display as a label preceeding the widget
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @param value   - 0 to have checkbox initially unselected, any other value for selected
   * @return the checkbox widget
   */
  public static JRadioButton makeRadiobutton(String owner, String name, String title, Orient pos,
              boolean end, int value) {
    // set default container to main frame
    Container container   = mainFrame;
    GridBagLayout gridbag = mainLayout;
    if (owner != null && !owner.isEmpty()) {
      container = getComponent(owner);
      gridbag = (GridBagLayout) ((JPanel)container).getLayout();
    }

    JRadioButton cbox = new JRadioButton(title);
    cbox.setSelected(value != 0);
    gridbag.setConstraints(cbox, setGbagConstraints(pos, end));

    // place entry in container & add entry to components list
    addComponent(name, cbox, container);
    return cbox;
  }

  /**
   * This creates a JComboBox and places it in the container.
   * 
   * @param owner   - the name of the jPanel container to place the component in (null if use main frame)
   * @param name    - the name id of the component
   * @param title   - the name to display as a label preceeding the widget
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @return the combo widget
   */
  public static JComboBox makeCombobox(String owner, String name, String title, Orient pos, boolean end) {
    // set default container to main frame
    Container container   = mainFrame;
    GridBagLayout gridbag = mainLayout;
    if (owner != null && !owner.isEmpty()) {
      container = getComponent(owner);
      gridbag = (GridBagLayout) ((JPanel)container).getLayout();
    }

    // insert a label before the component
    GridBagConstraints c = setGbagInsertLabel(container, gridbag, pos, end, true, title);
    
    JComboBox combobox = new JComboBox();
    gridbag.setConstraints(combobox, c);

    // place entry in container & add entry to components list
    addComponent(name, combobox, container);
    return combobox;
  }
  
  /**
   * This creates an integer JSpinner and places it in the container.
   * Step size (increment/decrement value) is always set to 1.
   * 
   * @param owner   - the name of the jPanel container to place the component in (null if use main frame)
   * @param name    - the name id of the component
   * @param title   - the name to display as a label preceeding the widget
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @param minval  - the min range limit for the spinner
   * @param maxval  - the max range limit for the spinner
   * @param step    - step size for the spinner
   * @param curval  - the current value for the spinner
   * @return the spinner widget
   */
  public static JSpinner makeSpinner(String owner, String name, String title, Orient pos, boolean end,
          int minval, int maxval, int step, int curval) {
    // set default container to main frame
    Container container   = mainFrame;
    GridBagLayout gridbag = mainLayout;
    if (owner != null && !owner.isEmpty()) {
      container = getComponent(owner);
      gridbag = (GridBagLayout) ((JPanel)container).getLayout();
    }

    // insert a label before the component
    GridBagConstraints c = setGbagInsertLabel(container, gridbag, pos, end, false, title);

    JSpinner spinner = new JSpinner();
    spinner.setModel(new SpinnerNumberModel(curval, minval, maxval, step));
    gridbag.setConstraints(spinner, c);

    // place entry in container & add entry to components list
    addComponent(name, spinner, container);
    return spinner;
  }
  
  /**
   * This creates an empty JPanel and places it in the container.
   * 
   * @param owner   - the name of the jPanel container to place the component in (null if use main frame)
   * @param name    - the name id of the component
   * @param title   - the name to display as a label preceeding the widget
   * @return the panel
   */
  public static JPanel makePanel(String owner, String name, String title, Orient pos, boolean end) {
    // set default container to main frame
    Container container   = mainFrame;
    GridBagLayout gridbag = mainLayout;
    if (owner != null && !owner.isEmpty()) {
      container = getComponent(owner);
      gridbag = (GridBagLayout) ((JPanel)container).getLayout();
    }

    // create the panel and apply constraints
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder(title));

    // create a layout for inside the panel
    GridBagLayout gbag = new GridBagLayout();
    panel.setFont(new Font("SansSerif", Font.PLAIN, 14));
    panel.setLayout(gbag);

    // place the panel in the specified container
    if (container != null) {
      gridbag.setConstraints(panel, setGbagConstraints(pos, end));

      // place entry in container & add entry to components list
      addComponent(name, panel, container);
    }

    return panel;
  }

  /**
   * This creates a JScrollPane containing a JList of Strings and places it in the container.
   * A List of String entries is passed to it that can be manipulated (adding & removing entries
   * that will be automatically reflected in the scroll pane.
   * 
   * @param owner   - the name of the jPanel container to place the component in (null if use main frame)
   * @param name    - the name id of the component
   * @param title   - the name to display as a label preceeding the widget
   * @param list    - the list of entries to associate with the panel
   * @return the JList corresponding to the list passed
   */
  public static JList makeScrollList(String owner, String name, String title, DefaultListModel list) {
    // set default container to main frame
    Container container   = mainFrame;
    GridBagLayout gridbag = mainLayout;
    if (owner != null && !owner.isEmpty()) {
      container = getComponent(owner);
      gridbag = (GridBagLayout) ((JPanel)container).getLayout();
    }

    // create the scroll panel and apply constraints
    JScrollPane spanel = new JScrollPane();
    spanel.setBorder(BorderFactory.createTitledBorder(title));
    gridbag.setConstraints(spanel, setGbagConstraintsPanel());

    // create a list component for the scroll panel and assign the list model to it
    JList scrollList = new JList();
    spanel.setViewportView(scrollList);
    scrollList.setModel(list);

    // place scroll panel in container & add its corresponding scroll list to components list
    addComponent(name, scrollList, spanel, container);
    return scrollList;
  }

  /**
   * This creates a JScrollPane containing a JTextPane for text and places it in the container.
   * 
   * @param owner   - the name of the jPanel container to place the component in (null if use main frame)
   * @param name    - the name id of the component
   * @param title     - the name to display as a label preceeding the widget
   * @return the text panel contained in the scroll panel
   */
  public static JTextPane makeScrollText(String owner, String name, String title) {
    // set default container to main frame
    Container container   = mainFrame;
    GridBagLayout gridbag = mainLayout;
    if (owner != null && !owner.isEmpty()) {
      container = getComponent(owner);
      gridbag = (GridBagLayout) ((JPanel)container).getLayout();
    }

    // create a text panel component
    JTextPane panel = new JTextPane();

    // create the scroll panel and apply constraints
    JScrollPane spanel = new JScrollPane(panel);
    spanel.setBorder(BorderFactory.createTitledBorder(title));
    gridbag.setConstraints(spanel, setGbagConstraintsPanel());
    if (container != null) {

      // place scroll pane entry in container & add its text pane to components list
      addComponent(name, panel, spanel, container);
    }

    return panel;
  }
}
