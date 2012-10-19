// ========================================================================
// $Id: AroundInterceptor.java,v 1.4 2004/05/09 20:30:47 gregwilkins Exp $
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

import java.util.ArrayList;
import java.util.ListIterator;

import javax.servlet.http.HttpSession;

import org.jboss.logging.Logger;

/**
 * Container
 * 
 * Container is Object at front of stack at beginning of call, sets up
 * threadlocals Interceptors divided into Stateful/less Interceptors
 * Interceptors implement cloneable
 */
public class Container extends ArrayList implements Cloneable
{
    protected static final Logger _log = Logger.getLogger(Container.class);

    // this will come into service when I figure out how to remove the
    // next interceptor from each interceptor's state...

    // public Object
    // clone()
    // {
    // Container c=new Container();
    //
    // for (Iterator i=iterator(); i.hasNext();)
    // c.add(((StateInterceptor)i.next()).clone());
    //
    // return c;
    // }

    public Object clone()
    {
        Container c = new Container();

        try
        {
            State state = null;

            for (ListIterator i = listIterator(size()); i.hasPrevious();)
            {
                State lastState = state;
                StateInterceptor si = (StateInterceptor) i.previous();
                si = (StateInterceptor) si.getClass().newInstance();
                si.setState(lastState);
                state = si;
                c.add(0, state);
            }
        }
        catch (Exception e)
        {
            _log.error("could not clone Container", e);
        }

        return c;
    }

    // newContainer(this, id, state, getMaxInactiveInterval(), currentSecond()

    public static HttpSession newContainer(Manager manager, String id,
            State state, int maxInactiveInterval, long currentSecond,
            StateInterceptor[] interceptors)
    {
        // put together the make-believe container and HttpSession state

        StateAdaptor adp = new StateAdaptor(id, manager, maxInactiveInterval,
                currentSecond);

        State last = state;
        try
        {
            Class[] ctorParams = {};
            // for (int i=interceptors.length; i>0; i--)
            // {
            // StateInterceptor si=interceptors[i-1];
            // // if (_log.isDebugEnabled()) _log.debug("adding interceptor
            // instance: "+name);
            // StateInterceptor interceptor=(StateInterceptor)si.clone();
            // si.setManager(manager); // overkill - but safe
            // si.setSession(adp); // overkill - but safe
            // interceptor.setState(last); // this is also passed into ctor -
            // make up your mind - TODO
            // interceptor.start();
            // last=interceptor;
            // }
        }
        catch (Exception e)
        {
            _log.error("could not build distributed HttpSession container", e);
        }

        adp.setState(last);

        return adp;
    }

    public static State destroyContainer(HttpSession session,
            StateInterceptor[] interceptors)
    {
        // dissasemble the container here to aid GC

        StateAdaptor sa = (StateAdaptor) session;
        State last = sa.getState();
        sa.setState(null);

        for (int i = interceptors.length; i > 0; i--)
        {
            StateInterceptor si = (StateInterceptor) last;
            si.stop();
            State s = si.getState();
            si.setState(null);
            last = s;
        }

        return last;
    }
}
