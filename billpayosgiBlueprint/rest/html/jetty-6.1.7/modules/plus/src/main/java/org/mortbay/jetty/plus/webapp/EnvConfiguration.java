// ========================================================================
// $Id: EnvConfiguration.java 1540 2007-01-19 12:24:10Z janb $
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.webapp;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.mortbay.jetty.plus.naming.EnvEntry;
import org.mortbay.jetty.plus.naming.NamingEntry;
import org.mortbay.jetty.plus.naming.Resource;
import org.mortbay.jetty.plus.naming.Transaction;
import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;

import org.mortbay.xml.XmlConfiguration;

/**
 * EnvConfiguration
 *
 *
 */
public class EnvConfiguration implements Configuration
{
    private WebAppContext webAppContext;
    private Context compCtx;
    private URL jettyEnvXmlUrl;

    protected void createEnvContext ()
    throws NamingException
    {
        Context context = new InitialContext();
        compCtx =  (Context)context.lookup ("java:comp");
        Context envCtx = compCtx.createSubcontext("env");
        if (Log.isDebugEnabled())
            Log.debug("Created java:comp/env for webapp "+getWebAppContext().getContextPath());
    }
    
    
    /** 
     * @see org.mortbay.jetty.webapp.Configuration#setWebAppContext(org.mortbay.jetty.webapp.WebAppContext)
     * @param context
     */
    public void setWebAppContext(WebAppContext context)
    {
        this.webAppContext = context;
    }

    public void setJettyEnvXml (URL url)
    {
        this.jettyEnvXmlUrl = url;
    }
    
    /** 
     * @see org.mortbay.jetty.webapp.Configuration#getWebAppContext()
     */
    public WebAppContext getWebAppContext()
    {
        return webAppContext;
    }

    /** 
     * @see org.mortbay.jetty.webapp.Configuration#configureClassLoader()
     * @throws Exception
     */
    public void configureClassLoader() throws Exception
    {
    }

    /** 
     * @see org.mortbay.jetty.webapp.Configuration#configureDefaults()
     * @throws Exception
     */
    public void configureDefaults() throws Exception
    {
    }

    /** 
     * @see org.mortbay.jetty.webapp.Configuration#configureWebApp()
     * @throws Exception
     */
    public void configureWebApp() throws Exception
    {
        //create a java:comp/env
        createEnvContext();
        
        //add java:comp/env entries for any globally defined EnvEntries
        bindGlobalEnvEntries();
        
        //set up java:comp/env as the Context in which to bind directly
        //the entries in jetty-env.xml
        NamingEntry.setScope(NamingEntry.SCOPE_LOCAL);
        
        //check to see if an explicit file has been set, if not,
        //look in WEB-INF/jetty-env.xml
        if (jettyEnvXmlUrl == null)
        {
            
            //look for a file called WEB-INF/jetty-env.xml
            //and process it if it exists
            org.mortbay.resource.Resource web_inf = getWebAppContext().getWebInf();
            if(web_inf!=null && web_inf.isDirectory())
            {
                org.mortbay.resource.Resource jettyEnv = web_inf.addPath("jetty-env.xml");
                if(jettyEnv.exists())
                {
                    jettyEnvXmlUrl = jettyEnv.getURL();
                }
            }
        }
        if (jettyEnvXmlUrl != null)
        {
            XmlConfiguration configuration = new XmlConfiguration(jettyEnvXmlUrl);
            configuration.configure(getWebAppContext());
        }

    }

    /** 
     * Remove all jndi setup
     * @see org.mortbay.jetty.webapp.Configuration#deconfigureWebApp()
     * @throws Exception
     */
    public void deconfigureWebApp() throws Exception
    {
        //get rid of any bindings only defined for the webapp
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(webAppContext.getClassLoader());
        NamingEntry.setScope(NamingEntry.SCOPE_LOCAL);
        unbindLocalNamingEntries();
        compCtx.destroySubcontext("env");
        NamingEntry.setScope(NamingEntry.SCOPE_GLOBAL);
        Thread.currentThread().setContextClassLoader(oldLoader);
    }
    
   
    /**
     * Bind all EnvEntries that have been globally declared.
     * @throws NamingException
     */
    public void bindGlobalEnvEntries ()
    throws NamingException
    {
        Log.debug("Finding global env entries");
        List  list = NamingEntry.lookupNamingEntries (NamingEntry.SCOPE_GLOBAL, EnvEntry.class);
        Iterator itor = list.iterator();
        
        Log.debug("Finding env entries: size="+list.size());
        while (itor.hasNext())
        {
            EnvEntry ee = (EnvEntry)itor.next();
            Log.debug("configuring env entry "+ee.getJndiName());
            ee.bindToENC();
        }
    }
    
  
    

    
    public void unbindLocalNamingEntries ()
    throws NamingException
    {
        List  list = NamingEntry.lookupNamingEntries(NamingEntry.SCOPE_LOCAL, EnvEntry.class);
        list.addAll(NamingEntry.lookupNamingEntries(NamingEntry.SCOPE_LOCAL, Resource.class));
        list.addAll(NamingEntry.lookupNamingEntries(NamingEntry.SCOPE_LOCAL, Transaction.class));
        
        Iterator itor = list.iterator();
        
        Log.debug("Finding all naming entries for webapp local naming context: size="+list.size());
        while (itor.hasNext())
        {
            NamingEntry ne = (NamingEntry)itor.next();
            Log.debug("Unbinding naming entry "+ne.getJndiName());
            ne.unbindENC();
            ne.release();
        }
    }
    
}
