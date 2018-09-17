package net.highwayfrogs.editor.gui;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Redirects System.out to somewhere else.
 * Created by Kneesnap on 9/17/2018.
 */
public class SystemOutputReplacement extends OutputStream {
    private PrintStream originalStream;
    private StringBuffer buffer = new StringBuffer();
    private static final String LINE_SEPARATOR = System.lineSeparator();

    public SystemOutputReplacement() {
        this.originalStream = System.out;
    }

    @Override
    public void write(int b) {
        this.originalStream.write(b);

        char ch = (char) b;
        buffer.append(ch);
        if (ch == LINE_SEPARATOR.charAt(LINE_SEPARATOR.length() - 1)) { // Check on a char by char basis for speed
            String s = buffer.toString();
            if (s.contains(LINE_SEPARATOR)) { // The whole separator string is written
                buffer.setLength(0);
                if (MainController.MAIN_WINDOW != null)
                    MainController.MAIN_WINDOW.printMessage(s.substring(0, s.length() - LINE_SEPARATOR.length()));
            }
        }
    }

    /**
     * Activate the rerouting.
     */
    public static void activateReplacement() {
        System.setOut(new PrintStream(new SystemOutputReplacement()));
    }
}
