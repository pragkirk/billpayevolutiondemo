//========================================================================
//$Id: JettyStopMojo.java 2262 2007-12-22 23:54:57Z gregw $
//Copyright 2000-2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.jetty.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * 
 * @author David Yu
 * 
 * @goal stop
 * @requiresDependencyResolution runtime
 * @execute phase="process-sources"
 * @description Stops jetty6 that is configured with &lt;stopKey&gt; and &lt;stopPort&gt;.
 */

public class JettyStopMojo extends AbstractMojo
{
    
    /**
     * Port to listen to stop jetty on executing -DSTOP.PORT=&lt;stopPort&gt; 
     * -DSTOP.KEY=&lt;stopKey&gt; -jar start.jar --stop
     * @parameter
     * @required
     */
    protected int stopPort;
    
    /**
     * Key to provide when stopping jetty on executing java -DSTOP.KEY=&lt;stopKey&gt; 
     * -DSTOP.PORT=&lt;stopPort&gt; -jar start.jar --stop
     * @parameter
     * @required
     */
    protected String stopKey;

    public void execute() throws MojoExecutionException, MojoFailureException 
    {
        if(stopPort<1)
            throw new MojoExecutionException("Please specify a valid port");        
        System.setProperty("STOP.PORT", String.valueOf(stopPort));
        System.setProperty("STOP.KEY", stopKey);
        new org.mortbay.start.Main().stop();        
    }

}
