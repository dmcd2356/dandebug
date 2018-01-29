/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import static debug.GuiControls.makeButton;
import static debug.GuiControls.makeLabel;
import static debug.GuiControls.makePanel;
import static debug.GuiControls.makeRadiobutton;
import static debug.GuiControls.makeTextField;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
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
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author dmcd2356
 */
public class GuiPanel {

  public enum GraphHighlight { NONE, STATUS, TIME, INSTRUCTION, ITERATION }

  private static JFrame         mainFrame;
  private static GridBagLayout  mainLayout;
  private static JTabbedPane    tabPanel;
  private static JTextPane      debugTextPane;
  private static JPanel         graphPanel;
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
   * @param tcp     - true if use TCP, false to use UDP
   * @param bLogger - true if enable the debug message display
   * @param bGraph  - true if enable the graphics display
 */  
  public void createDebugPanel(int port, boolean tcp, boolean bLogger, boolean bGraph) {
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
    GuiPanel.mainFrame.setFont(new Font("SansSerif", Font.PLAIN, 14));
    GuiPanel.mainLayout = new GridBagLayout();
    GuiPanel.mainFrame.setLayout(GuiPanel.mainLayout);
    GuiControls.setMainFrame(mainFrame, mainLayout);

    // we need a filechooser for the Save buttons
    GuiPanel.fileSelector = new JFileChooser();

    GuiPanel.graphMode = GraphHighlight.NONE;
    String portInfo = (tcp ? "TCP" : "UDP") + " port " + port;
    
    // add the components
    
    // the button controls
    makePanel(null, "PNL_CONTROL", "Controls", GuiControls.Orient.LEFT, false);

    makeButton("PNL_CONTROL", "BTN_PAUSE"    , "Pause"     , GuiControls.Orient.LEFT, false);
    makeLabel ("PNL_CONTROL", "LBL_3",         "",           GuiControls.Orient.LEFT, true); // dummy label keeps the columns even
    makeButton("PNL_CONTROL", "BTN_CLEAR"    , "Clear"     , GuiControls.Orient.LEFT, false);
    makeButton("PNL_CONTROL", "BTN_SAVEGRAPH", "Save Graph", GuiControls.Orient.LEFT, true);
    makeButton("PNL_CONTROL", "BTN_LOADFILE" , "Load File" , GuiControls.Orient.LEFT, false);
    makeButton("PNL_CONTROL", "BTN_LOADGRAPH", "Load Graph", GuiControls.Orient.LEFT, true);

    // the statistic information
    makePanel(null, "PNL_STATS", "Statistics", GuiControls.Orient.LEFT, false);
    makeLabel("PNL_STATS", "LBL_PORT", portInfo, GuiControls.Orient.RIGHT, true);
    makeLabel("PNL_STATS", "LBL_1"   ,       "", GuiControls.Orient.LEFT, true); // dummy seperator

    makeTextField("PNL_STATS", "TXT_BUFFER",    "Buffer",    GuiControls.Orient.LEFT, false, "------", false);
    makeTextField("PNL_STATS", "TXT_PROCESSED", "Processed", GuiControls.Orient.LEFT, true,  "------", false);
    makeTextField("PNL_STATS", "TXT_PKTSLOST",  "Pkts Lost", GuiControls.Orient.LEFT, false, "------", false);
    makeTextField("PNL_STATS", "TXT_METHODS",   "Methods",   GuiControls.Orient.LEFT, true,  "------", false);
    makeTextField("PNL_STATS", "TXT_PKTSREAD",  "Pkts Read", GuiControls.Orient.LEFT, false, "------", false);
    makeLabel    ("PNL_STATS", "LBL_2",         "",          GuiControls.Orient.LEFT, true); // dummy label keeps the columns even

    // the selections for graphics highlighting
    makePanel(null, "PNL_HIGHLIGHT", "Graph Highlighting", GuiControls.Orient.LEFT, true);

    makeRadiobutton("PNL_HIGHLIGHT", "RB_ELAPSED" , "Elapsed Time"   , GuiControls.Orient.LEFT, true, 0);
    makeRadiobutton("PNL_HIGHLIGHT", "RB_INSTRUCT", "Instructions"   , GuiControls.Orient.LEFT, true, 0);
    makeRadiobutton("PNL_HIGHLIGHT", "RB_ITER"    , "Iterations Used", GuiControls.Orient.LEFT, true, 0);
    makeRadiobutton("PNL_HIGHLIGHT", "RB_STATUS"  , "Status"         , GuiControls.Orient.LEFT, true, 0);
    makeRadiobutton("PNL_HIGHLIGHT", "RB_NONE"    , "Off"            , GuiControls.Orient.LEFT, true, 1);

    // add a tabbed panel to it
    GuiPanel.tabPanel = new JTabbedPane();
    GuiPanel.mainLayout.setConstraints(GuiPanel.tabPanel, GuiControls.setGbagConstraintsPanel());
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

    ((JRadioButton)GuiControls.getComponent("RB_ELAPSED")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setHighlightMode(GraphHighlight.TIME);
      }
    });

    ((JRadioButton)GuiControls.getComponent("RB_INSTRUCT")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setHighlightMode(GraphHighlight.INSTRUCTION);
      }
    });

    ((JRadioButton)GuiControls.getComponent("RB_ITER")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setHighlightMode(GraphHighlight.ITERATION);
      }
    });

    ((JRadioButton)GuiControls.getComponent("RB_STATUS")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setHighlightMode(GraphHighlight.STATUS);
      }
    });

    ((JRadioButton)GuiControls.getComponent("RB_NONE")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setHighlightMode(GraphHighlight.NONE);
      }
    });

    ((JButton)GuiControls.getComponent("BTN_SAVEGRAPH")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        saveGraphButtonActionPerformed(evt);
      }
    });

    ((JButton)GuiControls.getComponent("BTN_LOADGRAPH")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        loadGraphButtonActionPerformed(evt);
      }
    });

    ((JButton)GuiControls.getComponent("BTN_LOADFILE")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        loadDebugButtonActionPerformed(evt);
      }
    });

    ((JButton)GuiControls.getComponent("BTN_CLEAR")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        resetCapturedInput();
      }
    });

    ((JButton)GuiControls.getComponent("BTN_PAUSE")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        JButton pauseButton = (JButton)GuiControls.getComponent("BTN_PAUSE");
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
  
  private static void setHighlightMode(GraphHighlight mode) {
    JRadioButton timeSelBtn = (JRadioButton)GuiControls.getComponent("RB_ELAPSED");
    JRadioButton instrSelBtn = (JRadioButton)GuiControls.getComponent("RB_INSTRUCT");
    JRadioButton iterSelBtn = (JRadioButton)GuiControls.getComponent("RB_ITER");
    JRadioButton statSelBtn = (JRadioButton)GuiControls.getComponent("RB_STATUS");
    JRadioButton noneSelBtn = (JRadioButton)GuiControls.getComponent("RB_NONE");

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
            // invalid line - skip
            return;
          }

          // get the current method that is being executed
          MethodInfo mthNode = CallGraph.getLastMethod();

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
            case "STATS":
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
                    String method = words[1];
                    if (method.endsWith(",")) {
                      method = method.substring(0, method.length() - 1);
                    }
                    mthNode.addUninstrumented(method);
                  }
                }
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
      ((JTextField)GuiControls.getComponent("TXT_BUFFER")).setText("" + GuiPanel.udpThread.getBufferSize());
      ((JTextField)GuiControls.getComponent("TXT_PKTSREAD")).setText("" + GuiPanel.udpThread.getPktsRead());
      ((JTextField)GuiControls.getComponent("TXT_PKTSLOST")).setText("" + GuiPanel.udpThread.getPktsLost());
      ((JTextField)GuiControls.getComponent("TXT_PROCESSED")).setText("" + GuiPanel.linesRead);
      ((JTextField)GuiControls.getComponent("TXT_METHODS")).setText("" + CallGraph.getMethodCount());
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
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame);
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
    int retVal = GuiPanel.fileSelector.showOpenDialog(GuiPanel.mainFrame);
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
    ((JTextField)GuiControls.getComponent("TXT_BUFFER")).setText("------");
    ((JTextField)GuiControls.getComponent("TXT_PKTSREAD")).setText("------");
    ((JTextField)GuiControls.getComponent("TXT_PKTSLOST")).setText("------");
    ((JTextField)GuiControls.getComponent("TXT_PROCESSED")).setText("------");
    ((JTextField)GuiControls.getComponent("TXT_METHODS")).setText("------");
  }

}
