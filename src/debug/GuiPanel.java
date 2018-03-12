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

  private final static GuiControls  mainFrame = new GuiControls();
  private static PropertiesFile props;
  private static JTabbedPane    tabPanel;
  private static JTextPane      debugTextPane;
  private static JPanel         graphPanel;
  private static JFileChooser   fileSelector;
  private static ServerThread   udpThread;
  private static MyListener     listener;
  private static MsgListener    inputListener;
  private static Timer          pktTimer;
  private static Timer          graphTimer;
  private static Timer          statsTimer;
  private static int            linesRead;
  private static boolean        bRunLogger;
  private static boolean        bRunGraphics;
  private static GraphHighlight graphMode;
  
  private static final Dimension SCREEN_DIM = Toolkit.getDefaultToolkit().getScreenSize();

/**
 * creates a debug panel to display the Logger messages in.
   * @param port  - the port to use for reading messages
   * @param tcp     - true if use TCP, false to use UDP
   * @param bLogger - true if enable the debug message display
   * @param bGraph  - true if enable the graphics display
 */  
  public void createDebugPanel(int port, boolean tcp, boolean bLogger, boolean bGraph) {
    // if a panel already exists, close the old one
    if (GuiPanel.mainFrame.isValidFrame()) {
      GuiPanel.mainFrame.close();
    }

    GuiPanel.bRunLogger = bLogger;
    GuiPanel.bRunGraphics = bGraph;
    if (!bLogger && !bGraph) {
      System.out.println("Can't disable both logger and graphics!");
      System.exit(1);
    }
    
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
    mainFrame.makeButton(panel, "BTN_LOGFILE"  , "Set"               , LEFT, false);
    mainFrame.makeLabel (panel, "LBL_LOGFILE"  , ""                  , LEFT, true);
    tabPanel = mainFrame.makeTabbedPanel(null, "PNL_TABBED", "", LEFT, true);

    // now add controls to the sub-panels
    panel = "PNL_CONTROL";
    mainFrame.makeLabel (panel, ""             , ""          , LEFT, false); // dummy
    mainFrame.makeButton(panel, "BTN_LOADFILE" , "Load File" , LEFT, true);
    mainFrame.makeButton(panel, "BTN_SAVEGRAPH", "Save Graph", LEFT, false);
    mainFrame.makeButton(panel, "BTN_LOADGRAPH", "Load Graph", LEFT, true);
    mainFrame.makeButton(panel, "BTN_PAUSE"    , "Pause"     , LEFT, false);
    mainFrame.makeButton(panel, "BTN_CLEAR"    , "Clear"     , LEFT, true);

    panel = "PNL_STATS";
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

    // add the debug message panel to the tabs
    if (GuiPanel.bRunLogger) {
      GuiPanel.debugTextPane = new JTextPane();
      JScrollPane fileScrollPanel = new JScrollPane(GuiPanel.debugTextPane);
      fileScrollPanel.setBorder(BorderFactory.createTitledBorder(""));
      tabPanel.addTab("Debug Messages", fileScrollPanel);
    }

    // add the CallGraph panel to the tabs
    if (GuiPanel.bRunGraphics) {
      GuiPanel.graphPanel = new JPanel();
      JScrollPane graphScrollPanel = new JScrollPane(GuiPanel.graphPanel);
      tabPanel.addTab("Call Graph", graphScrollPanel);
    }

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

    // now setup the debug message handler
    if (GuiPanel.bRunLogger) {
      Logger debug = new Logger(GuiPanel.debugTextPane);
    }

    // pass the graph panel to CallGraph for it to use
    if (GuiPanel.bRunGraphics) {
      CallGraph.initCallGraph(GuiPanel.graphPanel);
    }
    
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
    pktTimer = new Timer(5, GuiPanel.inputListener);
    pktTimer.start();

    // create a slow timer for updating the call graph
    if (GuiPanel.bRunGraphics) {
      graphTimer = new Timer(1000, new GraphUpdateListener());
      graphTimer.start();
    }

    // create a timer for updating the statistics
    statsTimer = new Timer(100, new StatsUpdateListener());
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
    if (isCallGraphTabSelected()) {
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
    }
  }

  private class GraphUpdateListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      // if Call Graph tab selected, update graph
      if (isCallGraphTabSelected()) {
        if (CallGraph.updateCallGraph(graphMode)) {
          GuiPanel.mainFrame.repack();
        }
      }
    }
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
      if (isCallGraphTabSelected()) {
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
    // TODO: prefix with MM_DD_
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
    if (isCallGraphTabSelected()) {
      CallGraph.updateCallGraph(GuiPanel.GraphHighlight.NONE);
    }
    
    // clear the stats
    GuiPanel.linesRead = 0;
    (GuiPanel.mainFrame.getTextField("TXT_QUEUE")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_PKTSREAD")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_PKTSLOST")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_PROCESSED")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_METHODS")).setText("------");
  }

  private static void processMessage(String message) {
    // seperate message into the message type and the message content
    if (message == null || message.length() < 30) {
      return;
    }

    if (GuiPanel.bRunGraphics) {
      // if 8-digit line number omitted, skip it (this was an older format)
      String linenum = "00000000";
      if (!message.startsWith("[")) {
        linenum = message.substring(0, 8);
        message = message.substring(9);
      }
      // make sure we have a valid time stamp & the message length is valid
      // timestamp = [00:00.000] (followed by a space)
      if (!message.startsWith("[") || message.charAt(10) != ']' || message.length() < 20) {
        return; // time stamp missing - invalid format
      }
      String timeMin = message.substring(1, 3);
      String timeSec = message.substring(4, 6);
      String timeMs  = message.substring(7, 10);
      // next is the 5-char message type (followed by a space)
      String typestr = message.substring(12, 18).toUpperCase();
      // and finally the message content to display
      String content = message.substring(18);
      int  linecount = 0;
      long tstamp = 0;
      try {
        linecount = Integer.parseInt(linenum);
        tstamp = ((Integer.parseInt(timeMin) * 60) + Integer.parseInt(timeSec)) * 1000;
        tstamp += Integer.parseInt(timeMs);
      } catch (NumberFormatException ex) {
        // invalid syntax - skip
        return;
      }

      // generate a reconstructed string of the message
      String newmsg = linenum + " [" + timeMin + ":" + timeSec + "." + timeMs + "] " +
              typestr + " " + content;
      
      // get the current method that is being executed
      MethodInfo mthNode = CallGraph.getLastMethod();

      // extract call processing info and send to CallGraph
      switch (typestr.trim()) {
        case "CALL":
        {
          String[] splited = content.split("[\\|\\s]+");
          String method = "";
          String parent = "";
          String icount = "";
          int insCount = -1;
          switch (splited.length) {
            case 0:
              return; // invalid syntax - ignore
            case 1:
              method = splited[0].trim();
              break;
            case 2:
              method = splited[0].trim();
              parent = splited[1].trim();
              break;
            default:
            case 3:
              icount = splited[0].trim();
              method = splited[1].trim();
              parent = splited[2].trim();
              // convert count value to integer value (if invalid, just leave count value at -1)
              try {
                insCount = Integer.parseUnsignedInt(icount);
              } catch (NumberFormatException ex) {
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
          
      // now send to the debug message display
      // TODO: extract count and tstamp from message
      if (GuiPanel.bRunLogger) {
        Logger.print(newmsg);
      }

      GuiPanel.linesRead++;
      (GuiPanel.mainFrame.getTextField("TXT_PROCESSED")).setText("" + GuiPanel.linesRead);
    }
  }

}
