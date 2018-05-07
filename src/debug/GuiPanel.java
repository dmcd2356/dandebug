/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author dmcd2356
 */
public class GuiPanel {

  public enum GraphHighlight { NONE, STATUS, TIME, INSTRUCTION, ITERATION }

  private enum ElapsedMode { OFF, RUN, RESET }

  private enum PanelTabs { LIVE, GRAPH, FILE }
  
  private final static GuiControls  mainFrame = new GuiControls();
  private static PropertiesFile props;
  private static JTabbedPane    tabPanel;
  private static JTextPane      liveTextPane;
  private static JTextPane      fileTextPane;
  private static JPanel         graphPanel;
  private static JFileChooser   fileSelector;
  private static ServerThread   udpThread;
  private static MyListener     listener;
  private static MsgListener    inputListener;
  private static Timer          pktTimer;
  private static Timer          graphTimer;
  private static Timer          statsTimer;
  private static int            linesRead;
  private static long           elapsedStart;
  private static ElapsedMode    elapsedMode;
  private static GraphHighlight graphMode;
  private static HashMap<PanelTabs, Integer> tabSelect = new HashMap<>();
  
  private static final Dimension SCREEN_DIM = Toolkit.getDefaultToolkit().getScreenSize();

/**
 * creates a debug panel to display the Logger messages in.
   * @param port  - the port to use for reading messages
   * @param tcp     - true if use TCP, false to use UDP
 */  
  public void createDebugPanel(int port, boolean tcp) {
    // if a panel already exists, close the old one
    if (GuiPanel.mainFrame.isValidFrame()) {
      GuiPanel.mainFrame.close();
    }

    GuiPanel.elapsedStart = 0;
    GuiPanel.elapsedMode = ElapsedMode.OFF;
    
    GuiPanel.graphMode = GraphHighlight.NONE;
    String portInfo = (tcp ? "TCP" : "UDP") + " port " + port;
    
    // these just make the gui entries cleaner
    String panel;
    GuiControls.Orient LEFT = GuiControls.Orient.LEFT;
    GuiControls.Orient CENTER = GuiControls.Orient.CENTER;
    GuiControls.Orient RIGHT = GuiControls.Orient.RIGHT;
    
    // create the frame
    mainFrame.newFrame("dandebug", 1200, 600, false);

    // create the entries in the main frame
    mainFrame.makePanel (null, "PNL_CONTROL"  , "Controls"          , LEFT, false);
    mainFrame.makePanel (null, "PNL_STATS"    , "Statistics"        , LEFT, false);
    mainFrame.makePanel (null, "PNL_HIGHLIGHT", "Graph Highlighting", LEFT, true);
    mainFrame.makePanel (null, "PNL_LOGGER"   , "Debug Log File"    , LEFT, true);
    panel = "PNL_LOGGER";
    mainFrame.makeButton(panel, "BTN_ERASEFILE", "Erase"             , LEFT, false);
    mainFrame.makeButton(panel, "BTN_LOGFILE"  , "Set"               , LEFT, false);
    mainFrame.makeLabel (panel, "LBL_LOGFILE"  , ""                  , LEFT, true);
    tabPanel = mainFrame.makeTabbedPanel(null, "PNL_TABBED", "", LEFT, true);

    // now add controls to the sub-panels
    panel = "PNL_CONTROL";
    mainFrame.makeButton(panel, "BTN_LOADFILE" , "Load File" , LEFT, true);
    mainFrame.makeButton(panel, "BTN_LOADGRAPH", "Load Graph", LEFT, false);
    mainFrame.makeButton(panel, "BTN_SAVEGRAPH", "Save Graph", LEFT, true);
    mainFrame.makeButton(panel, "BTN_PAUSE"    , "Pause"     , LEFT, false);
    mainFrame.makeButton(panel, "BTN_CLEAR"    , "Clear"     , LEFT, true);

    panel = "PNL_STATS";
    mainFrame.makeTextField(panel, "FIELD_ELAPSED", "Elapsed", LEFT, false, "00:00", false);
    mainFrame.makeLabel    (panel, "LBL_PORT"     , portInfo   , RIGHT, true);
    mainFrame.makeLabel    (panel, "LBL_1"        ,  ""        , LEFT, true); // dummy seperator
    mainFrame.makeTextField(panel, "TXT_QUEUE"    , "Queue"    , LEFT, false, "------", false);
    mainFrame.makeLabel    (panel, "LBL_2"        ,  ""        , LEFT, true); // dummy seperator
    mainFrame.makeTextField(panel, "TXT_PKTSREAD" , "Pkts Read", LEFT, false, "------", false);
    mainFrame.makeTextField(panel, "TXT_PROCESSED", "Processed", LEFT, true,  "------", false);
    mainFrame.makeTextField(panel, "TXT_PKTSLOST" , "Pkts Lost", LEFT, false, "------", false);
    mainFrame.makeTextField(panel, "TXT_METHODS"  , "Methods"  , LEFT, true,  "------", false);

    panel = "PNL_HIGHLIGHT";
    mainFrame.makeRadiobutton(panel, "RB_ELAPSED" , "Elapsed Time"   , LEFT, true, 0);
    mainFrame.makeRadiobutton(panel, "RB_INSTRUCT", "Instructions"   , LEFT, true, 0);
    mainFrame.makeRadiobutton(panel, "RB_ITER"    , "Iterations Used", LEFT, true, 0);
    mainFrame.makeRadiobutton(panel, "RB_STATUS"  , "Status"         , LEFT, true, 0);
    mainFrame.makeRadiobutton(panel, "RB_NONE"    , "Off"            , LEFT, true, 1);

    // disable the Save Graph button (until we actually have a graph)
    GuiPanel.mainFrame.getButton("BTN_SAVEGRAPH").setEnabled(false);

    // setup the tab panels
    Integer tabIndex = 0;
    Logger debug;

    // add the debug message panel for "live" output to the tabs
    GuiPanel.liveTextPane = new JTextPane();
    JScrollPane liveScrollPanel = new JScrollPane(GuiPanel.liveTextPane);
    liveScrollPanel.setBorder(BorderFactory.createTitledBorder(""));
    tabPanel.addTab("Live", liveScrollPanel);
    tabSelect.put(PanelTabs.LIVE, tabIndex++);
    debug = new Logger(GuiPanel.liveTextPane);

    // add the CallGraph panel to the tabs
    GuiPanel.graphPanel = new JPanel();
    JScrollPane graphScrollPanel = new JScrollPane(GuiPanel.graphPanel);
    tabPanel.addTab("Call Graph", graphScrollPanel);
    tabSelect.put(PanelTabs.GRAPH, tabIndex++);
    CallGraph.initCallGraph(GuiPanel.graphPanel);

    // add the debug message panel for "file open" output to the tabs
//    GuiPanel.fileTextPane = new JTextPane();
//    JScrollPane fileScrollPanel = new JScrollPane(GuiPanel.fileTextPane);
//    fileScrollPanel.setBorder(BorderFactory.createTitledBorder(""));
//    tabPanel.addTab("File", fileScrollPanel);
//    tabSelect.put(PanelTabs.FILE, tabIndex++);
//    debug = new Logger(GuiPanel.fileTextPane);
    
    // we need a filechooser for the Save buttons
    GuiPanel.fileSelector = new JFileChooser();

    // setup the control actions
    GuiPanel.mainFrame.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
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
            GuiPanel.mainFrame.repack();
          }
          
          // if we captured any call graph info, we can now enable the Save Graph button
          if (CallGraph.getMethodCount() > 0) {
            GuiPanel.mainFrame.getButton("BTN_SAVEGRAPH").setEnabled(true);
          }
        }
      }
    });
    (GuiPanel.mainFrame.getRadioButton("RB_ELAPSED")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setHighlightMode(GraphHighlight.TIME);
      }
    });
    (GuiPanel.mainFrame.getRadioButton("RB_INSTRUCT")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setHighlightMode(GraphHighlight.INSTRUCTION);
      }
    });
    (GuiPanel.mainFrame.getRadioButton("RB_ITER")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setHighlightMode(GraphHighlight.ITERATION);
      }
    });
    (GuiPanel.mainFrame.getRadioButton("RB_STATUS")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setHighlightMode(GraphHighlight.STATUS);
      }
    });
    (GuiPanel.mainFrame.getRadioButton("RB_NONE")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setHighlightMode(GraphHighlight.NONE);
      }
    });
    (GuiPanel.mainFrame.getButton("BTN_SAVEGRAPH")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        saveGraphButtonActionPerformed(evt);
      }
    });
    (GuiPanel.mainFrame.getButton("BTN_LOADGRAPH")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        loadGraphButtonActionPerformed(evt);
      }
    });
    (GuiPanel.mainFrame.getButton("BTN_LOADFILE")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        loadFileButtonActionPerformed(evt);
      }
    });
    (GuiPanel.mainFrame.getButton("BTN_ERASEFILE")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        eraseStorageButtonActionPerformed(evt);
      }
    });
    (GuiPanel.mainFrame.getButton("BTN_LOGFILE")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setStorageButtonActionPerformed(evt);
      }
    });
    (GuiPanel.mainFrame.getButton("BTN_CLEAR")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        resetCapturedInput();
      }
    });
    (GuiPanel.mainFrame.getButton("BTN_PAUSE")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        JButton pauseButton = GuiPanel.mainFrame.getButton("BTN_PAUSE");
        if (pauseButton.getText().equals("Pause")) {
          enableUpdateTimers(false);
          pauseButton.setText("Resume");
        } else {
          enableUpdateTimers(true);
          pauseButton.setText("Pause");
        }
      }
    });

    // display the frame
    GuiPanel.mainFrame.display();

    // check for a properties file
    props = new PropertiesFile();
    String logfileName = props.getPropertiesItem("LogFile", "");
    if (!logfileName.isEmpty()) {
      GuiPanel.fileSelector.setCurrentDirectory(new File(logfileName));
    }

    // start the TCP or UDP listener thread
    try {
      GuiPanel.udpThread = new ServerThread(port, tcp, logfileName);
      GuiPanel.udpThread.start();
      GuiPanel.listener = GuiPanel.udpThread;
      GuiPanel.mainFrame.getLabel("LBL_LOGFILE").setText(udpThread.getOutputFile());
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
      System.exit(1);
    }

    // create a timer for reading and displaying the messages received (from either network or file)
    GuiPanel.inputListener = new MsgListener();
    pktTimer = new Timer(1, GuiPanel.inputListener);
    pktTimer.start();

    // create a slow timer for updating the call graph
    graphTimer = new Timer(1000, new GraphUpdateListener());
    graphTimer.start();

    // create a timer for updating the statistics
    statsTimer = new Timer(100, new StatsUpdateListener());
    statsTimer.start();
  }

  private static boolean isTabSelection(PanelTabs select) {
    return GuiPanel.tabPanel.getSelectedIndex() == tabSelect.get(select);
  }

  private static void startElapsedTime() {
    GuiPanel.elapsedStart = System.currentTimeMillis();
    GuiPanel.elapsedMode = ElapsedMode.RUN;
    GuiPanel.mainFrame.getTextField("FIELD_ELAPSED").setText("00:00");
  }
  
  private static void resetElapsedTime() {
    GuiPanel.elapsedStart = 0;
    GuiPanel.elapsedMode = ElapsedMode.RESET;
    GuiPanel.mainFrame.getTextField("FIELD_ELAPSED").setText("00:00");
  }
  
  private static void updateElapsedTime() {
    if (GuiPanel.elapsedMode == ElapsedMode.RUN) {
      long elapsed = System.currentTimeMillis() - GuiPanel.elapsedStart;
      if (elapsed > 0) {
        Integer msec = (int)(elapsed % 1000);
        elapsed = elapsed / 1000;
        Integer secs = (int)(elapsed % 60);
        Integer mins = (int)(elapsed / 60);
        String timestamp = ((mins < 10) ? "0" : "") + mins.toString() + ":" +
                           ((secs < 10) ? "0" : "") + secs.toString(); // + "." +
                           //((msec < 10) ? "00" : (msec < 100) ? "0" : "") + msec.toString();
        GuiPanel.mainFrame.getTextField("FIELD_ELAPSED").setText(timestamp);
      }
    }
  }
  
  private static void enableUpdateTimers(boolean enable) {
    if (enable) {
      if (pktTimer != null) {
        pktTimer.start();
      }
      if (graphTimer != null) {
        graphTimer.start();
      }
    } else {
      if (pktTimer != null) {
        pktTimer.stop();
      }
      if (graphTimer != null) {
        graphTimer.stop();
      }
    }
  }
  
  private static void setHighlightMode(GraphHighlight mode) {
    JRadioButton timeSelBtn  = GuiPanel.mainFrame.getRadioButton("RB_ELAPSED");
    JRadioButton instrSelBtn = GuiPanel.mainFrame.getRadioButton("RB_INSTRUCT");
    JRadioButton iterSelBtn  = GuiPanel.mainFrame.getRadioButton("RB_ITER");
    JRadioButton statSelBtn  = GuiPanel.mainFrame.getRadioButton("RB_STATUS");
    JRadioButton noneSelBtn  = GuiPanel.mainFrame.getRadioButton("RB_NONE");

    // turn on the selected mode and turn off all others
    switch(mode) {
      case TIME:
        timeSelBtn.setSelected(true);
        instrSelBtn.setSelected(false);
        iterSelBtn.setSelected(false);
        statSelBtn.setSelected(false);
        noneSelBtn.setSelected(false);
        break;
      case INSTRUCTION:
        timeSelBtn.setSelected(false);
        instrSelBtn.setSelected(true);
        iterSelBtn.setSelected(false);
        statSelBtn.setSelected(false);
        noneSelBtn.setSelected(false);
        break;
      case ITERATION:
        timeSelBtn.setSelected(false);
        instrSelBtn.setSelected(false);
        iterSelBtn.setSelected(true);
        statSelBtn.setSelected(false);
        noneSelBtn.setSelected(false);
        break;
      case STATUS:
        timeSelBtn.setSelected(false);
        instrSelBtn.setSelected(false);
        iterSelBtn.setSelected(false);
        statSelBtn.setSelected(true);
        noneSelBtn.setSelected(false);
        break;
      case NONE:
        timeSelBtn.setSelected(false);
        instrSelBtn.setSelected(false);
        iterSelBtn.setSelected(false);
        statSelBtn.setSelected(false);
        noneSelBtn.setSelected(true);
      default:
        break;
    }

    // set the mode flag & update graph
    graphMode = mode;
    if (isTabSelection(PanelTabs.GRAPH)) {
      CallGraph.updateCallGraph(graphMode);
    }
  }
  
  private class MsgListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      // read & process next message
      String message = GuiPanel.udpThread.getNextMessage();
      if (message != null) {
        processMessage(message);
      }
    }
  }

  private class StatsUpdateListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      // update statistics
      (GuiPanel.mainFrame.getTextField("TXT_QUEUE")).setText("" + GuiPanel.udpThread.getQueueSize());
      (GuiPanel.mainFrame.getTextField("TXT_PKTSREAD")).setText("" + GuiPanel.udpThread.getPktsRead());
      (GuiPanel.mainFrame.getTextField("TXT_PKTSLOST")).setText("" + GuiPanel.udpThread.getPktsLost());
      (GuiPanel.mainFrame.getTextField("TXT_PROCESSED")).setText("" + GuiPanel.linesRead);
      (GuiPanel.mainFrame.getTextField("TXT_METHODS")).setText("" + CallGraph.getMethodCount());

      // update elapsed time if enabled
      GuiPanel.updateElapsedTime();
    }
  }

  private class GraphUpdateListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      // if Call Graph tab selected, update graph
      if (isTabSelection(PanelTabs.GRAPH)) {
        if (CallGraph.updateCallGraph(graphMode)) {
          GuiPanel.mainFrame.repack();
        }
      }
    }
  }

  private static void eraseStorageButtonActionPerformed(java.awt.event.ActionEvent evt) {
    // stop the timers from updating the display
    enableUpdateTimers(false);

    // erase the current file selection
    udpThread.eraseBufferFile();
      
    // now restart the update timers
    enableUpdateTimers(true);
  }
  
  private static void setStorageButtonActionPerformed(java.awt.event.ActionEvent evt) {
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Log Files", "log");
    GuiPanel.fileSelector.setFileFilter(filter);
    GuiPanel.fileSelector.setSelectedFile(new File("debug.log"));
    GuiPanel.fileSelector.setMultiSelectionEnabled(false);
    GuiPanel.fileSelector.setApproveButtonText("Set");
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame.getFrame());
    if (retVal == JFileChooser.APPROVE_OPTION) {
      // stop the timers from updating the display
      enableUpdateTimers(false);

      // shut down the port input and specify the new name
      File file = GuiPanel.fileSelector.getSelectedFile();
      String fname = file.getAbsolutePath();
      udpThread.setBufferFile(fname);
      
      // display the new log file location & save it
      GuiPanel.mainFrame.getLabel("LBL_LOGFILE").setText(fname);
      props.setPropertiesItem("LogFile", fname);

      // now restart the update timers
      enableUpdateTimers(true);
    }
  }
  
  private static void loadFileButtonActionPerformed(java.awt.event.ActionEvent evt) {
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Log Files", "txt", "log");
    GuiPanel.fileSelector.setFileFilter(filter);
    GuiPanel.fileSelector.setSelectedFile(new File("debug.log"));
    GuiPanel.fileSelector.setMultiSelectionEnabled(false);
    GuiPanel.fileSelector.setApproveButtonText("Load");
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame.getFrame());
    if (retVal == JFileChooser.APPROVE_OPTION) {
      // stop the timers from updating the display
      enableUpdateTimers(false);

      // clear the current display
      resetCapturedInput();
      
      // read the file
      File file = GuiPanel.fileSelector.getSelectedFile();
      mainFrame.getLabel("LBL_PORT").setText("read from: " + file.getName());
      try {
        String message;
        BufferedReader in = new BufferedReader(new FileReader(file));
        while ((message = in.readLine()) != null) {
          processMessage(message);
        }
      } catch (IOException ex) {
        System.out.println(ex.getMessage());
      }
      
      // update the graphics (if enabled)
      if (isTabSelection(PanelTabs.GRAPH)) {
        if (CallGraph.updateCallGraph(graphMode)) {
          GuiPanel.mainFrame.repack();
          int methods = CallGraph.getMethodCount();
          (GuiPanel.mainFrame.getTextField("TXT_METHODS")).setText("" + methods);
        }
      }
      
      // now restart the update timers
      enableUpdateTimers(true);
    }
  }
  
  private static void loadGraphButtonActionPerformed(java.awt.event.ActionEvent evt) {
    String defaultName = "callgraph";
    FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Files", "json");
    GuiPanel.fileSelector.setFileFilter(filter);
    GuiPanel.fileSelector.setSelectedFile(new File(defaultName + ".json"));
    GuiPanel.fileSelector.setMultiSelectionEnabled(false);
    GuiPanel.fileSelector.setApproveButtonText("Load");
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame.getFrame());
    if (retVal == JFileChooser.APPROVE_OPTION) {
      // stop the timers from updating the display
      enableUpdateTimers(false);

      // shut down the port input
      udpThread.exit();

      // clear the current display
      resetCapturedInput();
      
      // set the file to read from
      File file = GuiPanel.fileSelector.getSelectedFile();
      CallGraph.callGraphDataRead(file);
      mainFrame.getLabel("LBL_PORT").setText("read from: " + file.getName());

      // now restart the update timers
      enableUpdateTimers(true);
    }
  }
  
  private static void saveGraphButtonActionPerformed(java.awt.event.ActionEvent evt) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-");
    Date date = new Date();
    String defaultName = dateFormat.format(date) + "callgraph";
    FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Files", "json");
    GuiPanel.fileSelector.setFileFilter(filter);
    GuiPanel.fileSelector.setApproveButtonText("Save");
    GuiPanel.fileSelector.setMultiSelectionEnabled(false);
    GuiPanel.fileSelector.setSelectedFile(new File(defaultName + ".json"));
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame.getFrame());
    if (retVal == JFileChooser.APPROVE_OPTION) {
      File file = GuiPanel.fileSelector.getSelectedFile();
      String basename = file.getAbsolutePath();
      
      // get the base name without extension so we can create matching json and png files
      int offset = basename.lastIndexOf('.');
      if (offset > 0) {
        basename = basename.substring(0, offset);
      }

      // remove any pre-existing file and convert method list to json file
      File graphFile = new File(basename + ".json");
      graphFile.delete();
      CallGraph.callGraphDataSave(graphFile);

      // remove any pre-existing file and save image as png file
      File pngFile = new File(basename + ".png");
      pngFile.delete();
      CallGraph.saveImageAsFile(pngFile);
    }
  }
  
  private static void formWindowClosing(java.awt.event.WindowEvent evt) {
    graphTimer.stop();
    pktTimer.stop();
    statsTimer.stop();
    listener.exit();
    udpThread.exit();
    mainFrame.close();
    System.exit(0);
  }

  private static void resetCapturedInput() {
    // clear the packet buffer and statistics
    udpThread.clear();

    // clear the text panel
    Logger.clear();
    
    // clear the graphics panel
    CallGraph.clear();
    if (isTabSelection(PanelTabs.GRAPH)) {
      CallGraph.updateCallGraph(GuiPanel.GraphHighlight.NONE);
    }
    
    // clear the stats
    GuiPanel.linesRead = 0;
    (GuiPanel.mainFrame.getTextField("TXT_QUEUE")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_PKTSREAD")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_PKTSLOST")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_PROCESSED")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_METHODS")).setText("------");
    
    // reset the elapsed time
    GuiPanel.resetElapsedTime();
  }

  private static void processMessage(String message) {
    // seperate message into the message type and the message content
    if (message == null) {
      return;
    }
    if (message.length() < 30) {
      Logger.printUnformatted(message);
      return;
    }

    // read the specific entries from the message
    String linenum = message.substring(0, 8);   // 8-digit line number
    String timestr = message.substring(9, 20);  // elapsed time expressed as: [XX:XX.XXX]
    String typestr = message.substring(21, 27).toUpperCase(); // 5-char message type (followed by a space)
    String content = message.substring(29);     // message content to display

    // make sure we have a valid time stamp & the message length is valid
    // timestamp = [00:00.000] (followed by a space)
    if (timestr.charAt(0) != '[' || timestr.charAt(10) != ']') {
      Logger.printUnformatted(message);
      return;
    }
    String timeMin = timestr.substring(1, 3);
    String timeSec = timestr.substring(4, 6);
    String timeMs  = timestr.substring(7, 10);
    int  linecount = 0;
    long tstamp = 0;
    try {
      linecount = Integer.parseInt(linenum);
      tstamp = ((Integer.parseInt(timeMin) * 60) + Integer.parseInt(timeSec)) * 1000;
      tstamp += Integer.parseInt(timeMs);
    } catch (NumberFormatException ex) {
      // invalid syntax - skip
      Logger.printUnformatted(message);
      return;
    }

    // if we detect the start of a new debug session, reset our elapsed display
    // (or if the user hit Clear to reset the info, we will restart timer on the 1st msh received)
    if (linecount == 0 || GuiPanel.elapsedMode == ElapsedMode.RESET) {
      GuiPanel.startElapsedTime();
    }
    if (GuiPanel.elapsedMode == ElapsedMode.RESET) {
      Logger.printSeparator();
    }

    // send message to the debug display
    Logger.print(linecount, timestr, typestr, content);
          
    GuiPanel.linesRead++;
    (GuiPanel.mainFrame.getTextField("TXT_PROCESSED")).setText("" + GuiPanel.linesRead);

    // get the current method that is being executed
    MethodInfo mthNode = CallGraph.getLastMethod();

    // extract call processing info and send to CallGraph
    switch (typestr.trim()) {
      case "CALL":
      {
        content = content.trim();
        String[] splited = content.split(" ");
        String method = "";
        String parent = "";
        String icount = "";
        int insCount = -1;
        switch (splited.length) {
          case 0:
          case 1:
            Logger.printUnformatted("invalid syntax: 0 length");
            return; // invalid syntax - ignore
          case 2:
            method = splited[1].trim();
            break;
          case 3:
            method = splited[1].trim();
            parent = splited[2].trim();
            break;
          default:
          case 4:
            icount = splited[1].trim();
            method = splited[2].trim();
            parent = splited[3].trim();
            // convert count value to integer value (if invalid, just leave count value at -1)
            try {
              insCount = Integer.parseUnsignedInt(icount);
            } catch (NumberFormatException ex) {
              Logger.printUnformatted("invalid syntax (non-integer value): '" + icount + "'");
              return; // invalid syntax - ignore
            }
            break;
          }
        CallGraph.callGraphAddMethod(tstamp, insCount, method, parent, linecount);
        }
        break;
      case "RETURN":
        int insCount;
        try {
          insCount = Integer.parseUnsignedInt(content);
        } catch (NumberFormatException ex) {
          insCount = -1;
        }
        CallGraph.callGraphReturn(tstamp, insCount);
        break;
      case "ENTRY":
        if (content.startsWith("catchException")) {
          if (mthNode != null) {
            mthNode.setExecption(linecount);
          }
        }
        break;
      case "ERROR":
        if (mthNode != null) {
          mthNode.setError(linecount);
        }
        break;
      case "UNINST":
        if (mthNode != null) {
          String method = content;
          if (method.endsWith(",")) {
            method = method.substring(0, method.length() - 1);
          }
          mthNode.addUninstrumented(method);
        }
        break;
      default:
        break;
    }
  }

}
