package org.mortbay.jetty.win32service;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.Server;
import org.tanukisoftware.wrapper.WrapperManager;


public class Win32Service extends AbstractLifeCycle implements Runnable
{
    private Server server;
    public void doStart()
    {
        
        
        JettyServiceWrapperListener.setServer(server);
         
    }
    
    public void doStop()
    {
        System.out.println("Listener is stopping Jetty Service Instance!!!");
        
    }
    
    public void run()
    {
        doStop();
        
    }

    public void stopServer()
    {
        try
        {
            System.out.println("Thread Test Stopper!!!");
            server.stop();
            //WrapperManager.stop(0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }
    
   

   
    
    
    
}