package org.cru.redegg.util;

/**
 * @author Matt Drees
 */
public class RedEggStrings
{

    /*
     * this exists in Guava 16:
     * https://github.com/google/guava/issues/1194
     * but we are not requiring that version of guava yet.
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
