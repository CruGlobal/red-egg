package org.cru.redegg.reporting;

import com.google.common.collect.Multimap;
import org.joda.time.DateTime;

import java.lang.reflect.Method;
import java.net.URI;

/**
 * @author Matt Drees
 */
public class WebContext {


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
        this.headers = headers;
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
        this.queryParameters = queryParameters;
    }

    public Multimap<String, String> getQueryParameters() {
        return queryParameters;
    }

    public void setPostParameters(Multimap<String, String> postParameters) {
        this.postParameters = postParameters;
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
}
