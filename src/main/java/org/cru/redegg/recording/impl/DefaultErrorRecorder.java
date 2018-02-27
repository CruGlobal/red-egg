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
import org.cru.redegg.recording.api.NotificationLevel;
import org.cru.redegg.recording.api.Serializer;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorLink;
import org.cru.redegg.reporting.api.ErrorQueue;
import org.cru.redegg.util.RedEggStrings;
import org.joda.time.format.ISODateTimeFormat;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import static org.cru.redegg.recording.api.NotificationLevel.ERROR;
import static org.cru.redegg.recording.api.NotificationLevel.NONE;
import static org.cru.redegg.recording.api.NotificationLevel.WARNING;

/**
 * @author Matt Drees
 */
public class DefaultErrorRecorder implements ErrorRecorder {

    private static final int LOG_RECORD_LIMIT = 100;

    private static final SimpleFormatter SIMPLE_FORMATTER = new SimpleFormatter();

    private ErrorQueue queue;
    private Serializer serializer;

    private Multimap<String, Object> context;
    private Object user;
    private LinkedHashSet<Throwable> thrown;
    private LinkedList<LogRecord> logRecords;
    private InetAddress localHost;
    private Map<String, String> environmentVariables;
    private Properties systemProperties;
    private Set<String> loggersToIgnoreErrors;
    private Set<String> loggersToIgnoreEntirely;

    private NotificationLevel level = NONE;
    private boolean sentError;
    private boolean mustNotify;
    private ErrorLink errorLink;

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
        addToThrownSet(throwable);
        ensureNotificationLevel(ERROR);
        return this;
    }

    @Override
    public ErrorRecorder recordLogRecord(LogRecord record) {
        checkNotSent();
        String loggerName = record.getLoggerName();
        if (messagesFromLoggerShouldBeIgnored(loggerName))
            return this;

        addRecordToList(record);

        if (errorsFromLoggerShouldTriggerNotification(loggerName))
        {
            if (isErrorLog(record))
            {
                ensureNotificationLevel(ERROR);
            }
            else if (isWarningLog(record))
            {
                ensureNotificationLevel(WARNING);
            }
            if (record.getThrown() != null)
                addToThrownSet(record.getThrown());
        }
        return this;
    }

    private boolean messagesFromLoggerShouldBeIgnored(String loggerName)
    {
        return loggersToIgnoreEntirely != null &&
               loggersToIgnoreEntirely.contains(loggerName);
    }

    private void addRecordToList(LogRecord record)
    {
        if (logRecords == null)
            logRecords = Lists.newLinkedList();
        if (logRecords.size() < LOG_RECORD_LIMIT)
            logRecords.add(record);
    }

    private boolean errorsFromLoggerShouldTriggerNotification(String loggerName)
    {
        return loggersToIgnoreErrors == null ||
               !loggersToIgnoreErrors.contains(loggerName);
    }

    private boolean isErrorLog(LogRecord record) {
        return record.getLevel().intValue() >= Level.SEVERE.intValue();
    }

    private boolean isWarningLog(LogRecord record) {
        int levelAsInt = record.getLevel().intValue();
        return
            levelAsInt >= Level.WARNING.intValue() &&
            levelAsInt < Level.SEVERE.intValue();
    }

    private void addToThrownSet(Throwable throwable)
    {
        if (thrown == null)
            thrown = Sets.newLinkedHashSetWithExpectedSize(4);
        this.thrown.add(throwable);
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
        ensureNotificationLevel(ERROR);
        return this;
    }


    public boolean shouldNotificationBeSent() {
        return level != NONE;
    }

    @Override
    public ErrorRecorder ignoreLogger(String loggerName)
    {
        checkNotSent();
        if (loggersToIgnoreEntirely == null)
            loggersToIgnoreEntirely = Sets.newHashSetWithExpectedSize(1);
        loggersToIgnoreEntirely.add(loggerName);
        return this;
    }

    @Override
    public ErrorRecorder ignoreErrorsFromLogger(String loggerName)
    {
        checkNotSent();
        if (loggersToIgnoreErrors == null)
            loggersToIgnoreErrors = Sets.newHashSetWithExpectedSize(1);
        loggersToIgnoreErrors.add(loggerName);
        return this;
    }

    @Override
    public void error() {
        checkNotSent();
        ensureNotificationLevel(ERROR);
        sendReport();
    }

    @Override
    public void sendReportIfNecessary() {
        if (shouldNotificationBeSent())
            sendReport();
    }

    @Override
    public Optional<ErrorLink> getErrorLink()
    {
        if (shouldNotificationBeSent())
        {
            if (errorLink == null)
            {
                errorLink = queue.buildLink().orElse(null);
            }
        }
        return Optional.ofNullable(errorLink);
    }

    private void sendReport()
    {
        addAdditionalContextIfPossible();
        queue.enqueue(buildReport());
        sentError = true;
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
        report.setErrorLink(errorLink);
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
        report.setMustNotify(mustNotify);
        report.setNotificationLevel(level);

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

    private List<ErrorReport.LogRecord> serializeLogRecords()
    {
        if (logRecords == null)
            return Collections.emptyList();

        List<ErrorReport.LogRecord> serializedLogRecords = Lists.newArrayListWithCapacity(logRecords.size());

        for (LogRecord logRecord : logRecords)
        {
            StacktraceSimplifier simplifier = new StacktraceSimplifier(logRecord.getThrown());
            simplifier.replaceStacktraceIfRedundant();
            try
            {
                serializedLogRecords.add(buildErrorReportLogRecord(logRecord));
            }
            finally
            {
                simplifier.restoreOriginalStacktraces();
            }
        }
        if (logRecords.size() == LOG_RECORD_LIMIT)
        {
            String message =
                "<limit of " +
                LOG_RECORD_LIMIT +
                " was reached; any further log records were not recorded>";

            ErrorReport.LogRecord warningMessage =
                new ErrorReport.LogRecord(NotificationLevel.NONE, "", message);
            serializedLogRecords.add(warningMessage);
        }
        return serializedLogRecords;
    }

    private ErrorReport.LogRecord buildErrorReportLogRecord(LogRecord logRecord)
    {
        String header = buildHeader(logRecord);
        String formattedMessage = SIMPLE_FORMATTER.formatMessage(logRecord);
        String message = RedEggStrings.truncate(formattedMessage, 2000, "...");

        NotificationLevel level = logLevelToNotificationLevel(logRecord);

        return new ErrorReport.LogRecord(level, header, message);
    }

    private String buildHeader(LogRecord logRecord)
    {
        long millis = logRecord.getMillis();
        return ISODateTimeFormat.time().print(millis) + " " + logRecord.getLoggerName();
    }

    private NotificationLevel logLevelToNotificationLevel(LogRecord logRecord)
    {
        Level logLevel = logRecord.getLevel();
        if (logLevel.intValue() >= Level.SEVERE.intValue())
        {
            return NotificationLevel.ERROR;
        }
        else if (logLevel.intValue() >= Level.WARNING.intValue())
        {
            return NotificationLevel.WARNING;
        }
        else if (logLevel.intValue() >= Level.INFO.intValue())
        {
            return NotificationLevel.INFO;
        }
        else if (logLevel.intValue() >= Level.FINEST.intValue())
        {
            return NotificationLevel.DEBUG;
        }
        else
        {
            return NotificationLevel.NONE;
        }
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
        level = WARNING;
        return this;
    }

    /** sets the notification level if it is higher than the current level */
    public void ensureNotificationLevel(NotificationLevel level)
    {
        if (this.level.compareTo(level) < 0)
        {
            this.level = level;
        }
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
