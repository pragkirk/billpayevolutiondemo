//========================================================================
//$Id: AbstractJettyRunMojo.java 2147 2007-10-23 05:08:49Z gregw $
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.mortbay.util.Scanner;
import org.mortbay.jetty.plugin.util.ScanTargetPattern;
import org.codehaus.plexus.util.FileUtils;

/**
 * AbstractJettyRunMojo
 * 
 * 
 * Base class for all jetty versions for the "run" mojo.
 * 
 */
public abstract class AbstractJettyRunMojo extends AbstractJettyMojo
{

    /**
     * If true, the &lt;testOutputDirectory&gt;
     * and the dependencies of &lt;scope&gt;test&lt;scope&gt;
     * will be put first on the runtime classpath.
     * @parameter default-value="false"
     */
    private boolean useTestClasspath;
    
    
    /**
     * The location of a jetty-env.xml file. Optional.
     * @parameter
     */
    private File jettyEnvXml;
    
    /**
     * The location of the web.xml file. If not
     * set then it is assumed it is in ${basedir}/src/main/webapp/WEB-INF
     * 
     * @parameter expression="${maven.war.webxml}"
     */
    private File webXml;
    
    /**
     * The directory containing generated classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * 
     */
    private File classesDirectory;
    
    
    
    /**
     * The directory containing generated test classes.
     * 
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     */
    private File testClassesDirectory;
    
    /**
     * Root directory for all html/jsp etc files
     *
     * @parameter expression="${basedir}/src/main/webapp"
     * @required
     */
    private File webAppSourceDirectory;
    
    /**
     * @parameter expression="${plugin.artifacts}"
     * @readonly
     */
    private List pluginArtifacts;
    
    /**
     * List of files or directories to additionally periodically scan for changes. Optional.
     * @parameter
     */
    private File[] scanTargets;
    
    
    /**
     * List of directories with ant-style &lt;include&gt; and &lt;exclude&gt; patterns
     * for extra targets to periodically scan for changes. Can be used instead of,
     * or in conjunction with &lt;scanTargets&gt;.Optional.
     * @parameter
     */
    private ScanTargetPattern[] scanTargetPatterns;

    /**
     * web.xml as a File
     */
    private File webXmlFile;
    
    
    /**
     * jetty-env.xml as a File
     */
    private File jettyEnvXmlFile;

    /**
     * List of files on the classpath for the webapp
     */
    private List classPathFiles;
    
    
    /**
     * Extra scan targets as a list
     */
    private List extraScanTargets;

    public File getWebXml()
    {
        return this.webXml;
    }
    
    public File getJettyEnvXml ()
    {
        return this.jettyEnvXml;
    }

    public File getClassesDirectory()
    {
        return this.classesDirectory;
    }

    public File getWebAppSourceDirectory()
    {
        return this.webAppSourceDirectory;
    }

    public void setWebXmlFile (File f)
    {
        this.webXmlFile = f;
    }
    
    public File getWebXmlFile ()
    {
        return this.webXmlFile;
    }
    
    public File getJettyEnvXmlFile ()
    {
        return this.jettyEnvXmlFile;
    }
    
    public void setJettyEnvXmlFile (File f)
    {
        this.jettyEnvXmlFile = f;
    }
    
    public void setClassPathFiles (List list)
    {
        this.classPathFiles = new ArrayList(list);
    }

    public List getClassPathFiles ()
    {
        return this.classPathFiles;
    }


    public List getExtraScanTargets ()
    {
        return this.extraScanTargets;
    }
    
    public void setExtraScanTargets(List list)
    {
        this.extraScanTargets = list;
    }

    /**
     * Run the mojo
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
       super.execute();
    }
    
    
    /**
     * Verify the configuration given in the pom.
     * 
     * @see org.mortbay.jetty.plugin.AbstractJettyMojo#checkPomConfiguration()
     */
    public void checkPomConfiguration () throws MojoExecutionException
    {
        // check the location of the static content/jsps etc
        try
        {
            if ((getWebAppSourceDirectory() == null) || !getWebAppSourceDirectory().exists())
                throw new MojoExecutionException("Webapp source directory "
                        + (getWebAppSourceDirectory() == null ? "null" : getWebAppSourceDirectory().getCanonicalPath())
                        + " does not exist");
            else
                getLog().info( "Webapp source directory = "
                        + getWebAppSourceDirectory().getCanonicalPath());
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Webapp source directory does not exist", e);
        }
        
       
        // get the web.xml file if one has been provided, otherwise assume it is
        // in the webapp src directory
        if (getWebXml() == null )
            webXml = new File(new File(getWebAppSourceDirectory(),"WEB-INF"), "web.xml");
        setWebXmlFile(webXml);
        
        try
        {
            if (!getWebXmlFile().exists())
                throw new MojoExecutionException( "web.xml does not exist at location "
                        + webXmlFile.getCanonicalPath());
            else
                getLog().info( "web.xml file = "
                        + webXmlFile.getCanonicalPath());
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("web.xml does not exist", e);
        }
        
        //check if a jetty-env.xml location has been provided, if so, it must exist
        if  (getJettyEnvXml() != null)
        {
            setJettyEnvXmlFile(jettyEnvXml);
            
            try
            {
                if (!getJettyEnvXmlFile().exists())
                    throw new MojoExecutionException("jetty-env.xml file does not exist at location "+jettyEnvXml);
                else
                    getLog().info(" jetty-env.xml = "+getJettyEnvXmlFile().getCanonicalPath());
            }
            catch (IOException e)
            {
                throw new MojoExecutionException("jetty-env.xml does not exist");
            }
        }
        
        
        // check the classes to form a classpath with
        try
        {
            //allow a webapp with no classes in it (just jsps/html)
            if (getClassesDirectory() != null)
            {
                if (!getClassesDirectory().exists())
                    getLog().info( "Classes directory "+ getClassesDirectory().getCanonicalPath()+ " does not exist");
                else
                    getLog().info("Classes = " + getClassesDirectory().getCanonicalPath());
            }
            else
                getLog().info("Classes directory not set");         
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Location of classesDirectory does not exist");
        }
        
        
        
        if (scanTargets == null)
            setExtraScanTargets(Collections.EMPTY_LIST);
        else
        {
            ArrayList list = new ArrayList();
            for (int i=0; i< scanTargets.length; i++)
            {
                getLog().info("Added extra scan target:"+ scanTargets[i]);
                list.add(scanTargets[i]);
            }
            setExtraScanTargets(list);
        }
        
        
        if (scanTargetPatterns!=null)
        {
            for (int i=0;i<scanTargetPatterns.length; i++)
            {
                Iterator itor = scanTargetPatterns[i].getIncludes().iterator();
                StringBuffer strbuff = new StringBuffer();
                while (itor.hasNext())
                {
                    strbuff.append((String)itor.next());
                    if (itor.hasNext())
                        strbuff.append(",");
                }
                String includes = strbuff.toString();
                
                itor = scanTargetPatterns[i].getExcludes().iterator();
                strbuff= new StringBuffer();
                while (itor.hasNext())
                {
                    strbuff.append((String)itor.next());
                    if (itor.hasNext())
                        strbuff.append(",");
                }
                String excludes = strbuff.toString();

                try
                {
                    List files = FileUtils.getFiles(scanTargetPatterns[i].getDirectory(), includes, excludes);
                    itor = files.iterator();
                    while (itor.hasNext())
                        getLog().info("Adding extra scan target from pattern: "+itor.next());
                    setExtraScanTargets(files);
                }
                catch (IOException e)
                {
                    throw new MojoExecutionException(e.getMessage());
                }
            }
            
           
        }
    }

   



    public void configureWebApplication() throws Exception
    {
       super.configureWebApplication();
        setClassPathFiles(setUpClassPath());
        webAppConfig.setWebXmlFile(getWebXmlFile());
        webAppConfig.setJettyEnvXmlFile(getJettyEnvXmlFile());
        webAppConfig.setClassPathFiles(getClassPathFiles());
        webAppConfig.setWar(getWebAppSourceDirectory().getCanonicalPath());
        getLog().info("Webapp directory = " + getWebAppSourceDirectory().getCanonicalPath());

        webAppConfig.configure();
    }
    
    public void configureScanner ()
    {
        // start the scanner thread (if necessary) on the main webapp
        final ArrayList scanList = new ArrayList();
        scanList.add(getWebXmlFile());
        if (getJettyEnvXmlFile() != null)
            scanList.add(getJettyEnvXmlFile());
        File jettyWebXmlFile = findJettyWebXmlFile(new File(getWebAppSourceDirectory(),"WEB-INF"));
        if (jettyWebXmlFile != null)
            scanList.add(jettyWebXmlFile);
        scanList.addAll(getExtraScanTargets());
        scanList.add(getProject().getFile());
        scanList.addAll(getClassPathFiles());
        setScanList(scanList);
        ArrayList listeners = new ArrayList();
        listeners.add(new Scanner.BulkListener()
        {
            public void filesChanged (List changes)
            {
                try
                {
                    getLog().info("restarting "+webAppConfig);
                    getLog().debug("Stopping webapp ...");
                    webAppConfig.stop();
                    getLog().debug("Reconfiguring webapp ...");

                    checkPomConfiguration();
                    configureWebApplication();

                    // check if we need to reconfigure the scanner,
                    // which is if the pom changes
                    if (changes.contains(getProject().getFile().getCanonicalPath()))
                    {
                        getLog().info("Reconfiguring scanner after change to pom.xml ...");
                        scanList.clear();
                        scanList.add(getWebXmlFile());
                        if (getJettyEnvXmlFile() != null)
                            scanList.add(getJettyEnvXmlFile());
                        scanList.addAll(getExtraScanTargets());
                        scanList.add(getProject().getFile());
                        scanList.addAll(getClassPathFiles());
                        getScanner().setScanDirs(scanList);
                    }

                    getLog().debug("Restarting webapp ...");
                    webAppConfig.start();
                    getLog().info("Restart completed at "+new Date().toString());
                }
                catch (Exception e)
                {
                    getLog().error("Error reconfiguring/restarting webapp after change in watched files",e);
                }
            }


        });
        setScannerListeners(listeners);
    }

    
    private List getDependencyFiles ()
    {
        List dependencyFiles = new ArrayList();
        for ( Iterator iter = getProject().getArtifacts().iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            // Include runtime and compile time libraries, and possibly test libs too
            if (((!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) && (!Artifact.SCOPE_TEST.equals( artifact.getScope()))) 
                    ||
                (useTestClasspath && Artifact.SCOPE_TEST.equals( artifact.getScope())))
            {
                dependencyFiles.add(artifact.getFile());
                getLog().debug( "Adding artifact " + artifact.getFile().getName() + " for WEB-INF/lib " );   
            }
        }
        return dependencyFiles; 
    }
    
    
   

    private List setUpClassPath()
    {
        List classPathFiles = new ArrayList();       
        
        //if using the test classes, make sure they are first
        //on the list
        if (useTestClasspath && (testClassesDirectory != null))
            classPathFiles.add(testClassesDirectory);
        
        if (getClassesDirectory() != null)
            classPathFiles.add(getClassesDirectory());
        
        //now add all of the dependencies
        classPathFiles.addAll(getDependencyFiles());
        
        if (getLog().isDebugEnabled())
        {
            for (int i = 0; i < classPathFiles.size(); i++)
            {
                getLog().debug("classpath element: "+ ((File) classPathFiles.get(i)).getName());
            }
        }
        return classPathFiles;
    }

}
