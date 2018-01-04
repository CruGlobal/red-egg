package org.cru.redegg.reporting;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.joda.time.DateTime;

import java.lang.reflect.Method;
import java.net.URI;

/**
 * @author Matt Drees
 */
public class WebContext implements Cloneable {


    // Note: all fields must be immutable; see clone()

    private URI url;
    private DateTime start;
    private Multimap<String, String> headers;
    private String method;
    private Multimap<String, String> queryParameters;
    private Multimap<String, String> postParameters;
    private String entityRepresentation;
    private DateTime finish;
    private Integer responseStatus;
    private Method component;
    private String remoteIpAddress;


    public Multimap<String, String> getCombinedQueryAndPostParameters()
    {
        Multimap<String, String> combined =
            LinkedHashMultimap.create(queryParameters.size() + postParameters.size(), 1);
        combined.putAll(queryParameters);
        combined.putAll(postParameters);
        return combined;
    }

    /**
     * Performs a shallow clone.
     * Because all of the attributes are immutable, the shallow clone is effectively a deep clone.
     *
     * @return an identical WebContext that will not share mutable state with this one.
     */
    @Override
    public WebContext clone()
    {
        try
        {
            return (WebContext) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new AssertionError("this class is Cloneable");
        }
    }

    public Method getComponent()
    {
        return component;
    }

    public void setComponent(Method component)
    {
        this.component = component;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    public URI getUrl() {
        return url;
    }

    public void setStart(DateTime start) {
        this.start = start;
    }

    public DateTime getStart() {
        return start;
    }

    public void setHeaders(Multimap<String, String> headers) {
        this.headers = ImmutableMultimap.copyOf(headers);
    }

    public Multimap<String, String> getHeaders() {
        return headers;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public void setQueryParameters(Multimap<String, String> queryParameters) {
        this.queryParameters = ImmutableMultimap.copyOf(queryParameters);
    }

    public Multimap<String, String> getQueryParameters() {
        return queryParameters;
    }

    public void setPostParameters(Multimap<String, String> postParameters) {
        this.postParameters = ImmutableMultimap.copyOf(postParameters);
    }

    public Multimap<String, String> getPostParameters() {
        return postParameters;
    }

    public void setFinish(DateTime finish) {
        this.finish = finish;
    }

    public DateTime getFinish() {
        return finish;
    }

    public String getEntityRepresentation()
    {
        return entityRepresentation;
    }

    public void setEntityRepresentation(String entityRepresentation)
    {
        this.entityRepresentation = entityRepresentation;
    }

    public Integer getResponseStatus()
    {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus)
    {
        this.responseStatus = responseStatus;
    }

    public void setRemoteIpAddress(String remoteIpAddress)
    {
        this.remoteIpAddress = remoteIpAddress;
    }

    public String getRemoteIpAddress()
    {
        return remoteIpAddress;
    }
}
