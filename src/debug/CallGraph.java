/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.handler.mxGraphHandler;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author dmcd2356
 */
public class CallGraph {
  
  private static final String NEWLINE = System.getProperty("line.separator");

  private static JPanel           graphPanel = null;
  private static mxGraphComponent graphComponent = null;
  private static BaseGraph<MethodInfo> callGraph = null;
  private static List<MethodInfo> graphMethList = null;
  private static MethodInfo       lastMethod = null;
  private static Stack<Integer>   callStack = null;
  private static int numNodes;
  private static int numEdges;
  private static GuiPanel.GraphHighlight curGraphMode;
  
  public static void initCallGraph(JPanel panel) {
    CallGraph.graphMethList = new ArrayList<>();
    CallGraph.callStack = new Stack<>();
    CallGraph.callGraph = new BaseGraph<>();
    CallGraph.graphComponent = null;
    CallGraph.numNodes = 0;
    CallGraph.numEdges = 0;
    CallGraph.curGraphMode = GuiPanel.GraphHighlight.NONE;

    CallGraph.graphPanel = panel;
  }

  public static void clear() {
    CallGraph.graphMethList = new ArrayList<>();
    CallGraph.callStack = new Stack<>();
    CallGraph.callGraph = new BaseGraph<>();
    CallGraph.graphComponent = null;
    CallGraph.numNodes = 0;
    CallGraph.numEdges = 0;
    CallGraph.curGraphMode = GuiPanel.GraphHighlight.NONE;

    CallGraph.graphPanel.removeAll();
    Graphics graphics = graphPanel.getGraphics();
    if (graphics != null) {
      graphPanel.update(graphics);
    }
  }

  public static int getMethodCount() {
    if (graphMethList != null) {
      return graphMethList.size();
    }
    return 0;
  }

  public static MethodInfo getLastMethod() {
    if (graphMethList == null || graphMethList.size() < 1) {
      return null;
    }
    return CallGraph.lastMethod;
  }
  
  public static void saveImageAsFile(String name) {
    BufferedImage bi = new BufferedImage(CallGraph.graphPanel.getSize().width,
      CallGraph.graphPanel.getSize().height, BufferedImage.TYPE_INT_ARGB); 
    Graphics graphics = bi.createGraphics();
//    Graphics graphics = graphPanel.getGraphics();
    CallGraph.graphPanel.paint(graphics);
    graphics.dispose();
    try {
      ImageIO.write(bi,"png",new File(name));
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }
  
  /**
   * updates the call graph display
   * @param gmode
   * @return true if graph was updated
   */  
  public static boolean updateCallGraph(GuiPanel.GraphHighlight gmode) {
    boolean updated = false;

    // exit if the graphics panel has not been established
    if (CallGraph.callGraph == null || CallGraph.graphPanel == null) {
      return false;
    }
    
    // only run if a node or edge has been added to the graph or a color mode has changed
    if (CallGraph.callGraph.getEdgeCount() != CallGraph.numEdges ||
        CallGraph.callGraph.getVertexCount() != CallGraph.numNodes ||
        CallGraph.curGraphMode != gmode) {

      // update the state
      CallGraph.numEdges = CallGraph.callGraph.getEdgeCount();
      CallGraph.numNodes = CallGraph.callGraph.getVertexCount();
      CallGraph.curGraphMode = gmode;

      // if no graph has been composed yet, set it up now
      mxGraph graph = CallGraph.callGraph.getGraph();
      if (graphComponent == null) {
        graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        
        // add listener to show details of selected element
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
          @Override
          public void mouseReleased(MouseEvent e) {
            displaySelectedMethodInfo(e.getX(), e.getY());
          }
        });
        CallGraph.graphPanel.add(graphComponent);
      }

      // find the max limits
      long maxDuration = 0;
      int maxIteration = 0;
      int maxInstrCount = 0;
      for(MethodInfo mthNode : CallGraph.graphMethList) {
        long duration = mthNode.getDuration();
        if (maxDuration < duration) {
          maxDuration = duration;
        }
        int count = mthNode.getCount();
        if (maxIteration < count) {
          maxIteration = count;
        }
        int newInsCount = mthNode.getInstructionCount();
        if (maxInstrCount < newInsCount) {
          maxInstrCount = newInsCount;
        }
      }
      
      // update colors based on time usage or number of calls
      for (int ix = 0; ix < CallGraph.graphMethList.size(); ix++) {
        MethodInfo mthNode = CallGraph.graphMethList.get(ix);
        int colorR, colorG, colorB;
        String color = "D2E9FF";  // default color is greay
        double ratio = 1.0;
        switch (gmode) {
          default :
            break;
          case STATUS :
            // mark methods that have not exited
            if (mthNode.getInstructionCount() < 0 || mthNode.getDuration() < 0) {
              color = "CCFFFF"; // cyan
            }
            if (mthNode.getExecption() >= 0) {
              color = "FF6666"; // orange
            }
            if (mthNode.getError() >= 0) {
              color = "FFCCCC"; // pink
            }
            break;
          case TIME :
            long duration = mthNode.getDuration();
            ratio = (duration < 0) ? 0.0 : (double) duration / (double) maxDuration;
            // this runs from FF6666 (red) to FFCCCC (light red)
            colorR = 255;
            colorG = 204 - (int) (102.0 * ratio);
            colorB = 204 - (int) (102.0 * ratio);
            color = String.format ("%06x", (colorR << 16) + (colorG << 8) + colorB);
            if (ratio < 0.2) {
              color = "D2E9FF";
            }
            break;
          case INSTRUCTION :
            int instruction = mthNode.getInstructionCount();
            ratio = (instruction < 0) ? 0.0 : (double) instruction / (double) maxInstrCount;
            // this runs from 66FF66 (green) to CCFFCC (light green)
            colorR = 204 - (int) (102.0 * ratio);
            colorG = 255;
            colorB = 204 - (int) (102.0 * ratio);
            color = String.format ("%06x", (colorR << 16) + (colorG << 8) + colorB);
            if (ratio < 0.2) {
              color = "D2E9FF";
            }
            break;
          case ITERATION :
            int count = mthNode.getCount();
            ratio = (double) count / (double) maxIteration;
            // this runs from 6666FF (blue) to CCCCFF (light blue)
            colorR = 204 - (int) (102.0 * ratio);
            colorG = 204 - (int) (102.0 * ratio);
            colorB = 255;
            color = String.format ("%06x", (colorR << 16) + (colorG << 8) + colorB);
            if (ratio < 0.2 || (count < 10 && count < maxIteration)) {
              color = "D2E9FF";
            }
            break;
        }

        // set minimum threshhold
        if (ratio < 0.2 ) {
          color = "D2E9FF";
        }

        CallGraph.callGraph.colorVertex(mthNode, color);
        //System.out.println(color + " for: " + mthNode.getFullName());
      }

      // update the contents of the graph component
      Graphics graphics = graphPanel.getGraphics();
      if (graphics != null) {
        graphPanel.update(graphics);
      }
      
      // update the graph layout
      CallGraph.callGraph.layoutGraph();
      updated = true;
    }

    return updated;
  }

  public static void displaySelectedMethodInfo(int x, int y) {
    mxGraphHandler handler = graphComponent.getGraphHandler();
    mxCell cell = (mxCell) handler.getGraphComponent().getCellAt(x, y);
    if (cell != null && cell.isVertex()) {
      MethodInfo selected = CallGraph.callGraph.getSelectedNode();
      JOptionPane.showMessageDialog (null,
          "Method:      " + selected.getFullName() + NEWLINE +
          (selected.getDuration() < 0 ?
              "(never returned)" + NEWLINE :
              "Duration:    " + selected.getDuration() + NEWLINE) +
          (selected.getInstructionCount() < 0 ?
              "" :
              "Instr Count: " + selected.getInstructionCount() + NEWLINE) +
          "Iter:        " + selected.getCount() + NEWLINE +
          "1st called @ line:     " + selected.getFirstLine() + NEWLINE +
          (selected.getExecption() <= 1 ?
              "" :
              "exception @ line:  " + selected.getExecption()) +
          (selected.getError() <= 1 ?
              "" :
              "error @ line:      " + selected.getError()),
          "Method Info",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }
  
  /**
   * finds the specified method entry in the list of saved methods
   * @param method
   * @return 
   */
  public static MethodInfo findMethodEntry(String method) {
    for (int ix = 0; ix < CallGraph.graphMethList.size(); ix++) {
      if (CallGraph.graphMethList.get(ix).getFullName().equals(method)) {
        return CallGraph.graphMethList.get(ix);
      }
    }
    return null;
  }

  /**
   * reads the CallGraph.graphMethList entries and saves to file
   * 
   * @param file - name of file to save content to
   */  
  public static void callGraphDataSave(File file) {
    // open the file to write to
    BufferedWriter bw = null;
    try {
      bw = new BufferedWriter(new FileWriter(file));
    } catch (IOException ex) {
      System.err.println(ex.getMessage());
      return;
    }

    // convert to json and save to file
		GsonBuilder builder = new GsonBuilder();
    builder.setPrettyPrinting().serializeNulls();
    //builder.excludeFieldsWithoutExposeAnnotation().create();
		Gson gson = builder.create();
    gson.toJson(CallGraph.graphMethList, bw);

    try {
      bw.close();
    } catch (IOException ex) {
      System.err.println(ex.getMessage());
    }
  }
  
  /**
   * reads method info from specified file and saves in CallGraph.graphMethList
   * 
   * @param file - name of file to load data from
   */  
  public static void callGraphDataRead(File file) {
    // open the file to read from
    BufferedReader br;
    try {
      br = new BufferedReader(new FileReader(file));
    } catch (FileNotFoundException ex) {
      System.err.println(ex.getMessage());
      return;
    }
    
    // remove all existing data before we start
    clear();

    // load the method list info from json file
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
    Type methodListType = new TypeToken<List<MethodInfo>>() {}.getType();
    CallGraph.graphMethList = gson.fromJson(br, methodListType);
    System.out.println("loaded: " + CallGraph.graphMethList.size() + " methods");

    // add vertexes to graph
    for(MethodInfo mthNode : CallGraph.graphMethList) {
      CallGraph.callGraph.addVertex(mthNode, mthNode.getCGName());
    }
    
    // now connect the methods to their parents
    for (MethodInfo mthNode : CallGraph.graphMethList) {
      // for each parent entry for a method...
      for (String parent : mthNode.getParents()) {
        // find MethodInfo for the parent
        MethodInfo parNode = findMethodEntry(parent);
        // only add connection if parent was found and there isn't already a connection
        if (parNode != null && CallGraph.callGraph.getEdge(parNode, mthNode) == null) {
          // now add the connection from the method to the parent
          CallGraph.callGraph.addEdge(parNode, mthNode, null);
        }
      }
    }
  }
  
  /**
   * adds a method to the call graph if it is not already there
   * 
   * @param tstamp - timestamp in msec from msg
   * @param method - the full name of the method to add
   * @param parent - the full name of the caller
   * @param line   - the line number corresponding to the call event
   */  
  public static void callGraphAddMethod(long tstamp, String method, String parent, int line) {
    if (method == null || method.isEmpty() || CallGraph.graphMethList == null) {
      return;
    }
    if (parent == null) {
      parent = "";
    }
    
    // find parent entry in list
    MethodInfo parNode = null;
    if (!parent.isEmpty()) {
      parNode = findMethodEntry(parent);
//      if (parNode == null) {
//        parNode = new MethodInfo(parent, null, 0, -1);
//        CallGraph.graphMethList.add(parNode);
//        CallGraph.callGraph.addVertex(parNode, parNode.getCGName());
//      }
    }

    // find method entry in list
    MethodInfo mthNode = null;
    boolean newnode = false;
    int count = CallGraph.graphMethList.size();
    for (int ix = 0; ix < count; ix++) {
      if (CallGraph.graphMethList.get(ix).getFullName().equals(method)) {
        CallGraph.callStack.push(ix); // save entry as last method called
        mthNode = CallGraph.graphMethList.get(ix);
        CallGraph.lastMethod = mthNode;
        mthNode.incCount(line); // inc # of times method called
        // if new caller found, add it to connection list
        if (!parent.isEmpty()) {
          mthNode.addParent(parent);
        }
      }
    }
    // if not found, create new one and add it to list
    if (mthNode == null) {
      CallGraph.callStack.push(count); // save entry as last method called
      mthNode = new MethodInfo(method, parent, tstamp, line);
      CallGraph.graphMethList.add(mthNode);
      CallGraph.lastMethod = mthNode;
      newnode = true;
    }

    // add node (if not previously defined) and/or edge (if parent defined) to graph
    if (newnode) {
      CallGraph.callGraph.addVertex(mthNode, mthNode.getCGName());
    }
    if (parNode != null && CallGraph.callGraph.getEdge(parNode, mthNode) == null) {
      CallGraph.callGraph.addEdge(parNode, mthNode, null);
    }
  }

  /**
   * adds exit condition info to a method in the call graph
   * 
   * @param tstamp - timestamp in msec from msg
   * @param method - the full name of the method that is exiting
   */  
  public static void callGraphReturn(long tstamp, String method) {
    if (CallGraph.graphMethList == null || method == null) {
      //System.out.println("Return: " + method + " - NOT FOUND!");
      return;
    }
    
    // get method we are returning from (last entry in stack)
    if (!CallGraph.callStack.isEmpty()) {
      int ix = CallGraph.callStack.pop();
      if (ix >= 0 && ix < CallGraph.graphMethList.size()) {
        MethodInfo mthNode = CallGraph.graphMethList.get(ix);
        mthNode.exit(tstamp);
        CallGraph.lastMethod = mthNode;
        //System.out.println("Return: (" + mthNode.getDuration() + ") " + mthNode.getClassAndMethod());
      }
    }
  }
  
}
