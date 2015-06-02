package org.cru.redegg.recording.api;

import javax.servlet.ServletRequest;
import java.util.regex.Matcher;

/**
 * Somewhat like a watered-down regular expression {@link Matcher},
 * this is an object that determines whether a given {@link ServletRequest}
 * matches a set of criteria.
 *
 * See {@link RequestMatchers} for creating concrete matchers.
 *
 * @author Matt Drees
 */
public interface RequestMatcher
{
    boolean matches(ServletRequest request);
}
