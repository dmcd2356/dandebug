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
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 *
 * @author dan
 */
public class Logger {
    
  // these are used for limiting the amount of text displayed in the logger display to limit
  // memory use. MAX_TEXT_BUFFER_SIZE defines the upper memory usage when the reduction takes
  // place and REDUCE_BUFFER_SIZE is the minimum number of bytes to reduce it by (it will look
  // for the next NEWLINE char).
  private final static int MAX_TEXT_BUFFER_SIZE = 1500000;
  private final static int REDUCE_BUFFER_SIZE   = 200000;
  
  private enum FontType {
    Normal, Bold, Italic, BoldItalic;
  }
    
  private enum TextColor {
    Black, DkGrey, DkRed, Red, LtRed, Orange, Brown, Gold, Green, Cyan,
    LtBlue, Blue, Violet, DkVio;
  }
    
  private static final String NEWLINE = System.getProperty("line.separator");

  private static JTextPane       debugTextPane = null;
  private static HashMap<String, FontInfo> messageTypeTbl = new HashMap<>();

  public Logger (JTextPane textpane) {
    debugTextPane = textpane;
    setColors();
  }

  /**
   * clears the display.
   */
  public static final void clear() {
    if (debugTextPane != null) {
      debugTextPane.setText("");
    }
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
   * @param message - the message to display
   */
  public static final void print(String message) {
    if (debugTextPane == null) {
      return;
    }

    if (message != null && !message.isEmpty() && message.length() >= 30) {
      // extract the packet count, elapsed time, and message type from the string
      String countstr = message.substring(0, 9);
      String elapsed  = message.substring(9, 21);
      String typestr  = message.substring(21, 27).toUpperCase();
      message  = message.substring(29);

      // print message (seperate into multiple lines if ASCII newlines are contained in it)
      if (!message.contains(NEWLINE)) {
        printRaw("INFO", countstr + " ");
        printRaw("INFO", elapsed);
        printRaw(typestr, typestr + ": " + message + NEWLINE);
      }
      else {
        // seperate into lines and print each independantly
        String[] msgarray = message.split(NEWLINE);
        for (String msg : msgarray) {
          printRaw("INFO", countstr + " ");
          printRaw("INFO", elapsed);
          printRaw(typestr, typestr + ": " + msg + NEWLINE);
        }
      }
    }
  }

  private void setColors () {
    if (debugTextPane == null) {
      return;
    }

    // these are for public consumption
    setTypeColor ("ERROR",  TextColor.Red,    FontType.Bold);
    setTypeColor ("WARN",   TextColor.Orange, FontType.Bold);
    setTypeColor ("INFO",   TextColor.Black,  FontType.Normal);
    setTypeColor ("ENTRY",  TextColor.Brown,  FontType.Normal);
    setTypeColor ("CALL",   TextColor.Gold,   FontType.Normal);
    setTypeColor ("RETURN", TextColor.Gold,   FontType.Normal);
    setTypeColor ("UNINST", TextColor.Gold,   FontType.BoldItalic);
    setTypeColor ("STATS",  TextColor.Gold,   FontType.BoldItalic); // obsolete
    setTypeColor ("STACK",  TextColor.Blue,   FontType.Normal);
    setTypeColor ("STACKS", TextColor.Blue,   FontType.Italic);
    setTypeColor ("STACKI", TextColor.Blue,   FontType.Bold);
    setTypeColor ("LOCAL",  TextColor.Green,  FontType.Normal);
    setTypeColor ("LOCALS", TextColor.Green,  FontType.Italic);
    setTypeColor ("SOLVE",  TextColor.DkVio,  FontType.Bold);
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

    // trim off earlier data to reduce memory usage if we exceed our bounds
    if (len > MAX_TEXT_BUFFER_SIZE) {
      try {
        int oldlen = len;
        int start = REDUCE_BUFFER_SIZE;
        String text = debugTextPane.getDocument().getText(start, 500);
        int offset = text.indexOf(NEWLINE);
        if (offset >= 0) {
          start += offset + 1;
        }
        debugTextPane.getDocument().remove(0, start);
        len = debugTextPane.getDocument().getLength();
        System.out.println("Reduced text from " + oldlen + " to " + len);
      } catch (BadLocationException ex) {
        System.out.println(ex.getMessage());
      }
    }

    debugTextPane.setCaretPosition(len);
    debugTextPane.setCharacterAttributes(aset, false);
    debugTextPane.replaceSelection(msg);
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
      FontInfo fontinfo = messageTypeTbl.get(type.trim());
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
