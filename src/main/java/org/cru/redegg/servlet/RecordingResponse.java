package org.cru.redegg.servlet;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * @author Matt Drees
 */
public class RecordingResponse extends HttpServletResponseWrapper
{
    public RecordingResponse(HttpServletResponse response)
    {
        super(response);
    }

    int statusCode = -1;

    @Override
    public void sendError(int statusCode) throws IOException
    {
        this.statusCode = statusCode;
        super.sendError(statusCode);
    }

    @Override
    public void sendError(int statusCode, String message) throws IOException
    {
        this.statusCode = statusCode;
        super.sendError(statusCode, message);
    }

    @Override
    public void sendRedirect(String location) throws IOException
    {
        statusCode = SC_FOUND;
        super.sendRedirect(location);
    }

    @Override
    public void setStatus(int statusCode)
    {
        this.statusCode = statusCode;
        super.setStatus(statusCode);
    }

    @Override
    @Deprecated
    public void setStatus(int statusCode, String statusMessage)
    {
        this.statusCode = statusCode;
        super.setStatus(statusCode, statusMessage);
    }

    public int getStatusCode()
    {
        return statusCode;
    }
}
