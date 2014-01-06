package org.cru.redegg.util;

/**
 * @author Matt Drees
 */
public class RedEggStrings
{

    /*
     * perhaps someday this will exist in Guava
     * https://code.google.com/p/guava-libraries/issues/detail?id=1194
     */
    public static String truncate(String string, int limit, String truncationIndicator)
    {
        if (string.length() > limit)
        {
            return string.substring(0, limit - truncationIndicator.length()) + truncationIndicator;
        }
        else
            return string;
    }

}
