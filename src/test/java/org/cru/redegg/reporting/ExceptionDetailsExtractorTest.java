package org.cru.redegg.reporting;


import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Matt Drees
 */
public class ExceptionDetailsExtractorTest
{

    @Test
    public void testExtract()
    {
        ExceptionDetailsExtractor extractor = new ExceptionDetailsExtractor();
        TestException exception = new TestException();
        List<String> details = extractor.extractDetails(exception);

        assertThat(details, hasSize(3));
        assertThat(details, containsInAnyOrder(
            "org.cru.redegg.reporting.ExceptionDetailsExtractorTest$TestException: this is a test",
            "  code: 45",
            "  query: select * from dual"
        ));
    }

    @Test
    public void testExtractForBoringException()
    {
        ExceptionDetailsExtractor extractor = new ExceptionDetailsExtractor();
        Exception exception = new Exception();
        List<String> details = extractor.extractDetails(exception);

        assertThat(details, hasSize(0));
    }

    @SuppressWarnings("unused")
    public static class TestException extends RuntimeException
    {

        public TestException()
        {
            super("this is a test");
        }

        //TODO: support public fields at some point
//        public final int code = 45;

        private int code = 45;

        private String query = "select * from dual";

        public int getCode()
        {
            return code;
        }

        public String getQuery()
        {
            return query;
        }

    }



}