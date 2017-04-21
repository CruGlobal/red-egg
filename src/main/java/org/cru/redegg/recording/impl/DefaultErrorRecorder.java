package org.cru.redegg.recording.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.Serializer;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorQueue;
import org.cru.redegg.util.RedEggStrings;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * @author Matt Drees
 */
public class DefaultErrorRecorder implements ErrorRecorder {

    private static final int LOG_RECORD_LIMIT = 100;
    private ErrorQueue queue;
    private Serializer serializer;

    private Multimap<String, Object> context;
    private Object user;
    private LinkedHashSet<Throwable> thrown;
    private LinkedList<LogRecord> logRecords;
    private InetAddress localHost;
    private Map<String, String> environmentVariables;
    private Properties systemProperties;
    private Set<String> loggersToIgnore;

    private boolean error;
    private boolean sentError;
    private boolean userError;
    private boolean mustNotify;

    @Inject
    public DefaultErrorRecorder(ErrorQueue queue, Serializer serializer) {
        this.queue = queue;
        this.serializer = serializer;
    }

    @Override
    public ErrorRecorder recordContext(String key, Object object) {
        checkNotSent();
        if (context == null)
            context = HashMultimap.create(10, 1);
        context.put(key, object);
        return this;
    }

    private void checkNotSent() {
        Preconditions.checkState(!sentError, "already sent the error report");
    }

    @Override
    public ErrorRecorder recordUser(Object user) {
        checkNotSent();
        this.user = user;
        return this;
    }

    @Override
    public ErrorRecorder recordThrown(Throwable throwable) {
        checkNotSent();
        if (thrown == null)
            thrown = Sets.newLinkedHashSetWithExpectedSize(4);
        this.thrown.add(throwable);

        error = true;
        return this;
    }

    @Override
    public ErrorRecorder recordLogRecord(LogRecord record) {
        checkNotSent();
        if (logRecords == null)
            logRecords = Lists.newLinkedList();
        if (logRecords.size() < LOG_RECORD_LIMIT)
            logRecords.add(record);
        if (isErrorLog(record) && isNotIgnored(record.getLoggerName()))
        {
            if (record.getThrown() != null)
                recordThrown(record.getThrown());
            else
                error = true;
        }
        return this;
    }

    private boolean isNotIgnored(String loggerName)
    {
        return loggersToIgnore == null ||
               !loggersToIgnore.contains(loggerName);
    }

    private boolean isErrorLog(LogRecord record) {
        return record.getLevel().intValue() >= Level.SEVERE.intValue();
    }

    @Override
    public ErrorRecorder recordSystemProperties(Properties systemProperties) {
        checkNotSent();
        this.systemProperties = systemProperties;
        return this;
    }

    @Override
    public ErrorRecorder recordEnvironmentVariables(Map<String, String> variables) {
        checkNotSent();
        this.environmentVariables = variables;
        return this;
    }

    @Override
    public ErrorRecorder recordLocalHost(InetAddress localHost) {
        checkNotSent();
        this.localHost = localHost;
        return this;
    }

    @Override
    public ErrorRecorder mustNotify()
    {
        checkNotSent();
        this.mustNotify = true;
        this.error = true;
        return this;
    }


    public boolean wereErrorsAdded() {
        return error;
    }

    @Override
    public ErrorRecorder ignoreErrorsFromLogger(String loggerName)
    {
        checkNotSent();
        if (loggersToIgnore == null)
            loggersToIgnore = Sets.newHashSetWithExpectedSize(1);
        loggersToIgnore.add(loggerName);
        return this;
    }

    @Override
    public void error() {
        checkNotSent();
        addAdditionalContextIfPossible();
        queue.enqueue(buildReport());
        sentError = true;
    }

    @Override
    public void sendReportIfNecessary() {
        if (wereErrorsAdded())
            error();
    }

    public void addAdditionalContextIfPossible()
    {
        addLocalHost();
    }

    private void addLocalHost()
    {
        if (localHost == null)
        {
            try
            {
                localHost = InetAddress.getLocalHost();
            }
            catch (UnknownHostException ignored)
            {
            }
        }
    }


    public ErrorReport buildReport() {
        ErrorReport report = new ErrorReport();
        report.setContext(serializeContext());
        report.setUser(serializeUser());
        report.setThrown(getThrown());
        report.setLogRecords(serializeLogRecords());
        if (localHost != null)
        {
            report.setLocalHostName(localHost.getHostName());
            report.setLocalHostAddress(localHost.getHostAddress());
        }

        report.setEnvironmentVariables(getEnvironmentVariables());
        report.setSystemProperties(getSystemProperties());
        report.setUserError(userError);
        report.setMustNotify(mustNotify);

        return report;
    }

    private Multimap<String, String> serializeContext()
    {
        if (context == null)
            return ImmutableMultimap.of();
        Multimap<String, String> serializedContext = HashMultimap.create(context.keys().size(), 1);
        for (Map.Entry<String, Object> entry : context.entries())
        {
            serializedContext.put(entry.getKey(), serializer.toString(entry.getValue()));
        }
        return serializedContext;
    }

    private Map<String, String> serializeUser()
    {
        if (user == null)
            return Collections.emptyMap();
        else
            return serializer.toStringMap(user);
    }

    private List<Throwable> getThrown()
    {
        if (thrown == null)
            return Collections.emptyList();
        return Lists.newArrayList(removeCauses());
    }

    /**
     * Removes redundant throwables from the list.
     * A throwable is redundant if it is a cause (directly or indirectly) for another exception in the list.
     * Such causes will be printed when their parent is printed, and don't need to be printed separately.
     */
    private Set<Throwable> removeCauses()
    {
        Set<Throwable> filtered = Sets.newLinkedHashSet(thrown);
        for (Throwable head : thrown)
        {
            Throwable cause = head.getCause();
            while (cause != null)
            {
                filtered.remove(cause);
                cause = cause.getCause();
            }
        }
        return filtered;
    }

    private List<String> serializeLogRecords()
    {
        if (logRecords == null)
            return Collections.emptyList();

        List<String> serializedLogRecords = Lists.newArrayListWithCapacity(logRecords.size());
        SimpleFormatter formatter = new SimpleFormatter();

        for (LogRecord logRecord : logRecords)
        {
            StacktraceSimplifier simplifier = new StacktraceSimplifier(logRecord.getThrown());
            simplifier.replaceStacktraceIfRedundant();
            try
            {
                String formatted = formatter.format(logRecord);
                String truncated = RedEggStrings.truncate(formatted, 2000, "...");
                serializedLogRecords.add(truncated);
            }
            finally
            {
                simplifier.restoreOriginalStacktraces();
            }
        }
        if (logRecords.size() == LOG_RECORD_LIMIT)
        {
            serializedLogRecords.add(
                "<limit of " +
                LOG_RECORD_LIMIT +
                " was reached; any further log records were not recorded>");
        }
        return serializedLogRecords;
    }

    private Map<String, String> getSystemProperties()
    {
        if (systemProperties == null)
            return Collections.emptyMap();

        Map<String, String> propertiesMap = new TreeMap<String, String>();
        for (Map.Entry<Object, Object> entry : systemProperties.entrySet())
        {
            propertiesMap.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return propertiesMap;
    }

    private Map<String, String> getEnvironmentVariables()
    {
        if (environmentVariables == null)
            return Collections.emptyMap();

        return new TreeMap<String, String>(environmentVariables);
    }

    public DefaultErrorRecorder userError()
    {
        userError = true;
        return this;
    }

    private class StacktraceSimplifier
    {
        private Throwable loggedThrowable;

        Map<Throwable, StackTraceElement[]> originalStacktraces = Maps.newHashMapWithExpectedSize(4);

        public StacktraceSimplifier(Throwable loggedThrowable)
        {
            this.loggedThrowable = loggedThrowable;
        }

        private void replaceStacktraceIfRedundant()
        {
            if (loggedThrowable == null)
                return;
            if (isThrowableRedundant())
            {
                removeStackTrace();
            }
        }

        private void removeStackTrace()
        {
            for (Throwable link : Throwables.getCausalChain(loggedThrowable))
            {
                StackTraceElement[] originalStacktrace = link.getStackTrace();
                originalStacktraces.put(link, originalStacktrace);

                StackTraceElement[] newStackTrace = new StackTraceElement[1];
                StackTraceElement dummy = new StackTraceElement("...(" + originalStacktrace.length  + " redundant stack frames removed)", "", "", 0);
                newStackTrace[0] = dummy;
                link.setStackTrace(newStackTrace);
            }
        }

        private boolean isThrowableRedundant()
        {
            for (Throwable recordedThrowable : getThrown())
            {
                for (Throwable link : Throwables.getCausalChain(recordedThrowable))
                {
                    if (link == loggedThrowable)
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        public void restoreOriginalStacktraces()
        {
            for (Map.Entry<Throwable, StackTraceElement[]> entry : originalStacktraces.entrySet())
            {
                Throwable t = entry.getKey();
                t.setStackTrace(entry.getValue());
            }
        }
    }
}
