package org.cru.redegg.servlet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import org.cru.redegg.recording.api.ParameterSanitizer;
import org.cru.redegg.recording.api.RequestMatcher;
import org.cru.redegg.servlet.ParameterCategorizer.Categorization;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.OngoingStubbing;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.compose;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeatureValue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class ParameterCategorizerTest
{

    ParameterCategorizer categorizer;

    @Mock
    ParameterSanitizer sanitizer;

    @Mock
    RequestMatcher streamPreservationMatcher;

    @Mock
    HttpServletRequest request;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        categorizer = new ParameterCategorizer(sanitizer, streamPreservationMatcher);
    }


    @Test
    public void testCategorizeWithNoSensitiveQueryParameters()
    {
        whenSanitizingParameter(anyString()).then(this::returnListAsIs);

        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("sensitive=false&boring=true");
        when(request.getParameterMap()).thenReturn(ImmutableMap.of(
           "sensitive", new String[]{ "false" },
           "boring", new String[]{ "true" }
        ));

        Categorization expectedCategorization = new Categorization();
        expectedCategorization.queryString = "sensitive=false&boring=true";
        expectedCategorization.postParameters = ImmutableMultimap.of();
        expectedCategorization.queryParameters = ImmutableSetMultimap.of( //note: ImmutableSetMultimap is required; ImmutableMultimap won't work
            "sensitive", "false",
            "boring", "true"
        );

        Categorization actualCategorization = categorizer.categorize(request);
        assertThat(actualCategorization, matches(expectedCategorization));
    }

    @Test
    public void testCategorizeWithOneSensitiveQueryParameterAtBeginning()
    {
        whenSanitizingParameter(eq("sensitive")).thenReturn(ImmutableList.of("<removed>"));
        whenSanitizingParameter(eq("boring")).then(this::returnListAsIs);

        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("sensitive=true&boring=false");
        when(request.getParameterMap()).thenReturn(ImmutableMap.of(
           "sensitive", new String[]{ "true" },
           "boring", new String[]{ "false" }
        ));

        Categorization expectedCategorization = new Categorization();
        expectedCategorization.queryString = "sensitive=<removed>&boring=false";
        expectedCategorization.postParameters = ImmutableMultimap.of();
        expectedCategorization.queryParameters = ImmutableSetMultimap.of(
            "sensitive", "<removed>",
            "boring", "false"
        );

        Categorization actualCategorization = categorizer.categorize(request);
        assertThat(actualCategorization, matches(expectedCategorization));
    }

    @Test
    public void testCategorizeWithOneSensitiveQueryParameterInMiddle()
    {
        whenSanitizingParameter(eq("first")).then(this::returnListAsIs);
        whenSanitizingParameter(eq("sensitive")).thenReturn(ImmutableList.of("<removed>"));
        whenSanitizingParameter(eq("boring")).then(this::returnListAsIs);

        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("first=true&sensitive=true&boring=false");
        when(request.getParameterMap()).thenReturn(ImmutableMap.of(
           "first", new String[]{ "true" },
           "sensitive", new String[]{ "true" },
           "boring", new String[]{ "false" }
        ));

        Categorization expectedCategorization = new Categorization();
        expectedCategorization.queryString = "first=true&sensitive=<removed>&boring=false";
        expectedCategorization.postParameters = ImmutableMultimap.of();
        expectedCategorization.queryParameters = ImmutableSetMultimap.of(
            "first", "true",
            "sensitive", "<removed>",
            "boring", "false"
        );

        Categorization actualCategorization = categorizer.categorize(request);
        assertThat(actualCategorization, matches(expectedCategorization));
    }

    @Test
    public void testCategorizeWithOneSensitiveQueryParameterAtEnd()
    {
        whenSanitizingParameter(eq("first")).then(this::returnListAsIs);
        whenSanitizingParameter(eq("boring")).then(this::returnListAsIs);
        whenSanitizingParameter(eq("sensitive")).thenReturn(ImmutableList.of("<removed>"));

        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("first=true&boring=false&sensitive=true");
        when(request.getParameterMap()).thenReturn(ImmutableMap.of(
            "first", new String[]{ "true" },
            "boring", new String[]{ "false" },
            "sensitive", new String[]{ "true" }
        ));

        Categorization expectedCategorization = new Categorization();
        expectedCategorization.queryString = "first=true&boring=false&sensitive=<removed>";
        expectedCategorization.postParameters = ImmutableMultimap.of();
        expectedCategorization.queryParameters = ImmutableSetMultimap.of(
            "first", "true",
            "boring", "false",
            "sensitive", "<removed>"
        );

        Categorization actualCategorization = categorizer.categorize(request);
        assertThat(actualCategorization, matches(expectedCategorization));
    }

    @Test
    public void testCategorizeWithTwoSensitiveQueryParameters()
    {
        whenSanitizingParameter(eq("sensitive")).thenReturn(ImmutableList.of("<removed>"));
        whenSanitizingParameter(eq("boring")).then(this::returnListAsIs);

        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("sensitive=maybe&boring=false&sensitive=true");
        when(request.getParameterMap()).thenReturn(ImmutableMap.of(
            "sensitive", new String[]{ "maybe", "true" },
            "boring", new String[]{ "false" }
        ));

        Categorization expectedCategorization = new Categorization();
        expectedCategorization.queryString = "sensitive=<removed>&boring=false&sensitive=<removed>";
        expectedCategorization.postParameters = ImmutableMultimap.of();
        expectedCategorization.queryParameters = ImmutableSetMultimap.of(
            "sensitive", "<removed>",
            "boring", "false"
        );

        Categorization actualCategorization = categorizer.categorize(request);
        assertThat(actualCategorization, matches(expectedCategorization));
    }

    private Object returnListAsIs(InvocationOnMock invocation)
    {
        return invocation.getArguments()[1];
    }

    private OngoingStubbing<List<String>> whenSanitizingParameter(String parameterMatcher)
    {
        return when(sanitizer.sanitizeQueryStringParameter(
            parameterMatcher,
            anyList()
        ));
    }

    private Matcher<Categorization> matches(Categorization expectedCategorization)
    {
        return compose(
            "a categorization with",
            hasFeatureValue(
                "queryString",
                (Categorization c) -> c.queryString,
                expectedCategorization.queryString)
        ).and(
            hasFeatureValue(
                "queryParameters",
                (Categorization c) -> c.queryParameters,
                expectedCategorization.queryParameters)
        ).and(
            hasFeatureValue(
                "postParameters",
                (Categorization c) -> c.postParameters,
                expectedCategorization.postParameters)
        );
    }
}
