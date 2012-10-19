package org.mortbay.terracotta.servlet;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import junit.framework.TestCase;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.jetty.servlet.AbstractSessionManager.Session;


public class TerracottaSessionManagerTest extends TestCase
{
    Server server = new Server();
    TerracottaSessionManager manager;
    TerracottaSessionIdManager idManager = new MySessionIdManager(server);
    SessionHandler handler;
   
    
    
    public TerracottaSessionManagerTest ()
    {
        idManager.setWorkerName("fred");
    }
    
    public void setUp () throws Exception
    {
        manager = new TerracottaSessionManager();
        manager.setIdManager(idManager);
        handler = new SessionHandler(manager);        
        ContextHandler context=new ContextHandler();
        manager.setSessionHandler(handler);
        server.setHandler(context);
        context.setHandler(handler);
        server.start();
    }
    
    public void tearDown () throws Exception
    {
        server.stop();
    }

    
    public void testSession () throws Exception
    {
        //test creating a session
        HttpSession session = manager.newHttpSession(null);
        assertTrue(session.isNew());
        assertTrue(manager.getSessionMap().containsValue(session));
        String instanceId = session.getId();
        String clusterId = manager.getClusterId(session);
        
        System.err.println("instanceId="+instanceId);
        System.err.println("clusterId="+clusterId);
        
        assertTrue(idManager.idInUse(clusterId));
        assertTrue(manager._sessionDataMap.containsKey(clusterId));
        
        //test removing it
        session.invalidate();
        
        assertFalse(manager.isValid(session));
        assertFalse(manager.getSessionMap().containsValue(session));
        assertFalse(idManager.idInUse(clusterId));
    }
    
    
    public void testCrossContext () throws Exception
    {
        //create a session 
        HttpSession session = manager.newHttpSession(null);
        assertTrue(session.isNew());
        assertTrue(manager.getSessionMap().containsValue(session));
        String instanceId = session.getId();
        String clusterId = manager.getClusterId(session);
        
        assertTrue(idManager.idInUse(clusterId));
        assertTrue(manager._sessionDataMap.containsKey(clusterId));
        
        
    }
    
    class MySessionIdManager extends TerracottaSessionIdManager
    {
        public MySessionIdManager(Server server)
        {
            super(server);
        }
        
        public String newSessionId(HttpServletRequest request, long created)
        {
            return getWorkerName()+_random.nextInt();
        }
    }

}
