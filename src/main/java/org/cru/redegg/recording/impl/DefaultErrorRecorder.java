package org.cru.redegg.recording.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.cru.redegg.org.cru.redegg.reporting.ErrorQueue;
import org.cru.redegg.org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.recording.api.ErrorRecorder;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author Matt Drees
 */
public class DefaultErrorRecorder implements ErrorRecorder {

    ErrorQueue queue;


    Multimap<String, Object> context;
    Object user;
    private LinkedList<Object> thrown;
    private LinkedList<LogRecord> logRecords;

    boolean error;
    private InetAddress localHost;
    private Map<String, String> environmentVariables;
    private Properties systemProperties;

    boolean sentError;

    public DefaultErrorRecorder(ErrorQueue queue) {
        this.queue = queue;
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
            thrown = Lists.newLinkedList();
        this.thrown.add(throwable);

        error = true;
        return this;
    }

    @Override
    public ErrorRecorder recordLogRecord(LogRecord record) {
        checkNotSent();
        if (logRecords == null)
            logRecords = Lists.newLinkedList();
        logRecords.add(record);
        if (isErrorLog(record))
            error = true;
        return this;
    }

    //TODO: decide whether to remove this or the error() call in logging appenders
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


    public boolean wereErrorsAdded() {
        return error;
    }

    public ErrorReport buildReport() {
        //TODO:
        throw new UnsupportedOperationException();
    }


    @Override
    public void error() {
        checkNotSent();
        queue.enqueue(buildReport());
        sentError = true;
    }
}
