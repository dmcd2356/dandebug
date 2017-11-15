/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import java.awt.Color;
import java.awt.Graphics;
import java.util.HashMap;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 *
 * @author dan
 */
public class DebugMessage {
    
  public static final boolean PRINT_TO_STDOUT = true; // set to false to create a panel for msgs

  public enum FontType {
    Normal, Bold, Italic, BoldItalic;
  }
    
  public enum TextColor {
    Black, DkGrey, DkRed, Red, LtRed, Orange, Brown, Gold, Green, Cyan,
    LtBlue, Blue, Violet, DkVio;
  }
    
  // used in formatting printArray
  private final static char[] HEXARRAY = "0123456789ABCDEF".toCharArray();
    
  private static final String NEWLINE = System.getProperty("line.separator");

  private static JTextPane debugTextPane;
  private static long      startTime = System.currentTimeMillis(); // get the start time
  private static boolean   showHours = false;
  private static boolean   showMsecs = false;
  private static boolean   timeSet = false;
  private static int       lastcount = 0;
  private static HashMap<String, FontInfo> messageTypeTbl = new HashMap<>();

  public DebugMessage (JTextPane textpane) {
    debugTextPane = textpane;
    setColors();
    showMsecs = true;
    showHours = false;
  }

  /**
   * closes the panel (if one was open)
   */
  public static void close () {
    debugTextPane = null;   // make sure the panel will no longer be accessed
  }
  
  /**
   * clears the display.
   */
  public static final void clear() {
    if (debugTextPane != null) {
      debugTextPane.setText("");
    }
    timeSet = false; // this will cause the start time to reset on the next msg received
  }

  /**
   * updates the display immediately
   */
  public static final void updateDisplay () {
    if (debugTextPane != null) {
      Graphics graphics = debugTextPane.getGraphics();
      if (graphics != null) {
        debugTextPane.update(graphics);
      }
    }
  }

  /**
   * outputs the various types of messages to the status display.
   * all messages will guarantee the previous line was terminated with a newline,
   * and will preceed the message with a timestamp value and terminate with a newline.
   * 
   * @param count   - message number
   * @param tstamp  - timestamp for message
   * @param message - the message to display
   */
  public static final void print(int count, long tstamp, String message) {
    if (debugTextPane == null) {
      return;
    }

    if (message != null && !message.isEmpty()) {
      // format the elapsed time value
      String elapsed = "[" + getElapsedTime(tstamp) + "] ";

      // format the packet counter info
      String countstr = "00000000" + count;
      countstr = countstr.substring(countstr.length() - 8);
      
      // verify packet counter is consecutive
      if (timeSet && count > lastcount + 1 && count != 0) {
        printRaw("INFO", "-------- ");
        printRaw("TSTAMP", elapsed);
        printRaw("ERROR", "Lost packets: " + (count - lastcount - 1) + NEWLINE);
      }
      lastcount = count;
      
      // extract type from message
      String typestr = message.substring(0, 6).toUpperCase().trim();
      
      // print message (seperate into multiple lines if ASCII newlines are contained in it)
      if (!message.contains(NEWLINE)) {
        printRaw("INFO", countstr + " ");
        printRaw("TSTAMP", elapsed);
        printRaw(typestr, message + NEWLINE);
      }
      else {
        // seperate into lines and print each independantly
        String[] msgarray = message.split(NEWLINE);
        for (String msg : msgarray) {
          printRaw("INFO", countstr + " ");
          printRaw("TSTAMP", elapsed);
          printRaw(typestr, msg + NEWLINE);
        }
      }
    }
  }

  private void setColors () {
    // this is only used locally
    setTypeColor ("TSTAMP",  TextColor.Brown, FontType.Normal);

    // these are for public consumption
    setTypeColor ("ERROR",  TextColor.Red,    FontType.Bold);
    setTypeColor ("WARN",   TextColor.Orange, FontType.Bold);
    setTypeColor ("INFO",   TextColor.Black,  FontType.Normal);
    setTypeColor ("ENTRY",  TextColor.Brown,  FontType.Normal);
    setTypeColor ("CALL",   TextColor.Gold,   FontType.Normal);
    setTypeColor ("RETURN", TextColor.Gold,   FontType.Normal);
    setTypeColor ("STACK",  TextColor.Blue,   FontType.Normal);
    setTypeColor ("STACKS", TextColor.Blue,   FontType.Italic);
    setTypeColor ("STACKI", TextColor.Blue,   FontType.Bold);
    setTypeColor ("LOCAL",  TextColor.Green,  FontType.Normal);
    setTypeColor ("LOCALS", TextColor.Green,  FontType.Italic);
    setTypeColor ("SOLVE",  TextColor.DkVio,  FontType.Bold);
  }
  
  /**
   * returns the elapsed time in seconds.
   * The format of the String is: "HH:MM:SS"
   * 
   * @return a String of the formatted time
   */
  private static String getElapsedTime (long currentTime) {
    // if time has not been set yet, use the current setting as the start value
    if (!timeSet) {
      timeSet = true;
      startTime = currentTime;
    }
    
    // calculate elapsed time
    long elapsedTime = currentTime - startTime;
    if (elapsedTime < 0) {
      elapsedTime = 0;
    }
        
    // split value into hours, min and secs
    Long msecs = elapsedTime % 1000;
    Long secs = (elapsedTime / 1000);
    Long hours = 0L;
    if (!showMsecs) {
      secs += msecs >= 500 ? 1 : 0;
    }
    if (showHours) {
      hours = secs / 3600;
    }
    secs %= 3600;
    Long mins = secs / 60;
    secs %= 60;

    // now stringify it
    String elapsed = "";
    if (showHours) {
      if (hours < 10) {
        elapsed = "0";
      }
      elapsed += hours.toString();
      elapsed += ":";
    }
    elapsed += (mins < 10) ? "0" + mins.toString() : mins.toString();
    elapsed += ":";
    elapsed += (secs < 10) ? "0" + secs.toString() : secs.toString();
    if (showMsecs) {
      String msecstr = "00" + msecs.toString();
      msecstr = msecstr.substring(msecstr.length() - 3);
      elapsed += "." + msecstr;
    }
    return elapsed;
  }

    
  /**
   * A generic function for appending formatted text to a JTextPane.
   * 
   * @param tp    - the TextPane to append to
   * @param msg   - message contents to write
   * @param color - color of text
   * @param font  - the font selection
   * @param size  - the font point size
   * @param ftype - type of font style
   */
  private static void appendToPane(String msg, TextColor color, String font, int size,
                                   FontType ftype) {
    if (debugTextPane == null) {
      return;
    }
        
    AttributeSet aset = setTextAttr(color, font, size, ftype);
    int len = debugTextPane.getDocument().getLength();
    debugTextPane.setCaretPosition(len);
    debugTextPane.setCharacterAttributes(aset, false);
    debugTextPane.replaceSelection(msg);
  }

  /**
   * A generic function for appending formatted text to a JTextPane.
   * 
   * @param tp    - the TextPane to append to
   * @param msg   - message contents to write
   * @param color - color of text
   * @param ftype - type of font style
   */
  private static void appendToPane(String msg, TextColor color, FontType ftype) {
    appendToPane(msg, color, "Courier", 11, ftype);
  }

  /**
   * sets the association between a type of message and the characteristics
   * in which to print the message.
   * 
   * @param type  - the type to associate with the font characteristics
   * @param color - the color to assign to the type
   * @param ftype - the font attributes to associate with the type
   */
  private void setTypeColor (String type, TextColor color, FontType ftype) {
    setTypeColor (type, color, ftype, 11, "Courier");
  }
    
  /**
   * same as above, but lets user select font family and size as well.
   * 
   * @param type  - the type to associate with the font characteristics
   * @param color - the color to assign to the type
   * @param ftype - the font attributes to associate with the type
   * @param size  - the size of the font
   * @param font  - the font family (e.g. Courier, Ariel, etc.)
   */
  private void setTypeColor (String type, TextColor color, FontType ftype, int size, String font) {
    FontInfo fontinfo = new FontInfo(color, ftype, size, font);
    if (messageTypeTbl.containsKey(type)) {
      messageTypeTbl.replace(type, fontinfo);
    }
    else {
      messageTypeTbl.put(type, fontinfo);
    }
  }
    
  /**
   * displays a message in the debug window (no termination).
   * 
   * @param type  - the type of message to display
   * @param message - message contents to display
   */
  private static void printRaw(String type, String message) {
    if (message != null && !message.isEmpty()) {
      if (debugTextPane == null) {
        System.out.print(message);
        return;
      }
        
      // set default values (if type was not found)
      TextColor color = TextColor.Black;
      FontType ftype = FontType.Normal;
      String font = "Courier";
      int size = 11;

      // get the color and font for the specified type
      FontInfo fontinfo = messageTypeTbl.get(type);
      if (fontinfo != null) {
        color = fontinfo.color;
        ftype = fontinfo.fonttype;
        font  = fontinfo.font;
        size  = fontinfo.size;
      }

      appendToPane(message, color, font, size, ftype);
    }
  }

  /**
   * generates the specified text color for the debug display.
   * 
   * @param colorName - name of the color to generate
   * @return corresponding Color value representation
   */
  private static Color generateColor (TextColor colorName) {
    float hue, sat, bright;
    switch (colorName) {
      default:
      case Black:
        return Color.BLACK;
      case DkGrey:
        return Color.DARK_GRAY;
      case DkRed:
        hue    = (float)0;
        sat    = (float)100;
        bright = (float)66;
        break;
      case Red:
        hue    = (float)0;
        sat    = (float)100;
        bright = (float)90;
        break;
      case LtRed:
        hue    = (float)0;
        sat    = (float)60;
        bright = (float)100;
        break;
      case Orange:
        hue    = (float)20;
        sat    = (float)100;
        bright = (float)100;
        break;
      case Brown:
        hue    = (float)20;
        sat    = (float)80;
        bright = (float)66;
        break;
      case Gold:
        hue    = (float)40;
        sat    = (float)100;
        bright = (float)90;
        break;
      case Green:
        hue    = (float)128;
        sat    = (float)100;
        bright = (float)45;
        break;
      case Cyan:
        hue    = (float)190;
        sat    = (float)80;
        bright = (float)45;
        break;
      case LtBlue:
        hue    = (float)210;
        sat    = (float)100;
        bright = (float)90;
        break;
      case Blue:
        hue    = (float)240;
        sat    = (float)100;
        bright = (float)100;
        break;
      case Violet:
        hue    = (float)267;
        sat    = (float)100;
        bright = (float)100;
        break;
      case DkVio:
        hue    = (float)267;
        sat    = (float)100;
        bright = (float)66;
        break;
    }
    hue /= (float)360.0;
    sat /= (float)100.0;
    bright /= (float) 100.0;
    return Color.getHSBColor(hue, sat, bright);
  }

  /**
   * A generic function for appending formatted text to a JTextPane.
   * 
   * @param color - color of text
   * @param font  - the font selection
   * @param size  - the font point size
   * @param ftype - type of font style
   * @return the attribute set
   */
  private static AttributeSet setTextAttr(TextColor color, String font, int size, FontType ftype) {
    boolean bItalic = false;
    boolean bBold = false;
    if (ftype == FontType.Italic || ftype == FontType.BoldItalic) {
      bItalic = true;
    }
    if (ftype == FontType.Bold || ftype == FontType.BoldItalic) {
      bBold = true;
    }

    StyleContext sc = StyleContext.getDefaultStyleContext();
    AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground,
                                        generateColor(color));

    aset = sc.addAttribute(aset, StyleConstants.FontFamily, font);
    aset = sc.addAttribute(aset, StyleConstants.FontSize, size);
    aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
    aset = sc.addAttribute(aset, StyleConstants.Italic, bItalic);
    aset = sc.addAttribute(aset, StyleConstants.Bold, bBold);
    return aset;
  }
    
  public class FontInfo {
    TextColor  color;      // the font color
    FontType   fonttype;   // the font attributes (e.g. Italics, Bold,..)
    String     font;       // the font family (e.g. Courier)
    int        size;       // the font size
        
    FontInfo (TextColor col, FontType type, int fsize, String fontname) {
      color = col;
      fonttype = type;
      font = fontname;
      size = fsize;
    }
  }
}
