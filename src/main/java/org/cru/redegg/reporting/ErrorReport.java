package org.cru.redegg.reporting;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Multimap;
import org.cru.redegg.recording.api.NotificationLevel;

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
    private List<LogRecord> logRecords;
    private String localHostName;
    private String localHostAddress;
    private Map<String, String> environmentVariables;
    private Map<String, String> systemProperties;

    private WebContext webContext;
    private boolean mustNotify;
    private NotificationLevel notificationLevel;

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

    public List<LogRecord> getLogRecords()
    {
        return logRecords;
    }

    public void setLogRecords(List<LogRecord> logRecords)
    {
        this.logRecords = logRecords;
    }

    public static class LogRecord
    {
        public final NotificationLevel level;
        public final String header;
        public final String message;

        public LogRecord(NotificationLevel level, String header, String message)
        {
            this.level = level;
            this.header = header;
            this.message = message;
        }

        @Override
        public String toString()
        {
            return header + " " + level + " " + message;
        }
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
            return shortenLogMessage(getFirstHighestLevelLogMessage());
        }
    }

    private Optional<String> shortenLogMessage(Optional<String> firstLogMessage)
    {
        return firstLogMessage.transform(new Function<String, String>()
        {
            public String apply(String input)
            {
                String stripped = stripStacktrace(input);
                return truncate(stripped, 200, "...");
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

    private Optional<String> getFirstHighestLevelLogMessage()
    {
        if (logRecords.isEmpty())
            return Optional.absent();
        else
        {
            LogRecord highestLevelSoFar = logRecords.get(0);
            for (LogRecord logRecord : logRecords)
            {
                if (logRecord.level.compareTo(highestLevelSoFar.level) > 1)
                {
                    highestLevelSoFar = logRecord;
                }
            }

            return Optional.of(highestLevelSoFar.message);
        }
    }

    public void setMustNotify(boolean mustNotify)
    {
        this.mustNotify = mustNotify;
    }

    public boolean isMustNotify()
    {
        return mustNotify;
    }

    public void setNotificationLevel(NotificationLevel notificationLevel)
    {
        this.notificationLevel = notificationLevel;
    }

    public NotificationLevel getNotificationLevel()
    {
        return notificationLevel;
    }
}
