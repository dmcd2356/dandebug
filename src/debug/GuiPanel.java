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

/**
 *
 * @author dmcd2356
 */
public class GuiPanel {

  final static private int GAPSIZE = 4; // gap size to place on each side of each widget
  
  public enum Orient { NONE, LEFT, RIGHT, CENTER }

  public enum GraphHighlight { NONE, TIME, INSTRUCTION, ITERATION }

  private static JFrame         mainFrame;
  private static JTabbedPane    tabPanel;
  private static JTextPane      debugTextPane;
  private static JPanel         graphPanel;
  private static JTextField     pktsBuffered;
  private static JTextField     pktsRead;
  private static JTextField     pktsLost;
  private static JTextField     linesProcessed;
  private static JTextField     methodsFound;
  private static JFileChooser   fileSelector;
  private static Dimension      framesize;
  private static ServerThread   udpThread;
  private static MyListener     listener;
  private static PacketListener pktListener;
  private static Timer          pktTimer;
  private static Timer          graphTimer;
  private static int            linesRead;
  private static boolean        bRunLogger;
  private static boolean        bRunGraphics;
  private static boolean        bFileLoading;
  private static GraphHighlight graphMode;
  
  private static final Dimension SCREEN_DIM = Toolkit.getDefaultToolkit().getScreenSize();

/**
 * creates a debug panel to display the Logger messages in.
   * @param port  - the port to use for reading messages
   * @param bLogger - true if enable the debug message display
   * @param bGraph  - true if enable the graphics display
 */  
  public void createDebugPanel(int port, boolean bLogger, boolean bGraph) {
    if (GuiPanel.mainFrame != null) {
      GuiPanel.mainFrame.dispose();
    }

    GuiPanel.bFileLoading = false;
    GuiPanel.bRunLogger = bLogger;
    GuiPanel.bRunGraphics = bGraph;
    if (!bLogger && !bGraph) {
      System.out.println("Can't disable both logger and graphics!");
      System.exit(1);
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

    GuiPanel.graphMode = GraphHighlight.NONE;
    
    // add the components
    
    // the button controls
    JPanel panel3 = makePanel(GuiPanel.mainFrame, gbag, GuiPanel.Orient.LEFT, false,
        "Controls");
    GridBagLayout gbag3 = (GridBagLayout) panel3.getLayout();
    
    JButton pauseButton = makeButton(panel3, gbag3, GuiPanel.Orient.LEFT, true,
        "Pause");
    JButton clearButton = makeButton(panel3, gbag3, GuiPanel.Orient.LEFT, true,
        "Clear");
    JButton saveGrphButton = makeButton(panel3, gbag3, GuiPanel.Orient.LEFT, true,
        "Save Graph");
    JButton loadFileButton = makeButton(panel3, gbag3, GuiPanel.Orient.LEFT, true,
        "Load File");

    // the statistic information
    JPanel panel1 = makePanel(GuiPanel.mainFrame, gbag, GuiPanel.Orient.LEFT, false,
        "Statistics");
    GridBagLayout gbag1 = (GridBagLayout) panel1.getLayout();

    GuiPanel.pktsBuffered = makeTextField(panel1, gbag1, GuiPanel.Orient.LEFT, false,
        "Buffer", "------", false);
    GuiPanel.linesProcessed = makeTextField(panel1, gbag1, GuiPanel.Orient.LEFT, true,
        "Processed", "------", false);
    GuiPanel.pktsLost = makeTextField(panel1, gbag1, GuiPanel.Orient.LEFT, false,
        "Pkts Lost", "------", false);
    GuiPanel.methodsFound = makeTextField(panel1, gbag1, GuiPanel.Orient.LEFT, true,
        "Methods", "------", false);
    GuiPanel.pktsRead = makeTextField(panel1, gbag1, GuiPanel.Orient.LEFT, false,
        "Pkts Read", "------", false);
    makeLabel(panel1, gbag1, GuiPanel.Orient.LEFT, true, ""); // keeps the columns even

    // the selections for graphics highlighting
    JPanel panel2 = makePanel(GuiPanel.mainFrame, gbag, GuiPanel.Orient.LEFT, true,
        "Graph Highlighting");
    GridBagLayout gbag2 = (GridBagLayout) panel2.getLayout();
    
    JRadioButton timeSelBtn = makeRadiobutton(panel2, gbag2, Orient.LEFT, true,
        "Highlight time elapsed", 0);
    JRadioButton instrSelBtn = makeRadiobutton(panel2, gbag2, Orient.LEFT, true,
        "Highlight instruction usage", 0);
    JRadioButton iterSelBtn = makeRadiobutton(panel2, gbag2, Orient.LEFT, true,
        "Highlight iterations", 0);
    JRadioButton noneSelBtn = makeRadiobutton(panel2, gbag2, Orient.LEFT, true,
        "Highlight off", 1);
    
    // add a tabbed panel to it
    GuiPanel.tabPanel = new JTabbedPane();
    gbag.setConstraints(GuiPanel.tabPanel, setGbagConstraintsPanel());
    GuiPanel.mainFrame.add(GuiPanel.tabPanel);
    
    // add the debug message panel to the tabs
    if (GuiPanel.bRunLogger) {
      GuiPanel.debugTextPane = new JTextPane();
      JScrollPane fileScrollPanel = new JScrollPane(GuiPanel.debugTextPane);
      fileScrollPanel.setBorder(BorderFactory.createTitledBorder(""));
      GuiPanel.tabPanel.addTab("Debug Messages", fileScrollPanel);
    }

    // add the CallGraph panel to the tabs
    if (GuiPanel.bRunGraphics) {
      GuiPanel.graphPanel = new JPanel();
      JScrollPane graphScrollPanel = new JScrollPane(GuiPanel.graphPanel);
      GuiPanel.tabPanel.addTab("Call Graph", graphScrollPanel);
    }

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
        if (GuiPanel.tabPanel.getSelectedIndex() == 1) { // 1 = the graph tab selection
          if (CallGraph.updateCallGraph(graphMode)) {
            repackFrame();
          }
        }
      }
    });
    timeSelBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        instrSelBtn.setSelected(false);
        iterSelBtn.setSelected(false);
        noneSelBtn.setSelected(false);
        graphMode = GraphHighlight.TIME;
        if (isCallGraphTabSelected()) {
          CallGraph.updateCallGraph(graphMode);
        }
      }
    });
    instrSelBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        timeSelBtn.setSelected(false);
        iterSelBtn.setSelected(false);
        noneSelBtn.setSelected(false);
        graphMode = GraphHighlight.INSTRUCTION;
        if (isCallGraphTabSelected()) {
          CallGraph.updateCallGraph(graphMode);
        }
      }
    });
    iterSelBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        timeSelBtn.setSelected(false);
        instrSelBtn.setSelected(false);
        noneSelBtn.setSelected(false);
        graphMode = GraphHighlight.ITERATION;
        if (isCallGraphTabSelected()) {
          CallGraph.updateCallGraph(graphMode);
        }
      }
    });
    noneSelBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        timeSelBtn.setSelected(false);
        instrSelBtn.setSelected(false);
        iterSelBtn.setSelected(false);
        graphMode = GraphHighlight.NONE;
        if (isCallGraphTabSelected()) {
          CallGraph.updateCallGraph(graphMode);
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
        resetCapturedInput();
      }
    });
    pauseButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (pauseButton.getText().equals("Pause")) {
          pktTimer.stop();
          if (graphTimer != null) {
            graphTimer.stop();
          }
          pauseButton.setText("Resume");
        } else {
          pktTimer.start();
          if (graphTimer != null) {
            graphTimer.start();
          }
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
    if (GuiPanel.bRunLogger) {
      Logger debug = new Logger(GuiPanel.debugTextPane);
    }

    // pass the graph panel to CallGraph for it to use
    if (GuiPanel.bRunGraphics) {
      CallGraph.initCallGraph(GuiPanel.graphPanel);
    }
    
    // start the UDP listener
    try {
      GuiPanel.udpThread = new ServerThread(port);
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
    if (GuiPanel.bRunGraphics) {
      graphTimer = new Timer(1000, new GraphUpdateListener());
      graphTimer.start();
    }

    // create a timer for updating the statistics
    Timer statsTimer = new Timer(100, new StatsUpdateListener());
    statsTimer.start();
  }
  
  public static boolean isDebugMsgTabSelected() {
    if (!GuiPanel.bRunGraphics) {
      return true;
    }
    return GuiPanel.tabPanel.getSelectedIndex() == 0;
  }
  
  public static boolean isCallGraphTabSelected() {
    if (!GuiPanel.bRunLogger) {
      return true;
    }
    return GuiPanel.tabPanel.getSelectedIndex() == 1;
  }
  
  private class PacketListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      // read & process next packet
      String message = GuiPanel.udpThread.getNextMessage();
      
      // close file if we are reading from file
      if (message == null && GuiPanel.bFileLoading == true) {
        GuiPanel.udpThread.closeInputFile();
        
        // also, stop the packet listener
        pktTimer.stop();
        GuiPanel.bFileLoading = false;
      }

      // seperate message into the message type and the message content
      if (message != null && message.length() > 30) {
        if (GuiPanel.bRunGraphics) {
          String linenum = message.substring(0, 8);
          String timeMin = message.substring(10, 12);
          String timeSec = message.substring(13, 15);
          String timeMs  = message.substring(16, 19);
          String typestr = message.substring(21, 27).toUpperCase().trim();
          String content = message.substring(29);
          int  count = 0;
          long tstamp = 0;
          try {
            count = Integer.parseInt(linenum);
            tstamp = ((Integer.parseInt(timeMin) * 60) + Integer.parseInt(timeSec)) * 1000;
            tstamp += Integer.parseInt(timeMs);
          } catch (NumberFormatException ex) {
            // invalid line - skip
            return;
          }
          // extract call processing info and send to CallGraph
          switch (typestr) {
            case "CALL":
              int offset = content.indexOf('|');
              if (offset > 0) {
                String method = content.substring(0, offset).trim();
                String parent = content.substring(offset + 1).trim();
                CallGraph.callGraphAddMethod(tstamp, method, parent, count);
              } break;
            case "RETURN":
              CallGraph.callGraphReturn(tstamp, content);
              break;
            case "STATS":
              MethodInfo mthNode = CallGraph.getLastMethod();
              if (mthNode != null) {
                String[] words = content.trim().split("[ ]+");
                if (words.length >= 2) {
                  if (words[0].equals("InsCount:")) {
                    try {
                      int insCount = Integer.parseUnsignedInt(words[1]);
                      if (mthNode.isReturned()) {
                        mthNode.setInstrExit(insCount);
                      } else {
                        mthNode.setInstrEntry(insCount);
                      }
                    } catch (NumberFormatException ex) {
                      // ignore
                    }
                  } else if (words[0].equals("uninstrumented:")) {
                    // save uninstrumented call in list
                  }
                }
              } break;
            default:
              break;
          }
        }
          
        // now send to the debug message display
        // TODO: extract count and tstamp from message
        if (GuiPanel.bRunLogger) {
          Logger.print(message.trim());
        }

        GuiPanel.linesRead++;
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
      GuiPanel.linesProcessed.setText("" + GuiPanel.linesRead);
      GuiPanel.methodsFound.setText("" + CallGraph.getMethodCount());
    }
  }

  private class GraphUpdateListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      // if Call Graph tab selected, update graph
      if (isCallGraphTabSelected()) {
        if (CallGraph.updateCallGraph(graphMode)) {
          repackFrame();
        }
      }
    }
  }

  private static void repackFrame() {
    GuiPanel.mainFrame.pack();
    GuiPanel.mainFrame.setSize(framesize);
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
      if (graphTimer != null) {
        graphTimer.stop();
      }

      // shut down the port input
      udpThread.exit();

      // clear the current display
      resetCapturedInput();
      
      // set the file to read from
      File file = GuiPanel.fileSelector.getSelectedFile();
      udpThread.setInputFile(file.getAbsolutePath());
      GuiPanel.bFileLoading = true;

      // now restart the update timers
      pktTimer.start();
      if (graphTimer != null) {
        graphTimer.start();
      }
    }
  }
  
  private static void saveGraphButtonActionPerformed(java.awt.event.ActionEvent evt) {
    GuiPanel.fileSelector.setApproveButtonText("Save");
    GuiPanel.fileSelector.setMultiSelectionEnabled(false);
    String defaultFile = "callgraph.png";
    GuiPanel.fileSelector.setSelectedFile(new File(defaultFile));
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      // output to the file
      File file = GuiPanel.fileSelector.getSelectedFile();
      file.delete();
      CallGraph.saveImageAsFile(file.getAbsolutePath());
    }
  }
  
  private static void formWindowClosing(java.awt.event.WindowEvent evt) {
    listener.exit();
    udpThread.exit();
  }

  private static void resetCapturedInput() {
    // clear the packet buffer and statistics
    udpThread.clear();

    // clear the text panel
    Logger.clear();
    
    // clear the graphics panel
    CallGraph.clear();
    if (isCallGraphTabSelected()) {
      CallGraph.updateCallGraph(GuiPanel.GraphHighlight.NONE);
    }
    
    // clear the stats
    GuiPanel.linesRead = 0;
    GuiPanel.pktsBuffered.setText("------");
    GuiPanel.pktsRead.setText("------");
    GuiPanel.pktsLost.setText("------");
    GuiPanel.linesProcessed.setText("------");
    GuiPanel.methodsFound.setText("------");
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
    field.setPreferredSize(new Dimension(80, 25));
    field.setMinimumSize(new Dimension(80, 25));
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
  private static JPanel makePanel(Container container, GridBagLayout gridbag, Orient pos,
                  boolean end, String name) {
    // create the panel and apply constraints
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder(name));

    // create a layout for inside the panel
    GridBagLayout gbag = new GridBagLayout();
    panel.setFont(new Font("SansSerif", Font.PLAIN, 14));
    panel.setLayout(gbag);

    // place the panel in the specified container
    if (container != null) {
      gridbag.setConstraints(panel, setGbagConstraints(pos, end));
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
