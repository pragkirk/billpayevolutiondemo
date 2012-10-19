//========================================================================
//$Id: Timeout.java,v 1.3 2005/11/11 22:55:41 gregwilkins Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.thread;

import org.mortbay.log.Log;


/* ------------------------------------------------------------ */
/** Timeout queue.
 * This class implements a timeout queue for timers that are at least as likely to be cancelled as they are to expire.
 * Unlike the util timeout class, the duration of the timouts is shared by all scheduled tasks and if the duration 
 * is changed, this affects all scheduled tasks.
 * <p>
 * The nested class Task should be extended by users of this class to obtain call back notification of 
 * expiries. 
 * <p>
 * This class is not synchronized and the caller must protect against multiple thread access.
 * 
 * @author gregw
 *
 */
public class Timeout
{
    
    private long _duration;
    private long _now=System.currentTimeMillis();
    private Task _head=new Task();
    
    public Timeout()
    {
        _head._timeout=this;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the duration.
     */
    public long getDuration()
    {
        return _duration;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param duration The duration to set.
     */
    public void setDuration(long duration)
    {
        _duration = duration;
    }

    /* ------------------------------------------------------------ */
    public void setNow()
    {
        _now=System.currentTimeMillis();
    }
    
    /* ------------------------------------------------------------ */
    public long getNow()
    {
        return _now;
    }

    /* ------------------------------------------------------------ */
    public void setNow(long now)
    {
        _now=now;
    }

    /* ------------------------------------------------------------ */
    public Task expired()
    {
        long _expiry = _now-_duration;
        
        if (_head._next!=_head)
        {
            Task task = _head._next;
            if (task._timestamp>_expiry)
                return null;
            
            task.unlink();
            synchronized (task)
            {
                task._expired=true;
            }
            return task;
        }
        return null;
    }

    /* ------------------------------------------------------------ */
    public void tick()
    {
        long _expiry = _now-_duration;
        
        while (_head._next!=_head)
        {
            Task task = _head._next;
            if (task._timestamp>_expiry)
                break;
            
            task.unlink();
            try
            {
                task.doExpire();
            }
            catch(Throwable th)
            {
                Log.warn(Log.EXCEPTION,th);
            }
        }
    }

    /* ------------------------------------------------------------ */
    public void schedule(Task task)
    {
        schedule(task,0L);
    }
    
    /* ------------------------------------------------------------ */
    public void schedule(Task task,long delay)
    {
        if (task._timestamp!=0)
        {
            task.unlink();
            task._timestamp=0;
        }
        task._expired=false;
        task._delay=delay;
        task._timestamp = _now+delay;
        
        Task last=_head._prev;
        while (last!=_head)
        {
            if (last._timestamp <= task._timestamp)
                break;
            last=last._prev;
        }
        last.setNext(task);
    }


    /* ------------------------------------------------------------ */
    public void cancelAll()
    {
        _head._next=_head._prev=_head;
    }

    /* ------------------------------------------------------------ */
    public boolean isEmpty()
    {
        return _head._next==_head;
    }

    /* ------------------------------------------------------------ */
    public long getTimeToNext()
    {
        if (_head._next==_head)
            return -1;
        long to_next = _duration+_head._next._timestamp-_now;
        return to_next<0?0:to_next;
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(super.toString());
        
        Task task = _head._next;
        while (task!=_head)
        {
            buf.append("-->");
            buf.append(task);
            task=task._next;
        }
        
        return buf.toString();
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** Task.
     * The base class for scheduled timeouts.  This class should be
     * extended to implement the expire() method, which is called if the
     * timeout expires.
     * 
     * @author gregw
     *
     */
    public static class Task
    {
        Task _next;
        Task _prev;
        Timeout _timeout;
        long _delay;
        long _timestamp=0;
        boolean _expired=false;

        public Task()
        {
            _next=_prev=this;
        }

        public long getTimestamp()
        {
            return _timestamp;
        }
        
        public long getAge()
        {
            Timeout t = _timeout;
            if (t!=null && t._now!=0 && _timestamp!=0)
                return t._now-_timestamp;
            return 0;
        }
        
        public void unlink()
        {
            _next._prev=_prev;
            _prev._next=_next;
            _next=_prev=this;
            _timeout=null;
            _expired=false;
        }

        public void setNext(Task task)
        {
            if (_timeout==null || 
                task._timeout!=null && task._timeout!=_timeout ||    
                task._next!=task)
                throw new IllegalStateException();
            Task next_next = _next;
            _next._prev=task;
            _next=task;
            _next._next=next_next;
            _next._prev=this;   
            _next._timeout=_timeout;
        }
        
        /* ------------------------------------------------------------ */
        /** Schedule the task on the given timeout.
         * The task exiry will be called after the timeout duration.
         * @param timer
         */
        public void schedule(Timeout timer)
        {
            unlink();
            timer.schedule(this);
        }
        
        /* ------------------------------------------------------------ */
        /** Schedule the task on the given timeout.
         * The task exiry will be called after the timeout duration.
         * @param timer
         */
        public void schedule(Timeout timer, long delay)
        {
            unlink();
            timer.schedule(this,delay);
        }
        
        /* ------------------------------------------------------------ */
        /** Reschedule the task on the current timeout.
         * The task timeout is rescheduled as if it had been canceled and
         * scheduled on the current timeout.
         */
        public void reschedule()
        {
            Timeout timer = _timeout;
            unlink();
            timer.schedule(this,_delay);
        }
        
        /* ------------------------------------------------------------ */
        /** Cancel the task.
         * Remove the task from the timeout.
         */
        public void cancel()
        {
            _timestamp=0;
            unlink();
        }
        
        /* ------------------------------------------------------------ */
        public boolean isExpired() { return _expired; }

        /* ------------------------------------------------------------ */
	public boolean isScheduled() { return _next!=this; }

        
        /* ------------------------------------------------------------ */
        /** Expire task.
         * This method is called when the timeout expires.
         * 
         */
        public void expire(){}
        
        private void doExpire()
        {
            synchronized (this)
            {
                _expired=true;
                expire();
            }
        }
    }

}
