/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

/**
 *
 * @author dmcd2356
 */
public class CallGraph {
  
  private static JFrame           graphFrame;
  private static JPanel           graphPanel;
  private static Dimension        framesize;
  private static mxGraphComponent graphComponent = null;
  private static BaseGraph<MethodInfo> callGraph = null;
  private static List<MethodInfo> graphMethList = null;
  private static final Dimension  SCREEN_DIM = Toolkit.getDefaultToolkit().getScreenSize();
  
  /**
   * creates a graph panel in which to display the call graph
   * @param name
   */  
  public static void createCallGraphPanel(String name) {
    CallGraph.graphMethList = new ArrayList<>();
    CallGraph.callGraph = new BaseGraph<>();
    
    // create the frame
    CallGraph.framesize = new Dimension(1200, 600);
    //CallGraph.framesize = Toolkit.getDefaultToolkit().getScreenSize();
    CallGraph.graphFrame = new JFrame(name);
    CallGraph.graphFrame.setSize(framesize);
    CallGraph.graphFrame.setMinimumSize(framesize);

    // setup the layout for the frame
    CallGraph.graphFrame.setFont(new Font("SansSerif", Font.PLAIN, 14));
    CallGraph.graphFrame.setLayout(new BorderLayout());

    // add components
    CallGraph.graphPanel = new JPanel();
    JScrollPane scrollPanel = new JScrollPane(CallGraph.graphPanel);
    CallGraph.graphFrame.add(scrollPanel, BorderLayout.CENTER);
    CallGraph.graphFrame.pack();
    
    // display the frame
    CallGraph.graphFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    CallGraph.graphFrame.setLocationRelativeTo(null);
    CallGraph.graphFrame.setVisible(true);
  }
  
  /**
   * updates the call graph display
   */  
  private static void updateCallGraph() {
    if (CallGraph.callGraph != null) {
      mxGraph graph = CallGraph.callGraph.getGraph();
      if (graphComponent == null) {
        // first time through - create the graph component
        graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        CallGraph.graphPanel.add(graphComponent);
      } else {
        // otherwise, just update the contents of the graph component
        Graphics graphics = graphPanel.getGraphics();
        if (graphics != null) {
          graphPanel.update(graphics);
        }
      }
    }
    
    // update the graph layout
//    CallGraph.graphFrame.revalidate();
//    CallGraph.graphFrame.repaint();
    CallGraph.callGraph.layoutGraph();
    CallGraph.graphFrame.pack();
    CallGraph.graphFrame.setSize(framesize);
  }
  
  /**
   * adds a method to the call graph if it is not already there
   * 
   * @param method - the full name of the method to add
   * @param parent - the full name of the caller
   */  
  public static void callGraphAddMethod(String method, String parent) {
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
        mthNode.incCount(); // inc # of times method called
      }
    }
    // if not found, create new one and add it to list
    if (mthNode == null) {
      mthNode = new MethodInfo(method);
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
        parNode = new MethodInfo(parent);
        CallGraph.graphMethList.add(parNode);
        //System.out.println("AddParent: " + parNode.getClassAndMethod());
        CallGraph.callGraph.addVertex(parNode, parNode.getCGName());
        bUpdate = true;
      }
      //System.out.println("AddMethod: (" + mthNode.getCount() + ") " + mthNode.getClassAndMethod() +
      //    ", parent = " + parNode.getClassAndMethod());
    } else {
      //System.out.println("AddMethod: (" + mthNode.getCount() + ") " + mthNode.getClassAndMethod() +
      //    ", (no parent)");
    }

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
      updateCallGraph();
    }
  }

  /**
   * adds exit condition info to a method in the call graph
   * 
   * @param method - the full name of the method that is exiting
   */  
  public static void callGraphReturn(String method) {
    if (CallGraph.graphMethList == null) {
      //System.out.println("Return: " + method + " - NOT FOUND!");
      return;
    }
    
    // find method entry in list
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
