package org.mortbay.jetty.spring;

import org.mortbay.jetty.Server;
import org.mortbay.resource.Resource;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.UrlResource;

public class Main
{
	public static void main(String[] args)
		throws Exception	
	{		
		Resource config=Resource.newResource(args.length==1?args[0]:"etc/jetty-spring.xml");
		XmlBeanFactory bf = new XmlBeanFactory(new UrlResource(config.getURL()));
		Server server = (Server) bf.getBean(args.length==2?args[1]:"Server");
		server.join();
	}
}
