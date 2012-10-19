package org.mortbay.jetty.example;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.xml.XmlConfiguration;

public class FromXmlConfiguration
{
    public static void main(String[] args)
        throws Exception
    {
        String server_config=
            "<Configure id=\"Server\" class=\"org.mortbay.jetty.Server\">\n"+
            "  <Call name=\"addConnector\">\n" +
            "    <Arg>\n" +
            "      <New class=\"org.mortbay.jetty.nio.SelectChannelConnector\">\n" +
            "        <Set name=\"port\"><SystemProperty name=\"jetty.port\" default=\"8080\"/></Set>\n" +
            "      </New>\n" +
            "    </Arg>\n"+
            "  </Call>\n"+
            "</Configure>\n";
   
        String context_config=
            "<Configure id=\"Server\" class=\"org.mortbay.jetty.servlet.Context\">\n"+
            "  <Set name=\"contextPath\">/</Set>\n"+
            "  <Set name=\"resourceBase\"><SystemProperty name=\"jetty.docroot\" default=\".\"/></Set>\n"+
            "  <Call name=\"addServlet\"><Arg>org.mortbay.jetty.servlet.DefaultServlet</Arg><Arg>/</Arg></Call>\n"+
            "</Configure>\n";
        
        // Apply configuration to an existing object
        Server server = new Server();
        XmlConfiguration configuration = new XmlConfiguration(server_config); 
        configuration.configure(server);
        
        // configuration creates new object
        configuration = new XmlConfiguration(context_config); 
        ContextHandler context = (ContextHandler)configuration.configure();
        
        server.setHandler(context);
        server.start();
    }
}
