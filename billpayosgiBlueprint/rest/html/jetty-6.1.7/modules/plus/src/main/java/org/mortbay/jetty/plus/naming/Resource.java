// ========================================================================
// $Id: Resource.java 1540 2007-01-19 12:24:10Z janb $
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

package org.mortbay.jetty.plus.naming;


import javax.naming.NamingException;



/**
 * Resource
 *
 *
 */
public class Resource extends NamingEntry
{
  
    public static Resource getResource (int scopeType, String jndiName)
    throws NamingException
    {
        return (Resource)lookupNamingEntry (scopeType, Resource.class, jndiName);
    }
    
    
    
    
    /**
     * @param jndiName
     * @param objToBind
     */
    public Resource (String jndiName, Object objToBind)
    throws NamingException
    {
        super(jndiName, objToBind);
    }

}
