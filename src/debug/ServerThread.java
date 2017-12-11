/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;

// provides callback interface
interface MyListener{
  void exit();
}

/**
 *
 * @author dan
 */
public final class ServerThread extends Thread implements MyListener {
 
  private static final String BUFFER_FILE_NAME = "debug.log";
  
  protected DatagramSocket  socket = null;
  private final int         serverPort;
  private Queue<DatagramPacket>  recvBuffer;
  private BufferedReader    bufferedReader;
  private BufferedWriter    bufferedWriter;
  private boolean           running;
  private static int        pktsRead;
  private static int        pktsLost;
  private static int        lastPktCount;
  private static Timer      fileTimer;
  private static String     fileName;

  public ServerThread(int port) throws IOException {
    super("ServerThread");
    
    serverPort = port;
    try {
      // init statistics
      lastPktCount = -1;
      pktsRead = 0;
      pktsLost = 0;
      fileName = null;

      // open the communications socket
      socket = new DatagramSocket(serverPort);
      System.out.println("server started on port: " + serverPort);

      // create file to hold the data from receive buffer (so we don't use too much memory)
      setOutputFile(BUFFER_FILE_NAME);
      
      // set up file for gui to read from (same file)
      setInputFile(BUFFER_FILE_NAME);
      
      // create the receive buffer to hold the messages read from the port
      recvBuffer = new LinkedList<>();

      // create a timer for copying the buffer data to a file
      fileTimer = new Timer(1, new ServerThread.CopyToFileListener());
      fileTimer.start();

      running = true;
    } catch (Exception ex) {
      System.out.println("port " + serverPort + "failed to start");
      System.out.println(ex.getMessage());
      System.exit(1);
    }
  }

  private class CopyToFileListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      // if backup file is available, save current buffer info to it
      if (bufferedWriter != null && !recvBuffer.isEmpty()) {
        try {
          // read packet from buffer & convert to message
          DatagramPacket packet = recvBuffer.remove();
          String message = extractMessageFromPacket(packet);

          // append message to file
          bufferedWriter.write(message + System.getProperty("line.separator"));
          bufferedWriter.flush();
        } catch (IOException ex) {
          System.out.println(ex.getMessage());
        }
      }
    }
  }

  public void clear() {
      // reset statistics and empty the buffer
      lastPktCount = -1;
      pktsRead = 0;
      pktsLost = 0;
      recvBuffer.clear();
      // TODO: flush the bufferedReader
  }
  
  public int getBufferSize() {
    return recvBuffer.size();
  }
  
  public int getPktsRead() {
    return pktsRead;
  }
  
  public int getPktsLost() {
    return pktsLost;
  }
  
  public String getNextMessage() {
    if (bufferedReader != null) {
      try {
        return bufferedReader.readLine();
      } catch (IOException ex) {
        System.out.println(ex.getMessage());
      }
    }

    return null;
  }

  public void closeInputFile() {
    if (bufferedReader != null) {
      if (ServerThread.fileName == null) {
        System.out.println("bufferedReader: disabling input from remote");
      } else {
        System.out.println("bufferedReader: disabling input from file: " + ServerThread.fileName);
      }
      try {
        bufferedReader.close();
      } catch (IOException ex) {
        System.out.println(ex.getMessage());
        return;
      }
      bufferedReader = null;
      ServerThread.fileName = null;
    }
  }
  
  public void setInputFile(String fname) {
    // close the current file reader (if any)
    closeInputFile();
    if (fname == null || fname.isEmpty()) {
      return;
    }
    
    if (!fname.equals(BUFFER_FILE_NAME)) {
      ServerThread.fileName = fname;
    }
    
    // make sure file exists
    File file = new File(fname);
    if (!file.isFile()) {
      System.out.println("Input file not found: " + file.getAbsolutePath());
      return;
    }

    // attach a file reader to it
    try {
      System.out.println("bufferedReader: read from file: " + fname);
      bufferedReader = new BufferedReader(new FileReader(file));
    } catch (FileNotFoundException ex) {
      System.out.println(ex.getMessage());
    }
  }

  private void setOutputFile(String fname) {
    // remove any existing file
    File file = new File(fname);
    file.delete();

    try {
      System.out.println("bufferedWriter: (port capture) started: " + fname);
      bufferedWriter = new BufferedWriter(new FileWriter(fname, true));
    } catch (IOException ex) {  // includes FileNotFoundException
      System.out.println(ex.getMessage());
    }
  }  
  
  private static String formatCounter(int count) {
    if (count < 0) {
      return "--------";
    }
    String countstr = "00000000" + ((Integer) count).toString();
    countstr = countstr.substring(countstr.length() - 8);
    return countstr;
  }
  
  private static String formatTimestamp(long elapsedTime) {
    // split value into hours, min and secs
    Long msecs = elapsedTime % 1000;
    Long secs = (elapsedTime / 1000) % 3600;
    Long mins = secs / 60;
    secs %= 60;

    // now stringify it
    String elapsed = "";
    elapsed += (mins < 10) ? "0" + mins.toString() : mins.toString();
    elapsed += ":";
    elapsed += (secs < 10) ? "0" + secs.toString() : secs.toString();
    String msecstr = "00" + msecs.toString();
    msecstr = msecstr.substring(msecstr.length() - 3);
    elapsed += "." + msecstr;
    return elapsed;
  }

  private static String extractMessageFromPacket(DatagramPacket packet) {
    int count = 0;
    long tstamp = 0;
    String message = "";
    try {
      // read packet
      byte[] bytes = packet.getData();
      ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
      DataInputStream dataIn = new DataInputStream(byteIn);

      // extract the data from the packet
      count = dataIn.readInt();
      tstamp = dataIn.readLong();
      message = dataIn.readUTF();
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }
        
    // keep track of lost packets (we'll skip searching for out-of-order packets for now)
    if (lastPktCount >= 0 && count > lastPktCount + 1) {
      pktsLost += count - lastPktCount - 1;
      message = "ERROR : Lost packets: " + (count - lastPktCount - 1);
      lastPktCount = count;
      return formatCounter(-1) + " [" + formatTimestamp(tstamp) + "] " + message;
    }
    
    lastPktCount = count;
    ++pktsRead;

    return formatCounter(count) + " [" + formatTimestamp(tstamp) + "] " + message;
  }
  
  /**
   * this is the callback to run when exiting
   */
  @Override
  public void exit() {
    running = false;
    try {
      System.out.println("bufferedWriter: closing");
//      bufferedWriter.flush();
      bufferedWriter.close();
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }
  }

  @Override
  public void run() {
    while (running) {
      try {
        byte[] buf = new byte[1024];
 
        // receive message to display
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        
        // add packet to buffer (leak packets if we are straining the buffer)
        if (recvBuffer.size() < 100000) {
          recvBuffer.add(packet);
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
