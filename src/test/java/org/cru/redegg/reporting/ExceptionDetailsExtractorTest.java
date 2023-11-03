package org.cru.redegg.reporting;


import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
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
        Map<String, Object> details = extractor.extractDetails(exception);

        assertThat(details.keySet(), hasSize(4));
        assertThat(details, hasEntry("summary", "org.cru.redegg.reporting.ExceptionDetailsExtractorTest$TestException: this is a test"));
        assertThat(details, hasEntry("code", 45));
        assertThat(details, hasEntry("query", "select * from dual"));
        assertThat(details, hasEntry("parameters", ImmutableMap.of("kind", "BY_NAME")));
    }

    @Test
    public void testExtractForBoringException()
    {
        ExceptionDetailsExtractor extractor = new ExceptionDetailsExtractor();
        Exception exception = new Exception();
        Map<String, Object> details = extractor.extractDetails(exception);

        assertThat(details.keySet(), hasSize(0));
    }

    @SuppressWarnings("unused")
    public static class TestException extends RuntimeException
    {
        public enum ParameterKind
        {
            BY_NAME,
            BY_POSITION
        }

        public TestException()
        {
            super("this is a test");
            parameters.put("kind", ParameterKind.BY_NAME);
        }

        //TODO: support public fields at some point
//        public final int code = 45;

        private int code = 45;

        private String query = "select * from dual";

        private Map<String, Object> parameters = new HashMap<>();

        public int getCode()
        {
            return code;
        }

        public String getQuery()
        {
            return query;
        }

        public Map<String, Object> getParameters()
        {
            return parameters;
        }
    }



}
