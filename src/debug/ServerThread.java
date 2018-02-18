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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
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
  
  protected DatagramSocket  dataSocket = null;
  protected ServerSocket    serverSocket = null;
  protected Socket          connectionSocket = null;
  private final int         serverPort;
  private Queue<String>     recvBuffer;
  private BufferedReader    inFromClient = null;
  private BufferedReader    bufferedReader;
  private BufferedWriter    bufferedWriter;
  private boolean           running;
  private static int        pktsRead;
  private static int        pktsLost;
  private static int        lastPktCount;
  private static Timer      fileTimer;
  private static String     fileName; // file to read from instead of from network (null if network used)
  private static String     storageFileName;

  public ServerThread(int port, boolean tcp, String fname) throws IOException {
    super("ServerThread");
    
    serverPort = port;
    try {
      // init statistics
      lastPktCount = -1;
      pktsRead = 0;
      pktsLost = 0;
      fileName = null;
      storageFileName = null;

      // open the communications socket
      if (tcp) {
        serverSocket = new ServerSocket(serverPort);
        System.out.println("TCP server started on port: " + serverPort + ", waiting for connection");
      } else {
        dataSocket = new DatagramSocket(serverPort);
        System.out.println("UDP server started on port: " + serverPort);
      }

      // create file to hold the data from receive buffer (so we don't use too much memory)
      if (fname == null || fname.isEmpty()) {
        fname = BUFFER_FILE_NAME;
      }
      setOutputFile(fname);
      
      // set up file for gui to read from (same file)
      setInputFile(fname);
      
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
          // read next message from input buffer
          String message = recvBuffer.remove();

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
  
  public String getOutputFile() {
    return storageFileName;
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

  public void setOutputFile(String fname) {
    // ignore if name was not changed
    if (fname == null || (storageFileName != null && fname.equals(storageFileName))) {
      return;
    }
    // remove any existing file
    File file = new File(fname);
    file.delete();

    try {
      if (bufferedWriter != null) {
        System.out.println("bufferedWriter: closing " + storageFileName);
        bufferedWriter.close();
      }
      storageFileName = file.getAbsolutePath();
      System.out.println("bufferedWriter: port capture saving to: " + fname);
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
    String message = "";
    try {
      // read message from packet
      byte[] bytes = packet.getData();
      ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
      DataInputStream dataIn = new DataInputStream(byteIn);
      message = dataIn.readUTF();

      // keep track of lost packets (1st 8 chars are the incrementing line count)
      int count = Integer.parseInt(message.substring(0, 8));
      if (lastPktCount >= 0 && count > lastPktCount + 1) {
        pktsLost += count - lastPktCount - 1;
        message = "ERROR : Lost packets: " + (count - lastPktCount - 1);
      } else {
        ++pktsRead;
      }
      lastPktCount = count;
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }

    return message;
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
        if (dataSocket != null) {
          // UDP socket connection...
          // get next packet received
          byte[] buf = new byte[1024];
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          dataSocket.receive(packet);
        
          // add message to buffer (lose messages if we are overrunning the buffer)
          if (recvBuffer.size() < 100000) {
            byte[] bytes = packet.getData();
            ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
            DataInputStream dataIn = new DataInputStream(byteIn);
            String message = dataIn.readUTF();

            recvBuffer.add(message);
            ++pktsRead;

            // keep track of lost packets (1st 8 chars are the incrementing line count)
            int count = Integer.parseInt(message.substring(0, 8));
            if (lastPktCount >= 0 && count > lastPktCount + 1) {
              pktsLost += count - lastPktCount - 1;
              message = "ERROR : Lost packets: " + (count - lastPktCount - 1);
              recvBuffer.add(message);
            }
            lastPktCount = count;
          }
        } else {
          // TCP socket connection...
          // accept connection if no connection yet
          // we only handle 1 connection at a time - any prev connection must first be closed
          if (connectionSocket == null) {
            try {
              connectionSocket = serverSocket.accept();
              System.out.println("connected to client port: " + connectionSocket.getPort());
              InputStream cis = connectionSocket.getInputStream();
              inFromClient = new BufferedReader(new InputStreamReader(cis));
              //DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            } catch (IOException ex) {
              System.err.println("ERROR: " + ex.getMessage());
              try {
                serverSocket.close();
              } catch (IOException ex1) {
                // ignore - we are exiting anyway
              } finally {
                System.exit(1);
              }
            }
          }
        
          // read input from client and add to buffer
          String message = inFromClient.readLine();
          if (message == null) {
            // client disconnected - close the socket so a new connection can be made
            connectionSocket.shutdownInput();
            connectionSocket.close();
            connectionSocket = null;
          }
          recvBuffer.add(message);
          ++pktsRead;
        }
      } catch (IOException ex) {
        System.err.println("ERROR: " + ex.getMessage());
      }
    }

    // terminating loop - close all sockets
    if (dataSocket != null) {
      System.out.println("closing UDP socket for port: " + serverPort);
      dataSocket.close();
    }
    try {
      if (connectionSocket != null) {
        System.out.println("closing TCP connection socket");
        connectionSocket.close();
      }
      if (serverSocket != null) {
        System.out.println("closing TCP server socket for port: " + serverPort);
        serverSocket.close();
      }
    } catch (IOException ex) {
      // ignore
    }
  }

}
