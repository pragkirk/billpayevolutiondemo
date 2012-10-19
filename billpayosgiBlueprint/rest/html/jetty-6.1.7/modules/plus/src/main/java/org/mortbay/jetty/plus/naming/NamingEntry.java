// ========================================================================
// $Id: NamingEntry.java 1667 2007-03-16 08:29:46Z janb $
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

import java.util.ArrayList;
import java.util.List;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.mortbay.log.Log;
import org.mortbay.naming.NamingUtil;



/**
 * NamingEntry
 *
 * Base class for all jndi related entities. Instances of
 * subclasses of this class are declared in jetty.xml or in a 
 * webapp's WEB-INF/jetty-env.xml file.
 *
 * NOTE: that all NamingEntries will be bound in a single namespace.
 *  The "global" level is just in the top level context. The "local"
 *  level is a context specific to a webapp.
 */
public abstract class NamingEntry
{
    public static final int SCOPE_GLOBAL = 0;
    public static final int SCOPE_LOCAL = 1;
    protected String jndiName;  //the name representing the object associated with the NamingEntry
    protected Object objectToBind; //the object associated with the NamingEntry
    protected String absoluteObjectNameString; //the absolute name of the object
    protected String namingEntryNameString; //the name of the NamingEntry relative to the context it is stored in
    protected String objectNameString; //the name of the object relative to the context it is stored in
    protected Context context; //the context in which both Naming Entry and object are saved
    protected boolean isGlobal;
    protected static ThreadLocal scope = new ThreadLocal();
    
    public static void setScope (int scopeType)
    {
        scope.set(new Integer(scopeType));
    }
    
    public static int getScope ()
    {
        Integer val = (Integer)scope.get();
        return (val == null?SCOPE_GLOBAL:val.intValue());
    }
    
    
    
    /**
     * Bind a NamingEntry into JNDI.
     * 
     * Locally scoped entries take precedence over globally scoped ones to
     * allow webapps to override.
     * 
     * @param name the name of the NamingEntry from the runtime environment
     * @param overrideName the name it should be bound as into java:comp/env
     * @param namingEntryType
     * @throws NamingException
     */
    public static void bindToENC (String name, String overrideName, Class namingEntryType)
    throws NamingException
    {  
        if (name==null||name.trim().equals(""))
            throw new NamingException ("No name for NamingEntry");
        if (overrideName==null||overrideName.trim().equals(""))
            overrideName=name;
        
        
        //locally scoped entries take precedence over globally scoped entries of the same name
        NamingEntry entry = lookupNamingEntry (NamingEntry.SCOPE_LOCAL, namingEntryType, name);
        if (entry!=null)
        {
            if (!overrideName.equals(name))
                entry.bindToENC(overrideName);
            else
                entry.bindToENC();
        }
        else
        {
            entry = lookupNamingEntry (NamingEntry.SCOPE_GLOBAL, namingEntryType, name);
            if (entry != null) 
            {
                if (!overrideName.equals(name))
                    entry.bindToENC(overrideName);
                else
                    entry.bindToENC();
            }
            else
            {
                //last ditch effort, check if something has been locally bound in java:comp/env
                try
                {
                    InitialContext ic = new InitialContext();
                    Context envContext = (Context)ic.lookup("java:comp/env");
                    envContext.lookup(name);
                    
                    if (!overrideName.equals(name))
                        NamingUtil.bind(envContext, overrideName, new LinkRef("."+name));
                        
                }
                catch (NamingException e)
                {
                    throw new NameNotFoundException("No resource to bind matching name="+name);
                }
            }   
        }
    }

    
    
    
    /**
     * Check to see if a NamingEntry exists in the given 
     * scope (local or global).
     * 
     * @param scopeType local or global
     * @param namingEntryType the type of the  NamingEntry
     * @param jndiName the name in jndi
     * @return
     */
    public static boolean exists (int scopeType, Class namingEntryType, String jndiName)
    {        
        switch (scopeType)
        {
            case SCOPE_GLOBAL: 
            {
                try
                {
                    return ((NamingEntry)lookupNamingEntry (new InitialContext(), namingEntryType, jndiName) != null);
                }
                catch (NameNotFoundException e)
                {
                    return false;
                } 
                catch (NamingException e)
                {
                    Log.warn(e);
                    return false;
                }
            }
            case SCOPE_LOCAL:
            {
                try
                {
                    InitialContext ic = new InitialContext();
                    return ((NamingEntry)lookupNamingEntry((Context)ic.lookup("java:comp/env"), namingEntryType, jndiName) != null);
                }
                catch (NameNotFoundException e)
                {
                    return false;
                }
                catch (NamingException e)
                {
                    Log.warn(e);
                    return false;
                }
            }
            default:
            {
               return false;
            }
        }
    }
  
    
    
    /**
     * Find a NamingEntry of the given scope.
     * 
     * @param scopeType local or global
     * @param clazz the type of the value stored by the NamingEntry
     * @param jndiName the name in jndi
     * @return
     * @throws NamingException
     */
    public static NamingEntry lookupNamingEntry (int scopeType, Class clazz, String jndiName)
    throws NamingException
    {
        NamingEntry namingEntry = null;
        
        switch (scopeType)
        {
            case SCOPE_GLOBAL: 
            {
                try
                {
                    namingEntry  = (NamingEntry)lookupNamingEntry (new InitialContext(), clazz, jndiName);
                }
                catch (NameNotFoundException e)
                {
                    namingEntry = null;
                }
                break;
            }
            case SCOPE_LOCAL:
            {
                try
                {
                    InitialContext ic = new InitialContext();
                    namingEntry = (NamingEntry)lookupNamingEntry((Context)ic.lookup("java:comp/env"), clazz, jndiName);
                }
                catch (NameNotFoundException e)
                {
                    namingEntry = null;
                }
                break;
            }
            default:
            {
                Log.debug("No scope to lookup name: "+jndiName);
            }
        }
        return namingEntry;
    }
    
    
   
    
 
    
    
    /** 
     * Get all NameEntries of a certain type in either the local or global
     * namespace.
     * 
     * @param scopeType local or global
     * @param clazz the type of the entry
     * @return
     * @throws NamingException
     */
    public static List lookupNamingEntries (int scopeType, Class clazz)
    throws NamingException
    {
        ArrayList list = new ArrayList();
        switch (scopeType)
        {
            case SCOPE_GLOBAL:
            {
                lookupNamingEntries(list, new InitialContext(), clazz);
                break;
            }
            case SCOPE_LOCAL:
            {
                //WARNING: you can only look up local scope if you are indeed in the scope
                InitialContext ic = new InitialContext();                   
                lookupNamingEntries(list, (Context)ic.lookup("java:comp/env"), clazz);

                break;
            }
        }
        return list;
    }
    
    
    private static List lookupNamingEntries (List list, Context context, Class clazz)
    throws NamingException
    {
        try
        {
            String name = (clazz==null?"": clazz.getName());
            NamingEnumeration nenum = context.listBindings(name);
            while (nenum.hasMoreElements())
            {
                Binding binding = (Binding)nenum.next();
                if (binding.getObject() instanceof Context)
                {
                    lookupNamingEntries (list, (Context)binding.getObject(), null);
                } 
                else               
                  list.add(binding.getObject());
            }
        }
        catch (NameNotFoundException e)
        {
            Log.debug("No entries of type "+clazz.getName()+" in context="+context);
        }
        
        return list;
    }
  
    
    /**
     * Find a NamingEntry.
     * @param context the context to search
     * @param clazz the type of the entry (ie subclass of this class)
     * @param jndiName the name of the class instance
     * @return
     * @throws NamingException
     */
    private static Object lookupNamingEntry (Context context, Class clazz, String jndiName)
    throws NamingException
    {
        NameParser parser = context.getNameParser("");       
        Name name = parser.parse("");
        name.add(clazz.getName());
        name.addAll(parser.parse(jndiName));
        return context.lookup(name);
    }
    
    
    
    /** 
     * Create a NamingEntry. 
     * A NamingEntry is a name associated with a value which can later
     * be looked up in JNDI by a webapp.
     * 
     * We create the NamingEntry and put it into JNDI where it can
     * be linked to the webapp's env-entry, resource-ref etc entries.
     * 
     * @param jndiName the name of the object which will eventually be in java:comp/env
     * @param object the object to be bound
     * @throws NamingException
     */
    public NamingEntry (String jndiName, Object object)
    throws NamingException
    {
        this.jndiName = jndiName;
        this.objectToBind = object;
        
        //if a threadlocal is set indicating we are inside a
        //webapp, then save naming entries to the webapp's
        //local context instead of the global context
        switch (getScope())
        {
            case SCOPE_GLOBAL: 
            {          
                isGlobal = true;
                break;
            }
            case SCOPE_LOCAL:
            {               
                isGlobal = false;
                break;
            }
        }
        save(); 
    }

    
    
    /**
     * Add a java:comp/env binding for the object represented by
     * this NamingEntry
     * @throws NamingException
     */
    public void bindToENC ()
    throws NamingException
    {
        if (isLocal())
        {
            //don't bind local scope naming entries as they are already bound to java:comp/env
        }
        else if (isGlobal())
        {
            InitialContext ic = new InitialContext();
            Context env = (Context)ic.lookup("java:comp/env");
            Log.debug("Binding java:comp/env/"+getJndiName()+" to "+absoluteObjectNameString);
            NamingUtil.bind(env, getJndiName(), new LinkRef(absoluteObjectNameString));
        }
    }
    
    
    /**
     * Add a java:comp/env binding for the object represented by this NamingEntry,
     * but bind it as a different name to the one supplied
     * @throws NamingException
     */
    public void bindToENC(String overrideName)
    throws NamingException
    {
        InitialContext ic = new InitialContext();
        Context env = (Context)ic.lookup("java:comp/env");
        Log.debug("Binding java:comp/env/"+overrideName+" to "+absoluteObjectNameString);
        NamingUtil.bind(env, overrideName, new LinkRef(absoluteObjectNameString));
    }
    
    /**
     * Unbind this NamingEntry from a java:comp/env
     */
    public void unbindENC ()
    {
        try
        {
            InitialContext ic = new InitialContext();
            Context env = (Context)ic.lookup("java:comp/env");
            Log.debug("Unbinding java:comp/env/"+getJndiName());
            env.unbind(getJndiName());
        }
        catch (NamingException e)
        {
            Log.warn(e);
        }
    }
    
    /**
     * Unbind this NamingEntry entirely
     */
    public void release ()
    {
        try
        {
            context.unbind(objectNameString);
            context.unbind(namingEntryNameString);
            this.absoluteObjectNameString=null;
            this.jndiName=null;
            this.namingEntryNameString=null;
            this.objectNameString=null;
            this.objectToBind=null;
            this.context=null;
        }
        catch (NamingException e)
        {
            Log.warn(e);
        }
    }
    
    /**
     * Get the unique name of the object
     * @return
     */
    public String getJndiName ()
    {
        return this.jndiName;
    }
    
    /**
     * Get the object that is to be bound
     * @return
     */
    public Object getObjectToBind()
    throws NamingException
    {   
        return this.objectToBind;
    }
    
    /**
     * Check if this naming entry was global or locally scoped to a webapp
     * @return true if naming entry was bound at global scope, false otherwise
     */
    public boolean isGlobal ()
    {
        return this.isGlobal;
    }
    
    public boolean isLocal()
    {
        return !this.isGlobal;
    }
    
 
    
    
    /**
     * Save the NamingEntry for later use.
     * 
     * Saving is done by binding the NamingEntry
     * itself, and the value it represents into
     * JNDI. In this way, we can link to the
     * value it represents later, but also
     * still retrieve the NamingEntry itself too.
     * 
     * @throws NamingException
     */
    private void save ()
    throws NamingException
    {
        InitialContext icontext = new InitialContext();
        if (isGlobal())
            context = icontext;
        else
            context = (Context)icontext.lookup("java:comp/env");
        
        NameParser parser = context.getNameParser("");
        Name contextName = parser.parse(context.getNameInNamespace());
        
        //save the NamingEntry itself so it can be accessed later       
        Name name = parser.parse("");
        name.add(getClass().getName());
        name.add(getJndiName());
        namingEntryNameString = name.toString();
        NamingUtil.bind(context, namingEntryNameString, this);
        Log.debug("Bound "+(isGlobal()?"":"java:")+name.addAll(0,contextName));
        
        //put the Object into JNDI so it can be linked to later  
        Name objectName = parser.parse(getJndiName());
        objectNameString = objectName.toString();
        NamingUtil.bind(context, objectNameString, getObjectToBind());       
       
        //remember the full name of the bound object so that it can be used in
        //link references later
        Name fullName = objectName.addAll(0,contextName);
        absoluteObjectNameString = (isGlobal()?"":"java:")+fullName.toString();       
        Log.debug("Bound "+absoluteObjectNameString);
    }
}
