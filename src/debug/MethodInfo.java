/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

/**
 *
 * @author dmcd2356
 */
public class MethodInfo {
  private String  fullName;       // full name of method (package, class, method + signature)
  private String  className;      // class name (no package info or method name)
  private String  methName;       // method name (no class info)
  private int     count;          // number of times method called
  private int     instrEntry;     // the number of instructions executed upon entry to the method
  private int     instrExit;      // the number of instructions executed upon exit from the method
  private int     lineFirst;      // line number corresponding to 1st call to method
  private int     lineLast;       // line number corresponding to last call to method
  private long    duration_ms;    // total duration in method
  private long    start_ref;      // timestamp when method last called
  private boolean returned;       // indicates the return has been logged for the method
  
  private static final String NEWLINE = System.getProperty("line.separator");

  public MethodInfo(String method, long tstamp, int line) {
    fullName = className = methName = "";
    if (method != null && !method.isEmpty()) {
      // fullName should be untouched - it is used for comparisons
      fullName = method;
      String cleanName = method.replace("/", ".");
      if (cleanName.contains("(")) {
        cleanName = cleanName.substring(0, cleanName.lastIndexOf("("));
      }
      if (!cleanName.contains(".")) {
        methName = cleanName;
      } else {
        methName = cleanName.substring(cleanName.lastIndexOf(".") + 1);
        className = cleanName.substring(0, cleanName.lastIndexOf("."));
        if (className.contains(".")) {
          className = className.substring(className.lastIndexOf(".") + 1);
        }
      }
    }

    count = 1;
    lineFirst = line;
    lineLast = line;
    duration_ms = 0;
    start_ref = tstamp;
    instrEntry = 0;
    instrExit = 0;
    returned = false;
    //System.out.println("start time: " + start_ref + " (init) - " + fullName);
  }
  
  public void incCount(int line) {
    ++count;
    lineLast = line;
    start_ref = System.currentTimeMillis();
    //System.out.println("start time: " + start_ref + ", count " + count + " - " +  fullName);
  }
  
  public void exit(long tstamp) {
    long elapsedTime = tstamp - start_ref;
    if (elapsedTime > 0) {
      duration_ms += elapsedTime;
    }
    returned = true;
    //System.out.println("exit time: " + currentTime + ", elapsed " + duration_ms + " - " +  fullName);
  }
  
  public void setInstrEntry(int count) {
    instrEntry = count;
  }
  
  public void setInstrExit(int count) {
    instrExit = count;
  }
  
  public int getInstructionCount() {
    return (instrExit > instrEntry) ? instrExit - instrEntry : 0;
  }
  
  public boolean isReturned() {
    return returned;
  }
  
  public String getFullName() {
    return fullName;
  }
  
  public String getClassAndMethod() {
    return className.isEmpty() ? methName : className + "." + methName;
  }
  
  public String getCGName() {
    return className.isEmpty() ? methName : className + NEWLINE + methName;
  }
  
  public String getMethodName() {
    return methName;
  }
  
  public int getCount() {
    return count;
  }
  
  public int getFirstLine() {
    return lineFirst;
  }
  
  public int getLastLine() {
    return lineLast;
  }
  
  public long getDuration() {
    return duration_ms;
  }
}
