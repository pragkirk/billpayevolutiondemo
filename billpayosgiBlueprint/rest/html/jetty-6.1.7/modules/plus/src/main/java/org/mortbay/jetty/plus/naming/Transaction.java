// ========================================================================
// $Id: Transaction.java 1540 2007-01-19 12:24:10Z janb $
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
import javax.naming.LinkRef;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import org.mortbay.log.Log;
import org.mortbay.naming.NamingUtil;

/**
 * Transaction
 *
 * Class to represent a JTA UserTransaction impl.
 * 
 * 
 */
/**
 * Transaction
 *
 *
 */
public class Transaction extends NamingEntry
{
    public static final String USER_TRANSACTION = "UserTransaction";
    

    public static Transaction getTransaction (int scopeType)
    throws NamingException
    {
       return (Transaction)lookupNamingEntry(scopeType, Transaction.class, USER_TRANSACTION);
    }
    


    
    
    public Transaction (UserTransaction userTransaction)
    throws NamingException
    {
        super (USER_TRANSACTION, userTransaction);           
    }
    
    
    public void bindToENC ()
    throws NamingException
    {   
        InitialContext ic = new InitialContext();
        Context env = (Context)ic.lookup("java:comp");
        Log.debug("Binding java:comp/"+getJndiName()+" to "+absoluteObjectNameString);
        NamingUtil.bind(env, getJndiName(), new LinkRef(absoluteObjectNameString));
    }
    
    
    
    /**
     * Unbind this Transaction from a java:comp
     */
    public void unbindENC ()
    {
        try
        {
            InitialContext ic = new InitialContext();
            Context env = (Context)ic.lookup("java:comp");
            Log.debug("Unbinding java:comp/"+getJndiName());
            env.unbind(getJndiName());
        }
        catch (NamingException e)
        {
            Log.warn(e);
        }
    }
    
    
}
