package org.cru.redegg.reporting;

/**
 * This class should not change much,
 * so it's less likely that the line numbers of these exceptions will accidentally change.
 */
public class TestExceptions
{

    public static NullPointerException boom()
    {
        //The following should be on line 13.  This is depended upon by a test.
        return new NullPointerException();
    }

    public static RuntimeException runtimeWrappingNullPointer()
    {
        //the following should be on line 19
        return new RuntimeException("something bad happened", boom());
    }

    public static String filename()
    {
        return TestExceptions.class.getPackage().getName().replace(".", "/") + "/" + TestExceptions.class.getSimpleName() + ".java";
    }
}
