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
import java.lang.reflect.Method;

import org.mortbay.util.Loader;



/*-----------------------------------------------------------------------*/
/** Logging.
 * This class provides a static logging interface.  If an instance of the 
 * org.slf4j.Logger class is found on the classpath, the static log methods
 * are directed to a slf4j logger for "org.mortbay.log".   Otherwise the logs
 * are directed to stderr.
 * 
 * If the system property VERBOSE is set, then ignored exceptions are logged in detail.
 * 
 */
public class Log 
{    
    private static final String[] __nestedEx =
        {"getTargetException","getTargetError","getException","getRootCause"};
    /*-------------------------------------------------------------------*/
    private static final Class[] __noArgs=new Class[0];
    public final static String EXCEPTION= "EXCEPTION ";
    public final static String IGNORED= "IGNORED";
    public final static String IGNORED_FMT= "IGNORED: {}";
    public final static String NOT_IMPLEMENTED= "NOT IMPLEMENTED ";
    
    private static String logClass=System.getProperty("org.mortbay.log.class","org.mortbay.log.Slf4jLog");
    private static boolean verbose = System.getProperty("VERBOSE",null)!=null;
    private static Logger log;
   
    static
    {
        Class log_class=null;
        try
        {
            log_class=Loader.loadClass(Log.class, logClass);
            log=(Logger) log_class.newInstance();
        }
        catch(Exception e)
        {
            log_class=StdErrLog.class;
            log=new StdErrLog();
            if(verbose)
                e.printStackTrace();
        }
        
        log.info("Logging to {} via {}",log,log_class.getName());
    }
    
    public static void setLog(Logger log)
    {
        Log.log=log;
    }
    
    public static Logger getLog()
    {
        return log;
    }
    
    
    public static void debug(Throwable th)
    {
        if (log==null)
            return;
        log.debug(EXCEPTION,th);
        unwind(th);
    }

    public static void debug(String msg)
    {
        if (log==null)
            return;
        log.debug(msg,null,null);
    }
    
    public static void debug(String msg,Object arg)
    {
        if (log==null)
            return;
        log.debug(msg,arg,null);
    }
    
    public static void debug(String msg,Object arg0, Object arg1)
    {
        if (log==null)
            return;
        log.debug(msg,arg0,arg1);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Ignore an exception unless trace is enabled.
     * This works around the problem that log4j does not support the trace level.
     */
    public static void ignore(Throwable th)
    {
        if (log==null)
            return;
        if (verbose)
        {
            log.debug(IGNORED,th);
            unwind(th);
        }
    }
    
    public static void info(String msg)
    {
        if (log==null)
            return;
        log.info(msg,null,null);
    }
    
    public static void info(String msg,Object arg)
    {
        if (log==null)
            return;
        log.info(msg,arg,null);
    }
    
    public static void info(String msg,Object arg0, Object arg1)
    {
        if (log==null)
            return;
        log.info(msg,arg0,arg1);
    }
    
    public static boolean isDebugEnabled()
    {
        if (log==null)
            return false;
        return log.isDebugEnabled();
    }
    
    public static void warn(String msg)
    {
        if (log==null)
            return;
        log.warn(msg,null,null);
    }
    
    public static void warn(String msg,Object arg)
    {
        if (log==null)
            return;
        log.warn(msg,arg,null);        
    }
    
    public static void warn(String msg,Object arg0, Object arg1)
    {
        if (log==null)
            return;
        log.warn(msg,arg0,arg1);        
    }
    
    public static void warn(String msg, Throwable th)
    {
        if (log==null)
            return;
        log.warn(msg,th);
        unwind(th);
    }

    public static void warn(Throwable th)
    {
        if (log==null)
            return;
        log.warn(EXCEPTION,th);
        unwind(th);
    }

    /** Obtain a named Logger.
     * Obtain a named Logger or the default Logger if null is passed.
     */
    public static Logger getLogger(String name)
    {
        if (log==null)
            return log;
        if (name==null)
          return log;
        return log.getLogger(name);
    }

    private static void unwind(Throwable th)
    {
        if (th==null)
            return;
        for (int i=0;i<__nestedEx.length;i++)
        {
            try
            {
                Method get_target = th.getClass().getMethod(__nestedEx[i],__noArgs);
                Throwable th2=(Throwable)get_target.invoke(th,(Object[])null);
                if (th2!=null && th2!=th)
                    warn("Nested in "+th+":",th2);
            }
            catch(Exception ignore){}
        }
    }
    

}

