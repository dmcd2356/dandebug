/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

//import com.google.gson.annotations.Expose;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author dmcd2356
 */
public class MethodInfo {
  private static final String NEWLINE = System.getProperty("line.separator");

  private String  fullName;       // full name of method (package, class, method + signature)
  private String  className;      // class name (no package info or method name)
  private String  methName;       // method name (no class info)
  private int     lineFirst;      // line number corresponding to 1st call to method
  private int     callCount;      // number of times method called
  private int     instrCount;     // the number of instructions executed by the method
  private long    duration_ms;    // total duration in method
  private int     lineExcept;     // line number of last exception that occurred in this method
  private int     lineError;      // line number of last error that occurred in this method
  private ArrayList<String> parents; // list of caller methods
  private HashMap<String, Integer> uninstrumented;  // the uninstrumented (library) calls
  // these are intermediate values
  private long    start_ref;      // timestamp when method last called
  private int     instrEntry;     // the number of instructions executed upon entry to the method
  private boolean exit;           // true if return has been logged, false if method just entered
  
  public MethodInfo(String method, String parent, long tstamp, int insCount, int line) {
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
    uninstrumented = new HashMap<>();
    parents = new ArrayList<>();
    if (parent != null && !parent.isEmpty()) {
      parents.add(parent);
    }

    lineFirst = line;
    callCount = 1;
    start_ref = tstamp;
    duration_ms = -1;
    instrEntry = insCount;
    instrCount = -1;
    exit = false;
    lineExcept = -1;
    lineError = -1;
    //System.out.println("start time: " + start_ref + " (init) - " + fullName);
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
  
  public void addUninstrumented(String method) {
    // if caller entry not already in list add it, otherwise incr the value associated with it
    if (uninstrumented.containsKey(method)) {
      uninstrumented.replace(method, uninstrumented.get(method) + 1);
    } else {
      uninstrumented.put(method, 1);
    }
  }
  
  public void setExecption(int line) {
    lineExcept = line;
  }
  
  public void setError(int line) {
    lineError = line;
  }
  
  public void exit(long tstamp, int insCount) {
    if (duration_ms < 0) {
      duration_ms = 0;
    }
    long elapsedTime = (tstamp > start_ref) ? tstamp - start_ref : 0;
    duration_ms += elapsedTime;
    
    // if instruction count was defined, calc the time spent in the method & add it to current value
    if (insCount >= 0) {
      int delta = (insCount > instrEntry) ? insCount - instrEntry : 0;
      if (instrCount < 0) {
        instrCount = delta;
      } else {
        instrCount += delta;
      }
    }
    exit = true;
    //System.out.println("exit time: " + currentTime + ", elapsed " + duration_ms + " - " +  fullName);
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
