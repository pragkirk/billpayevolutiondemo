// ========================================================================
// $Id: State.java,v 1.3 2004/05/09 20:30:47 gregwilkins Exp $
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
import java.util.Enumeration;
import java.util.Map;

//----------------------------------------
// The API around the isolated state encapsulated by an HttpSession -
// NOT quite the same as an HttpSession interface...
// It would be much cheaper to have set/removeAttribute return a
// boolean or void - but we HAVE TO HAVE the old binding to use in
// ValueUnbound events...
//----------------------------------------

/**
 * Implemented by objects wishing to be used to store the state from
 * an HttpSession.
 *
 * @author <a href="mailto:jules@mortbay.com">Jules Gosnell</a>
 * @version 1.0
 */
public interface State
{
    // invariant field accessors
    String getId() throws RemoteException;

    int getActualMaxInactiveInterval() throws RemoteException;

    long getCreationTime() throws RemoteException;

    // variant field accessors
    Map getAttributes() throws RemoteException;

    void setAttributes(Map attributes) throws RemoteException;

    long getLastAccessedTime() throws RemoteException;

    void setLastAccessedTime(long time) throws RemoteException;

    int getMaxInactiveInterval() throws RemoteException;

    void setMaxInactiveInterval(int interval) throws RemoteException;

    // compound fn-ality
    Object getAttribute(String name) throws RemoteException;

    Object setAttribute(String name, Object value, boolean returnValue)
            throws RemoteException;

    Object removeAttribute(String name, boolean returnValue)
            throws RemoteException;

    Enumeration getAttributeNameEnumeration() throws RemoteException;

    String[] getAttributeNameStringArray() throws RemoteException;

    boolean isValid() throws RemoteException;
}
