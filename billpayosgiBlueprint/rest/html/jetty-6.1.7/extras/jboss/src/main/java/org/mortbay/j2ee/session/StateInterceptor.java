//========================================================================
//$Id: StateInterceptor.java,v 1.4 2004/05/09 20:30:47 gregwilkins Exp $
//Copyright 2002-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpSession;

//----------------------------------------
/**
 * Superlass for StateInterceptors - objects which
 * wrap-n-delegate/decorate a State instance. A stack of
 * StateInterceptors form a StateContainer.
 *
 * @author <a href="mailto:jules@mortbay.com">Jules Gosnell</a>
 * @version 1.0
 */
public class StateInterceptor implements State, Cloneable
{
    //   protected final ThreadLocal _state  =new ThreadLocal();
    //   protected State       getState   ()                   {return (State)_state.get();}
    //   protected void        setState(State state)           {_state.set(state);}
    //
    private final static ThreadLocal _manager = new ThreadLocal();

    protected Manager getManager()
    {
        return (Manager) _manager.get();
    }

    protected void setManager(Manager manager)
    {
        _manager.set(manager);
    }

    private final static ThreadLocal _session = new ThreadLocal();

    protected HttpSession getSession()
    {
        return (HttpSession) _session.get();
    }

    protected void setSession(HttpSession session)
    {
        _session.set(session);
    }

    // management of this attribute needs to move into the container...
    private State _state;

    protected State getState()
    {
        return _state;
    }

    protected void setState(State state)
    {
        _state = state;
    }

    //   protected HttpSession _session;
    //   protected HttpSession getSession ()                   {return _session;}
    //   protected void        setSession(HttpSession session) {_session=session;}

    //----------------------------------------
    // 'StateInterceptor' API
    //----------------------------------------

    // lifecycle
    public void start()
    {
    }

    public void stop()
    {
    }

    // misc
    public String toString()
    {
        return "<" + getClass() + "->" + getState() + ">";
    }

    //----------------------------------------
    // wrapped-n-delegated-to 'State' API
    //----------------------------------------
    // invariant field accessors
    public String getId() throws RemoteException
    {
        return getState().getId();
    }

    public int getActualMaxInactiveInterval() throws RemoteException
    {
        return getState().getActualMaxInactiveInterval();
    }

    public long getCreationTime() throws RemoteException
    {
        return getState().getCreationTime();
    }

    // variant field accessors
    public Map getAttributes() throws RemoteException
    {
        return getState().getAttributes();
    }

    public void setAttributes(Map attributes) throws RemoteException
    {
        getState().setAttributes(attributes);
    }

    public long getLastAccessedTime() throws RemoteException
    {
        return getState().getLastAccessedTime();
    }

    public void setLastAccessedTime(long time) throws RemoteException
    {
        getState().setLastAccessedTime(time);
    }

    public int getMaxInactiveInterval() throws RemoteException
    {
        return getState().getMaxInactiveInterval();
    }

    public void setMaxInactiveInterval(int interval) throws RemoteException
    {
        getState().setMaxInactiveInterval(interval);
    }

    // compound fn-ality
    public Object getAttribute(String name) throws RemoteException
    {
        return getState().getAttribute(name);
    }

    public Object setAttribute(String name, Object value, boolean returnValue)
            throws RemoteException
    {
        return getState().setAttribute(name, value, returnValue);
    }

    public Object removeAttribute(String name, boolean returnValue)
            throws RemoteException
    {
        return getState().removeAttribute(name, returnValue);
    }

    public Enumeration getAttributeNameEnumeration() throws RemoteException
    {
        return getState().getAttributeNameEnumeration();
    }

    public String[] getAttributeNameStringArray() throws RemoteException
    {
        return getState().getAttributeNameStringArray();
    }

    public boolean isValid() throws RemoteException
    {
        return getState().isValid();
    }

    public Object clone()
    {
        Object tmp = null;
        try
        {
            tmp = getClass().newInstance();
        }
        catch (Exception e)
        {
            //	_log.error("could not clone "+getClass().getName(),e); - TODO
        }

        return tmp;
    }
}
