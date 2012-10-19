// ========================================================================
// Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.log;

import org.mortbay.util.DateCache;

/*-----------------------------------------------------------------------*/
/** StdErr Logging.
 * This implementation of the Logging facade sends all logs to StdErr with minimal formatting.
 * 
 * If the system property DEBUG is set, then debug logs are printed if stderr is being used.
 * 
 */
public class StdErrLog implements Logger
{    
    private static DateCache _dateCache;
    private static boolean debug = System.getProperty("DEBUG",null)!=null;
    private String name;
    
    static
    {
        try
        {
            _dateCache=new DateCache("yyyy-MM-dd HH:mm:ss");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
    }
    
    public StdErrLog()
    {
        this(null);
    }
    
    public StdErrLog(String name)
    {    
        this.name=name==null?"":name;
    }
    
    public boolean isDebugEnabled()
    {
        return debug;
    }
    
    public void setDebugEnabled(boolean enabled)
    {
        debug=enabled;
    }
    
    public void info(String msg,Object arg0, Object arg1)
    {
        String d=_dateCache.now();
        int ms=_dateCache.lastMs();
        System.err.println(d+(ms>99?".":(ms>0?".0":".00"))+ms+":"+name+":INFO:  "+format(msg,arg0,arg1));
    }
    
    public void debug(String msg,Throwable th)
    {
        if (debug)
        {
            String d=_dateCache.now();
            int ms=_dateCache.lastMs();
            System.err.println(d+(ms>99?".":(ms>0?".0":".00"))+ms+":"+name+":DEBUG: "+msg);
            if (th!=null) th.printStackTrace();
        }
    }
    
    public void debug(String msg,Object arg0, Object arg1)
    {
        if (debug)
        {
            String d=_dateCache.now();
            int ms=_dateCache.lastMs();
            System.err.println(d+(ms>99?".":(ms>0?".0":".00"))+ms+":"+name+":DEBUG: "+format(msg,arg0,arg1));
        }
    }
    
    public void warn(String msg,Object arg0, Object arg1)
    {
        String d=_dateCache.now();
        int ms=_dateCache.lastMs();
        System.err.println(d+(ms>99?".":(ms>0?".0":".00"))+ms+":"+name+":WARN:  "+format(msg,arg0,arg1));
    }
    
    public void warn(String msg, Throwable th)
    {
        String d=_dateCache.now();
        int ms=_dateCache.lastMs();
        System.err.println(d+(ms>99?".":(ms>0?".0":".00"))+ms+":"+name+":WARN:  "+msg);
        if (th!=null)
            th.printStackTrace();
    }

    private String format(String msg, Object arg0, Object arg1)
    {
        int i0=msg.indexOf("{}");
        int i1=i0<0?-1:msg.indexOf("{}",i0+2);
        
        if (arg1!=null && i1>=0)
            msg=msg.substring(0,i1)+arg1+msg.substring(i1+2);
        if (arg0!=null && i0>=0)
            msg=msg.substring(0,i0)+arg0+msg.substring(i0+2);
        return msg;
    }
    
    public Logger getLogger(String name)
    {
        if ((name==null && this.name==null) ||
            (name!=null && name.equals(this.name)))
            return this;
        return new StdErrLog(name);
    }
    
    public String toString()
    {
        return "STDERR"+name;
    }

}

