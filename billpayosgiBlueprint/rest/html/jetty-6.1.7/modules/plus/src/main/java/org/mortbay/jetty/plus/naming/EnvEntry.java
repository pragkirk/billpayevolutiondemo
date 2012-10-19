// ========================================================================
// $Id: EnvEntry.java 1540 2007-01-19 12:24:10Z janb $
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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.mortbay.log.Log;
import org.mortbay.naming.NamingUtil;


/**
 * EnvEntry
 *
 *
 */
public class EnvEntry extends NamingEntry
{
    private boolean overrideWebXml;
    
    public static EnvEntry getEnvEntry (int scopeType, String jndiName)
    throws NamingException
    {
       return (EnvEntry)lookupNamingEntry(scopeType, EnvEntry.class, jndiName);
    }
    
    
    
    /**
     * @param name
     * @param value
     * @throws NamingException
     */
    public static void bindToENC (String name, String overrideName, Object value)
    throws NamingException
    {       
        if (name==null||name.trim().equals(""))
            throw new NamingException("No name for EnvEntry");
        if (overrideName==null||overrideName.trim().equals(""))
            overrideName = name;
        
        //if a locally scoped EnvEntry is present (which takes precedence over globally scoped EnvEntry of the same name)
        //then it should be bound to the ENC
        EnvEntry envEntry = EnvEntry.getEnvEntry(NamingEntry.SCOPE_LOCAL, name);
        if ((envEntry != null) && envEntry.isOverrideWebXml())
        {
            if (!overrideName.equals(name))
                envEntry.bindToENC(overrideName);
            else
                envEntry.bindToENC();
            return;
        }

        //otherwise, we either have no locally scoped EnvEntry, or it was not
        //supposed to override what is in web.xml, so check to see if we have
        //a global scoped EnvEntry that should override web.xml
        envEntry = EnvEntry.getEnvEntry(NamingEntry.SCOPE_GLOBAL, name);
        if ((envEntry != null) && envEntry.isOverrideWebXml())
        {
            if(!overrideName.equals(name))
                envEntry.bindToENC(overrideName);
            else
                envEntry.bindToENC();
            return;
        }
        
        //No EnvEntrys of this name, so we use the value from web.xml
        InitialContext ic = new InitialContext();
        Context envCtx = (Context)ic.lookup("java:comp/env");
        NamingUtil.bind(envCtx, name, value);
        Log.debug("Bound java:comp/env/"+name+"="+value);  
        
    }
    
    
    public EnvEntry (String jndiName, Object objToBind)
    throws NamingException
    {
        this(jndiName, objToBind, false);
    }
    
    public EnvEntry (String jndiName, Object objToBind, boolean overrideWebXml)
    throws NamingException
    {
        super(jndiName, objToBind);
        this.overrideWebXml = overrideWebXml;
    }
    
    
    public boolean isOverrideWebXml ()
    {
        return this.overrideWebXml;
    }
    
    /** Bind the object wrapped in this EnvEntry into java:comp/env.
     * If, however, it is set to NOT override the web.xml entry,
     * then don't bind it. This method works in conjunction with
     * org.mortbay.jetty.plus.webapp.Configuration.bindEnvEntry().
     * TODO clean this up
     * @see org.mortbay.jetty.plus.naming.NamingEntry#bindToENC()
     * @throws NamingException
     */
    public void bindToENC ()
    throws NamingException
    {
        super.bindToENC();
    }
}
