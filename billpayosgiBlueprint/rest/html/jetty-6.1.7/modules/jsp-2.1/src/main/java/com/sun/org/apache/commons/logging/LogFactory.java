// ========================================================================
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
package com.sun.org.apache.commons.logging;

import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;


/**
 * LogFactory
 *
 * Bridges com.sun.org.apache.commons.logging.LogFactory to
 * Jetty's log.
 *
 */
public class LogFactory
{
    private static Map _logs = new HashMap();
    
    public static Log getLog (Class c)
    {
        Log log = (Log)_logs.get(c.getName());
        if (log == null)
        {
            log = new JettyLog(c.getName());
            _logs.put(c.getName(), log);
        }
            
        return log;
    }
    
    public static Log getLog (String str)
    {
        Log log = (Log)_logs.get(str);
        if (log == null)
        {
            log = new JettyLog(str);
            _logs.put(str, log);
        }
        return log;
    }
    
    public static void release (URLClassLoader cl)
    {
        releaseAll ();
    }
    
    public static void releaseAll ()
    {
        _logs.clear();
    }
}
