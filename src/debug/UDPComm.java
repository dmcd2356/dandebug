/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import java.io.IOException;

/**
 *
 * @author dan
 */
public class UDPComm {

  public static int SERVER_PORT = 5000;
  
  /**
   * @param args the command line arguments
   * @throws java.io.IOException
   */
  public static void main(String[] args) throws IOException {
    // start the debug message panel
    GuiPanel gui = new GuiPanel();
    gui.createDebugPanel();
  }
  
}
