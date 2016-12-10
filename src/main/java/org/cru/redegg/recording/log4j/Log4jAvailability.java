package org.cru.redegg.recording.log4j;

public class Log4jAvailability
{
    public static boolean isAvailable()
    {
        try
        {
            Log4jAvailability.class.getClassLoader().loadClass("org.apache.log4j.AppenderSkeleton");
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }
}
