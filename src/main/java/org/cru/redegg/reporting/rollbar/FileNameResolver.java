package org.cru.redegg.reporting.rollbar;

/**
 * @author Matt Drees
 */
public interface FileNameResolver
{
    /**
     * Adds an appropriate path prefix to the given filename,
     * if it is part of the SCM repository that contains the application.
     *
     * The path should be relative to either the root of the repository,
     * or relative to the specific subdirectory in the repository
     * that is configured in the Rollbar UI.
     */
    public String addPath(String fileName, String className);
}
