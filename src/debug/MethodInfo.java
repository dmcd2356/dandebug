/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import com.google.gson.annotations.Expose;
import java.util.ArrayList;

/**
 *
 * @author dmcd2356
 */
public class MethodInfo {
  private static final String NEWLINE = System.getProperty("line.separator");

  @Expose
  private String  fullName;       // full name of method (package, class, method + signature)
  @Expose
  private String  className;      // class name (no package info or method name)
  @Expose
  private String  methName;       // method name (no class info)
  @Expose
  private int     lineFirst;      // line number corresponding to 1st call to method
  @Expose
  private int     callCount;      // number of times method called
  @Expose
  private int     instrCount;     // the number of instructions executed by the method
  @Expose
  private long    duration_ms;    // total duration in method
  @Expose
  private int     lineExcept;     // line number of last exception that occurred in this method
  @Expose
  private int     lineError;      // line number of last error that occurred in this method
  @Expose
  private ArrayList<String> parents; // list of caller methods
  // these are intermediate values
  private long    start_ref;      // timestamp when method last called
  private int     instrEntry;     // the number of instructions executed upon entry to the method
  private boolean exit;           // true if return has been logged, false if method just entered
  
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
    callCount = 1;
    start_ref = tstamp;
    duration_ms = -1;
    instrEntry = 0;
    instrCount = -1;
    exit = false;
    lineExcept = -1;
    lineError = -1;
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
    callCount = cnt;
    start_ref = 0;          // don't care
    duration_ms = duration;
    instrEntry = 0;         // don't care
    instrCount = instructions;
    exit = duration >= 0;
    lineExcept = exception;
    lineError = error;
  }
  
  public void incCount(int line) {
    ++callCount;
    start_ref = System.currentTimeMillis();
    exit = false;
    //System.out.println("start time: " + start_ref + ", callCount " + callCount + " - " +  fullName);
  }

  public void addParent(String parent) {
    // if caller entry not already in list, add it
    if (parents.indexOf(parent) < 0) {
      parents.add(parent);
    }
  }
  
  public void setExecption(int line) {
    lineExcept = line;
  }
  
  public void setError(int line) {
    lineError = line;
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
    instrCount += (count > instrEntry) ? count - instrEntry : 0;
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
    return callCount;
  }
  
  public int getExecption() {
    return lineExcept;
  }
  
  public int getError() {
    return lineError;
  }
  
  public int getFirstLine() {
    return lineFirst;
  }
  
  public long getDuration() {
    return duration_ms;
  }
}
