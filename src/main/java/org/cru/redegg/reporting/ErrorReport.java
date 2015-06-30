package org.cru.redegg.reporting;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Map;

import static org.cru.redegg.util.RedEggStrings.truncate;

/**
 * @author Matt Drees
 */
public class ErrorReport {


    Multimap<String, String> context;
    Map<String, String> user;
    private List<Throwable> thrown;
    private List<String> logRecords;
    private String localHostName;
    private String localHostAddress;
    private Map<String, String> environmentVariables;
    private Map<String, String> systemProperties;
    private boolean userError;

    private WebContext webContext;
    private boolean mustNotify;

    public void addWebContext(WebContext webContext) {
        this.webContext = webContext;
    }

    public Multimap<String, String> getContext()
    {
        return context;
    }

    public void setContext(Multimap<String, String> context)
    {
        this.context = context;
    }

    public Map<String, String> getUser()
    {
        return user;
    }

    public void setUser(Map<String, String> user)
    {
        this.user = user;
    }

    public List<Throwable> getThrown()
    {
        return thrown;
    }

    public void setThrown(List<Throwable> thrown)
    {
        this.thrown = thrown;
    }

    public List<String> getLogRecords()
    {
        return logRecords;
    }

    public void setLogRecords(List<String> logRecords)
    {
        this.logRecords = logRecords;
    }

    public String getLocalHostName()
    {
        return localHostName;
    }

    public void setLocalHostName(String localHostName)
    {
        this.localHostName = localHostName;
    }

    public String getLocalHostAddress()
    {
        return localHostAddress;
    }

    public void setLocalHostAddress(String localHostAddress)
    {
        this.localHostAddress = localHostAddress;
    }

    public Map<String, String> getEnvironmentVariables()
    {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables)
    {
        this.environmentVariables = environmentVariables;
    }

    public Map<String, String> getSystemProperties()
    {
        return systemProperties;
    }

    public void setSystemProperties(Map<String, String> systemProperties)
    {
        this.systemProperties = systemProperties;
    }

    public WebContext getWebContext()
    {
        return webContext;
    }

    public void setWebContext(WebContext webContext)
    {
        this.webContext = webContext;
    }

    /**
     * Returns the exception that is at the root of the 'exception chain' that is first in the 'thrown' list.
     */
    public Optional<Throwable> getRootException()
    {

        if (!thrown.isEmpty())
        {
            return Optional.of(Throwables.getRootCause(thrown.get(0)));
        }
        else
        {
            return Optional.absent();
        }
    }

    public Optional<String> getRootErrorMessage()
    {
        if (getRootException().isPresent())
        {
            return Optional.of(getRootException().get().toString());
        }
        else
        {
            return shortenLogMessage(getFirstLogMessage());
        }
    }

    private Optional<String> shortenLogMessage(Optional<String> firstLogMessage)
    {
        return firstLogMessage.transform(new Function<String, String>()
        {
            public String apply(String input)
            {
                String stripped = stripStacktrace(input);
                return truncate(stripped, 100, "...");
            }

            private String stripStacktrace(String input)
            {
                int stacktraceBegin = input.indexOf("\n\tat ");
                if (stacktraceBegin != -1)
                    return input.substring(0, stacktraceBegin);
                else
                    return input;
            }
        });
    }

    private Optional<String> getFirstLogMessage()
    {
        if (logRecords.isEmpty())
            return Optional.absent();
        else
            return Optional.of(logRecords.get(0));
    }

    public void setUserError(boolean userError)
    {
        this.userError = userError;
    }

    public boolean isUserError()
    {
        return userError;
    }

    public void setMustNotify(boolean mustNotify)
    {
        this.mustNotify = mustNotify;
    }

    public boolean isMustNotify()
    {
        return mustNotify;
    }
}
