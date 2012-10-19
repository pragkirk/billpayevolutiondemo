// ========================================================================
// Copyright 2007 Mort Bay Consulting Pty. Ltd.
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
//========================================================================

package org.mortbay.cometd;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.cometd.filter.JSONDataFilter;
import org.mortbay.log.Log;
import org.mortbay.util.IO;
import org.mortbay.util.StringMap;
import org.mortbay.util.ajax.JSON;

import dojox.cometd.Bayeux;
import dojox.cometd.DataFilter;
import dojox.cometd.Message;

/**
 * Cometd Filter Servlet implementing the {@link AbstractBayeux} protocol.
 * 
 * The Servlet can be initialized with a json file mapping channels to
 * {@link DataFilter} definitions. The servlet init parameter "filters" should
 * point to a webapplication resource containing a JSON array of filter
 * definitions. For example:
 * 
 * <pre>
 *  [
 *    { 
 *      &quot;channels&quot;: &quot;/**&quot;,
 *      &quot;class&quot;   : &quot;org.mortbay.cometd.filter.NoMarkupFilter&quot;,
 *      &quot;init&quot;    : {}
 *    }
 *  ]
 * </pre>
 * The following init parameters can be used to configure the servlet:<dl>
 * <dt>timeout</dt>
 * <dd>The server side poll timeout in milliseconds (default 250000). This is how
 * long the server will hold a reconnect request before responding.</dd>
 * 
 * <dt>interval</dt>
 * <dd>The client side poll timeout in milliseconds (default 0). How long a client
 * will wait between reconnects</dd>
 * 
 * <dt>maxInterval</dt>
 * <dd>The max client side poll timeout in milliseconds (default 30000). A client will
 * be removed if a connection is not received in this time.
 * 
 * <dt>multiFrameInterval</dt>
 * <dd>the client side poll timeout
 * if multiple connections are detected from the same browser (default 1500).</dd>
 * 
 * <dt>JSONCommented</dt>
 * <dd>If "true" then the server will accept JSON wrapped
 * in a comment and will generate JSON wrapped in a comment. This is a defence against
 * Ajax Hijacking.</dd>
 * 
 * <dt>alwaysResumePoll</dt>
 * <dd>If true, then reconnect requests will always
 * be resumed when a message is delivered. This may be needed for some cross domain 
 * transports that need strict ordering of responses.</dd>
 * 
 * <dt>filters</dt>
 * <dd>the location of a JSON file describing {@link DataFilter} instances to be installed</dd>
 * 
 * <dt></dt>
 * <dd></dd>
 * 
 * <dt>loglevel</dt>
 * <dd><0=none, 1=info, 2=debug/dd>
 * 
 * </dl>
 * 
 * @author gregw
 * @author aabeling: added JSONP transport
 * 
 * @see {@link AbstractBayeux}
 * @see {@link ChannelId}
 */
public abstract class AbstractCometdServlet extends HttpServlet
{
    public static final String CLIENT_ATTR="org.mortbay.cometd.client";
    public static final String TRANSPORT_ATTR="org.mortbay.cometd.transport";
    public static final String MESSAGE_PARAM="message";
    public static final String TUNNEL_INIT_PARAM="tunnelInit";
    public static final String HTTP_CLIENT_ID="BAYEUX_HTTP_CLIENT";
    public final static String BROWSER_ID="BAYEUX_BROWSER";
    

    protected AbstractBayeux _bayeux;
    protected long _timeout=240000;

    public AbstractBayeux getBayeux()
    {
        return _bayeux;
    }
    
    protected abstract AbstractBayeux newBayeux();
    
    public void init() throws ServletException
    {
        synchronized (AbstractCometdServlet.class)
        {
            _bayeux=(AbstractBayeux)getServletContext().getAttribute(Bayeux.DOJOX_COMETD_BAYEUX);
            if (_bayeux==null)
            {    
                _bayeux=newBayeux(); 
            }
        }
        
        synchronized(_bayeux)
        {
            boolean was_initialized=_bayeux.isInitialized();
            _bayeux.initialize(getServletContext());
            getServletContext().setAttribute(Bayeux.DOJOX_COMETD_BAYEUX,_bayeux);
            
            if (!was_initialized)
            {
                String filters=getInitParameter("filters");
                if (filters!=null)
                {
                    try
                    {
                        InputStream is = getServletContext().getResourceAsStream(filters);
                        if (is==null)
                            throw new FileNotFoundException(filters);
                        
                        Object[] objects=(Object[])JSON.parse(new InputStreamReader(getServletContext().getResourceAsStream(filters),"utf-8"));
                        for (int i=0; objects!=null&&i<objects.length; i++)
                        {
                            Map filter_def=(Map)objects[i];

                            String fc = (String)filter_def.get("class");
                            if (fc!=null)
                                Log.warn(filters+" file uses deprecated \"class\" name. Use \"filter\" instead");
                            else
                                fc=(String)filter_def.get("filter");
                            Class c=Thread.currentThread().getContextClassLoader().loadClass(fc);
                            DataFilter filter=(DataFilter)c.newInstance();

                            if (filter instanceof JSONDataFilter)
                                ((JSONDataFilter)filter).init(filter_def.get("init"));

                            _bayeux.addFilter((String)filter_def.get("channels"),filter);
                        }
                    }
                    catch (Exception e)
                    {
                        getServletContext().log("Could not parse: "+filters,e);
                        throw new ServletException(e);
                    }
                }

                String timeout=getInitParameter("timeout");
                if (timeout!=null)
                    _timeout=Long.parseLong(timeout);
                
                String maxInterval=getInitParameter("maxInterval");
                if (maxInterval!=null)
                    _bayeux.setMaxInterval(Long.parseLong(maxInterval));

                String commentedJSON=getInitParameter("JSONCommented");
                _bayeux.setJSONCommented(commentedJSON!=null && Boolean.parseBoolean(commentedJSON));

                String l=getInitParameter("logLevel");
                if (l!=null&&l.length()>0)
                    _bayeux.setLogLevel(Integer.parseInt(l));
                
                String interval=getInitParameter("interval");
                if (interval!=null)
                {
                    JSON.Literal advice = new JSON.Literal("{\"reconnect\":\"retry\",\"interval\":"+interval+"}");
                    _bayeux.setAdvice(advice);
                }
                
                String mfInterval=getInitParameter("multiFrameInterval");
                if (mfInterval!=null)
                    _bayeux.setMultiFrameInterval(Integer.parseInt(mfInterval));

            }
        }
    }

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String init=req.getParameter(TUNNEL_INIT_PARAM);
        if ("iframe".equals(init))
        {
            throw new IllegalStateException("Not supported");
            /*
            Transport transport=new IFrameTransport();
            ((IFrameTransport)transport).initTunnel(resp);
            */
        }
        else
        {
            super.service(req,resp);
        }
    }


    protected String browserId(HttpServletRequest request)
    {
        Cookie[] cookies = request.getCookies();
        if (cookies!=null)
        {
            for (Cookie cookie : cookies)
            {
                if (BROWSER_ID.equals(cookie.getName()))
                    return cookie.getValue();
            }
        }
        
        return null;
    }

    protected String newBrowserId(HttpServletRequest request,HttpServletResponse response)
    {
        String browser_id=Long.toHexString(request.getRemotePort())+
        Long.toHexString(_bayeux.getRandom(request.hashCode()))+
        Long.toHexString(System.currentTimeMillis())+
        Long.toHexString(request.getRemoteAddr().hashCode());
        
        Cookie cookie = new Cookie(BROWSER_ID,browser_id);
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        response.addCookie(cookie);
        return browser_id;
    }
    
    private static Message[] __EMPTY_BATCH=new Message[0];

    protected Message[] getMessages(HttpServletRequest request) throws IOException
    {
        String fodder=null;
        try
        {
            // Get message batches either as JSON body or as message parameters
            if (request.getContentType() != null && !request.getContentType().startsWith("application/x-www-form-urlencoded"))
            {
                String s=IO.toString(request.getReader());
                return _bayeux.parse(s);
                // return _bayeux.parse(request.getReader());
            }

            String[] batches=request.getParameterValues(MESSAGE_PARAM);

            if (batches==null || batches.length==0)
                return __EMPTY_BATCH;

            if (batches.length==0)
            {
                fodder=batches[0];
                return _bayeux.parse(fodder);
            }

            List<Message> messages = new ArrayList<Message>();
            for (int i=0;i<batches.length;i++)
            {
                if (batches[i]==null)
                    continue;

                fodder=batches[i];
                _bayeux.parseTo(fodder,messages);
                
            }

            return messages.toArray(new Message[messages.size()]);
        }
        catch(IOException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            throw new Error(fodder,e);
        }
    }

}
