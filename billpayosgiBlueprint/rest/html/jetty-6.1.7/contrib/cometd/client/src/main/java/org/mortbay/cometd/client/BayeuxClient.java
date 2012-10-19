package org.mortbay.cometd.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.Cookie;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpSchemes;
import org.mortbay.jetty.client.HttpClient;
import org.mortbay.jetty.client.HttpExchange;
import org.mortbay.log.Log;
import org.mortbay.util.QuotedStringTokenizer;
import org.mortbay.util.ajax.JSON;
import org.mortbay.cometd.MessageImpl;
import org.mortbay.cometd.MessagePool;

import dojox.cometd.Client;
import dojox.cometd.Listener;
import dojox.cometd.Message;

public class BayeuxClient extends MessagePool implements Client
{
    private HttpClient _client;
    private InetSocketAddress _address;
    private HttpExchange _pull;
    private HttpExchange _push;
    private String _uri="/cometd";
    private boolean _initialized=false;
    private boolean _disconnecting=false;
    private String _clientId;
    private Listener _listener;
    private List<Message> _inQ;
    private List<Message> _outQ;
    private int _batch;
    private boolean _formEncoded;
    private Map<String, Cookie> _cookies=new ConcurrentHashMap<String, Cookie>();

    /* ------------------------------------------------------------ */
    public BayeuxClient(HttpClient client, InetSocketAddress address, String uri) throws IOException
    {
        _client=client;
        _address=address;
        _uri=uri;

        _inQ=new LinkedList<Message>();
        _outQ=new LinkedList<Message>();
    }

    /* ------------------------------------------------------------ */
    public String getClientId()
    {
        return _clientId;
    }

    /* ------------------------------------------------------------ */
    public void start()
    {
        _pull=new Handshake();
    }

    /* ------------------------------------------------------------ */
    public boolean isPolling()
    {
        synchronized (_outQ)
        {
            return _pull!=null;
        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Client#deliver(dojox.cometd.Client, java.util.Map)
     */
    public void deliver(Client from, Message message)
    {
        synchronized (_inQ)
        {
            if (_listener==null)
                _inQ.add(message);
            else
            {
                _listener.deliver(from,this,message);
            }
        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Client#getId()
     */
    public String getId()
    {
        return _clientId;
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Client#getListener()
     */
    public Listener getListener()
    {
        synchronized (_inQ)
        {
            return _listener;
        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Client#hasMessages()
     */
    public boolean hasMessages()
    {
        synchronized (_inQ)
        {
            return _inQ.size()>0;
        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Client#isLocal()
     */
    public boolean isLocal()
    {
        return false;
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Client#publish(java.lang.String, java.lang.Object, java.lang.String)
     */
    public void publish(String toChannel, Object data, String msgId)
    {
        Message msg=new MessageImpl();
        msg.put("channel",toChannel);
        msg.put("data",data);
        if (msgId!=null)
            msg.put("id",msgId);

        synchronized (_outQ)
        {
            _outQ.add(msg);
            if (_batch==0&&_initialized&&_push==null)
                _push=new Publish();
        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Client#subscribe(java.lang.String)
     */
    public void subscribe(String toChannel)
    {
        Message msg=new MessageImpl();
        msg.put("channel","/meta/subscribe");
        msg.put("subscription",toChannel);

        synchronized (_outQ)
        {
            _outQ.add(msg);

            if (_batch==0&&_initialized&&_push==null)
                _push=new Publish();
        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Client#unsubscribe(java.lang.String)
     */
    public void unsubscribe(String toChannel)
    {
        Message msg=new MessageImpl();
        msg.put("channel","/meta/unsubscribe");
        msg.put("subscription",toChannel);

        synchronized (_outQ)
        {
            _outQ.add(msg);

            if (_batch==0&&_initialized&&_push==null)
                _push=new Publish();
        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Client#remove(boolean)
     */
    public void remove(boolean timeout)
    {
        Message msg=new MessageImpl();
        msg.put("channel","/meta/disconnect");

        synchronized (_outQ)
        {
            _outQ.add(msg);

            _initialized=false;
            _disconnecting=true;

            if (_batch==0&&_initialized&&_push==null)
                _push=new Publish();

        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Client#setListener(dojox.cometd.Listener)
     */
    public void setListener(Listener listener)
    {
        synchronized (_inQ)
        {
            _listener=listener;
        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Client#takeMessages()
     */
    public List<Message> takeMessages()
    {
        synchronized (_inQ)
        {
            LinkedList<Message> list=new LinkedList<Message>(_inQ);
            _inQ.clear();
            return list;
        }
    }

    /* ------------------------------------------------------------ */
    public void endBatch()
    {
        synchronized (_outQ)
        {
            if (--_batch<=0)
            {
                _batch=0;
                if ((_initialized||_disconnecting)&&_push==null&&_outQ.size()>0)
                    _push=new Publish();
            }
        }
    }

    /* ------------------------------------------------------------ */
    public void startBatch()
    {
        synchronized (_outQ)
        {
            _batch++;
        }
    }

    /* ------------------------------------------------------------ */
    /** Customize an Exchange.
     * Called when an exchange is about to be sent to allow Cookies
     * and Credentials to be customized.  Default implementation sets
     * any cookies 
     */
    protected void customize(HttpExchange exchange)
    {
        for (Cookie cookie : _cookies.values())
        {
            exchange.addRequestHeader(HttpHeaders.COOKIE,cookie.getName()+"="+cookie.getValue()); // TODO quotes
        }
    }

    /* ------------------------------------------------------------ */
    public void setCookie(Cookie cookie)
    {
        _cookies.put(cookie.getName(),cookie);
    }

    /* ------------------------------------------------------------ */
    private class Exchange extends HttpExchange.ContentExchange
    {
        Object[] _responses;
        int _connectFailures;

        Exchange(String info)
        {
            setMethod("POST");
            setScheme(HttpSchemes.HTTP_BUFFER);
            setAddress(_address);
            setURI(_uri+"/"+info);

            setRequestContentType(_formEncoded?"application/x-www-form-urlencoded;charset=utf-8":"text/json;charset=utf-8");
        }

        protected void setMessage(String message)
        {
            try
            {
                if (_formEncoded)
                    setRequestContent(new ByteArrayBuffer("message="+URLEncoder.encode(message,"utf-8")));
                else
                    setRequestContent(new ByteArrayBuffer(message,"utf-8"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        protected void setMessages(List<Message> messages)
        {
            try
            {
                for (Message msg : messages)
                {
                    msg.put("clientId",_clientId);
                }
                String json=JSON.toString(messages);

                if (_formEncoded)
                    setRequestContent(new ByteArrayBuffer("message="+URLEncoder.encode(json,"utf-8")));
                else
                    setRequestContent(new ByteArrayBuffer(json,"utf-8"));

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }

        /* ------------------------------------------------------------ */
        protected void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException
        {
            super.onResponseStatus(version,status,reason);
        }

        /* ------------------------------------------------------------ */
        protected void onResponseHeader(Buffer name, Buffer value) throws IOException
        {
            super.onResponseHeader(name,value);
            if (HttpHeaders.CACHE.getOrdinal(name)==HttpHeaders.SET_COOKIE_ORDINAL)
            {
                String cname=null;
                String cvalue=null;

                QuotedStringTokenizer tok=new QuotedStringTokenizer(value.toString(),"=;",false,false);
                tok.setSingle(false);

                if (tok.hasMoreElements())
                    cname=tok.nextToken();
                if (tok.hasMoreElements())
                    cvalue=tok.nextToken();

                Cookie cookie=new Cookie(cname,cvalue);

                while (tok.hasMoreTokens())
                {
                    String token=tok.nextToken();
                    if ("Version".equalsIgnoreCase(token))
                        cookie.setVersion(Integer.parseInt(tok.nextToken()));
                    else if ("Comment".equalsIgnoreCase(token))
                        cookie.setComment(tok.nextToken());
                    else if ("Path".equalsIgnoreCase(token))
                        cookie.setPath(tok.nextToken());
                    else if ("Domain".equalsIgnoreCase(token))
                        cookie.setDomain(tok.nextToken());
                    else if ("Expires".equalsIgnoreCase(token))
                    {
                        tok.nextToken();
                        // TODO
                    }
                    else if ("Max-Age".equalsIgnoreCase(token))
                    {
                        tok.nextToken();
                        // TODO
                    }
                    else if ("Secure".equalsIgnoreCase(token))
                        cookie.setSecure(true);
                }

                BayeuxClient.this.setCookie(cookie);
            }
        }

        /* ------------------------------------------------------------ */
        protected void onResponseComplete() throws IOException
        {
            super.onResponseComplete();

            if (getResponseStatus()==200)
            {
                _responses=parse(getResponseContent());
            }
        }

        /* ------------------------------------------------------------ */
        protected void onExpire()
        {
            super.onExpire();
        }

        /* ------------------------------------------------------------ */
        protected void onConnectionFailed(Throwable ex)
        {
            super.onConnectionFailed(ex);
            if (++_connectFailures<5)
            {
                try
                {
                    _client.send(this);
                }
                catch (IOException e)
                {
                    // TODO handle better
                    e.printStackTrace();
                }
            }
        }

        /* ------------------------------------------------------------ */
        protected void onException(Throwable ex)
        {
            super.onException(ex);
        }

    }

    private class Handshake extends Exchange
    {
        final static String handshake="[{"+"\"channel\":\"/meta/handshake\","+"\"version\":\"0.9\","+"\"minimumVersion\":\"0.9\""+"}]";

        Handshake()
        {
            super("handshake");
            setMessage(handshake);

            try
            {
                customize(this);
                _client.send(this);
            }
            catch (IOException e)
            {
                // TODO handle better
                e.printStackTrace();
            }
        }

        /* ------------------------------------------------------------ */
        /* (non-Javadoc)
         * @see org.mortbay.jetty.client.HttpExchange#onException(java.lang.Throwable)
         */
        protected void onException(Throwable ex)
        {
            Log.warn("Handshake:"+ex);
            Log.debug(ex);
        }

        protected void onResponseComplete() throws IOException
        {
            super.onResponseComplete();
            if (getResponseStatus()==200&&_responses!=null&&_responses.length>0)
            {
                Map response=(Map)_responses[0];
                Boolean successful=(Boolean)response.get("successful");
                if (successful!=null&&successful.booleanValue())
                {
                    _clientId=(String)response.get("clientId");
                    _pull=new Connect();
                }
                else
                    throw new IOException("Handshake failed:"+_responses[0]);
            }
            else
            {
                throw new IOException("Handshake failed: "+getResponseStatus());
            }
        }
    }

    private class Connect extends Exchange
    {
        Connect()
        {
            super("connect/"+_clientId);
            String connect="{"+"\"channel\":\"/meta/connect\","+"\"clientId\":\""+_clientId+"\","+"\"connectionType\":\"long-polling\""+"}";
            setMessage(connect);

            try
            {
                customize(this);
                _client.send(this);
            }
            catch (IOException e)
            {
                // TODO handle better
                e.printStackTrace();
            }
        }

        protected void onResponseComplete() throws IOException
        {
            super.onResponseComplete();
            if (getResponseStatus()==200&&_responses!=null&&_responses.length>0)
            {
                try
                {
                    startBatch();

                    for (int i=0; i<_responses.length; i++)
                    {
                        Message msg=(Message)_responses[i];

                        if ("/meta/connect".equals(msg.get("channel")))
                        {
                            Boolean successful=(Boolean)msg.get("successful");
                            if (successful!=null&&successful.booleanValue())
                            {
                                if (!_initialized)
                                {
                                    _initialized=true;
                                    synchronized (_outQ)
                                    {
                                        if (_outQ.size()>0)
                                            _push=new Publish();
                                    }
                                }

                                _pull=new Connect();
                            }
                            else
                                throw new IOException("Connect failed:"+_responses[0]);
                        }

                        deliver(null,msg);
                    }
                }
                finally
                {
                    endBatch();
                }

            }
            else
            {
                throw new IOException("Connect failed: "+getResponseStatus());
            }
        }
    }

    private class Publish extends Exchange
    {
        Publish()
        {
            super("publish/"+_clientId);
            synchronized (_outQ)
            {
                if (_outQ.size()==0)
                    return;
                setMessages(_outQ);
                _outQ.clear();
            }
            try
            {
                customize(this);
                _client.send(this);
            }
            catch (IOException e)
            {
                // TODO handle better
                e.printStackTrace();
            }
        }

        protected void onResponseComplete() throws IOException
        {
            super.onResponseComplete();

            try
            {
                synchronized (_outQ)
                {
                    startBatch();
                    _push=null;
                }

                if (getResponseStatus()==200&&_responses!=null&&_responses.length>0)
                {

                    for (int i=0; i<_responses.length; i++)
                    {
                        Message msg=(Message)_responses[i];
                        deliver(null,msg);
                    }
                }
                else
                {
                    throw new IOException("Reconnect failed: "+getResponseStatus());
                }
            }
            finally
            {
                endBatch();
            }
        }
    }

}
