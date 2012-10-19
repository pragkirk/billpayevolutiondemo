// ========================================================================
// Copyright 2007 Mort Bay Consulting Pty. Ltd.
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
//========================================================================

package dojox.cometd.demo;


import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;

import org.mortbay.cometd.BayeuxService;

import dojox.cometd.Bayeux;
import dojox.cometd.Client;
import dojox.cometd.Listener;

public class BayeuxStartupListener implements ServletContextAttributeListener
{
    public void initialize(Bayeux bayeux)
    {
        synchronized(bayeux)
        {
            if(!bayeux.hasChannel("/service/echo"))
            {
                new EchoRPC(bayeux);
            }
        }
    }
    
    public void attributeAdded(ServletContextAttributeEvent scab)
    {
        if (scab.getName().equals(Bayeux.DOJOX_COMETD_BAYEUX))
        {
            Bayeux bayeux=(Bayeux)scab.getValue();
            initialize(bayeux);
        }
    }

    public void attributeRemoved(ServletContextAttributeEvent scab)
    {

    }

    public void attributeReplaced(ServletContextAttributeEvent scab)
    {

    }

    
    public static class EchoRPC extends BayeuxService
    {
        
        public EchoRPC(Bayeux bayeux)
        {
            super(bayeux,"echo");
            subscribe("/service/echo","echo");
        }
        
        public Object echo(Client client, Object data)
        {
	    System.err.println("ECHO from "+client+" "+data);
	    return data;
        }
        
    }
    
}
