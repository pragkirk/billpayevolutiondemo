package org.mortbay.jetty.handler.management;

import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.management.ObjectMBean;

public class ContextHandlerMBean extends ObjectMBean
{
    public ContextHandlerMBean(Object managedObject)
    {
        super(managedObject);
    }

    /* ------------------------------------------------------------ */
    public String getObjectNameBasis()
    {
        if (_managed!=null && _managed instanceof ContextHandler)
        {
            ContextHandler context = (ContextHandler)_managed;
            String name = context.getDisplayName();
            if (name!=null)
                return name;
            
            if (context.getBaseResource()!=null && context.getBaseResource().getName().length()>1)
                return context.getBaseResource().getName();
        }
        return super.getObjectNameBasis();
    }
}
