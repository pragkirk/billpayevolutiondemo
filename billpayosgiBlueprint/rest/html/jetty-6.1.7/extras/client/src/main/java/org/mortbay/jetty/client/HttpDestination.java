package org.mortbay.jetty.client;
//========================================================================
//Copyright 2006-2007 Mort Bay Consulting Pty. Ltd.
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.log.Log;

/**
*
* @author Greg Wilkins
* @author Guillaume Nodet
*/
public class HttpDestination
{
    private ByteArrayBuffer _hostHeader;
    private InetSocketAddress _address;
    private LinkedList<HttpConnection> _connections=new LinkedList<HttpConnection>();
    private ArrayList<HttpConnection> _idle=new ArrayList<HttpConnection>();
    private HttpClient _client;
    private boolean _ssl;
    private int _maxConnections;
    private AtomicInteger _pendingConnections = new AtomicInteger(0);
    
    /* The queue of exchanged for this destination if connections are limited */
    private LinkedList<HttpExchange> _queue=new LinkedList<HttpExchange>();

    /* ------------------------------------------------------------ */
    HttpDestination(HttpClient pool, InetSocketAddress address, boolean ssl, int maxConnections)
    {
        _client=pool;
        _address=address;
        _ssl=ssl;
        _maxConnections=maxConnections;
        String host = address.getHostName();
        if (address.getPort() != (_ssl ? 443 : 80)) {
            host += ":" + address.getPort();
        }
        _hostHeader = new ByteArrayBuffer (host);
    }

    /* ------------------------------------------------------------ */
    public InetSocketAddress getAddress()
    {
        return _address;
    }
    
    public Buffer getHostHeader()
    {
        return _hostHeader;
    }
    
    /* ------------------------------------------------------------ */
    public HttpClient getHttpClient()
    {
        return _client;
    }

    /* ------------------------------------------------------------------------------- */
    public HttpConnection getIdleConnection() throws IOException
    {
        synchronized (this)
        {
            long now = System.currentTimeMillis();
            long idleTimeout=_client.getIdleTimeout();

            // remove any really old connections
            while (_idle.size() > 0)
            {
                HttpConnection connection = _idle.get(0);
                long last = connection.getLast();
                if (!connection.getEndPoint().isOpen() ||((now-last)>=idleTimeout) )
                {
                    _idle.remove(0);
                    _connections.remove(connection);
                    connection.getEndPoint().close();
                }
                else 
                    break;
            }
            
            // Find an idle connection
            while (_idle.size() > 0)
            {
                HttpConnection connection = _idle.remove(_idle.size()-1);
                long last = connection.getLast();
                if (connection.getEndPoint().isOpen() && (last==0 || ((now-last)<idleTimeout)) )
                    return connection;
                else
                {
                    _connections.remove(connection);
                    connection.getEndPoint().close();
                }
            }

            return null;
        }
    }

    /* ------------------------------------------------------------------------------- */
    protected void startNewConnection() 
    {
        try
        {
            _pendingConnections.incrementAndGet();
            _client._connector.startConnection(this);
        }
        catch(Exception e)
        {
            onException(e);
        }
    }

    /* ------------------------------------------------------------------------------- */
    public void onConnectionFailed(Throwable throwable)
    {
        _pendingConnections.decrementAndGet();
        synchronized (this)
        {
            if (_queue.size()>0)
            {
                HttpExchange ex=_queue.removeFirst();
                ex.onConnectionFailed(throwable);
            }
        }
    }

    /* ------------------------------------------------------------------------------- */
    public void onException(Throwable throwable)
    {
        _pendingConnections.decrementAndGet();
        synchronized (this)
        {
            if (_queue.size()>0)
            {
                HttpExchange ex=_queue.removeFirst();
                ex.onException(throwable);
            }
        }
    }
    
    /* ------------------------------------------------------------------------------- */
    public void onNewConnection(HttpConnection connection) throws IOException
    {
        _pendingConnections.decrementAndGet();
        synchronized (this)
        {
            _connections.add(connection);
            if (_queue.size()==0)
            {
                _idle.add(connection);
            }
            else
            {
                HttpExchange ex=_queue.removeFirst();
                connection.send(ex);
            }
        }
    }

    /* ------------------------------------------------------------------------------- */
    public void returnConnection(HttpConnection connection, boolean close) throws IOException
    {
        if (!close && connection.getEndPoint().isOpen())
        {
            synchronized (this)
            {
                if (_queue.size()==0)
                {
                    connection.setLast(System.currentTimeMillis());
                    _idle.add(connection);
                }
                else
                {
                    HttpExchange ex=_queue.removeFirst();
                    connection.send(ex);
                }
            }
        }
        else 
        {
            synchronized (this)
            {
                _connections.remove(connection);
                if (!_queue.isEmpty())
                    startNewConnection();
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    public void send(HttpExchange ex) throws IOException
    {
        synchronized(this)
        {
            HttpConnection connection=null;
            if (_queue.size()>0 || (connection=getIdleConnection())==null || !connection.send(ex))
            {
                _queue.add(ex);
                if (_connections.size()+_pendingConnections.get() <_maxConnections)
                {
                     startNewConnection();
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    public synchronized String toString()
    {
        return "HttpDestination@"+hashCode()+"//"+_address.getHostName()+":"+_address.getPort()+"("+_connections.size()+","+_idle.size()+","+_queue.size()+")";
    }
    
    /* ------------------------------------------------------------ */
    public synchronized String toDetailString()
    {
        StringBuilder b = new StringBuilder();
        b.append(toString());
        b.append('\n');
        synchronized(this)
        {
            for (HttpConnection connection : _connections)
            {
                if (connection._exchange!=null)
                {
                    b.append(connection.toDetailString());
                    if (_idle.contains(connection))
                        b.append(" IDLE");
                    b.append('\n');
                }
            }
        }
        b.append("--");
        b.append('\n');
        
        return b.toString();
    }
    
}