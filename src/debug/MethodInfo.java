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
  private String  fullName;
  private String  className;
  private String  methName;
  private int     count;
  private long    duration_ms;
  private long    start_ref;
  
  private static final String NEWLINE = System.getProperty("line.separator");

  public MethodInfo(String method) {
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
    duration_ms = 0;
    start_ref = System.currentTimeMillis();
    //System.out.println("start time: " + start_ref + " (init) - " + fullName);
  }
  
  public void incCount() {
    ++count;
    start_ref = System.currentTimeMillis();
    //System.out.println("start time: " + start_ref + ", count " + count + " - " +  fullName);
  }
  
  public void exit() {
    long currentTime = System.currentTimeMillis();
    long elapsedTime = currentTime - start_ref;
    if (elapsedTime > 0) {
      duration_ms += elapsedTime;
    }
    //System.out.println("exit time: " + currentTime + ", elapsed " + duration_ms + " - " +  fullName);
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
  
  public long getDuration() {
    return duration_ms;
  }
}
