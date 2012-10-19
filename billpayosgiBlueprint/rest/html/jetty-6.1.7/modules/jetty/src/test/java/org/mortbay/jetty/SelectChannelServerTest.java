package org.mortbay.jetty;
import org.mortbay.jetty.nio.SelectChannelConnector;

/**
 * HttpServer Tester.
 */
public class SelectChannelServerTest extends HttpServerTestBase
{
    public SelectChannelServerTest()
    {
        super(new SelectChannelConnector());
    }   
}