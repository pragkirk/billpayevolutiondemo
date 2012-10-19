package org.mortbay.jetty.webapp.management;

import org.mortbay.jetty.handler.management.ContextHandlerMBean;
import org.mortbay.jetty.webapp.WebAppContext;

public class WebAppContextMBean extends ContextHandlerMBean
{

    public WebAppContextMBean(Object managedObject)
    {
        super(managedObject);
    }

    /* ------------------------------------------------------------ */
    public String getObjectNameBasis()
    {
        String basis = super.getObjectNameBasis();
        if (basis!=null)
            return basis;
        
        if (_managed!=null && _managed instanceof WebAppContext)
        {
            WebAppContext context = (WebAppContext)_managed;
            String name = context.getWar();
            if (name!=null)
                return name;
        }
        return null;
    }
}
