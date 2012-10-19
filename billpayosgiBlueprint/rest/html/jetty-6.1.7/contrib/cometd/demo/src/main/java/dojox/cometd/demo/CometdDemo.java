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

package dojox.cometd.demo;


import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.cometd.ClientImpl;
import org.mortbay.cometd.continuation.ContinuationClient;
import org.mortbay.cometd.continuation.ContinuationCometdServlet;
import org.mortbay.cometd.ext.TimestampExtension;
import org.mortbay.cometd.ext.TimesyncExtension;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import dojox.cometd.Client;
import dojox.cometd.Message;


/* ------------------------------------------------------------ */
/** Main class for cometd demo.
 * 
 * This is of use when running demo in a terracotta cluster
 * 
 * @author gregw
 *
 */
public class CometdDemo
{
    private static int _testHandshakeFailure;
    
    /* ------------------------------------------------------------ */
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        int port = args.length==0?8080:Integer.parseInt(args[0]);
        
        // Manually contruct context to avoid hassles with webapp classloaders for now.
        Server server = new Server();
        SelectChannelConnector connector=new SelectChannelConnector();
        connector.setPort(port);
        server.addConnector(connector);
        Context context = new Context(server,"/",Context.NO_SECURITY|Context.NO_SESSIONS);
        context.setResourceBase("./demo/src/main/webapp");
        
        ContinuationCometdServlet cometd_servlet=new ContinuationCometdServlet();
        ServletHolder cometd_holder = new ServletHolder(cometd_servlet);
        // cometd_holder.setInitParameter("filters","/WEB-INF/filters.json");
        cometd_holder.setInitParameter("timeout","240000");
        cometd_holder.setInitParameter("interval","0");
        cometd_holder.setInitParameter("maxInterval","30000");
        cometd_holder.setInitParameter("multiFrameInterval","1500");
        cometd_holder.setInitParameter("JSONCommented","true");
        cometd_holder.setInitParameter("logLevel","1");
        
        context.addServlet(cometd_holder, "/cometd/*");
        context.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
        
        context.addEventListener(new BayeuxStartupListener());
        
        server.start();
        AbstractBayeux bayeux = cometd_servlet.getBayeux();
        // bayeux.addExtension(new TimesyncExtension());
        BayeuxStartupListener listener = new BayeuxStartupListener();
        listener.initialize(bayeux);
        
        bayeux.setSecurityPolicy(new AbstractBayeux.DefaultPolicy(){
            public boolean canHandshake(Message message)
            {
                if (_testHandshakeFailure<0)
                {
                    _testHandshakeFailure++;
                    return false;
                }
                return true;
            }
            
        });
        
        while (true)
        {
            Thread.sleep(2000);
            java.util.Set<String> ids=bayeux.getClientIDs();
            ClientImpl[] clients=new ClientImpl[ids.size()];
            long[] last=new long[ids.size()];
            int i=0;
            long now = System.currentTimeMillis();
            for (String id : ids)
            {
                clients[i]=(ClientImpl)bayeux.getClient(id);
                
                if (clients[i] instanceof ContinuationClient)
                {
                    ContinuationClient cc= (ContinuationClient)clients[i];

                    last[i]=now - cc.lastAccessed();
                    if (cc.hasMessages() && cc.getContinuation()==null && !cc._timeout.isScheduled() && last[i]>1000)
                        System.err.println("??? "+cc+" last="+last[i]);
                }
                
                i++;
            }
            
            i=0;
            
        }
        
    }

}
