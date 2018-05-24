Graphical debug monitor for viewing the debug log messages output from danalyzer in a color-highlighted format for discerning the different types of content and providing a method of viewing the call tree graphically.

The command line has the following format:

java -jar dandebug.jar [-t] [-u] [port]

Where:

-t = use TCP (default)

-u = use UDP

port = the port selection (5000 is default)

This program uses the network to receive live debug messages issued from a running Java program that has been instrumented with danalyzer and has been configured to output messages using either a UDP or TCP port. The instrumented code always sends to localhost port 5000, so dandebug must run on the same machine as the program being debugged. Statistics in the center of the panel show Elapsed time (the timer is started and re-started whenever a debug message is received that has a line count value of 0). The Pkts Read and Lost indicate the number of debug message lines received and lost based on the line counter value in the messages received. Note that lines may be lost when using UDP but not TCP. As the data is received, it is saved to the location specified by the Debug Log File selection. You may specify the location using the Set button and can reset the current file contents (and screen contents as well) using the Erase button. The messages from the port are read and placed in a queue for another thread to read and copy to the specified file. The Queue entry in the Statistics shows the number of lines that have been placed into the queue but have not yet been written to the file. Processed shows the number of lines that have been read, displayed and saved in the call graph list (see discussion below on Call Graph). If CALL and RETURN debug messages are being sent, the number of unique method calls are tallied in the Methods text area. The messages are scrolled in the Live tab window, which will only hold around 2000 lines of text (between 100k and 150k of characters) before it chops off older information, since this causes the display to slow down drastically when the file size gets very large. Note that the data is never eliminated in the log file unless the user presses the Erase button. When data is being sent, the user can scroll back to look at old data by first pressing the Pause button, which prevents dandebug from processing the input until the Resume button is pressed again. The Clear button will clear the current display and indicate the date and time this action was taken, but does not erase data from the file.

The Call Graph tab selection sorts through only the CALL and RETURN log entries to monitor the call stack flow for instrumented calls (uninstrumented methods are not tracked). If the debug log does not include the CALL and RETURN messages, the call graph cannot be generated. A List is created of each method call along with several useful characteristics is uses in the graph, such as the list of methods that call it (parents), the number of times the method was called, total time elapsed between the call and return and the corresponding number of instructions executed between the call and return. It also logs the line number corresponding to the last entry in that method where an ERROR message or an exception (ENTRY message type) occurred, if any. A call graph is generated that graphically connects the methods indicating the call flow is generated and displayed when the Call Graph tab is selected. Selecting one of the methods in this display will show the details of the data gathered on that method. Once the call graph has been displayed, clicking the Save Graph button will save both the graphical image (as a PNG file) and the List information (as a JSON file). The JSON file allows the user to be able to re-load a file that has previously been saved (using the Load Graph button) and be able to view the call graph and again get information about each method by clicking on it. The Graph Highlighting section of the panel allows you to color code the method blocks to highlight those that consumed the most resources based on: elapsed time, instruction count or the number of times it was called. By selecting Status it will mark in pink those methods where an ERROR message occurred and in light blue which methods never reached a RETURN message.

The Load File allows the user to load a previously saved debug log file, which will also run the messages through the call graph extraction process, allowing you to view the textual information in the color-highlighted mode as well as observe the call graph and information about each method, just as in the Live mode.

The format of the debug data read is as follows as shown by the following example line:

00015621 [08:05.281] CALL : 3850 com/ainfosec/STACPoker/Card.getSuitAsString()Ljava/lang/String; com/ainfosec/STACPoker/Card.getImage()Ljava/awt/image/BufferedImage;

offset 0: 8-digit line number starting at 00000000

offset 9: 11-char timestamp value formatted as minutes:seconds.milliseconds, enclosed in []

offset 21: 6-char message type followed by colon { ERROR, WARN, INFO, ENTRY, AGENT, CALL, RETURN, UNINST, STATS, STACK, STACKS, STACKI, LOCAL, LOCALS, SOLVE }

offset 29: the message contents

It is assumed that the contents of the RETURN message is a numeric value representing the current instruction count and that the contents of the CALL message are (3) ASCII space seperated entries consisting of: the current instruction count, the method being called, adn the caller of the method. Note that the method names are the full name including the class path, the argument list and the return value type.