// ========================================================================
// $Id: Manager.java,v 1.6 2004/12/22 23:28:11 janb Exp $
// Copyright 2002-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.jboss.jetty.JBossWebAppContext;
import org.mortbay.jetty.HttpOnlyCookie;
import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;

//----------------------------------------

// TODO 
// This class needs a lot of work to make it run.
//-----

// we need a SnapshotInterceptor
// we need to package this into JBoss and Mort Bay spaces
// Cluster/CMR & JDO Stores should be finished/done
// a DebugInterceptor could be fun....
// Jetty should be user:jetty, role:webcontainer in order to use the session EJBs - wil probably need a SecurityInterceptor
// can I optimise return of objects from set/removeAttribute?
// CMPState should use local not remote interfaces
// FAQ entry should be finished
// Throttle/BufferInterceptor - could be written very like MigrationInterceptor
// StateAdaptor needs a name change
// We need some predefined containers
// tighten up access priviledges
// javadoc
//----------------------------------------

// we need to rethink the strategies for flushing the local cache into
// the distributed cache - specifically how they can be aggregated
// (or-ed as opposed to and-ed).

// the spec does not say (?) whether session attribute events should
// be received in the order that the changes took place - we can
// control this by placing the SynchronizationInterceptor before or
// after the BindingInterceptor

// we could use a TransactionInterceptor to ensure that compound
// operations on e.g. a CMPState are atomic - or we could explicitly
// code the use of transactions where needed (we should be able to
// make all multiple calls collapse into one call to server - look
// into this). Since HttpSessions have no transactional semantics,
// there is no way for the user to inform us of any requirements...

//----------------------------------------

public class Manager implements org.mortbay.jetty.SessionManager
{
    // ----------------------------------------
    protected WebAppContext _context;
    
    /**
     * Whether or not the session manager will use cookies
     * or URLs only.
     */
    protected boolean _usingCookies = true;

    public WebAppContext getContext()
    {
        return _context;
    }

    public void setContext(WebAppContext context)
    {
        _context = context;
    }

    // ----------------------------------------
    protected int _scavengerPeriod = 60; // every 1 min

    public void setScavengerPeriod(int period)
    {
        _scavengerPeriod = period;
    }

    public int getScavengerPeriod()
    {
        return _scavengerPeriod;
    }

    // ----------------------------------------
    protected StateInterceptor[] _interceptorStack = null;

    public StateInterceptor[] getInterceptorStack()
    {
        return _interceptorStack;
    }

    public void setInterceptorStack(StateInterceptor[] interceptorStack)
    {
        _interceptorStack = interceptorStack;
    }

    // ----------------------------------------
    protected IdGenerator _idGenerator = null;

    public IdGenerator getIdGenerator()
    {
        return _idGenerator;
    }

    public void setIdGenerator(IdGenerator idGenerator)
    {
        _idGenerator = idGenerator;
    }

    // ----------------------------------------
    protected int _maxInactiveInterval;

    public int getMaxInactiveInterval()
    {
        return _maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int seconds)
    {
        _maxInactiveInterval = seconds;
    }

    // ----------------------------------------
    protected Store _store = null;

    private boolean _crossContextSessionIDs = false;

    public Store getStore()
    {
        return _store;
    }

    public void setStore(Store store)
    {
        _store = store;

        if (_store != null)
            _store.setManager(this);
    }

    // ----------------------------------------

    public Object clone()
    {
        // Log.info("cloning Manager: "+this);
        Manager m = new Manager();

        // deep-copy Store attribute - each Manager gets it's own Store instance
        Store store = getStore();
        if (store != null)
            m.setStore((Store) store.clone());

        // deep-copy IdGenerator attribute - each Manager gets it's own
        // IdGenerator instance
        IdGenerator ig = getIdGenerator();
        if (ig != null)
            m.setIdGenerator((IdGenerator) ig.clone());

        // Container uses InterceptorStack as a prototype to clone a stack for
        // each new session...
        m.setInterceptorStack(getInterceptorStack());

        m.setMaxInactiveInterval(getMaxInactiveInterval());
        m.setScavengerPeriod(getScavengerPeriod());

        return m;
    }

    // ----------------------------------------

    final Map _sessions = new HashMap();

    public String getContextPath()
    {
        return _context.getContextPath();
    }

    // ----------------------------------------
    // LifeCycle API
    // ----------------------------------------

    boolean _started = false;

    Object _startedLock = new Object();

    Timer _scavenger;

    class Scavenger extends TimerTask
    {
        public void run()
        {
            try
            {
                scavenge();
            }
            catch (Throwable e)
            {
                Log.warn("could not scavenge local sessions", e);
            }
        }
    }

    public void start()
    {
        Log.debug("starting...");
        synchronized (_startedLock)
        {
            if (_started)
            {
                Log.warn("already started");
                return;
            }

            if (_store == null)
            {
                Log.warn("No Store. Falling back to a local session implementation - NO HTTPSESSION DISTRIBUTION");
                setStore(new LocalStore());
            }

            if (_idGenerator == null)
                _idGenerator = new DistributableIdGenerator();

            try
            {
                _store.start();
            }
            catch (Exception e)
            {
                Log.warn("Faulty Store. Falling back to a local session implementation - NO HTTPSESSION DISTRIBUTION", e);
                setStore(new LocalStore());
                try
                {
                    _store.start();
                }
                catch (Exception e2)
                {
                    Log.warn("could not start Store", e2);
                }
            }

            if (Log.isDebugEnabled())
                Log.debug("starting local scavenger thread...(period: " + getScavengerPeriod() + " secs)");
            long delay = getScavengerPeriod() * 1000;
            boolean isDaemon = true;
            _scavenger = new Timer(isDaemon);
            _scavenger.scheduleAtFixedRate(new Scavenger(), delay, delay);
            Log.debug("...local scavenger thread started");
            _started = true;
        }

        Log.debug("...started");
    }

    public boolean isStarted()
    {
        synchronized (_startedLock)
        {
            return _started;
        }
    }

    public void stop()
    {
        Log.debug("stopping...");

        synchronized (_startedLock)
        {
            if (!_started)
            {
                Log.warn("already stopped/not yet started");
                return;
            }

            // I guess we will have to ask the store for a list of sessions
            // to migrate... - TODO

            synchronized (_sessions)
            {
                List copy = new ArrayList(_sessions.values());
                for (Iterator i = copy.iterator(); i.hasNext();)
                    ((StateAdaptor) i.next()).migrate();

                _sessions.clear();
            }

            Log.debug("stopping local scavenger thread...");
            _scavenger.cancel();
            _scavenger = null;
            Log.debug("...local scavenger thread stopped");
            scavenge();

            _store.stop();
            _store.destroy();
            setStore(null);
            _started = false;
        }

        Log.debug("...stopped");
    }

    // ----------------------------------------
    // SessionManager API
    // ----------------------------------------

    protected SessionHandler _handler;

    public void setSessionHandler(SessionHandler handler)
    {
        this._handler = handler;
    }

    // ----------------------------------------
    // SessionManager API
    // ----------------------------------------

    public HttpSession getHttpSession(String id)
    {
        return findSession(id, true);
    }

    public boolean sessionExists(String id)
    {
        return findSession(id, false) != null;
    }

    public HttpSession newHttpSession(HttpServletRequest request) // TODO
    {
        return newSession(request);
    }

    // ----------------------------------------
    // this does not need locking as it is an int and access should be atomic...

    // ----------------------------------------
    // Listeners

    // These lists are only modified at webapp [un]deployment time, by a
    // single thread, so although read by multiple threads whilst the
    // Manager is running, need no synchronization...

    final List _sessionListeners = new ArrayList();

    final List _sessionAttributeListeners = new ArrayList();

    public void addEventListener(EventListener listener) throws IllegalArgumentException, IllegalStateException
    {
        synchronized (_startedLock)
        {
            if (isStarted())
                throw new IllegalStateException("EventListeners must be added before a Session Manager starts");

            boolean known = false;
            if (listener instanceof HttpSessionAttributeListener)
            {
                // Log.info("adding HttpSessionAttributeListener: "+listener);
                _sessionAttributeListeners.add(listener);
                known = true;
            }
            if (listener instanceof HttpSessionListener)
            {
                // Log.info("adding HttpSessionListener: "+listener);
                _sessionListeners.add(listener);
                known = true;
            }

            if (!known)
                throw new IllegalArgumentException("Unknown EventListener type " + listener);
        }
    }

    public void clearEventListeners()
    {
        _sessionAttributeListeners.clear();
        _sessionListeners.clear();
    }

    public void removeEventListener(EventListener listener) throws IllegalStateException
    {
        synchronized (_startedLock)
        {
            if (isStarted())
                throw new IllegalStateException("EventListeners may not be removed while a Session Manager is running");

            if (listener instanceof HttpSessionAttributeListener)
                _sessionAttributeListeners.remove(listener);
            if (listener instanceof HttpSessionListener)
                _sessionListeners.remove(listener);
        }
    }

    // ----------------------------------------
    // Implementation...
    // ----------------------------------------

    public ServletContext getServletContext()
    {
        return _context.getServletHandler().getServletContext();
    }

    public HttpSessionContext getSessionContext()
    {
        return null;
    }

    // --------------------
    // session lifecycle
    // --------------------

    // I need to think more about where the list of extant sessions is
    // held...

    // is it held by the State Factory/Type (static), which best knows
    // how to find it ? The trouble is we are not asking for just the
    // State but the whole container...

    // if the State was an EJB, the Manager could hold a HashMap of
    // id:State and the State would just be an EJB handle...

    // How does the ThrottleInterceptor fit into this. If it is holding
    // a local cache in front of a DistributedState, do we check them
    // both and compare timestamps, or flush() all ThrottleInterceptors
    // in the WebApp before we do the lookup (could be expensive...)

    // when finding a distributed session assume we are on nodeB
    // receiving a request for a session immediately after it has just
    // been created on NodeA. If we can't find it straight away, we need
    // to take into account how long it's flushing and distribution may
    // take, wait that long and then look again. This may hold up the
    // request, but without the session the request is not much good.

    // overload this to change the construction of the Container....

    protected HttpSession newContainer(String id, State state)
    {
        // put together the make-believe container and HttpSession state

        return Container.newContainer(this, id, state, getMaxInactiveInterval(), currentSecond(), getInterceptorStack());
    }

    protected Session newSession(HttpServletRequest request)
    {
        String id = null;
        HttpSession session = null;
        try
        {
            id = _store.allocateId(request);
            State state = _store.newState(id, getMaxInactiveInterval());
            session = newContainer(id, state);
        }
        catch (Exception e)
        {
            Log.debug("could not create HttpSession", e);
            return null; // BAD - TODO
        }

        if (Log.isDebugEnabled())
            Log.debug("remembering session - " + id);

        synchronized (_sessions)
        {
            _sessions.put(id, session);
        }

        notifySessionCreated(session);

        return (Session) session;
    }

    protected State destroyContainer(HttpSession session)
    {
        return Container.destroyContainer(session, getInterceptorStack());
    }

    protected void destroySession(HttpSession container)
    {
        String id = container.getId();
        if (Log.isDebugEnabled())
            Log.debug("forgetting session - " + id);
        Object tmp;
        synchronized (_sessions)
        {
            tmp = _sessions.remove(id);
        }
        container = (HttpSession) tmp;
        if (Log.isDebugEnabled())
            Log.debug("forgetting session - " + container);

        if (container == null)
        {
            Log.warn("session - " + id + " has already been destroyed");
            return;
        }

        // TODO remove all the attributes - generating correct events
        // check ordering on unbind and destroy notifications - The
        // problem is that we want these calls to go all the way through
        // the container - but not to the store - because that would be
        // too expensive and we can predict the final state...

        // we don't need to do this if we know that none of the attributes
        // are BindingListers AND there are no AttributeListeners
        // registered... - TODO

        // This will do for the moment...

        // LATER - TODO

        try
        {
            State state = ((StateAdaptor) container).getState();

            // filthy hack...
            // stop InvalidInterceptors - otherwise we can't clean up session...
            // - TODO
            {
                State s = state;
                StateInterceptor si = null;
                while (s instanceof StateInterceptor)
                {
                    si = (StateInterceptor) s;
                    s = si.getState(); // next interceptor
                    if (si instanceof ValidatingInterceptor)
                        si.stop();
                }
            }

            String[] names = state.getAttributeNameStringArray();
            for (int i = 0; i < names.length; i++)
                state.removeAttribute(names[i], false);

            // should just do this for attributes which are BindingListeners
            // - then just clear() the rest... - TODO
        }
        catch (RemoteException e)
        {
            Log.debug("could not raise events on session destruction - problem in distribution layer", e);
        }

        if (Log.isDebugEnabled())
            Log.debug("notifying session - " + id);
        notifySessionDestroyed(container);

        if (Log.isDebugEnabled())
            Log.debug("destroying container - " + id);
        State state = destroyContainer(container);

        try
        {
            if (state != null) // an interceptor may preempt us, if
            // it does not want this state
            // removed...
            {
                if (Log.isDebugEnabled())
                    Log.debug("removing state - " + id);
                _store.removeState(state);
            }
        }
        catch (Exception e)
        {
            Log.debug("could not remove session state", e);
        }
    }

    protected HttpSession findSession(String id, boolean create)
    {
        HttpSession container = null;

        try
        {
            // find the state
            State state = _store.loadState(id);

            // is it valid ?
            state = ((state != null) && state.isValid()) ? state : null; // expensive
            // ?

            // if so
            if (state != null)
            {

                // this looks slow - but to be 100% safe we need to make sure
                // that no-one can enter another container for the same id,
                // whilst we are thinking about it...

                // is there a container already available ?
                synchronized (_sessions)
                {
                    // do we have an existing container ?
                    container = (HttpSession) _sessions.get(id);

                    // if not...
                    if (container == null && create)
                    {
                        // make a new one...
                        container = newContainer(id, state);// we could lower
                        // contention by
                        // preconstructing
                        // containers... -
                        // TODO
                        _sessions.put(id, container);
                    }
                }
            }
        }
        catch (Exception ignore)
        {
            if (Log.isDebugEnabled())
                Log.debug("did not find distributed session: " + id);
        }

        return container;
    }

    // --------------------
    // session events
    // --------------------

    // should this all be delegated to the event raising interceptor....

    public Object notifyAttributeAdded(HttpSession session, String name, Object value)
    {
        int n = _sessionAttributeListeners.size();
        if (n > 0)
        {
            HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, name, value);

            for (int i = 0; i < n; i++)
                ((HttpSessionAttributeListener) _sessionAttributeListeners.get(i)).attributeAdded(event);

            event = null;
        }

        return value;
    }

    public Object notifyAttributeReplaced(HttpSession session, String name, Object value)
    {
        int n = _sessionAttributeListeners.size();
        if (n > 0)
        {
            HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, name, value);

            for (int i = 0; i < n; i++)
                ((HttpSessionAttributeListener) _sessionAttributeListeners.get(i)).attributeReplaced(event);

            event = null;
        }

        return value;
    }

    public Object notifyAttributeRemoved(HttpSession session, String name, Object value)
    {
        int n = _sessionAttributeListeners.size();
        if (n > 0)
        {
            HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, name, value);

            for (int i = 0; i < n; i++)
                ((HttpSessionAttributeListener) _sessionAttributeListeners.get(i)).attributeRemoved(event);

            event = null;
        }

        return value;
    }

    public void notifySessionCreated(HttpSession session)
    {
        int n = _sessionListeners.size();
        if (n > 0)
        {
            HttpSessionEvent event = new HttpSessionEvent(session);

            for (int i = 0; i < n; i++)
                ((HttpSessionListener) _sessionListeners.get(i)).sessionCreated(event);

            event = null;
        }
    }

    public void notifySessionDestroyed(HttpSession session)
    {
        int n = _sessionListeners.size();
        if (n > 0)
        {
            HttpSessionEvent event = new HttpSessionEvent(session);

            for (int i = 0; i < n; i++)
                ((HttpSessionListener) _sessionListeners.get(i)).sessionDestroyed(event);

            event = null;
        }
    }

    // this is to help sessions decide if they have timed out... It is
    // wrapped here so that if I decide that System.currentTimeMillis()
    // is too heavy, I can figure out a lighter way to return a rough
    // time to the sessions...

    public long currentSecond()
    {
        return System.currentTimeMillis();
    }

    // ensure that this code is run with the correct ContextClassLoader...
    protected void scavenge()
    {
        Log.debug("starting local scavenging...");
        try
        {
            // prevent session destruction (from scavenging)
            // from being replicated to other members in the cluster.
            // Let them scavenge locally instead.
            AbstractReplicatedStore.setReplicating(true);
            //
            // take a quick copy...
            Collection copy;
            synchronized (_sessions)
            {
                copy = new ArrayList(_sessions.values());
            }
            if (Log.isDebugEnabled())
                Log.debug(copy.size() + " local sessions");
            //
            // iterate over it at our leisure...
            int n = 0;
            for (Iterator i = copy.iterator(); i.hasNext();)
            {
                // all we have to do is check if a session isValid() to force it
                // to examine itself and invalidate() itself if necessary... -
                // because it has a local cache of the necessary details, it
                // will only go to the Stored State if it really thinks that it
                // is invalid...
                StateAdaptor sa = null;
                try
                {
                    sa = (StateAdaptor) i.next();
                    // the ValidationInterceptor should pick this up and throw an IllegalStateException
                    long lat = sa.getLastAccessedTime();
                }
                catch (IllegalStateException ignore)
                {
                    if (Log.isDebugEnabled())
                        Log.debug("scavenging local session " + sa.getId());
                    destroySession(sa);
                    i.remove();
                    ++n;
                }
            }
            if (Log.isDebugEnabled())
                Log.debug("scavenged " + n + " local sessions");
        }
        finally
        {
            AbstractReplicatedStore.setReplicating(false);
        }
        Log.debug("...finished local scavenging");
    }

    /** 
     * @return True if cross context session IDs are first considered for new
     * session IDs
     */
    public boolean getCrossContextSessionIDs()
    {
        return _crossContextSessionIDs;
    }

    /* ------------------------------------------------------------ */
    /** Set Cross Context sessions IDs
     * This option activates a mode where a requested session ID can be used to create a 
     * new session. This facilitates the sharing of session cookies when cross context
     * dispatches use sessions.   
     * 
     * @param useRequestedId True if cross context session ID are first considered for new
     * session IDs
     */
    public void setCrossContextSessionIDs(boolean useRequestedId)
    {
        _crossContextSessionIDs = useRequestedId;
    }

    public Cookie getSessionCookie(HttpSession session, String contextPath, boolean requestIsSecure)
    {
//        if (_handler.isUsingCookies())
//        { what's the jetty6 counterpart for isUsingCookies? - nik
            Cookie cookie = _handler.getSessionManager().getHttpOnly() ? new HttpOnlyCookie(SessionManager.__SessionCookieProperty, session.getId()) : new Cookie(
                SessionManager.__SessionCookieProperty, session.getId());
            String domain = getServletContext().getInitParameter(SessionManager.__SessionDomainProperty);
            String maxAge = getServletContext().getInitParameter(SessionManager.__MaxAgeProperty);
            String path = getServletContext().getInitParameter(SessionManager.__SessionPathProperty);
            if (path == null)
                path = getCrossContextSessionIDs() ? "/" : ((JBossWebAppContext)_context).getUniqueName();
            if (path == null || path.length() == 0)
                path = "/";

            if (domain != null)
                cookie.setDomain(domain);
            if (maxAge != null)
                cookie.setMaxAge(Integer.parseInt(maxAge));
            else
                cookie.setMaxAge(-1);

            cookie.setSecure(requestIsSecure && getSecureCookies());
            cookie.setPath(path);

            return cookie;
//        }
//        return null;
    }

    public boolean isStarting()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public SessionIdManager getMetaManager()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isFailed()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isRunning()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isStopped()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isStopping()
    {
        // TODO Auto-generated method stub
        return false;
    }


    public String getSessionPath()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSessionCookie()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSessionURL()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Cookie access(HttpSession arg0,boolean secure)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void complete(HttpSession arg0)
    {
        // TODO Auto-generated method stub
        
    }

    public boolean getHttpOnly()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public int getMaxCookieAge()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean getSecureCookies()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public String getSessionDomain()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSessionURLPrefix()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isValid(HttpSession session)
    {
        // TODO Auto-generated method stub
        return ((Session)session).isValid();
    }

    public void setIdManager(SessionIdManager arg0)
    {
        // TODO Auto-generated method stub
        
    }

    public void setMaxCookieAge(int arg0)
    {
        // TODO Auto-generated method stub
        
    }

    public void setSessionCookie(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    public void setSessionDomain(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    public void setSessionPath(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    public void setSessionURL(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    public interface Session extends HttpSession
    {
        /* ------------------------------------------------------------ */
        public boolean isValid();

        /* ------------------------------------------------------------ */
        public void access();
    }

    /** 
     * @see org.mortbay.jetty.SessionManager#getClusterId(javax.servlet.http.HttpSession)
     */
    public String getClusterId(HttpSession session)
    {
        // TODO check that this is correct. There does not
        // seem to be any difference between the id as the
        // node knows it and the id within the cluster, so 
        // we presume that the id is unique within the cluster.
        return session.getId();
    }

    /** 
     * @see org.mortbay.jetty.SessionManager#getIdManager()
     */
    public SessionIdManager getIdManager()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /** 
     * @see org.mortbay.jetty.SessionManager#getNodeId(javax.servlet.http.HttpSession)
     */
    public String getNodeId(HttpSession session)
    {
        // TODO check that this is correct. There does not
        // seem to be any difference between the id as the
        // node knows it and the id within the cluster, so 
        // we presume that the id is unique within the cluster.
        return session.getId();
    }

    /** 
     * @see org.mortbay.jetty.SessionManager#isUsingCookies()
     */
    public boolean isUsingCookies()
    {
        return _usingCookies;
    }
}
