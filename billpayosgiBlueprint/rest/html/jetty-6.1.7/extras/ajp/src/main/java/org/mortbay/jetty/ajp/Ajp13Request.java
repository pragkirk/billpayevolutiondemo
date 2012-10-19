package org.mortbay.jetty.ajp;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;

public class Ajp13Request extends Request
{
    protected String _remoteAddr;
    protected String _remoteHost;
    protected String _remoteUser;
    protected HttpConnection _connection;



    public Ajp13Request(HttpConnection connection)
    {
        super(connection);
        _remoteAddr = null;
        _remoteHost = null;
        _remoteUser = null;
        
    }

    public void setRemoteUser(String remoteUser)
    {
        _remoteUser = remoteUser;
    }

    public String getRemoteUser()
    {
        if(_remoteUser != null)
            return _remoteUser;
        return super.getRemoteUser();
    }

    public String getRemoteAddr()
    {
        if (_remoteAddr != null)
            return _remoteAddr;
        if (_remoteHost != null)
            return _remoteHost;
        return super.getRemoteAddr();
    }



    public void setRemoteAddr(String remoteAddr)
    {
        _remoteAddr = remoteAddr;
    }

    public String getRemoteHost()
    {
        if (_remoteHost != null)
            return _remoteHost;
        if (_remoteAddr != null)
            return _remoteAddr;
        return super.getRemoteHost();
    }

    public void setRemoteHost(String remoteHost)
    {
        _remoteHost = remoteHost;
    }

    protected void recycle()
    {
        super.recycle();
        _remoteAddr = null;
        _remoteHost = null;
    }

}
