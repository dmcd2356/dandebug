/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

// provides callback interface
interface MyListener{
  void exit();
}

/**
 *
 * @author dan
 */
public class ServerThread extends Thread implements MyListener {
 
  protected DatagramSocket      socket = null;
  private final int             serverPort;
  private Queue<DatagramPacket> recvBuffer;
  private boolean               running;

  public ServerThread(int port) throws IOException {
    super("ServerThread");
    
    serverPort = port;
    try {
      // open the communications socket
      socket = new DatagramSocket(serverPort);
      System.out.println("server started on port: " + serverPort);

      // create the receive buffer to hold the messages
      recvBuffer = new LinkedList<>();
      running = true;
    } catch (Exception ex) {
      System.out.println("port " + serverPort + "failed to start");
      System.out.println(ex.getMessage());
      System.exit(1);
    }
  }
  
  public void getNextPacket() {
    if (!recvBuffer.isEmpty()) {
      try {
        DatagramPacket packet = recvBuffer.remove();
        
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
          
          // now send to the debug message display
          DebugMessage.print(count, tstamp, message.trim());
        }
      } catch (IOException ex) {
        System.out.println(ex.getMessage());
      }
    }
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
        byte[] buf = new byte[1024];
 
        // receive message to display
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        recvBuffer.add(packet);
        
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
