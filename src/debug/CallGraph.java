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
  private static boolean bUpdateNeeded = false;
  
  public static void initCallGraph(JPanel panel) {
    CallGraph.graphMethList = new ArrayList<>();
    CallGraph.callGraph = new BaseGraph<>();
    CallGraph.graphComponent = null;
    CallGraph.graphPanel = panel;
  }

  public static void clear() {
    CallGraph.graphMethList = new ArrayList<>();
    CallGraph.callGraph = new BaseGraph<>();
    CallGraph.graphComponent = null;

    CallGraph.graphPanel.removeAll();
    Graphics graphics = graphPanel.getGraphics();
    if (graphics != null) {
      graphPanel.update(graphics);
    }

    updateCallGraph();
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

      // update the graph layout
      CallGraph.callGraph.layoutGraph();
      updated = true;
    }

    return updated;
  }
  
  /**
   * adds a method to the call graph if it is not already there
   * 
   * @param method - the full name of the method to add
   * @param parent - the full name of the caller
   * @param line   - the line number corresponding to the call event
   */  
  public static void callGraphAddMethod(String method, String parent, int line) {
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
    for (int ix = 0; ix < CallGraph.graphMethList.size(); ix++) {
      if (CallGraph.graphMethList.get(ix).getFullName().equals(method)) {
        mthNode = CallGraph.graphMethList.get(ix);
        mthNode.incCount(line); // inc # of times method called
      }
    }
    // if not found, create new one and add it to list
    if (mthNode == null) {
      mthNode = new MethodInfo(method, line);
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
        parNode = new MethodInfo(parent, -1);
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
   * @param method - the full name of the method that is exiting
   */  
  public static void callGraphReturn(String method) {
    if (CallGraph.graphMethList == null || method == null) {
      //System.out.println("Return: " + method + " - NOT FOUND!");
      return;
    }
    
    // find method entry in list
    method = method.trim();
    for (int ix = 0; ix < CallGraph.graphMethList.size(); ix++) {
      if (CallGraph.graphMethList.get(ix).getFullName().equals(method)) {
        MethodInfo mthNode = CallGraph.graphMethList.get(ix);
        mthNode.exit();
        //System.out.println("Return: " + mthNode.getClassAndMethod() + " time = " + mthNode.getDuration());
        return;
      }
    }
  }
  
}
