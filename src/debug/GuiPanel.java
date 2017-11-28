/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import static debug.UDPComm.SERVER_PORT;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author dmcd2356
 */
public class GuiPanel {

  final static private int GAPSIZE = 4; // gap size to place on each side of each widget
  
  public enum Orient { NONE, LEFT, RIGHT, CENTER }

  private static JFrame         mainFrame;
  private static JTabbedPane    tabPanel;
  private static JTextPane      debugTextPane;
  private static JPanel         graphPanel;
  private static JTextField     pktsBuffered;
  private static JTextField     pktsRead;
  private static JTextField     pktsLost;
  private static JFileChooser   fileSelector;
  private static Dimension      framesize;
  private static ServerThread   udpThread;
  private static MyListener     listener;
  private static PacketListener pktListener;
  private static Timer          pktTimer;
  private static Timer          graphTimer;
  
  private static final Dimension SCREEN_DIM = Toolkit.getDefaultToolkit().getScreenSize();
    
/**
 * creates a debug panel to display the Logger messages in.
 */  
  public void createDebugPanel() {
    if (GuiPanel.mainFrame != null) {
      GuiPanel.mainFrame.dispose();
    }

    // create the frame
    framesize = new Dimension(1200, 600);
    GuiPanel.mainFrame = new JFrame("dandebug");
    GuiPanel.mainFrame.setSize(framesize);
    GuiPanel.mainFrame.setMinimumSize(framesize);

    // setup the layout for the frame
    GridBagLayout gbag = new GridBagLayout();
    GuiPanel.mainFrame.setFont(new Font("SansSerif", Font.PLAIN, 14));
    GuiPanel.mainFrame.setLayout(gbag);

    // we need a filechooser for the Save buttons
    GuiPanel.fileSelector = new JFileChooser();

    // add the components
    GuiPanel.pktsBuffered = makeTextField(GuiPanel.mainFrame, gbag, GuiPanel.Orient.LEFT, false,
        "Buffer", "------", false);
    GuiPanel.pktsRead = makeTextField(GuiPanel.mainFrame, gbag, GuiPanel.Orient.LEFT, false,
        "Pkts Read", "------", false);
    GuiPanel.pktsLost = makeTextField(GuiPanel.mainFrame, gbag, GuiPanel.Orient.LEFT, true,
        "Pkts Lost", "------", false);
    JButton clearButton = makeButton(GuiPanel.mainFrame, gbag, GuiPanel.Orient.LEFT, false,
        "Clear");
    JButton pauseButton = makeButton(GuiPanel.mainFrame, gbag, GuiPanel.Orient.LEFT, false,
        "Pause");
    JButton saveGrphButton = makeButton(GuiPanel.mainFrame, gbag, GuiPanel.Orient.LEFT, false,
        "Save Graph");
    JButton loadFileButton = makeButton(GuiPanel.mainFrame, gbag, GuiPanel.Orient.LEFT, true,
        "Load File");

    // add a tabbed panel to it
    GuiPanel.tabPanel = new JTabbedPane();
    gbag.setConstraints(GuiPanel.tabPanel, setGbagConstraintsPanel());
    GuiPanel.mainFrame.add(GuiPanel.tabPanel);
    
    // add the debug message panel to the tabs
    GuiPanel.debugTextPane = new JTextPane();
    JScrollPane fileScrollPanel = new JScrollPane(GuiPanel.debugTextPane);
    fileScrollPanel.setBorder(BorderFactory.createTitledBorder(""));
    GuiPanel.tabPanel.addTab("Debug Messages", fileScrollPanel);

    // add the CallGraph panel to the tabs
    GuiPanel.graphPanel = new JPanel();
    JScrollPane graphScrollPanel = new JScrollPane(GuiPanel.graphPanel);
    GuiPanel.tabPanel.addTab("Call Graph", graphScrollPanel);

    // setup the control actions
    GuiPanel.mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent evt) {
        formWindowClosing(evt);
      }
    });
    GuiPanel.tabPanel.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if (GuiPanel.tabPanel.getSelectedIndex() == 1) {
          if (CallGraph.updateCallGraph()) {
            repackFrame();
          }
        }
      }
    });
    saveGrphButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        saveGraphButtonActionPerformed(evt);
      }
    });
    loadFileButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        loadDebugButtonActionPerformed(evt);
      }
    });
    clearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        Logger.clear();
        CallGraph.clear();
        udpThread.clear();
        GuiPanel.pktsBuffered.setText("------");
        GuiPanel.pktsRead.setText("------");
        GuiPanel.pktsLost.setText("------");
      }
    });
    pauseButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (pauseButton.getText().equals("Pause")) {
          pktTimer.stop();
          graphTimer.stop();
          pauseButton.setText("Resume");
        } else {
          pktTimer.start();
          graphTimer.start();
          pauseButton.setText("Pause");
        }
      }
    });

    // display the frame
    GuiPanel.mainFrame.pack();
    GuiPanel.mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    GuiPanel.mainFrame.setLocationRelativeTo(null);
    GuiPanel.mainFrame.setVisible(true);

    // now setup the debug message handler
    Logger debug = new Logger(GuiPanel.debugTextPane);

    // pass the graph panel to CallGraph for it to use
    CallGraph.initCallGraph(GuiPanel.graphPanel);
    
    // start the UDP listener
    try {
      GuiPanel.udpThread = new ServerThread(SERVER_PORT);
      GuiPanel.udpThread.start();
      GuiPanel.listener = GuiPanel.udpThread;
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
      System.exit(1);
    }

    // create a timer for reading and displaying the messages received
    GuiPanel.pktListener = new PacketListener();
    pktTimer = new Timer(5, GuiPanel.pktListener);
    pktTimer.start();

    // create a slow timer for updating the call graph
    graphTimer = new Timer(1000, new GraphUpdateListener());
    graphTimer.start();

    // create a timer for updating the statistics
    Timer statsTimer = new Timer(100, new StatsUpdateListener());
    statsTimer.start();
  }
  
  public static boolean isDebugMsgTabSelected() {
    return GuiPanel.tabPanel.getSelectedIndex() == 0;
  }
  
  public static boolean isCallGraphTabSelected() {
    return GuiPanel.tabPanel.getSelectedIndex() == 1;
  }
  
  private class PacketListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      // read & process next packet
      String message = GuiPanel.udpThread.getNextMessage();

      // seperate message into the message type and the message content
      if (message != null && message.length() > 30) {
        String linenum  = message.substring(0, 8);
        String typestr  = message.substring(21, 27).toUpperCase().trim();
        String content  = message.substring(29);
        // send CALL & RETURN info to CallGraph
        if (typestr.equals("CALL")) {
          int offset = content.indexOf('|');
          if (offset > 0) {
            String method = content.substring(0, offset).trim();
            String parent = content.substring(offset + 1).trim();
            int count;
            try {
              count = Integer.parseInt(linenum);
            } catch (Exception ex) {
              count = -1;
            }
            CallGraph.callGraphAddMethod(method, parent, count);
          }
        }
        else if (typestr.equals("RETURN")) {
          CallGraph.callGraphReturn(content);
        }
          
        // now send to the debug message display
        // TODO: extract count and tstamp from message
        Logger.print(message.trim());
      }
    }
  }

  private class StatsUpdateListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      // update statistics
      GuiPanel.pktsBuffered.setText("" + GuiPanel.udpThread.getBufferSize());
      GuiPanel.pktsRead.setText("" + GuiPanel.udpThread.getPktsRead());
      GuiPanel.pktsLost.setText("" + GuiPanel.udpThread.getPktsLost());
    }
  }

  private class GraphUpdateListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      // if Call Graph tab selected, update graph
      if (isCallGraphTabSelected()) {
        if (CallGraph.updateCallGraph()) {
          repackFrame();
        }
      }
    }
  }

  private static void repackFrame() {
    GuiPanel.mainFrame.pack();
    GuiPanel.mainFrame.setSize(framesize);
  }

  private static void saveDebugButtonActionPerformed(java.awt.event.ActionEvent evt) {
    GuiPanel.fileSelector.setApproveButtonText("Save");
    GuiPanel.fileSelector.setMultiSelectionEnabled(false);
    String defaultFile = "debug.log";
    GuiPanel.fileSelector.setSelectedFile(new File(defaultFile));
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      // copy debug text to specified file
      String content = GuiPanel.debugTextPane.getText();

      // output to the file
      File file = GuiPanel.fileSelector.getSelectedFile();
      file.delete();
      try {
        FileUtils.writeStringToFile(file, content, "UTF-8");
      } catch (IOException ex) {
        System.err.println(ex.getMessage());
      }
    }
  }
  
  private static void loadDebugButtonActionPerformed(java.awt.event.ActionEvent evt) {
    GuiPanel.fileSelector.setApproveButtonText("Load");
    GuiPanel.fileSelector.setMultiSelectionEnabled(false);
    String defaultFile = "debug.log";
    GuiPanel.fileSelector.setSelectedFile(new File(defaultFile));
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      // stop the timers from updating the display
      pktTimer.stop();
      graphTimer.stop();

      // clear the current display
      GuiPanel.debugTextPane.setText("");
      
      // set the file to read from
      File file = GuiPanel.fileSelector.getSelectedFile();
      udpThread.setInputFile(file.getAbsolutePath());

      // now restart the update timers
      pktTimer.start();
      graphTimer.start();
    }
  }
  
  private static void saveGraphButtonActionPerformed(java.awt.event.ActionEvent evt) {
    GuiPanel.fileSelector.setApproveButtonText("Save");
    GuiPanel.fileSelector.setMultiSelectionEnabled(false);
    String defaultFile = "callgraph.png";
    GuiPanel.fileSelector.setSelectedFile(new File(defaultFile));
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      // copy debug text to specified file
      String content = GuiPanel.debugTextPane.getText();

      // output to the file
      File file = GuiPanel.fileSelector.getSelectedFile();
      file.delete();
      CallGraph.saveImageAsFile(file.getAbsolutePath());
    }
  }
  
  private static void formWindowClosing(java.awt.event.WindowEvent evt) {
    listener.exit();
  }
  
  /**
   * this sets up the gridbag constraints for a single panel to fill the container
   * 
   * @return the constraints
   */
  private static GridBagConstraints setGbagConstraintsPanel() {
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
  private static GridBagConstraints setGbagConstraints(Orient pos, boolean end) {
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
   * @param name      - name of label to add
   * @return the constraints
   */
  private static GridBagConstraints setGbagInsertLabel(Container container, GridBagLayout gridbag,
                             Orient pos, boolean end, boolean fullline, String name) {
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
    JLabel label = new JLabel(name);
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
   * @param container - the container to place the component in (e.g. JFrame or JPanel)
   * @param gridbag - the layout info
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @param name    - the name to display as a label preceeding the widget
   * @return the button widget
   */
  private static void makeLabel(Container container, GridBagLayout gridbag, Orient pos,
                             boolean end, String name) {
    JLabel label = new JLabel(name);
    gridbag.setConstraints(label, setGbagConstraints(pos, end));
    container.add(label);
  }

  /**
   * This creates a JButton and places it in the container.
   * 
   * @param container - the container to place the component in (e.g. JFrame or JPanel)
   * @param gridbag - the layout info
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @param name    - the name to display as a label preceeding the widget
   * @return the button widget
   */
  private static JButton makeButton(Container container, GridBagLayout gridbag, Orient pos,
                             boolean end, String name) {
    JButton button = new JButton(name);
    gridbag.setConstraints(button, setGbagConstraints(pos, end));
    container.add(button);
    return button;
  }

  /**
   * This creates a JCheckBox and places it in the container.
   * 
   * @param container - the container to place the component in (e.g. JFrame or JPanel)
   * @param gridbag - the layout info
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @param name    - the name to display as a label preceeding the widget
   * @param value   - 0 to have checkbox initially unselected, any other value for selected
   * @return the checkbox widget
   */
  private static JCheckBox makeCheckbox(Container container, GridBagLayout gridbag, Orient pos,
                                 boolean end, String name, int value) {
    JCheckBox cbox = new JCheckBox(name);
    cbox.setSelected(value != 0);
    gridbag.setConstraints(cbox, setGbagConstraints(pos, end));
    container.add(cbox);
    return cbox;
  }

  /**
   * This creates a JTextField and places it in the container.
   * These are single line String displays.
   * 
   * @param container - the container to place the component in (e.g. JFrame or JPanel)
   * @param gridbag - the layout info
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @param name    - the name to display as a label preceeding the widget
   * @param value   - 0 to have checkbox initially unselected, any other value for selected
   * @param writable - true if field is writable by user, false if display only
   * @return the checkbox widget
   */
  private static JTextField makeTextField(Container container, GridBagLayout gridbag, Orient pos,
                                 boolean end, String name, String value, boolean writable) {
    // insert a label before the component
    GridBagConstraints c = setGbagInsertLabel(container, gridbag, pos, end, true, name);
    
    JTextField field = new JTextField();
    field.setText(value);
    field.setPreferredSize(new Dimension(100, 25));
    field.setEditable(writable);
    gridbag.setConstraints(field, c);
    container.add(field);
    return field;
  }

  /**
   * This creates a JRadioButton and places it in the container.
   * 
   * @param container - the container to place the component in (e.g. JFrame or JPanel)
   * @param gridbag - the layout info
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @param name    - the name to display as a label preceeding the widget
   * @param value   - 0 to have checkbox initially unselected, any other value for selected
   * @return the checkbox widget
   */
  private static JRadioButton makeRadiobutton(Container container, GridBagLayout gridbag, Orient pos,
                                 boolean end, String name, int value) {
    JRadioButton cbox = new JRadioButton(name);
    cbox.setSelected(value != 0);
    gridbag.setConstraints(cbox, setGbagConstraints(pos, end));
    container.add(cbox);
    return cbox;
  }

  /**
   * This creates a JComboBox and places it in the container.
   * 
   * @param container - the container to place the component in (e.g. JFrame or JPanel)
   * @param gridbag - the layout info
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @param name    - the name to display as a label preceeding the widget
   * @return the combo widget
   */
  private static JComboBox makeCombobox(Container container, GridBagLayout gridbag, Orient pos,
                                 boolean end, String name) {
    // insert a label before the component
    GridBagConstraints c = setGbagInsertLabel(container, gridbag, pos, end, true, name);
    
    JComboBox combobox = new JComboBox();
    gridbag.setConstraints(combobox, c);
    container.add(combobox);
    return combobox;
  }
  
  /**
   * This creates an integer JSpinner and places it in the container.
   * Step size (increment/decrement value) is always set to 1.
   * 
   * @param container - the container to place the component in (e.g. JFrame or JPanel)
   * @param gridbag - the layout info
   * @param pos     - orientatition on the line: LEFT, RIGHT or CENTER
   * @param end     - true if this is last widget in the line
   * @param name    - the name to display as a label preceeding the widget
   * @param minval  - the min range limit for the spinner
   * @param maxval  - the max range limit for the spinner
   * @param step    - step size for the spinner
   * @param curval  - the current value for the spinner
   * @return the spinner widget
   */
  private static JSpinner makeSpinner(Container container, GridBagLayout gridbag, Orient pos,
                          boolean end, String name, int minval, int maxval, int step, int curval) {
    // insert a label before the component
    GridBagConstraints c = setGbagInsertLabel(container, gridbag, pos, end, false, name);

    JSpinner spinner = new JSpinner();
    spinner.setModel(new SpinnerNumberModel(curval, minval, maxval, step));
    gridbag.setConstraints(spinner, c);
    container.add(spinner);
    return spinner;
  }
  
  /**
   * This creates a JScrollPane containing a JList of Strings and places it in the container.
   * A List of String entries is passed to it that can be manipulated (adding & removing entries
   * that will be automatically reflected in the scroll pane.
   * 
   * @param container - the container to place the component in (e.g. JFrame or JPanel)
   * @param gridbag - the layout info
   * @param name    - the name to display as a label preceeding the widget
   * @param list    - the list of entries to associate with the panel
   * @return the JList corresponding to the list passed
   */
  private static JList makeScrollList(Container container, GridBagLayout gridbag, String name,
                               DefaultListModel list) {
    // create the scroll panel and apply constraints
    JScrollPane spanel = new JScrollPane();
    spanel.setBorder(BorderFactory.createTitledBorder(name));
    gridbag.setConstraints(spanel, setGbagConstraintsPanel());
    container.add(spanel);

    // create a list component for the scroll panel and assign the list model to it
    JList scrollList = new JList();
    spanel.setViewportView(scrollList);
    scrollList.setModel(list);
    return scrollList;
  }

  /**
   * This creates an empty JPanel and places it in the container.
   * 
   * @param container - the container to place the component in (e.g. JFrame or JPanel)
   * @param gridbag - the layout info
   * @param name    - the name to display as a label preceeding the widget
   * @return the panel
   */
  private static JPanel makePanel(Container container, GridBagLayout gridbag, String name) {
    // create the scroll panel and apply constraints
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder(name));
    gridbag.setConstraints(panel, setGbagConstraintsPanel());
    if (container != null) {
      container.add(panel);
    }
    return panel;
  }

  /**
   * This creates a JScrollPane containing a JTextPane for text and places it in the container.
   * 
   * @param container - the container to place the component in (e.g. JFrame or JPanel)
   * @param gridbag - the layout info
   * @param name    - the name to display as a label preceeding the widget
   * @return the text panel contained in the scroll panel
   */
  private static JTextPane makeScrollText(Container container, GridBagLayout gridbag, String name) {
    // create a text panel component
    JTextPane panel = new JTextPane();

    // create the scroll panel and apply constraints
    JScrollPane spanel = new JScrollPane(panel);
    spanel.setBorder(BorderFactory.createTitledBorder(name));
    gridbag.setConstraints(spanel, setGbagConstraintsPanel());
    if (container != null) {
      container.add(spanel);
    }
    return panel;
  }

}
