// ========================================================================
// $Id: Store.java,v 1.3 2004/05/09 20:30:47 gregwilkins Exp $
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

import javax.servlet.http.HttpServletRequest;

//----------------------------------------
// a store provides 3 APIs :
// It's own start/stop methods. These will e.g. start/stop the session GC thread
// State LifeCyle methods - The Store encapsulates the LifeCycle of the State
// Session ID management methods - The session ID is a responsibility attribute of the store...
// Stores manage State, and will have to notify the Session Manager
// when they believe that this has timed-out.

public interface Store extends Cloneable
{
    Manager getManager();

    void setManager(Manager manager);

    // Store LifeCycle
    void start() throws Exception;

    void stop();

    void destroy(); // corresponds to ctor

    // Store accessors
    void setScavengerPeriod(int secs);

    void setScavengerExtraTime(int secs);

    void setActualMaxInactiveInterval(int secs);

    int getActualMaxInactiveInterval();

    boolean isDistributed();

    // ID allocation
    String allocateId(HttpServletRequest request) throws Exception;

    void deallocateId(String id) throws Exception;

    // State LifeCycle
    State newState(String id, int maxInactiveInterval) throws Exception;

    State loadState(String id) throws Exception;

    void storeState(State state) throws Exception;

    void removeState(State state) throws Exception;

    // Store misc
    void scavenge() throws Exception;

    void passivateSession(StateAdaptor sa);

    public Object clone();
}

