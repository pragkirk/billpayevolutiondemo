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

package org.mortbay.jetty;

import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.SimpleBuffers;
import org.mortbay.io.bio.StringEndPoint;
import org.mortbay.util.StringUtil;

/**
 * @author gregw
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class HttpParserTest extends TestCase
{
    /**
     * Constructor for HttpParserTest.
     * @param arg0
     */
    public HttpParserTest(String arg0)
    {
        super(arg0);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(HttpParserTest.class);
    }

    /**
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testLineParse0()
	throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        io.setInput("POST /foo HTTP/1.0\015\012" + "\015\012");
        ByteArrayBuffer buffer= new ByteArrayBuffer(4096);
        SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer});

        Handler handler = new Handler();
        HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), 0);
        parser.parse();
        assertEquals("POST", f0);
        assertEquals("/foo", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(-1, h);
    }

    public void testLineParse1()
	throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        io.setInput("GET /999\015\012");
        ByteArrayBuffer buffer= new ByteArrayBuffer(4096);
        SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer});

        f2= null;
        Handler handler = new Handler();
        HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), 0);
        parser.parse();
        assertEquals("GET", f0);
        assertEquals("/999", f1);
        assertEquals(null, f2);
        assertEquals(-1, h);
    }

    public void testLineParse2()
	throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        io.setInput("POST /222  \015\012");
        ByteArrayBuffer buffer= new ByteArrayBuffer(4096);
        SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer});

        f2= null;
        Handler handler = new Handler();
        HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), 0);
        parser.parse();
        assertEquals("POST", f0);
        assertEquals("/222", f1);
        assertEquals(null, f2);
        assertEquals(-1, h);
    }

    public void testLineParse3()
        throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        io.setInput("POST /fo\u0690 HTTP/1.0\015\012" + "\015\012");
        ByteArrayBuffer buffer= new ByteArrayBuffer(4096);
        SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer});

        Handler handler = new Handler();
        HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), 0);
        parser.parse();
        assertEquals("POST", f0);
        assertEquals("/fo\u0690", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(-1, h);
    }

    public void testLineParse4()
        throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        io.setInput("POST /foo?param=\u0690 HTTP/1.0\015\012" + "\015\012");
        ByteArrayBuffer buffer= new ByteArrayBuffer(4096);
        SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer});

        Handler handler = new Handler();
        HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), 0);
        parser.parse();
        assertEquals("POST", f0);
        assertEquals("/foo?param=\u0690", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(-1, h);
    }

    public void testHeaderParse()
	throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        io.setInput(
            "GET / HTTP/1.0\015\012"
                + "Header1: value1\015\012"
                + "Header2  :   value 2a  \015\012"
                + "                    value 2b  \015\012"
                + "Header3: \015\012"
                + "Header4 \015\012"
                + "  value4\015\012"
                + "Server5: notServer\015\012"
                + "\015\012");
        ByteArrayBuffer buffer= new ByteArrayBuffer(4096);
        SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer});

        Handler handler = new Handler();
        HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), 0);
        parser.parse();
        assertEquals("GET", f0);
        assertEquals("/", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("Header2", hdr[1]);
        assertEquals("value 2a value 2b", val[1]);
        assertEquals("Header3", hdr[2]);
        assertEquals("", val[2]);
        assertEquals("Header4", hdr[3]);
        assertEquals("value4", val[3]);
        assertEquals("Server5", hdr[4]);
        assertEquals("notServer", val[4]);
        assertEquals(4, h);
    }

    public void testChunkParse()
    	throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        io.setInput(
            "GET /chunk HTTP/1.0\015\012"
                + "Header1: value1\015\012"
				+ "Transfer-Encoding: chunked\015\012"
                + "\015\012"
                + "a;\015\012"
                + "0123456789\015\012"
                + "1a\015\012"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"
                + "0\015\012");
        ByteArrayBuffer buffer= new ByteArrayBuffer(4096);
        ByteArrayBuffer content=new ByteArrayBuffer(8192);
        SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer,content});

        Handler handler = new Handler();
        HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), content.capacity());
        parser.parse();
        assertEquals("GET", f0);
        assertEquals("/chunk", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(1, h);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", _content);
    }

    public void testMultiParse()
		throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        io.setInput(
            "GET /mp HTTP/1.0\015\012"
                + "Header1: value1\015\012"
				+ "Transfer-Encoding: chunked\015\012"
                + "\015\012"
                + "a;\015\012"
                + "0123456789\015\012"
                + "1a\015\012"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"
                + "0\015\012"
                + "POST /foo HTTP/1.0\015\012"
                + "Header2: value2\015\012"
				+ "Content-Length: 0\015\012"
                + "\015\012"
                + "PUT /doodle HTTP/1.0\015\012"
                + "Header3: value3\015\012"
				+ "Content-Length: 10\015\012"
                + "\015\012"
                + "0123456789\015\012");

        ByteArrayBuffer buffer= new ByteArrayBuffer(4096);
        ByteArrayBuffer content=new ByteArrayBuffer(8192);
        SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer,content});

        Handler handler = new Handler();
        HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), content.capacity());
        parser.parse();
        assertEquals("GET", f0);
        assertEquals("/mp", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(1, h);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", _content);

        parser.parse();
        assertEquals("POST", f0);
        assertEquals("/foo", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(1, h);
        assertEquals("Header2", hdr[0]);
        assertEquals("value2", val[0]);
        assertEquals(null, _content);

        parser.parse();
        assertEquals("PUT", f0);
        assertEquals("/doodle", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(1, h);
        assertEquals("Header3", hdr[0]);
        assertEquals("value3", val[0]);
        assertEquals("0123456789", _content);
    }

    public void testStreamParse() throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        String http="GET / HTTP/1.0\015\012"
                + "Header1: value1\015\012"
				+ "Transfer-Encoding: chunked\015\012"
                + "\015\012"
                + "a;\015\012"
                + "0123456789\015\012"
                + "1a\015\012"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"
                + "0\015\012"
                + "POST /foo HTTP/1.0\015\012"
                + "Header2: value2\015\012"
                + "Content-Length: 0\015\012"
                + "\015\012"
                + "PUT /doodle HTTP/1.0\015\012"
                + "Header3: value3\015\012"
				+ "Content-Length: 10\015\012"
                + "\015\012"
                + "0123456789\015\012";

        
        int[] tests=
            {
                1024,
                http.length() + 3,
                http.length() + 2,
                http.length() + 1,
                http.length() + 0,
                http.length() - 1,
                http.length() - 2,
                http.length() / 2,
                http.length() / 3,
                64,
                32
            };
        
        for (int t= 0; t < tests.length; t++)
        {
            String tst="t"+tests[t];
            try
            {
                ByteArrayBuffer buffer= new ByteArrayBuffer(tests[t]);
                ByteArrayBuffer content=new ByteArrayBuffer(8192);
                SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer,content});

                Handler handler = new Handler();
                HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), content.capacity());
                
                
                io.setInput(http);
                
                parser.parse();
                assertEquals(tst,"GET", f0);
                assertEquals(tst,"/", f1);
                assertEquals(tst,"HTTP/1.0", f2);
                assertEquals(tst,1, h);
                assertEquals(tst,"Header1", hdr[0]);
                assertEquals(tst,"value1", val[0]);
                assertEquals(tst,"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", _content);
                
                parser.parse();
                assertEquals(tst,"POST", f0);
                assertEquals(tst,"/foo", f1);
                assertEquals(tst,"HTTP/1.0", f2);
                assertEquals(tst,1, h);
                assertEquals(tst,"Header2", hdr[0]);
                assertEquals(tst,"value2", val[0]);
                assertEquals(tst,null, _content);
                
                parser.parse();
                assertEquals(tst,"PUT", f0);
                assertEquals(tst,"/doodle", f1);
                assertEquals(tst,"HTTP/1.0", f2);
                assertEquals(tst,1, h);
                assertEquals(tst,"Header3", hdr[0]);
                assertEquals(tst,"value3", val[0]);
                assertEquals(tst,"0123456789", _content);
            }
            catch(Exception e)
            {
                if (t+1 < tests.length)
                    throw e;
                assertTrue(e.toString().indexOf("FULL")>=0);
            }
        }
    }

    public void testResponseParse0()
	throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        io.setInput(
	    "HTTP/1.1 200 Correct\015\012" 
	    + "Content-Length: 10\015\012"
	    + "Content-Type: text/plain\015\012"
	    + "\015\012"
	    + "0123456789\015\012");
        ByteArrayBuffer buffer= new ByteArrayBuffer(4096);
        SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer});

        Handler handler = new Handler();
        HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), 0);
        parser.parse();
        assertEquals("HTTP/1.1", f0);
        assertEquals("200", f1);
        assertEquals("Correct", f2);
	assertEquals(_content.length(), 10);
	assertTrue(headerCompleted);
	assertTrue(messageCompleted);
    }

    public void testResponseParse1()
	throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        io.setInput(
	    "HTTP/1.1 304 Not-Modified\015\012" 
	    + "Connection: close\015\012"
	    + "\015\012");
        ByteArrayBuffer buffer= new ByteArrayBuffer(4096);
        SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer});

        Handler handler = new Handler();
        HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), 0);
        parser.parse();
        assertEquals("HTTP/1.1", f0);
        assertEquals("304", f1);
        assertEquals("Not-Modified", f2);
	assertTrue(headerCompleted);
	assertTrue(messageCompleted);
    }

    public void testResponseParse2()
	throws Exception
    {
        StringEndPoint io=new StringEndPoint();
        io.setInput(
	    "HTTP/1.1 204 No-Content\015\012" 
	    + "Connection: close\015\012"
	    + "\015\012"
	    + "HTTP/1.1 200 Correct\015\012" 
	    + "Content-Length: 10\015\012"
	    + "Content-Type: text/plain\015\012"
	    + "\015\012"
	    + "0123456789\015\012");
        ByteArrayBuffer buffer= new ByteArrayBuffer(4096);
        SimpleBuffers buffers=new SimpleBuffers(new Buffer[]{buffer});

        Handler handler = new Handler();
        HttpParser parser= new HttpParser(buffers,io, handler, buffer.capacity(), 0);
        parser.parse();
        assertEquals("HTTP/1.1", f0);
        assertEquals("204", f1);
        assertEquals("No-Content", f2);
	assertTrue(headerCompleted);
	assertTrue(messageCompleted);

        parser.parse();
        assertEquals("HTTP/1.1", f0);
        assertEquals("200", f1);
        assertEquals("Correct", f2);
	assertEquals(_content.length(), 10);
	assertTrue(headerCompleted);
	assertTrue(messageCompleted);
    }

    String _content;
    String f0;
    String f1;
    String f2;
    String[] hdr;
    String[] val;
    int h;
    
    boolean headerCompleted;
    boolean messageCompleted;

    class Handler extends HttpParser.EventHandler
    {   
        HttpFields fields;
        
        public void content(Buffer ref)
        {
            if (_content==null)
                _content="";
            _content= _content + ref;
        }


        public void startRequest(Buffer tok0, Buffer tok1, Buffer tok2)
        {
            try
            {
                h= -1;
                hdr= new String[9];
                val= new String[9];
                f0= tok0.toString();
                f1=new String(tok1.array(),tok1.getIndex(),tok1.length(),StringUtil.__UTF8);
                if (tok2!=null)
                    f2= tok2.toString();
                else
                    f2=null;

                fields=new HttpFields();
            }
            catch (UnsupportedEncodingException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

	    messageCompleted = false;
	    headerCompleted = false;
        }

        public void parsedHeader(Buffer name, Buffer value)
        {
            fields.add(name, value);
            hdr[++h]= name.toString();
            val[h]= value.toString();
        }

        public void headerComplete()
        {
            _content= null;
            String s0=fields.toString();
            String s1=fields.toString();
            if (!s0.equals(s1))
            {
                //System.err.println(s0);
                //System.err.println(s1);
                throw new IllegalStateException();
            }

	    headerCompleted = true;
        }

        public void messageComplete(long contentLength)
        {
	    messageCompleted = true;
        }


        public void startResponse(Buffer version, int status, Buffer reason)
        {
            f0 = version.toString();
	    f1 = Integer.toString(status);
	    f2 = reason.toString();

            fields=new HttpFields();
            hdr= new String[9];
            val= new String[9];

	    messageCompleted = false;
	    headerCompleted = false;
        }
    }
}
