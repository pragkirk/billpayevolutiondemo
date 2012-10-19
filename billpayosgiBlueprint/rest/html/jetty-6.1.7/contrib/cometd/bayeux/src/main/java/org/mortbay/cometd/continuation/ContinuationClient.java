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

package org.mortbay.cometd.continuation;

import org.mortbay.cometd.ClientImpl;
import org.mortbay.thread.Timeout;
import org.mortbay.util.ajax.Continuation;

/* ------------------------------------------------------------ */
/**
 * Extension of {@link ClientImpl} that uses {@link Continuation}s to
 * resume clients waiting for messages. Continuation clients are used for
 * remote clients and have removed if they are not accessed within
 * an idle timeout (@link {@link ContinuationBayeux#_clientTimer}).
 * 
 * @author gregw
 *
 */
public class ContinuationClient extends ClientImpl
{
    private long _accessed;
    public transient Timeout.Task _timeout; 
    private ContinuationBayeux _bayeux;
    private transient Continuation _continuation;

    /* ------------------------------------------------------------ */
    ContinuationClient(ContinuationBayeux bayeux)
    {
        super(bayeux,null,null);
        _bayeux=bayeux;

        _timeout=new Timeout.Task()
        {
            public void expire()
            {
                remove(true);
            }
            public String toString()
            {
                return "T-"+ContinuationClient.this.toString();
            }
        };
        if (!isLocal())
            _bayeux.startTimeout(_timeout);
    }


    /* ------------------------------------------------------------ */
    public void setContinuation(Continuation continuation)
    {
        synchronized (this)
        {
            if (_continuation!=null)
            {
                if(_continuation.isPending())
                    _continuation.resume(); 
            }
            _continuation=continuation;
            if (_continuation==null)
            {
                if (_timeout!=null && !_timeout.isScheduled())
                    _bayeux.startTimeout(_timeout);
            }
            else
            {
                _bayeux.cancelTimeout(_timeout);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    public Continuation getContinuation()
    {
        return _continuation;
    }

    /* ------------------------------------------------------------ */
    public void resume()
    {
        synchronized (this)
        {
            if (_continuation!=null)
            {
                _continuation.resume();
                if (_timeout!=null)
                    _bayeux.startTimeout(_timeout);
            }
            _continuation=null;
        }
    }

    /* ------------------------------------------------------------ */
    public boolean isLocal()
    {
        return false;
    }

    /* ------------------------------------------------------------ */
    public void access()
    {
        synchronized(this)
        {
            // distribute access time in cluster
            _accessed=System.currentTimeMillis();
            
            if (_timeout!=null && _timeout.isScheduled())
                _bayeux.startTimeout(_timeout);
        }
    }


    /* ------------------------------------------------------------ */
    public synchronized long lastAccessed()
    {
        return _accessed;
    }
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.cometd.ClientImpl#remove(boolean)
     */
    public void remove(boolean timeout) 
    {
        synchronized(this)
        {
            if (_timeout!=null)
                _bayeux.cancelTimeout(_timeout);
            _timeout=null;
        }
        super.remove(timeout);
    }

}