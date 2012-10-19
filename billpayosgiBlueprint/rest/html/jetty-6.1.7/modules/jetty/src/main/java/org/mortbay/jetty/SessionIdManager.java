package org.mortbay.jetty;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.mortbay.component.LifeCycle;

/** Session ID Manager.
 * Manages session IDs across multiple contexts.
 * @author gregw
 *
 */
/* ------------------------------------------------------------ */
/**
 * @author gregw
 *
 */
public interface SessionIdManager extends LifeCycle
{
    /**
     * @param id The session ID without any cluster node extension
     * @return True if the session ID is in use by at least one context.
     */
    public boolean idInUse(String id);
    
    /**
     * Add a session to the list of known sessions for a given ID.
     * @param session The session
     */
    public void addSession(HttpSession session);
    
    /**
     * Remove session from the list of known sessions for a given ID.
     * @param session
     */
    public void removeSession(HttpSession session);
    
    /**
     * Call {@link HttpSession#invalidate()} on all known sessions for the given id.
     * @param id The session ID without any cluster node extension
     */
    public void invalidateAll(String id);
    
    /**
     * @param request
     * @param created
     * @return
     */
    public String newSessionId(HttpServletRequest request,long created);
    
    public String getWorkerName();
    
    
    /* ------------------------------------------------------------ */
    /** Get a cluster ID from a node ID.
     * Strip node identifier from a located session ID.
     * @param nodeId
     * @return
     */
    public String getClusterId(String nodeId);
    
    /* ------------------------------------------------------------ */
    /** Get a node ID from a cluster ID and a request
     * @param clusterId The ID of the session
     * @param request The request that for the session (or null)
     * @return The session ID qualified with the node ID.
     */
    public String getNodeId(String clusterId,HttpServletRequest request);
    
}