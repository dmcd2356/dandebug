/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import java.io.*;
import java.net.*;

// provides callback interface
interface MyListener{
  void exit();
}

/**
 *
 * @author dan
 */
public class ServerThread extends Thread implements MyListener {
 
  protected DatagramSocket socket = null;
  private final GuiPanel   guipanel;
  private final int        serverPort;
  private boolean          running;

  public ServerThread(int port) throws IOException {
    super("ServerThread");
    
    // open the communications socket
    serverPort = port;
    socket = new DatagramSocket(serverPort);
    System.out.println("server started on port: " + serverPort);
    
    // create call graph panel
    CallGraph.createCallGraphPanel("Call Graph");

    // create the debug message panel to direct messages to
    guipanel = new GuiPanel();
    GuiPanel.createDebugPanel("Debug Messages");
    guipanel.addListener(this);
    running = true;
  }
  
  /**
   * this is the callback to run when exiting
   */
  @Override
  public void exit() {
    running = false;
  }

  @Override
  public void run() {
    while (running) {
      try {
        byte[] buf = new byte[256];
 
        // receive message to display
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
 
        // send message to debug panel
        byte[] bytes = packet.getData();
        ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
        DataInputStream dataIn = new DataInputStream(byteIn);
        int count = dataIn.readInt();
        long tstamp = dataIn.readLong();
        String message = dataIn.readUTF();

        // seperate message into the message type and the message content
        if (message.length() > 7) {
          String typestr = message.substring(0, 6).trim();
          String content = message.substring(7);
          // send CALL & RETURN info to CallGraph
          if (typestr.equals("CALL")) {
            int offset = content.indexOf('|');
            if (offset > 0) {
              String method = content.substring(0, offset).trim();
              String parent = content.substring(offset + 1).trim();
              CallGraph.callGraphAddMethod(method, parent);
            }
          }
          else if (typestr.equals("RETURN")) {
            CallGraph.callGraphReturn(content);
          }
//          else {
            // the remainder goes to the debug message display
            DebugMessage.print(count, tstamp, message.trim());
//          }
        }
        
        // send the response to the client at "address" and "port"
//        InetAddress address = packet.getAddress();
//        int port = packet.getPort();
//        packet = new DatagramPacket(buf, buf.length, address, port);
//        socket.send(packet);
      } catch (IOException ex) {
        System.out.println("ERROR: " + ex.getMessage());
      }
    }

    System.out.println("closing socket for port: " + serverPort);
    socket.close();
  }

}
