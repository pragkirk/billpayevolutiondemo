package org.mortbay.setuid;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.log.Log;

/**
 * This extension of {@link Server} will make a JNI call to set the unix UID.
 *
 * This can be used to start the server as root so that privileged ports may
 * be accessed and then switch to a non-root user for security.
 * Depending on the value of {@link #setStartServerAsPrivileged(boolean)}, either the
 * server will be started and then the UID set; or the {@link Server#getConnectors()} will be 
 * opened with a call to {@link Connector#open()}, the UID set and then the server is started.
 * The later is the default and avoids any webapplication code being run as a privileged user,
 * but will not work if the application code also needs to open privileged ports.
 *
 *<p>
 * The configured umask is set before the server is started and the configured
 * uid is set after the server is started.
 * </p>
 * @author gregw
 *
 */
public class SetUIDServer extends Server
{
    private int _uid=0;
    private int _gid=0;
    private int _umask=0;
    private boolean _startServerAsPrivileged;
    
    public void setUsername(String username)
    {
        Passwd passwd = SetUID.getpwnam(username);
        _uid = passwd.getPwUid();
    }
    
    public String getUsername()
    {
        Passwd passwd = SetUID.getpwuid(_uid);
        return passwd.getPwName();
    }
    
    public void setGroupname(String groupname)
    {
        Group group = SetUID.getgrnam(groupname);
        _gid = group.getGrGid();
    }
    
    public String getGroupname()
    {
        Group group = SetUID.getgrgid(_gid);
        return group.getGrName();
    }


    public int getUmask ()
    {
        return _umask;
    }

    public void setUmask(int umask)
    {
        _umask=umask;
    }
    
    public int getUid()
    {
        return _uid;
    }

    public void setUid(int uid)
    {
        _uid=uid;
    }
    
    public void setGid(int gid)
    {
        _gid=gid;
    }
    
    public int getGid()
    {
        return _gid;
    }

    protected void doStart() throws Exception
    {
        if (_umask!=0)
        {
            Log.info("Setting umask=0"+Integer.toString(_umask,8));
            SetUID.setumask(_umask);
        }
        
        if (_startServerAsPrivileged)
        {
            super.doStart();
            if (_gid!=0)
            {
                Log.info("Setting GID="+_gid);
                SetUID.setgid(_gid);
            }
            if (_uid!=0)
            {
                Log.info("Setting UID="+_uid);
                SetUID.setuid(_uid);
            }
        }
        else
        {
            Connector[] connectors = getConnectors();
            for (int i=0;connectors!=null && i<connectors.length;i++)
                connectors[i].open();
            if (_gid!=0)
            {
                Log.info("Setting GID="+_gid);
                SetUID.setgid(_gid);
            }
            if (_uid!=0)
            {
                Log.info("Setting UID="+_uid);
                SetUID.setuid(_uid);
            }
            super.doStart();
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the startServerAsPrivileged 
     */
    public boolean isStartServerAsPrivileged()
    {
        return _startServerAsPrivileged;
    }

    /* ------------------------------------------------------------ */
    /**
     * @see {@link Connector#open()}
     * @param startServerAsPrivileged if true, the server is started and then the process UID is switched. If false, the connectors are opened, the UID is switched and then the server is started.
     */
    public void setStartServerAsPrivileged(boolean startContextsAsPrivileged)
    {
        _startServerAsPrivileged=startContextsAsPrivileged;
    }
    
}
