package org.cru.redegg.reporting;

import com.google.common.collect.Multimap;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogRecord;

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


    private WebContext webContext;

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
}
