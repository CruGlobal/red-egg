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
    public void sendError(int sc) throws IOException
    {
        statusCode = sc;
        super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException
    {
        statusCode = sc;
        super.sendError(sc, msg);
    }

    @Override
    public void sendRedirect(String location) throws IOException
    {
        statusCode = SC_FOUND;
        super.sendRedirect(location);
    }

    @Override
    public void setStatus(int sc)
    {
        statusCode = sc;
        super.setStatus(sc);
    }

    @Override
    @Deprecated
    public void setStatus(int sc, String sm)
    {
        statusCode = sc;
        super.setStatus(sc, sm);
    }

    public int getStatusCode()
    {
        return statusCode;
    }
}
