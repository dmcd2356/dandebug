/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import java.util.ArrayList;

/**
 *
 * @author dmcd2356
 */
public class MethodInfo {
  private String  fullName;       // full name of method (package, class, method + signature)
  private String  className;      // class name (no package info or method name)
  private String  methName;       // method name (no class info)
  private ArrayList<String> parents; // list of caller methods
  private int     count;          // number of times method called
  private int     instrEntry;     // the number of instructions executed upon entry to the method
  private int     instrExit;      // the number of instructions executed upon exit from the method
  private int     instrCount;     // the number of instructions executed by the method
  private int     lineFirst;      // line number corresponding to 1st call to method
  private int     lineLast;       // line number corresponding to last call to method
  private long    duration_ms;    // total duration in method
  private long    start_ref;      // timestamp when method last called
  private boolean exit;           // true if return has been logged, false if method just entered
  private int     ln_except;      // line number of last exception that occurred in this method
  private int     ln_error;       // line number of last error that occurred in this method
  
  private static final String NEWLINE = System.getProperty("line.separator");

  public MethodInfo(String method, String parent, long tstamp, int line) {
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

    // init 1st caller of method
    parents = new ArrayList<>();
    if (parent != null && !parent.isEmpty()) {
      parents.add(parent);
    }

    lineFirst = line;
    lineLast = line;
    count = 1;
    start_ref = tstamp;
    duration_ms = -1;
    instrEntry = 0;
    instrExit = 0;
    instrCount = -1;
    exit = false;
    ln_except = -1;
    ln_error = -1;
    //System.out.println("start time: " + start_ref + " (init) - " + fullName);
  }

  public MethodInfo(String method, String parent, long duration, int line, int cnt, int instructions, int exception, int error) {
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

    // init 1st caller of method
    parents = new ArrayList<>();
    if (parent != null && !parent.isEmpty()) {
      parents.add(parent);
    }

    lineFirst = line;
    lineLast = line;        // don't care
    count = cnt;
    start_ref = 0;          // don't care
    duration_ms = duration;
    instrEntry = 0;         // don't care
    instrExit = 0;          // don't care
    instrCount = instructions;
    exit = duration >= 0;
    ln_except = exception;
    ln_error = error;
  }
  
  public void incCount(int line) {
    ++count;
    lineLast = line;
    start_ref = System.currentTimeMillis();
    exit = false;
    //System.out.println("start time: " + start_ref + ", count " + count + " - " +  fullName);
  }

  public void addParent(String parent) {
    // if caller entry not already in list, add it
    if (parents.indexOf(parent) < 0) {
      parents.add(parent);
    }
  }
  
  public void setExecption(int line) {
    ln_except = line;
  }
  
  public void setError(int line) {
    ln_error = line;
  }
  
  public void exit(long tstamp) {
    if (duration_ms < 0) {
      duration_ms = 0;
    }
    long elapsedTime = (tstamp > start_ref) ? tstamp - start_ref : 0;
    duration_ms += elapsedTime;
    exit = true;
    //System.out.println("exit time: " + currentTime + ", elapsed " + duration_ms + " - " +  fullName);
  }
  
  public void setInstrEntry(int count) {
    instrEntry = count;
  }
  
  public void setInstrExit(int count) {
    if (instrCount < 0) {
      instrCount = 0;
    }
    instrExit = count;
    instrCount += (instrExit > instrEntry) ? instrExit - instrEntry : 0;
  }

  public ArrayList<String> getParents() {
    return parents;
  }
  
  public int getInstructionCount() {
    return instrCount;
  }
  
  public boolean isReturned() {
    return exit;
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
  
  public int getExecption() {
    return ln_except;
  }
  
  public int getError() {
    return ln_error;
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
