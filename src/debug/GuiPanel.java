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
import java.io.File;
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
  private static JTabbedPane    tabPanel;
  private static JTextPane      debugTextPane;
  private static JPanel         graphPanel;
  private static JFileChooser   fileSelector;
  private static ServerThread   udpThread;
  private static MyListener     listener;
  private static PacketListener pktListener;
  private static Timer          pktTimer;
  private static Timer          graphTimer;
  private static Timer          statsTimer;
  private static int            linesRead;
  private static boolean        bRunLogger;
  private static boolean        bRunGraphics;
  private static boolean        bFileLoading;
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

    GuiPanel.bFileLoading = false;
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
    mainFrame.makePanel(null, "PNL_CONTROL", "Controls", LEFT, false);
    mainFrame.makePanel(null, "PNL_STATS", "Statistics", LEFT, false);
    mainFrame.makePanel(null, "PNL_HIGHLIGHT", "Graph Highlighting", LEFT, true);
    tabPanel = mainFrame.makeTabbedPanel(null, "PNL_TABBED", "", LEFT, true);

    // now add controls to the sub-panels
    panel = "PNL_CONTROL";
    mainFrame.makeButton(panel, "BTN_PAUSE"    , "Pause"     , LEFT, false);
    mainFrame.makeLabel (panel, "LBL_3",         "",           LEFT, true); // dummy to keep even
    mainFrame.makeButton(panel, "BTN_CLEAR"    , "Clear"     , LEFT, false);
    mainFrame.makeButton(panel, "BTN_SAVEGRAPH", "Save Graph", LEFT, true);
    mainFrame.makeButton(panel, "BTN_LOADFILE" , "Load File" , LEFT, false);
    mainFrame.makeButton(panel, "BTN_LOADGRAPH", "Load Graph", LEFT, true);

    panel = "PNL_STATS";
    mainFrame.makeLabel    (panel, "LBL_PORT", portInfo, RIGHT, true);
    mainFrame.makeLabel    (panel, "LBL_1"   ,       "", LEFT, true); // dummy seperator
    mainFrame.makeTextField(panel, "TXT_BUFFER",    "Buffer",    LEFT, false, "------", false);
    mainFrame.makeTextField(panel, "TXT_PROCESSED", "Processed", LEFT, true,  "------", false);
    mainFrame.makeTextField(panel, "TXT_PKTSLOST",  "Pkts Lost", LEFT, false, "------", false);
    mainFrame.makeTextField(panel, "TXT_METHODS",   "Methods",   LEFT, true,  "------", false);
    mainFrame.makeTextField(panel, "TXT_PKTSREAD",  "Pkts Read", LEFT, false, "------", false);
    mainFrame.makeLabel    (panel, "LBL_2",         "",          LEFT, true); // dummy to keep even

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
        loadDebugButtonActionPerformed(evt);
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
          if (pktTimer != null) {
            pktTimer.stop();
          }
          if (graphTimer != null) {
            graphTimer.stop();
          }
          pauseButton.setText("Resume");
        } else {
          if (pktTimer != null) {
            pktTimer.start();
          }
          if (graphTimer != null) {
            graphTimer.start();
          }
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
    
    // start the UDP listener
    try {
      GuiPanel.udpThread = new ServerThread(port, tcp);
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
  
  private class PacketListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      // read & process next packet
      String message = GuiPanel.udpThread.getNextMessage();
      
      // close file if we are reading from file
      if (message == null && GuiPanel.bFileLoading == true) {
        GuiPanel.udpThread.closeInputFile();
        
        // also, stop the packet listener
        if (pktTimer != null) {
          pktTimer.stop();
        }
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
            // invalid syntax - skip
            return;
          }

          // get the current method that is being executed
          MethodInfo mthNode = CallGraph.getLastMethod();

          // extract call processing info and send to CallGraph
          switch (typestr) {
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
              CallGraph.callGraphAddMethod(tstamp, insCount, method, parent, count);
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
                  mthNode.setExecption(count);
                }
              }
              break;
            case "ERROR":
              if (mthNode != null) {
                mthNode.setError(count);
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
      (GuiPanel.mainFrame.getTextField("TXT_BUFFER")).setText("" + GuiPanel.udpThread.getBufferSize());
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

  private static void loadDebugButtonActionPerformed(java.awt.event.ActionEvent evt) {
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", "txt");
    GuiPanel.fileSelector.setFileFilter(filter);
    GuiPanel.fileSelector.setApproveButtonText("Load");
    GuiPanel.fileSelector.setMultiSelectionEnabled(false);
    String defaultFile = "debug.log";
    GuiPanel.fileSelector.setSelectedFile(new File(defaultFile));
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame.getFrame());
    if (retVal == JFileChooser.APPROVE_OPTION) {
      // stop the timers from updating the display
      if (pktTimer != null) {
        pktTimer.stop();
      }
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
      if (pktTimer != null) {
        pktTimer.start();
      }
      if (graphTimer != null) {
        graphTimer.start();
      }
    }
  }
  
  private static void loadGraphButtonActionPerformed(java.awt.event.ActionEvent evt) {
    String defaultName = "callgraph";
    FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Files", "json");
    GuiPanel.fileSelector.setFileFilter(filter);
    GuiPanel.fileSelector.setApproveButtonText("Load");
    GuiPanel.fileSelector.setMultiSelectionEnabled(false);
    GuiPanel.fileSelector.setSelectedFile(new File(defaultName + ".json"));
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame.getFrame());
    if (retVal == JFileChooser.APPROVE_OPTION) {
      // stop the timers from updating the display
      if (pktTimer != null) {
        pktTimer.stop();
      }
      if (graphTimer != null) {
        graphTimer.stop();
      }

      // shut down the port input
      udpThread.exit();

      // clear the current display
      resetCapturedInput();
      
      // set the file to read from
      File file = GuiPanel.fileSelector.getSelectedFile();
      CallGraph.callGraphDataRead(file);

      // now restart the update timers
      if (pktTimer != null) {
        pktTimer.start();
      }
      if (graphTimer != null) {
        graphTimer.start();
      }
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
    (GuiPanel.mainFrame.getTextField("TXT_BUFFER")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_PKTSREAD")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_PKTSLOST")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_PROCESSED")).setText("------");
    (GuiPanel.mainFrame.getTextField("TXT_METHODS")).setText("------");
  }

}
