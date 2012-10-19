/*
 * Created on 9/01/2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.mortbay.jetty;

import junit.framework.TestCase;

/**
 * @author gregw
 *
 */
public class HttpConnectionTest extends TestCase
{
    Server server = new Server();
    LocalConnector connector = new LocalConnector();
    
    /**
     * Constructor 
     * @param arg0
     */
    public HttpConnectionTest(String arg0)
    {
        super(arg0);
        server.setConnectors(new Connector[]{connector});
        server.setHandler(new DumpHandler());
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        server.start();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        server.stop();
    }
    
    

    /* --------------------------------------------------------------- */
    public void testFragmentedChunk()
    {        
        
        String response=null;
        try
        {
            int offset=0;
            
            // Chunk last
            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"12345");
            

            response=connector.getResponses("GET /R2 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain\n"+
                                           "\015\012"+
                                           "5;\015\012",true);
            response=connector.getResponses("ABCDE\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"/R2");
            offset = checkContains(response,offset,"ABCDE");
            
            
        }
        catch(Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
            if (response!=null)
                System.err.println(response);
        }
    }

    /* --------------------------------------------------------------- */
    public void testEmpty() throws Exception
    {        
        String response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                "Host: localhost\n"+
                "Transfer-Encoding: chunked\n"+
                "Content-Type: text/plain\n"+
                "\015\012"+
        "0\015\012\015\012");

        int offset=0;
        offset = checkContains(response,offset,"HTTP/1.1 200");
        offset = checkContains(response,offset,"/R1");
    }

    /* --------------------------------------------------------------- */
    public void testAutoFlush() throws Exception
    {        
        
        String response=null;
            int offset=0;
            
            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            checkNotContained(response,offset,"IgnoreMe");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"12345");
    }
    
    /* --------------------------------------------------------------- */
    public void testCharset()
    {        
        
        String response=null;
        try
        {
            int offset=0;
            
            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain; charset=utf-8\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"encoding=utf-8");
            offset = checkContains(response,offset,"12345");

            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain; charset =  iso-8859-1 ; other=value\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"encoding=iso-8859-1");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"12345");

            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain; charset=unknown\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"encoding=unknown");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"12345");

            
        }
        catch(Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
            if (response!=null)
                System.err.println(response);
        }
    }

    
    public void testConnection ()
    { 
        String response=null;
        try
        {
            int offset=0;
           
            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: TE, close"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain; charset=utf-8\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"Connection: TE");
            offset = checkContains(response,offset,"Connection: close");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
            if (response!=null)
                System.err.println(response);
        }
    }
    
    
    
    private int checkContains(String s,int offset,String c)
    {
        int o=s.indexOf(c,offset);
        if (o<offset)
        {
            System.err.println("FAILED");
            System.err.println("'"+c+"' not in:");
            System.err.println(s.substring(offset));
            System.err.flush();
            System.out.println("--\n"+s);
            System.out.flush();
            assertTrue(false);
        }
        return o;
    }

    private void checkNotContained(String s,int offset,String c)
    {
        int o=s.indexOf(c,offset);
        if (o>=offset)
        {
            System.err.println("FAILED");
            System.err.println("'"+c+"' IS in:");
            System.err.println(s.substring(offset));
            System.err.flush();
            System.out.println("--\n"+s);
            System.out.flush();
            assertTrue(false);
        }
    }


    

}


