// ========================================================================
// Copyright 2006-2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.BufferCache.CachedBuffer;
import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.HttpParser;
import org.mortbay.jetty.HttpSchemes;
import org.mortbay.jetty.HttpURI;
import org.mortbay.jetty.HttpVersions;
import org.mortbay.log.Log;
import org.mortbay.thread.Timeout;
import org.mortbay.util.StringUtil;

/**
 * VERY rough start to a client API - inpired by javascript XmlHttpRequest.
 * 
 * @author gregw
 * @author Guillaume Nodet
 */
public class HttpExchange
{
    public static final int STATUS_UNKOWN = 0;
    public static final int STATUS_WAITING_FOR_CONNECTION = 1;
    public static final int STATUS_WAITING_FOR_COMMIT = 2;
    public static final int STATUS_SENDING_REQUEST = 3;
    public static final int STATUS_WAITING_FOR_RESPONSE = 4;
    public static final int STATUS_PARSING_HEADERS = 5;
    public static final int STATUS_PARSING_CONTENT = 6;
    public static final int STATUS_COMPLETED = 7;
    public static final int STATUS_EXPIRED = 8;


    InetSocketAddress _address;
    String _method = HttpMethods.GET;
    Buffer _scheme;
    int _version = HttpVersions.HTTP_1_1_ORDINAL;
    String _uri;
    int _status = STATUS_UNKOWN;
    HttpFields _requestFields = new HttpFields();
    Buffer _requestContent;
    InputStream _requestContentSource;
    Buffer _requestContentChunk;
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    // methods to build request
    

    /* ------------------------------------------------------------ */
    public int getStatus()
    {
        return _status;
    }

    /* ------------------------------------------------------------ */
    public void waitForStatus(int status) throws InterruptedException
    {
        synchronized (this)
        {
            while (_status < status)
            {
                this.wait();
            }
        }
    }

    /* ------------------------------------------------------------ */
    void setStatus(int status)
    {
        synchronized (this)
        {
            _status=status;
            this.notifyAll();
            
            try
            {
                switch(status)
                {
                    case STATUS_WAITING_FOR_CONNECTION:
                        break;

                    case STATUS_WAITING_FOR_COMMIT:
                        break;

                    case STATUS_SENDING_REQUEST:
                        break;

                    case HttpExchange.STATUS_WAITING_FOR_RESPONSE:
                        onRequestCommitted();
                        break;

                    case STATUS_PARSING_HEADERS:
                        break;

                    case STATUS_PARSING_CONTENT:
                        onResponseHeaderComplete();
                        break;

                    case STATUS_COMPLETED:
                        onResponseComplete(); 
                        break;

                    case STATUS_EXPIRED:
                        onExpire();
                        break;

                }
            }
            catch(IOException e)
            {
                Log.warn(e);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param url
     */
    public void setURL(String url)
    {
        HttpURI uri=new HttpURI(url);
        String scheme=uri.getScheme();
        if (scheme!=null)
        {
            if (HttpSchemes.HTTP.equalsIgnoreCase(scheme))
                setScheme(HttpSchemes.HTTP_BUFFER);
            else if (HttpSchemes.HTTPS.equalsIgnoreCase(scheme))
                setScheme(HttpSchemes.HTTPS_BUFFER);
            else
                setScheme(new ByteArrayBuffer(scheme));
        }
        
        int port=uri.getPort();
        if (port<=0)
            port="https".equalsIgnoreCase(scheme)?443:80;
        
        setAddress(new InetSocketAddress(uri.getHost(),port));
        
        String completePath=uri.getCompletePath();
        if (completePath!=null)
            setURI(completePath);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param address
     */
    public void setAddress(InetSocketAddress address)
    {
        _address=address;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public InetSocketAddress getAddress()
    {
        return _address;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param scheme
     */
    public void setScheme(Buffer scheme)
    {
        _scheme=scheme;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public Buffer getScheme()
    {
        return _scheme;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param version as integer, 9, 10 or 11 for 0.9, 1.0 or 1.1
     */
    public void setVersion(int version)
    {
        _version=version;
    }
    
   public void setVersion(String version)
   {
       CachedBuffer v = HttpVersions.CACHE.get(version);
       if (v==null)
           _version=10;
       else
           _version=v.getOrdinal();
   }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public int getVersion()
    {
        return _version;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param method
     */
    public void setMethod(String method)
    {
        _method=method;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public String getMethod()
    {
        return _method;
    }
    

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public String getURI()
    {
        return _uri;
    }


    /* ------------------------------------------------------------ */
    /**
     * @param uri
     */
    public void setURI(String uri)
    {
        _uri=uri;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param name
     * @param value
     */
    public void addRequestHeader(String name, String value)
    {
        getRequestFields().add(name,value);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param name
     * @param value
     */
    public void addRequestHeader(Buffer name, Buffer value)
    {
        getRequestFields().add(name,value);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param name
     * @param value
     */
    public void setRequestHeader(String name, String value)
    {
        getRequestFields().put(name,value);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param name
     * @param value
     */
    public void setRequestHeader(Buffer name, Buffer value)
    {
        getRequestFields().put(name,value);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param value
     */
    public void setRequestContentType(String value)
    {
        getRequestFields().put(HttpHeaders.CONTENT_TYPE_BUFFER,value);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public HttpFields getRequestFields()
    {
        return _requestFields;
    }

    

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    // methods to commit and/or send the request
    

    /* ------------------------------------------------------------ */
    /**
     * @param requestContent
     */
    public void setRequestContent(Buffer requestContent)
    {
        _requestContent=requestContent;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param requestContent
     */
    public void setRequestContentSource(InputStream in)
    {
        _requestContentSource=in;
    }
    
    /* ------------------------------------------------------------ */
    public InputStream getRequestContentSource()
    {
        return _requestContentSource;
    }

    /* ------------------------------------------------------------ */
    public Buffer getRequestContentChunk() throws IOException
    {
        synchronized(this)
        {
            if (_requestContentChunk==null)
                _requestContentChunk=new ByteArrayBuffer(4096); // TODO configure
            else
            {
                if (_requestContentChunk.hasContent())
                    throw new IllegalStateException();
                _requestContentChunk.clear();
            }
            
            int read=_requestContentChunk.capacity();
            int length = _requestContentSource.read(_requestContentChunk.array(),0,read);
            if (length>=0)
            {
                _requestContentChunk.setPutIndex(length);
                return _requestContentChunk;
            }
            return null;
        }
    }

    /* ------------------------------------------------------------ */
    public Buffer getRequestContent()
    {
        return _requestContent;
    }
    
    public String toString()
    {
        return "HttpExchange@"+hashCode()+"="+_method+"//"+_address.getHostName()+":"+_address.getPort()+_uri+"#"+_status;
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    // methods to handle response
    protected void onRequestCommitted() throws IOException
    {
    }
    
    protected void onRequestComplete() throws IOException
    {
    }
    
    protected void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException
    {
    }

    protected void onResponseHeader(Buffer name, Buffer value) throws IOException
    {
    }
    
    protected void onResponseHeaderComplete() throws IOException
    {
    }

    protected void onResponseContent(Buffer content) throws IOException
    {
    }

    protected void onResponseComplete() throws IOException
    {
    }

    protected void onConnectionFailed(Throwable ex)
    {
        System.err.println("CONNECTION FAILED on "+this);
        // ex.printStackTrace();
    }

    protected void onException(Throwable ex)
    {
        System.err.println("EXCEPTION on "+this);
        ex.printStackTrace();
    }
    
    protected void onExpire()
    {
        System.err.println("EXPIRED "+this);
    }


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static class CachedExchange extends HttpExchange
    {
        int _responseStatus;
        HttpFields _responseFields;
        
        public CachedExchange(boolean cacheFields)
        {
            if (cacheFields)
                _responseFields = new HttpFields();
        }
        
        /* ------------------------------------------------------------ */
        public int getResponseStatus()
        {
            if (_status < STATUS_PARSING_HEADERS)
                throw new IllegalStateException("Response not received");
            return _responseStatus;
        }

        /* ------------------------------------------------------------ */
        public HttpFields getResponseFields()
        {
            if (_status < STATUS_PARSING_CONTENT)
                throw new IllegalStateException("Headers not complete");
            return _responseFields;
        }
        
        /* ------------------------------------------------------------ */
        protected void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException
        {
            _responseStatus = status;
        }

        /* ------------------------------------------------------------ */
        protected void onResponseHeader(Buffer name, Buffer value) throws IOException
        {
            if (_responseFields!=null)
                _responseFields.add(name, value);
        }

    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static class ContentExchange extends CachedExchange
    {
        int _contentLength=1024;
        String _encoding="utf-8";
        ByteArrayOutputStream _responseContent;
        
        public ContentExchange()
        {
            super(false);
        }
        
        /* ------------------------------------------------------------ */
        public String getResponseContent() throws UnsupportedEncodingException
        {
            if (_responseContent!=null)
            {
                return _responseContent.toString(_encoding);
            }
            return null;
        }
        
        /* ------------------------------------------------------------ */
        protected void onResponseHeader(Buffer name, Buffer value) throws IOException
        {
            int header = HttpHeaders.CACHE.getOrdinal(value);
            switch(header)
            {
                case HttpHeaders.CONTENT_LANGUAGE_ORDINAL:
                    _contentLength=BufferUtil.toInt(value);
                    break;
                case HttpHeaders.CONTENT_TYPE_ORDINAL:
                    
                    String mime=StringUtil.asciiToLowerCase(value.toString());
                    int i=mime.indexOf("charset=");
                    if (i>0)
                    {
                        mime=mime.substring(i+8);
                        i=mime.indexOf(';');
                        if (i>0)
                            mime=mime.substring(0,i);
                    }
                    if (mime!=null && mime.length()>0)
                        _encoding=mime;
                    break;
            }
        }

        protected void onResponseContent(Buffer content) throws IOException
        {
            if (_responseContent==null)
                _responseContent= new ByteArrayOutputStream(_contentLength);
            content.writeTo(_responseContent);
        }
    }

}
