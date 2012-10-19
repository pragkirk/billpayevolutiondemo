package org.mortbay.jetty;
import org.mortbay.jetty.nio.BlockingChannelConnector;

/**
 * HttpServer Tester.
 */
public class BlockingChannelServerTest extends HttpServerTestBase
{
    public BlockingChannelServerTest()
    {
        super(new BlockingChannelConnector());
    }   
}