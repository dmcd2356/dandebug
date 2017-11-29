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
    // get any arguments passed
    // -m     = disable the debug message display
    // -g     = disable the graphics display
    // <port> = the specified port to use (default is 5000)
    int port = SERVER_PORT;
    boolean bMessage = true;
    boolean bGraph = true;
    for (int ix = 0; ix < args.length; ix++) {
      if (args[ix].equals("-m")) {
        bMessage = false;
      }
      else if (args[ix].equals("-g")) {
        bGraph = false;
      }
      else {
        try {
          port = Integer.parseInt(args[ix]);
          if (port < 100 || port > 65535) {
            System.out.println("Invalid port selection: " + port);
            System.exit(1);
          }
        } catch (Exception ex) {
          System.out.println("Invalid option: " + args[ix]);
          System.exit(1);
        }
      }
    }
    
    // start the debug message panel
    GuiPanel gui = new GuiPanel();
    gui.createDebugPanel(port, bMessage, bGraph);
  }
  
}
