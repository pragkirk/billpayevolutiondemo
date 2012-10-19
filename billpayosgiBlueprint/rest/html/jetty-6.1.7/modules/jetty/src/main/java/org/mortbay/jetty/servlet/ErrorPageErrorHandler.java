// ========================================================================
// Copyright 2006 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ErrorHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.util.TypeUtil;

/** Error Page Error Handler
 * 
 * An ErrorHandler that maps exceptions and status codes to URIs for dispatch using
 * the internal ERROR style of dispatch.
 * @author gregw
 *
 */
public class ErrorPageErrorHandler extends ErrorHandler
{
    protected ServletContext _servletContext;
    protected Map _errorPages; // code or exception to URL

    /* ------------------------------------------------------------ */
    /**
     * @param context
     */
    public ErrorPageErrorHandler()
    {}

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.handler.ErrorHandler#handle(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException
    {
        if (_errorPages!=null)
        {
            String error_page= null;
            Class exClass= (Class)request.getAttribute(ServletHandler.__J_S_ERROR_EXCEPTION_TYPE);
            
            if (ServletException.class.equals(exClass))
            {
                error_page= (String)_errorPages.get(exClass.getName());
                if (error_page == null)
                {
                    Throwable th= (Throwable)request.getAttribute(ServletHandler.__J_S_ERROR_EXCEPTION);
                    while (th instanceof ServletException)
                        th= ((ServletException)th).getRootCause();
                    if (th != null)
                        exClass= th.getClass();
                }
            }
            
            while (error_page == null && exClass != null )
            {
                error_page= (String)_errorPages.get(exClass.getName());
                exClass= exClass.getSuperclass();
            }
            
            if (error_page == null)
            {
                Integer code=(Integer)request.getAttribute(ServletHandler.__J_S_ERROR_STATUS_CODE);
                if (code!=null)
                    error_page= (String)_errorPages.get(TypeUtil.toString(code.intValue()));
            }
            
            if (error_page!=null)
            {
                String old_error_page=(String)request.getAttribute(WebAppContext.ERROR_PAGE);
                if (old_error_page==null || !old_error_page.equals(error_page))
                {
                    request.setAttribute(WebAppContext.ERROR_PAGE, error_page);
                    
                    Dispatcher dispatcher = (Dispatcher) _servletContext.getRequestDispatcher(error_page);
                    try
                    {
                        if(dispatcher!=null)
                        {    
                            dispatcher.error(request, response);
                            return;
                        }
                        else
                        {
                            Log.warn("No error page "+error_page);
                        }
                    }
                    catch (ServletException e)
                    {
                        Log.warn(Log.EXCEPTION, e);
                        return;
                    }
                }
            }
        }
        
        super.handle(target, request, response, dispatch);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the errorPages.
     */
    public Map getErrorPages()
    {
        return _errorPages;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param errorPages The errorPages to set. A map of Exception class name  or error code as a string to URI string
     */
    public void setErrorPages(Map errorPages)
    {
        _errorPages = errorPages;
    }
    
    /* ------------------------------------------------------------ */
    /**
     */
    public void addErrorPage(Class exception,String uri)
    {
        if (_errorPages==null)
            _errorPages=new HashMap();
        _errorPages.put(exception.getName(),uri);
    }
    
    /* ------------------------------------------------------------ */
    /**
     */
    public void addErrorPage(int code,String uri)
    {
        if (_errorPages==null)
            _errorPages=new HashMap();
        _errorPages.put(TypeUtil.toString(code),uri);
    }

    /* ------------------------------------------------------------ */
    protected void doStart() throws Exception
    {
        super.doStart();
        _servletContext=ContextHandler.getCurrentContext();
    }

    /* ------------------------------------------------------------ */
    protected void doStop() throws Exception
    {
        // TODO Auto-generated method stub
        super.doStop();
    }
    
}
