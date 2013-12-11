package org.cru.redegg.reporting;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.List;

/**
 * A handy class with {@link StringBuilder}-like semantics for the
 * purpose of creating a multi-line String containing multiple informational
 * sections.
 * @author Matt Drees
 *
 */
public class ReportStringBuilder
{
    private final StringBuilder builder = new StringBuilder();
    private final String lineSeparator = System.getProperty("line.separator");
    
    public void appendLine(String key, Object object)
    {
        Preconditions.checkNotNull(key, "key is null");
        internalAppendLine(key, object);
    }

    public void appendLine(String key, String message, Object... params)
    {
        Preconditions.checkNotNull(key, "key is null");
        Preconditions.checkNotNull(message, "message is null");
        internalAppendLine(key, String.format(message, params));
    }

    private void internalAppendLine(String key, Object object)
    {
        builder.append(key).append(": ").append(object).append(lineSeparator);
    }

    /**
     * print a bunch of strings, one on each line.  Print
     * the first string right after the key, and left-align
     * the subsequent strings with the first line.
     * @param key
     * @param listOfLines
     */
    public void appendList(String key, List<?> listOfLines)
    {
        Preconditions.checkNotNull(key, "key is null");
        builder.append(key).append(": ");
        int offset = key.length() + ": ".length();
        String offsetSpacing = Strings.repeat(" ", offset);
        Joiner.on(lineSeparator + offsetSpacing)
            .appendTo(builder, listOfLines);
        builder.append(lineSeparator);
    }

    /**
     * print the key, and then on the next line print the object. For large multi-line strings that either are
     * too wide to waste space offsetting, or for when we are too lazy to break up the string into a list, as
     * required by {@link #appendList(String, List)}
     * 
     * @param key
     * @param object
     */
    public void appendChunk(String key, Object object)
    {
        Preconditions.checkNotNull(key, "key is null");
        builder.append(key).append(":").append(lineSeparator);
        builder.append(object).append(lineSeparator);
    }
    
    public void appendBreak()
    {
        builder.append(lineSeparator);
    }
    
    public void appendNote(String string)
    {
        builder.append(string).append(lineSeparator);
    }
    
    @Override
    public String toString()
    {
        return builder.toString();
    }


}