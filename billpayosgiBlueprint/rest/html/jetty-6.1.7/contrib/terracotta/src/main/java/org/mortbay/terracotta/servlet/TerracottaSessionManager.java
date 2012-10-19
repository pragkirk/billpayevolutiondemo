package org.mortbay.terracotta.servlet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.log.Log;
import org.mortbay.util.LazyList;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

/**
 * TerracottaSessionManager
 *
 * Each context has it's own TerracottaSessionManager. 
 *
 */
public class TerracottaSessionManager extends AbstractSessionManager
{
    ConcurrentHashMap _sessions;
    ConcurrentHashMap _sessionDataMap;
    private long _scavengePeriodMs=30000;
    private Thread _scavenger=null;
    private Timer _timer;
    private TimerTask _task;
    private static int __id;
    
    public TerracottaSessionManager ()
    {
        super();
    }
    
    public void setIdManager(SessionIdManager idManager)
    {
        super.setIdManager(idManager);
    }
 
    public void doStart()
    throws Exception
    {
        _sessions=new ConcurrentHashMap();
        _sessionDataMap=new ConcurrentHashMap();
        
        super.doStart();
        
        _timer=new Timer("TerracottaSessionScavenger-"+__id++, true);
        setScavengePeriodMs(_scavengePeriodMs);
    }
    
    
    public void doStop()
    throws Exception
    {
        super.doStop();
        
        // stop the scavenger
        Thread scavenger=_scavenger;
        _scavenger=null;
        if (scavenger!=null)
            scavenger.interrupt();
    }
    
 
    protected void addSession(AbstractSessionManager.Session session)
    {
        synchronized (this)
        {
            String id = getClusterId(session);
            _sessions.put(id,session);          
            _sessionDataMap.put(id, ((TerracottaSessionManager.Session)session).getSessionData());
            Log.debug("Added session with key="+id);
        }
    }


    protected void removeSession(String idInCluster)
    {
        synchronized (this)
        {
           Object o = _sessions.remove(idInCluster);
            Log.debug("Removed session id="+idInCluster+(o==null?"unsuccessfully":"successfully"));
            o = _sessionDataMap.remove(idInCluster);
            Log.debug("Removed session data id="+idInCluster+(o==null?"unsuccessfully":"successfully"));
        }
    }
    
    
    public void setScavengePeriodMs (long ms)
    {
        long old_period=_scavengePeriodMs;
        _scavengePeriodMs=ms;
        if (_timer!=null && (ms!=old_period || _task==null))
        {
            synchronized (this)
            {
                if (_task!=null)
                    _task.cancel();
                _task = new TimerTask()
                {
                    public void run()
                    {
                        scavenge();
                    }   
                };
                _timer.schedule(_task,_scavengePeriodMs,_scavengePeriodMs);
            }
        }
    }

    public long getScavengePeriodMs ()
    {
        return _scavengePeriodMs;
    }
    
    public AbstractSessionManager.Session getSession(String idInCluster)
    {
        synchronized (this)
        {   
            //find the matching SessionData in the distributed map      
            Log.debug("Checking distributed sessions for id="+idInCluster);
            SessionData sessionData = (SessionData)_sessionDataMap.get(idInCluster);
            if (sessionData == null)
                return null;
            
            Log.debug("Found distributed session "+idInCluster);
            Session session = null;
            //See if the session in already in my local map of sessions
            session = (Session)_sessions.get(idInCluster);
            if (session != null)
            {
                Log.debug("Found local session "+idInCluster);
            }
            else
            {
                //The distributed session has been requested on my node,
                //so move it into my local map
                session = new Session(sessionData);
                _sessions.put(sessionData.getId(),session); 
            }
            
            
            return session;
        }
    }


    public Map getSessionMap()
    {
        return Collections.unmodifiableMap(_sessions);
    }


    public int getSessions()
    {
        synchronized (this)
        {
            return _sessions.size();
        }
    }


    protected Session newSession(HttpServletRequest request)
    {           
        return new Session(request);
    }


    protected void invalidateSessions()
    {
        //Do nothing - we don't want to remove and
        //invalidate all the sessions because this
        //method is called from doStop(), and just
        //because this context is stopping does not
        //mean that we should remove the session from
        //any other nodes (remember the session map is
        //shared)
        
    }
    /* ------------------------------------------------------------ */
    /**
     * @param seconds
     */
    public void setMaxInactiveInterval(int seconds)
    {
        _dftMaxIdleSecs=seconds;
    }
  
    private void scavenge()
    {
        Thread thread=Thread.currentThread();
        ClassLoader old_loader=thread.getContextClassLoader();
        try
        {
            if (_loader!=null)
                thread.setContextClassLoader(_loader);

            long now=System.currentTimeMillis();

            Log.debug("Scavenging at "+now+", scavenge period="+_scavengePeriodMs);
            
            // Since Hashtable enumeration is not safe over deletes,
            // we build a list of stale sessions, then go back and invalidate
            // them
            Object stale=null;

            synchronized (this)
            {
                // For each session id
                for (Iterator i=_sessionDataMap.values().iterator(); i.hasNext();)
                {
                    SessionData sd = (SessionData)i.next();
                    long idleTime = sd.getMaxIdleMs();
                    
                    Log.debug("Session "+sd.getId()+" last accessed = "+sd.getLastAccessed()+ " idle time = "+idleTime);
                    if (idleTime>0 && sd.getLastAccessed()+idleTime<now)
                    {
                        stale=LazyList.add(stale,sd);
                        Log.debug("Session "+sd.getId()+" is stale");
                    }
                }
            }

            // Remove the stale sessions
            for (int i=LazyList.size(stale); i-->0;)
            {
                // check it has not been accessed in the meantime
                SessionData sessionData=(SessionData)LazyList.get(stale,i);
                long idleTime=sessionData.getMaxIdleMs();
                if (idleTime>0&& sessionData.getLastAccessed()+idleTime<System.currentTimeMillis())
                {
                    Session session = (TerracottaSessionManager.Session)getSession(sessionData._id);
                    if (session != null)
                    {
                        Log.debug("Removing stale session "+sessionData.getId());
                        session.lock();
                        session.invalidate();
                        session.unlock();

                        //update the statistics
                        int sessionCount=this._sessions.size();
                        if (sessionCount<this._minSessions)
                            this._minSessions=sessionCount;
                    }
                }
            }
        }
        finally
        {
            thread.setContextClassLoader(old_loader);
        }
    }
    
    class Session extends AbstractSessionManager.Session
    {
        /* ------------------------------------------------------------ */
        private static final long serialVersionUID=-2134521374206116367L;
        
        
        private SessionData _sessionData;
        private Lock _lock;


        /* ------------------------------------------------------------- */
        protected Session(HttpServletRequest request)
        {
            super(request);
            _sessionData = new SessionData(getClusterId());
            _sessionData.setMaxIdleMs(_dftMaxIdleSecs*1000);
            _lock = new Lock(getClusterId());
            lock();
        }
        
        protected Session (SessionData sd)
        {
            super(sd.getCreated(), sd.getId());
            _sessionData = sd;
            _values = sd.getAttributeMap(); 
            _lock = new Lock(sd.getId());  
           // _lock.lock();
        }
        
        public SessionData getSessionData ()
        {
            return _sessionData;
        }
       
        protected void cookieSet()
        {
            _sessionData.setCookieSet(_sessionData.getLastAccessed());
        }
        
        public long getLastAccessedTime()
        {
            if (!isValid())
                throw new IllegalStateException();
            
            return _sessionData.getLastAccessed();
        }    
        
        public long getCreationTime() throws IllegalStateException
        {
            if (!isValid())
                throw new IllegalStateException();
            return _sessionData.getCreated();
        }
        

        public void setMaxInactiveInterval(int secs)
        {
           _sessionData.setMaxIdleMs(secs*1000);
        }
        
        public String getIdWithinCluster ()
        {
            return super.getClusterId();
        }
        
        /* ------------------------------------------------------------ */
        protected Map newAttributeMap()
        {
            return _sessionData.newAttributes();
        }
        
        protected void access(long time)
        {
            lock();
            synchronized (this)
            {
                SessionData sessionData = (SessionData)_sessionDataMap.get(getClusterId());

                super.access(time);

                if (sessionData!=null)
                {
                    sessionData.setLastAccessed(time);
                }
            }
        }  
        
        protected void complete()
        {
            synchronized (this)
            {
                super.complete();
            }
            unlock();
        }

        protected void lock ()
        {
            _lock.lock();
        }
        
        protected void unlock ()
        {
            _lock.unlock();
        }
    }
    
    
    /**
     * SessionData
     *
     * The data related to a Session. It is this information which is
     * distributed to nodes via Terracotta.
     *
     */
    public static class SessionData
    {
        private String _id;
        private long _accessed;
        private long _maxIdleMs;
        private long _cookieSet;
        private long _created;
        
        
        private Map _attributes;
        
        public SessionData (String sessionId)
        {
            _id=sessionId; 
            _created=System.currentTimeMillis();
            _accessed = _created;
        }

        public synchronized String getId ()
        {
            return _id;
        }

        public synchronized long getCreated ()
        {
            return _created;
        }

        public synchronized void setMaxIdleMs (long ms)
        {
            _maxIdleMs = ms;
        }

        public synchronized long getMaxIdleMs()
        {
            return _maxIdleMs;
        }

        public synchronized void setLastAccessed (long ms)
        {
            _accessed = ms;
        }

        public synchronized long getLastAccessed()
        {
            return _accessed;
        }

        public void setCookieSet (long ms)
        {
            _cookieSet = ms;
        }

        public synchronized long getCookieSet ()
        {
            return _cookieSet;
        }

        public synchronized Map newAttributes ()
        {
            _attributes = new ConcurrentHashMap();
            return _attributes;
        }
        
        protected synchronized Map getAttributeMap ()
        {
            return _attributes;
        }
    }
    
    public class Lock 
    {
        private String _id;

        
        public Lock (String id)
        {
            this._id = id;
        }
        
        public  void lock ()
        {
            Log.debug("Locking id="+_id+"by thread="+Thread.currentThread().getName()); 
            ManagerUtil.beginLock(_id, Manager.LOCK_TYPE_WRITE);
        }
        
        public  void unlock ()
        {
            Log.debug("Unlocking id="+_id+" thread="+Thread.currentThread().getName());
            ManagerUtil.commitLock(_id);
        }
     
        public  boolean tryLock ()
        {
            return ManagerUtil.tryBeginLock(_id, Manager.LOCK_TYPE_WRITE);
        }
    }
}
