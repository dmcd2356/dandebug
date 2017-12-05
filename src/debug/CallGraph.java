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
  private static Stack<Integer>   callStack = null;
  private static boolean bUpdateNeeded = false;
  private static long maxDuration = 0;
  private static int maxCount = 0;
  
  public static void initCallGraph(JPanel panel) {
    CallGraph.graphMethList = new ArrayList<>();
    CallGraph.callStack = new Stack<>();
    CallGraph.callGraph = new BaseGraph<>();
    CallGraph.graphComponent = null;
    CallGraph.maxDuration = 0;
    CallGraph.maxCount = 1;

    CallGraph.graphPanel = panel;
  }

  public static void clear() {
    CallGraph.graphMethList = new ArrayList<>();
    CallGraph.callStack = new Stack<>();
    CallGraph.callGraph = new BaseGraph<>();
    CallGraph.graphComponent = null;
    CallGraph.maxDuration = 0;
    CallGraph.maxCount = 1;

    CallGraph.graphPanel.removeAll();
    Graphics graphics = graphPanel.getGraphics();
    if (graphics != null) {
      graphPanel.update(graphics);
    }

    updateCallGraph();
  }

  public static int getMethodCount() {
    if (graphMethList != null) {
      return graphMethList.size();
    }
    return 0;
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
   * @return true if graph was updated
   */  
  public static boolean updateCallGraph() {
    boolean updated = false;
    if (!GuiPanel.isCallGraphTabSelected()) {
      return updated;
    }
    if (bUpdateNeeded && CallGraph.callGraph != null && CallGraph.graphPanel != null) {
      bUpdateNeeded = false;

      mxGraph graph = CallGraph.callGraph.getGraph();
      if (graphComponent == null) {
        // first time through - create the graph component
        graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        
        // add listener to show details of selected element
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
          @Override
          public void mouseReleased(MouseEvent e) {
            mxGraphHandler handler = graphComponent.getGraphHandler();
            mxCell cell = (mxCell) handler.getGraphComponent().getCellAt(e.getX(), e.getY());
            if (cell != null && cell.isVertex()) {
              MethodInfo selected = CallGraph.callGraph.getSelectedNode();
              JOptionPane.showMessageDialog (null,
                  "Method:   " + selected.getFullName() + NEWLINE +
                  "Count:    " + selected.getCount() + NEWLINE +
                  "Duration: " + selected.getDuration() + NEWLINE +
                  "1st call: " + selected.getFirstLine() + NEWLINE +
                  "last call:" + selected.getLastLine(),
                  "Method Info",
                  JOptionPane.INFORMATION_MESSAGE);
            }
          }
        });
        CallGraph.graphPanel.add(graphComponent);
      } else {
        // otherwise, just update the contents of the graph component
        Graphics graphics = graphPanel.getGraphics();
        if (graphics != null) {
          graphPanel.update(graphics);
        }
      }

      // update colors based on time usage or number of calls
      for (int ix = 0; ix < CallGraph.graphMethList.size(); ix++) {
        MethodInfo mthNode = CallGraph.graphMethList.get(ix);
        long duration = CallGraph.graphMethList.get(ix).getDuration();
        // colors used in janalyzer: "#FFA07A", "#FA8072", "#FF0000", "#8B0000", "pink", "green"
        String color = "D2E9FF";
        if (duration >= (maxDuration * 8) / 10) {
          color = "FF6666";
        }
        else if (duration >= (maxDuration * 6) / 10) {
          color = "FF8888";
        }
        else if (duration >= (maxDuration * 4) / 10) {
          color = "FFAAAA";
        }
        else if (duration >= (maxDuration * 2) / 10) {
          color = "FFCCCC";
        }
        CallGraph.callGraph.colorVertex(mthNode, color);
        //System.out.println(color + " for: " + mthNode.getFullName());
      }

      // update the graph layout
      CallGraph.callGraph.layoutGraph();
      updated = true;
    }

    return updated;
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
    boolean bUpdate = false;
    MethodInfo mthNode = null;
    boolean newnode = false;
    int count = CallGraph.graphMethList.size();
    for (int ix = 0; ix < count; ix++) {
      if (CallGraph.graphMethList.get(ix).getFullName().equals(method)) {
        CallGraph.callStack.push(ix); // save entry as last method called
        mthNode = CallGraph.graphMethList.get(ix);
        mthNode.incCount(line); // inc # of times method called
        int newCount = mthNode.getCount();
        if (CallGraph.maxCount < newCount) {
          CallGraph.maxCount = newCount;
        }
      }
    }
    // if not found, create new one and add it to list
    if (mthNode == null) {
      CallGraph.callStack.push(count); // save entry as last method called
      mthNode = new MethodInfo(method, tstamp, line);
      CallGraph.graphMethList.add(mthNode);
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
      if (parNode == null) {
        parNode = new MethodInfo(parent, 0, -1);
        CallGraph.graphMethList.add(parNode);
        //System.out.println("AddParent: " + parNode.getClassAndMethod());
        CallGraph.callGraph.addVertex(parNode, parNode.getCGName());
        bUpdate = true;
      }
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
      bUpdate = true;
    }
    if (parNode != null && CallGraph.callGraph.getEdge(parNode, mthNode) == null) {
      CallGraph.callGraph.addEdge(parNode, mthNode, null);
      bUpdate = true;
    }
    
    // update graph
    if (bUpdate) {
      bUpdateNeeded = true; // TODO: really needs to be locked
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
        long newDuration = mthNode.getDuration();
        if (CallGraph.maxDuration < newDuration) {
          CallGraph.maxDuration = newDuration;
        }
        //System.out.println("Return: (" + mthNode.getDuration() + ") " + mthNode.getClassAndMethod());
      }
    }
  }
  
}
