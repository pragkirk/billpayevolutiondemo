package org.mortbay.terracotta.servlet;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.jetty.servlet.AbstractSessionManager.Session;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.util.LazyList;



public class TerracottaSessionIdManager extends AbstractLifeCycle
        implements SessionIdManager
{
    private final static String __NEW_SESSION_ID="org.mortbay.jetty.newSessionId";  
    protected final static String SESSION_ID_RANDOM_ALGORITHM = "SHA1PRNG";
    protected final static String SESSION_ID_RANDOM_ALGORITHM_ALT = "IBMSecureRandom";
    
    private String _workerName;
    protected Random _random;
    private boolean _weakRandom;
    private HashSet _sessionIds;
    private Server _server;
    
    public TerracottaSessionIdManager (Server server)
    {
        _server = server;
    }

    public void addSession(HttpSession session)
    {
        synchronized (_sessionIds)
        {
            synchronized (session)
            {
                _sessionIds.add(((TerracottaSessionManager.Session)session).getIdWithinCluster());
            }
        }
    }

    public String getWorkerName()
    {
        return _workerName;
    }
    
    public void setWorkerName(String workerName)
    {
        _workerName=workerName;
    }

    public boolean idInUse(String id)
    {
        synchronized (_sessionIds)
        {
            int dot=id.lastIndexOf('.');
            String cluster_id=(dot>0)?id.substring(0,dot):id;
            return _sessionIds.contains(id);
        }
    }

    /** 
     * When told to invalidate all session instances that share the same id, the
     * TerracottaSessionManager will tell all contexts on the server for which
     * it is defined to delete any session object they might have matching the id.
     * @see org.mortbay.jetty.SessionIdManager#invalidateAll(java.lang.String)
     */
    public void invalidateAll(String id)
    {
        synchronized (this)
        {
            //take the session id out of the set first. 
            synchronized(_sessionIds)
            {
                _sessionIds.remove(id);
            }

            //tell all contexts that may have a session object with this id to
            //get rid of them
            Handler[] contexts = _server.getChildHandlersByClass(WebAppContext.class);
            for (int i=0; contexts!=null && i<contexts.length; i++)
            {
                Session session = ((AbstractSessionManager)((WebAppContext)contexts[i]).getSessionHandler().getSessionManager()).getSession(id);
                if (session !=null)
                    session.invalidate();
            }
        }
    }

    public String newSessionId(HttpServletRequest request, long created)
    {
        // Generate a unique id. This id must be unique
        // across all nodes in the cluster, as the table
        // of session ids is shared across all nodes.
        synchronized (this)
        {
            // A requested session ID can only be used if it is in use already.
            String requested_id=request.getRequestedSessionId();
            if (requested_id!=null&&idInUse(requested_id))
                return requested_id;

            // Else reuse any new session ID already defined for this request.
            String new_id=(String)request.getAttribute(__NEW_SESSION_ID);
            if (new_id!=null&&idInUse(new_id))
                return new_id;

            // pick a new unique ID!
            String id=null;
            while (id==null||id.length()==0||idInUse(id))
            {
                long r=_weakRandom
                ?(hashCode()^Runtime.getRuntime().freeMemory()^_random.nextInt()^(((long)request.hashCode())<<32))
                :_random.nextLong();
                r^=created;
                if (request!=null && request.getRemoteAddr()!=null)
                    r^=request.getRemoteAddr().hashCode();
                if (r<0)
                    r=-r;
                id=Long.toString(r,36);
                
                //prefix with the id of the node to ensure unique across cluster
                id=_workerName + id;
            }

            request.setAttribute(__NEW_SESSION_ID,id);
            return id;
        }
    }

    public void removeSession(HttpSession session)
    {
        synchronized (_sessionIds)
        {
            _sessionIds.remove(((TerracottaSessionManager.Session)session).getId());
        }
    }
    
    public void doStart()
    {
        if (_random==null)
        {      
            try 
            {
                _random=SecureRandom.getInstance(SESSION_ID_RANDOM_ALGORITHM);
            }
            catch (NoSuchAlgorithmException e)
            {
                try
                {
                    _random=SecureRandom.getInstance(SESSION_ID_RANDOM_ALGORITHM_ALT);
                    _weakRandom=false;
                }
                catch (NoSuchAlgorithmException e_alt)
                {
                    Log.warn("Could not generate SecureRandom for session-id randomness",e);
                    _random=new Random();
                    _weakRandom=true;
                }
            }
        }
        _random.setSeed(_random.nextLong()^System.currentTimeMillis()^hashCode()^Runtime.getRuntime().freeMemory());
        _sessionIds = new HashSet();
    }
    
    public void doStop()
    {
    }
    
    /**
     * Get a cluster ID from a node ID.
     * Strip node identifier from a located session ID.
     * @see org.mortbay.jetty.SessionIdManager#getClusterId(java.lang.String)
     */
    public String getClusterId(String nodeId)
    {
        int dot=nodeId.lastIndexOf('.');
        return (dot>0)?nodeId.substring(0,dot):nodeId;
    }

    /**
     * Get a node ID from a cluster ID and a request
     * @param clusterId The ID of the session
     * @param request The request that for the session (or null)
     * @return The session ID qualified with the node ID.t the id of the session within the cluster
     * @see org.mortbay.jetty.SessionIdManager#getNodeId(java.lang.String, javax.servlet.http.HttpServletRequest)
     */
    public String getNodeId(String clusterId, HttpServletRequest request)
    {
        if (_workerName!=null)
            return clusterId+'.'+_workerName;

        return clusterId;
    }

    
}
