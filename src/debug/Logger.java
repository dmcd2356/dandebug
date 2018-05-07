/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debug;

import java.awt.Color;
import java.awt.Graphics;
import java.time.LocalDateTime;
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
  private final static int MAX_TEXT_BUFFER_SIZE = 150000;
  private final static int REDUCE_BUFFER_SIZE   = 50000;
  
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

  public Logger (String name, JTextPane textpane) {
    if (textpane == null) {
      System.out.println("ERROR: Textpane passed to '" + name + "' Logger was null!");
      System.exit(1);
    }
    debugTextPane = textpane;
    setColors();
  }

  /**
   * clears the display.
   */
  public final void clear() {
    debugTextPane.setText("");
  }

  /**
   * updates the display immediately
   */
  public final void updateDisplay () {
    Graphics graphics = debugTextPane.getGraphics();
    if (graphics != null) {
      debugTextPane.update(graphics);
    }
  }

  /**
   * outputs the various types of messages to the status display.
   * all messages will guarantee the previous line was terminated with a newline,
   * and will preceed the message with a timestamp value and terminate with a newline.
   * 
   * @param linenum - the line number
   * @param elapsed - the elapsed time
   * @param typestr - the type of message to display (all caps)
   * @param content - the message content
   */
  public final void print(int linenum, String elapsed, String typestr, String content) {
    if (linenum >= 0 && elapsed != null && typestr != null && content != null && !content.isEmpty()) {
      // make sure the linenum is 8-digits in length and the type is 6-chars in length
      String linestr = "00000000" + linenum;
      linestr = linestr.substring(linestr.length() - 8);
      typestr = (typestr + "      ").substring(0, 6);
      
      // print message (seperate into multiple lines if ASCII newlines are contained in it)
      if (!content.contains(NEWLINE)) {
        printRaw("INFO", linestr + "  ");
        printRaw("INFO", elapsed + " ");
        printRaw(typestr, typestr + ": " + content + NEWLINE);
      }
      else {
        // seperate into lines and print each independantly
        String[] msgarray = content.split(NEWLINE);
        for (String msg : msgarray) {
          printRaw("INFO", linestr + "  ");
          printRaw("INFO", elapsed + " ");
          printRaw(typestr, typestr + ": " + msg + NEWLINE);
        }
      }
    }
  }

  public final void printSeparator() {
    String message = "" + LocalDateTime.now();
    message = message.replace('T', ' ');
    printRaw("NOFMT", message + "------------------------------------------------------------" + NEWLINE);
  }
  
  public final void printUnformatted(String message) {
    printRaw("NOFMT", message + NEWLINE);
  }
  
  private void setColors () {
    // these are for public consumption
    setTypeColor ("NOFMT",  TextColor.DkGrey, FontType.Italic);
    setTypeColor ("ERROR",  TextColor.Red,    FontType.Bold);
    setTypeColor ("WARN",   TextColor.Orange, FontType.Bold);
    setTypeColor ("INFO",   TextColor.Black,  FontType.Normal);
    setTypeColor ("ENTRY",  TextColor.Brown,  FontType.Normal);
    setTypeColor ("AGENT",  TextColor.Violet, FontType.Italic);
    setTypeColor ("CALL",   TextColor.Gold,   FontType.Bold);
    setTypeColor ("RETURN", TextColor.Gold,   FontType.Bold);
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
  private void appendToPane(String msg, TextColor color, String font, int size,
                                   FontType ftype) {
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
  private void printRaw(String type, String message) {
    if (message != null && !message.isEmpty()) {
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
  private Color generateColor (TextColor colorName) {
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
  private AttributeSet setTextAttr(TextColor color, String font, int size, FontType ftype) {
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
