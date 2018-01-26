/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.handler.mxGraphHandler;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
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
  private static long maxDuration;
  private static int maxCount;
  private static GuiPanel.GraphHighlight curGraphMode;
  
  public static void initCallGraph(JPanel panel) {
    CallGraph.graphMethList = new ArrayList<>();
    CallGraph.callStack = new Stack<>();
    CallGraph.callGraph = new BaseGraph<>();
    CallGraph.graphComponent = null;
    CallGraph.numNodes = 0;
    CallGraph.numEdges = 0;
    CallGraph.maxDuration = 0;
    CallGraph.maxCount = 1;
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
    CallGraph.maxDuration = 0;
    CallGraph.maxCount = 1;
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

      // find max instruction count
      int maxInstrCount = 0;
      for (int ix = 0; ix < CallGraph.graphMethList.size(); ix++) {
        MethodInfo mthNode = CallGraph.graphMethList.get(ix);
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
            ratio = (double) count / (double) maxCount;
            // this runs from 6666FF (blue) to CCCCFF (light blue)
            colorR = 204 - (int) (102.0 * ratio);
            colorG = 204 - (int) (102.0 * ratio);
            colorB = 255;
            color = String.format ("%06x", (colorR << 16) + (colorG << 8) + colorB);
            if (ratio < 0.2 || (count < 10 && count < maxCount)) {
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
          (selected.getCount() <= 1 ?
              "" :
              "last called @ line:" + selected.getLastLine()) +
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
   * reads the specified method entries from the list and converts to string (for saving to file)
   * 
   * @return a string containing the method data
   */  
  public static String callGraphDataSave() {
    String response = "";
    for (MethodInfo mthNode : CallGraph.graphMethList) {
      response += mthNode.getFullName() + ", ";
      response += mthNode.getDuration() + ", ";
      response += mthNode.getFirstLine() + ", ";
      response += mthNode.getCount() + ", ";
      response += mthNode.getInstructionCount() + ", ";
      response += mthNode.getExecption() + ", ";
      response += mthNode.getError() + NEWLINE;
      if (mthNode.getParents().size() > 0) {
        for (String parent : mthNode.getParents()) {
          response += "    " + parent;
        }
        response += NEWLINE;
      }
    }
    return response;
  }
  
  /**
   * reads the specified method entry from the list and converts to string (for saving to file)
   * 
   * @param content - string containing method information to setup
   */  
  public static void callGraphDataRead(String content) {
    clear(); // remove all existing data before we start
    
    String method = "";
    String eachline[] = content.split(NEWLINE); //("\\r\\n|\\n|\\r");
    for (String line : eachline) {
      String eachitem[] = line.split(",");
      switch (eachitem.length) {
        case 1:
          // this handles the list of parents to a method
          if (method.isEmpty()) { // if this happens, the file started with this entry, which is wrong
            continue;
          } String parent = eachitem[0].substring(1).trim();
          callGraphAddParent(method, parent);
          break;
        case 7:
          // this handles the method definition
          method = eachitem[0].trim();
          long duration = Long.parseLong(eachitem[1].trim());
          int  linenum = Integer.parseInt(eachitem[2].trim());
          int  count   = Integer.parseInt(eachitem[3].trim());
          int  instr   = Integer.parseInt(eachitem[4].trim());
          int  except  = Integer.parseInt(eachitem[5].trim());
          int  error   = Integer.parseInt(eachitem[6].trim());
          callGraphSetEntry(method, null, duration, linenum, count, instr, except, error);
          break;
        default:
          // error
          break;
      }
    }
    
    // now connect the methods to their parents
    callGraphConnectParents();
  }
  
  /**
   * adds a method entry (when loading from file)
   * 
   * @param method       - the full name of the method to add
   * @param parent       - the full name of the caller
   * @param duration     - duration of method call in msec
   * @param line         - the line number corresponding to the call event
   * @param count        - number of times method was called
   * @param instructions - number of instruction executed in method
   * @param exception    - line # of last exception (-1 if none)
   * @param error        - line # of last error     (-1 if none)
   */  
  public static void callGraphSetEntry(String method, String parent, long duration, int line,
                                       int count, int instructions, int exception, int error) {
    if (method == null || method.isEmpty()) {
      return;
    }

    MethodInfo mthNode = new MethodInfo(method, parent, duration, line, count, instructions, exception, error);
    CallGraph.graphMethList.add(mthNode);
    CallGraph.lastMethod = mthNode;
    CallGraph.callGraph.addVertex(mthNode, mthNode.getCGName());
  }

  /**
   * adds a new caller to a method entry (when loading from file)
   * 
   * @param method - the full name of the method to add (must have been previous defined)
   * @param parent - the full name of the caller
   */  
  public static void callGraphAddParent(String method, String parent) {
    if (method == null || method.isEmpty() || parent == null || parent.isEmpty()) {
      return;
    }
    MethodInfo mthNode = findMethodEntry(method);
    if (mthNode != null) {
      mthNode.addParent(parent);
    }
  }

  /**
   * this will traverse the methods saved in graphMethList and connect each method to its parent.
   * This assumes that the vertexes have already been defined in callGraph prior to this call.
   */
  public static void callGraphConnectParents() {
    // loop through all methods defined
    for (MethodInfo mthNode : CallGraph.graphMethList) {
      // for each parent entry for a method...
      for (String parent : mthNode.getParents()) {
        // find MethodInfo for the parent
        MethodInfo parNode = findMethodEntry(parent);
        if (parNode != null) {
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
        int newCount = mthNode.getCount();
        if (CallGraph.maxCount < newCount) {
          CallGraph.maxCount = newCount;
        }
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

    // find parent entry
    MethodInfo parNode = null;
    if (!parent.isEmpty()) {
      for (int ix = 0; ix < CallGraph.graphMethList.size(); ix++) {
        if (CallGraph.graphMethList.get(ix).getFullName().equals(parent)) {
          parNode = CallGraph.graphMethList.get(ix);
          break;
        }
      }
//      if (parNode == null) {
//        parNode = new MethodInfo(parent, 0, -1);
//        CallGraph.graphMethList.add(parNode);
//        //System.out.println("AddParent: " + parNode.getClassAndMethod());
//        CallGraph.callGraph.addVertex(parNode, parNode.getCGName());
//      }
      //System.out.println("AddMethod: (" + mthNode.getCount() + ") " + mthNode.getClassAndMethod() +
      //    ", parent = " + parNode.getClassAndMethod());
    }
    //else {
    //  System.out.println("AddMethod: (" + mthNode.getCount() + ") " + mthNode.getClassAndMethod() +
    //      ", (no parent)");
    //}

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
        long newDuration = mthNode.getDuration();
        if (CallGraph.maxDuration < newDuration) {
          CallGraph.maxDuration = newDuration;
        }
        //System.out.println("Return: (" + mthNode.getDuration() + ") " + mthNode.getClassAndMethod());
      }
    }
  }
  
}
