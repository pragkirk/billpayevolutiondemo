/*
 * Generated file - Do not edit!
 */
package org.jboss.jetty;

/**
 * MBean interface.
 * TODO - use JMXDoclet to autogenerate
 */
public interface JettyServiceMBean extends org.jboss.web.AbstractWebContainerMBean 
{
   //default object name
   public static final javax.management.ObjectName OBJECT_NAME = org.jboss.mx.util.ObjectNameFactory.create("jboss.jetty:service=Jetty");

  void setJava2ClassLoadingCompliance(boolean loaderCompliance) ;

  boolean getJava2ClassLoadingCompliance() ;

  boolean getUnpackWars() ;

  void setUnpackWars(boolean unpackWars) ;

  boolean getSupportJSR77() ;

  void setSupportJSR77(boolean supportJSR77) ;

  java.lang.String getWebDefaultResource() ;

  void setWebDefaultResource(java.lang.String webDefaultResource) ;

   /**
    * Get the extended Jetty configuration XML fragment
    * @return Jetty XML fragment embedded in jboss-service.xml    */
  org.w3c.dom.Element getConfig() ;

   /**
    * Configure Jetty
    * @param configElement XML fragment from jboss-service.xml
    */
  void setConfigurationElement(org.w3c.dom.Element configElement) ;

  java.lang.String getSubjectAttributeName() ;

  void setSubjectAttributeName(java.lang.String subjectAttributeName) ;
}
