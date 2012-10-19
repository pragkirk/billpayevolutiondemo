// ========================================================================
// Copyright 2006 Mort Bay Consulting Pty. Ltd.
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
//========================================================================

package org.mortbay.cometd;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletContext;

import org.mortbay.util.ajax.JSON;

import dojox.cometd.Bayeux;
import dojox.cometd.Channel;
import dojox.cometd.Client;
import dojox.cometd.DataFilter;
import dojox.cometd.Extension;
import dojox.cometd.Message;
import dojox.cometd.SecurityPolicy;

/* ------------------------------------------------------------ */
/**
 * @author gregw
 * @author aabeling: added JSONP transport
 * 
 */
public abstract class AbstractBayeux extends MessagePool implements Bayeux
{   
    public static final ChannelId META_ID=new ChannelId(META);
    public static final ChannelId META_CONNECT_ID=new ChannelId(META_CONNECT);
    public static final ChannelId META_CLIENT_ID=new ChannelId(META_CLIENT);
    public static final ChannelId META_DISCONNECT_ID=new ChannelId(META_DISCONNECT);
    public static final ChannelId META_HANDSHAKE_ID=new ChannelId(META_HANDSHAKE);
    public static final ChannelId META_PING_ID=new ChannelId(META_PING);
    public static final ChannelId META_RECONNECT_ID=new ChannelId(META_RECONNECT); // deprecated
    public static final ChannelId META_STATUS_ID=new ChannelId(META_STATUS);
    public static final ChannelId META_SUBSCRIBE_ID=new ChannelId(META_SUBSCRIBE);
    public static final ChannelId META_UNSUBSCRIBE_ID=new ChannelId(META_UNSUBSCRIBE);
    
    public static final JSON.Literal TRANSPORTS=new JSON.Literal("[\"long-polling\",\"callback-polling\"]");

    private static final Map<String,Object> EXT_JSON_COMMENTED=new HashMap<String,Object>(2){
        {
            this.put("json-comment-filtered",Boolean.TRUE);
        }
    };
    
    
    private HashMap<String,Handler> _handlers=new HashMap<String,Handler>();
    
    private ChannelImpl _root = new ChannelImpl("/",this);
    private ConcurrentHashMap<String,ClientImpl> _clients=new ConcurrentHashMap<String,ClientImpl>();
    SecurityPolicy _securityPolicy=new DefaultPolicy();
    Object _advice=new JSON.Literal("{\"reconnect\":\"retry\",\"interval\":0}");
    int _adviceVersion=0;
    Object _unknownAdvice=new JSON.Literal("{\"reconnect\":\"handshake\",\"interval\":500}");
    int _logLevel;
    long _maxInterval=30000;
    boolean _JSONCommented;
    boolean _initialized;
    ConcurrentHashMap<String, List<String>> _browser2client=new ConcurrentHashMap<String, List<String>>();
    int _multiFrameInterval=-1;
    JSON.Literal _multiFrameAdvice; 
    
    transient ServletContext _context;
    transient Random _random;
    transient ConcurrentHashMap<String, ChannelId> _channelIdCache;
    protected Handler _publishHandler;
    
    protected List<Extension> _extensions=new CopyOnWriteArrayList<Extension>();
    
    /* ------------------------------------------------------------ */
    /**
     * @param context.
     *            The logLevel init parameter is used to set the logging to
     *            0=none, 1=info, 2=debug
     */
    protected AbstractBayeux()
    {
        _handlers.put("*",_publishHandler=new PublishHandler());
        _handlers.put(META_HANDSHAKE,new HandshakeHandler());
        _handlers.put(META_CONNECT,new ConnectHandler());
        _handlers.put(META_RECONNECT,new ReConnectHandler());
        _handlers.put(META_DISCONNECT,new DisconnectHandler());
        _handlers.put(META_SUBSCRIBE,new SubscribeHandler());
        _handlers.put(META_UNSUBSCRIBE,new UnsubscribeHandler());
    }

    /* ------------------------------------------------------------ */
    /**
     * @param channels
     *            A {@link ChannelId}
     * @param filter
     *            The filter instance to apply to new channels matching the
     *            pattern
     */
    public void addFilter(String channels, DataFilter filter)
    {
        synchronized (this)
        {
            ChannelImpl channel = (ChannelImpl)getChannel(channels,true);
            channel.addDataFilter(filter);
        }
    }

    /* ------------------------------------------------------------ */
    public void removeFilter(String channels, DataFilter filter)
    {
        synchronized (this)
        {
            ChannelImpl channel = (ChannelImpl)getChannel(channels,false);
            if (channel!=null)
                channel.removeDataFilter(filter);
        }
    }

    /* ------------------------------------------------------------ */
    public void addExtension(Extension ext)
    {
        _extensions.add(ext);
    }
    
    /* ------------------------------------------------------------ */
    public List<Extension> getExtensions()
    {
        // TODO - remove this hack of a method!
        return _extensions;
    }

    /* ------------------------------------------------------------ */
    public void removeExtension(Extension ext)
    {
        _extensions.remove(ext);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param id
     * @return
     */
    public ChannelImpl getChannel(ChannelId id)
    {
        return _root.getChild(id);
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.cometd.Bx#getChannel(java.lang.String)
     */
    public ChannelImpl getChannel(String id)
    {
        ChannelId cid=getChannelId(id);
        if (cid.depth()==0)
            return null;
        return _root.getChild(cid);
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.cometd.Bx#getChannel(java.lang.String, boolean)
     */
    public Channel getChannel(String id, boolean create)
    {
        synchronized(this)
        {
            ChannelImpl channel=getChannel(id);

            if (channel==null && create)
            {
                channel=new ChannelImpl(id,this);
                _root.addChild(channel);
                
                if (isLogInfo())
                    logInfo("newChannel: "+channel);
            }
            return channel;
        }
    }
    
    /* ------------------------------------------------------------ */
    public ChannelId getChannelId(String id)
    {
        ChannelId cid = _channelIdCache.get(id);
        if (cid==null)
        {
            // TODO shrink cache!
            cid=new ChannelId(id);
            _channelIdCache.put(id,cid);
        }
        return cid;
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.cometd.Bx#getClient(java.lang.String)
     */
    public Client getClient(String client_id)
    {
        synchronized(this)
        {
            if (client_id==null)
                return null;
            Client client = _clients.get(client_id);
            return client;
        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.cometd.Bx#getClientIDs()
     */
    public Set getClientIDs()
    {
        return _clients.keySet();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return The maximum time in ms to wait between polls before timing out a client
     */
    public long getMaxInterval()
    {
        return _maxInterval;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the logLevel. 0=none, 1=info, 2=debug
     */
    public int getLogLevel()
    {
        return _logLevel;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.cometd.Bx#getSecurityPolicy()
     */
    public SecurityPolicy getSecurityPolicy()
    {
        return _securityPolicy;
    }
    
    /* ------------------------------------------------------------ */
    /** Handle a Bayeux message.
     * This is normally only called by the bayeux servlet or a test harness.
     * @param client The client if known
     * @param transport The transport to use for the message
     * @param message The bayeux message.
     */
    public String handle(ClientImpl client, Transport transport, Message message) throws IOException
    {
        String channel_id=message.getChannel();
        
        Handler handler=(Handler)_handlers.get(channel_id);
        if (handler!=null)
        {
            ListIterator<Extension> iter = _extensions.listIterator(_extensions.size());
            while(iter.hasPrevious())
                message=iter.previous().rcvMeta(message);
        }
        else
        {
            handler=_publishHandler;
            ListIterator<Extension> iter = _extensions.listIterator(_extensions.size());
            while(iter.hasPrevious())
                message=iter.previous().rcv(message);
        }

        handler.handle(client,transport,message);
        
        return channel_id;
    }

    /* ------------------------------------------------------------ */
    public boolean hasChannel(String id)
    {
        ChannelId cid=getChannelId(id);
        return _root.getChild(cid)!=null;
    }

    /* ------------------------------------------------------------ */
    public boolean isInitialized()
    {
        return _initialized;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the commented
     */
    public boolean isJSONCommented()
    {
        return _JSONCommented;
    }

    /* ------------------------------------------------------------ */
    public boolean isLogDebug()
    {
        return _logLevel>1;
    }

    /* ------------------------------------------------------------ */
    public boolean isLogInfo()
    {
        return _logLevel>0;
    }
    
    /* ------------------------------------------------------------ */
    public void logDebug(String message)
    {
        if (_logLevel>1)
            _context.log(message);
    }

    /* ------------------------------------------------------------ */
    public void logDebug(String message, Throwable th)
    {
        if (_logLevel>1)
            _context.log(message,th);
    }

    /* ------------------------------------------------------------ */
    public void logWarn(String message, Throwable th)
    {
        _context.log(message+": "+th.toString());
    }

    /* ------------------------------------------------------------ */
    public void logWarn(String message)
    {
        _context.log(message);
    }

    /* ------------------------------------------------------------ */
    public void logInfo(String message)
    {
        if (_logLevel>0)
            _context.log(message);
    }
    
    /* ------------------------------------------------------------ */
    public Client newClient(String idPrefix,dojox.cometd.Listener listener)
    {
        return new ClientImpl(this,idPrefix,listener);
    }

    /* ------------------------------------------------------------ */
    public abstract ClientImpl newRemoteClient();

    /* ------------------------------------------------------------ */
    /** Create new transport object for a bayeux message
     * @param client The client
     * @param message the bayeux message
     * @return the negotiated transport.
     */
    public Transport newTransport(ClientImpl client, Map message)
    {
        if (isLogDebug())
            logDebug("newTransport: client="+client+",message="+message);

        Transport result=null;

        try
        {
            String type=client==null?null:client.getConnectionType();
            if (type==null)
                type=(String)message.get("connectionType");
            
            if ("callback-polling".equals(type))
                result=new JSONPTransport(client!=null&&client.isJSONCommented());
            else
                result=new JSONTransport(client!=null&&client.isJSONCommented());
                
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        if (isLogDebug())
            logDebug("newTransport: result="+result);
        return result;
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.cometd.Bx#publish(org.mortbay.cometd.ChannelId, org.mortbay.cometd.Client, java.lang.Object, java.lang.String)
     */
    public void publish(ChannelId to, Client from, Object data, String msgId)
    {
        Message msg = newMessage();
        msg.put(CHANNEL_FIELD,to.toString());
        
        if (msgId==null)
        {
            long id=msg.hashCode()
            ^(to==null?0:to.hashCode())
            ^(from==null?0:from.hashCode());
            id=id<0?-id:id;
            msg.put(ID_FIELD,Long.toString(id,36));
        }
        else
            msg.put(ID_FIELD,msgId);
            
        msg.put(DATA_FIELD,data);
        
        for (Extension e:_extensions)
            msg=e.send(msg);
        _root.deliver(to,from,msg);
        ((MessageImpl)msg).decRef();
    }

    /* ------------------------------------------------------------ */
    public void publish(Client fromClient, String toChannelId, Object data, String msgId)
    {
        publish(getChannelId(toChannelId),fromClient,data,msgId);
    }


    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see dojox.cometd.Bayeux#deliver(dojox.cometd.Client, java.lang.String, java.util.Map)
     */
    public void deliver(Client fromClient,Client toClient, String toChannel, Message message)
    {
        if (toChannel!=null)
            message.put(Bayeux.CHANNEL_FIELD,toChannel);

        for (Extension e:_extensions)
            message=e.send(message);
        
        if (toClient!=null)
            toClient.deliver(fromClient,message);
    }
    

    /* ------------------------------------------------------------ */
    public boolean removeChannel(ChannelId channelId)
    {
        // TODO Auto-generated method stub
        return false;
    }


    /* ------------------------------------------------------------ */
    void addClient(ClientImpl client)
    {
        _clients.put(client.getId(),client);
    }
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.cometd.Bx#removeClient(java.lang.String)
     */
    Client removeClient(String client_id)
    {
        ClientImpl client;
        synchronized(this)
        {
            if (client_id==null)
                return null;
            client = _clients.remove(client_id);
        }
        if (client!=null)
        {
            client.unsubscribeAll();
        }
        return client;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param ms The maximum time in ms to wait between polls before timing out a client
     */
    public void setMaxInterval(long ms)
    {
        _maxInterval=ms;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param commented the commented to set
     */
    public void setJSONCommented(boolean commented)
    {
        _JSONCommented=commented;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param logLevel
     *            the logLevel: 0=none, 1=info, 2=debug
     */
    public void setLogLevel(int logLevel)
    {
        _logLevel=logLevel;
    }
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.cometd.Bx#setSecurityPolicy(org.mortbay.cometd.SecurityPolicy)
     */
    public void setSecurityPolicy(SecurityPolicy securityPolicy)
    {
        _securityPolicy=securityPolicy;
    }

    /* ------------------------------------------------------------ */
    public void subscribe(String toChannel, Client subscriber)
    {
        ChannelImpl channel = (ChannelImpl)getChannel(toChannel,true);
        if (channel!=null)
            channel.subscribe(subscriber);
    }

    /* ------------------------------------------------------------ */
    public void unsubscribe(String toChannel, Client subscriber)
    {
        ChannelImpl channel = (ChannelImpl)getChannel(toChannel);
        if (channel!=null)
            channel.unsubscribe(subscriber);
    }

    
    /* ------------------------------------------------------------ */
    /**
     * @return the multiFrameInterval in milliseconds
     */
    public int getMultiFrameInterval()
    {
        return _multiFrameInterval;
    }

    /* ------------------------------------------------------------ */
    /**
     * The time a client should delay between reconnects when multiple
     * connections from the same browser are detected. This effectively 
     * produces traditional polling.
     * @param multiFrameInterval the multiFrameInterval to set
     */
    public void setMultiFrameInterval(int multiFrameInterval)
    {
        _multiFrameInterval=multiFrameInterval;
        if (multiFrameInterval>0)
            _multiFrameAdvice=new JSON.Literal("{\"reconnect\":\"retry\",\"interval\":"+_multiFrameInterval+",\"multiple-clients\":true}");
        else
            _multiFrameAdvice=new JSON.Literal("{\"reconnect\":\"none\",\"interval\":30000,\"multiple-clients\":true}");
    }


    /* ------------------------------------------------------------ */
    public Object getAdvice()
    {
        return _advice;
    }


    /* ------------------------------------------------------------ */
    public void setAdvice(Object advice)
    {
        synchronized(this)
        {
            _advice=advice;
            _adviceVersion++;
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     * 
     */
    protected void initialize(ServletContext context)
    {
        synchronized(this)
        {
            _initialized=true;
            _context=context;
            try
            {
                _random=SecureRandom.getInstance("SHA1PRNG");
            }
            catch (Exception e)
            {
                context.log("Could not get secure random for ID generation",e);
                _random=new Random();
            }
            _random.setSeed(_random.nextLong()^hashCode()^(context.hashCode()<<32)^Runtime.getRuntime().freeMemory());
            _channelIdCache=new ConcurrentHashMap<String, ChannelId>();
            
            _root.addChild(new ServiceChannel("/service"));
            
        }
    }


    /* ------------------------------------------------------------ */
    public long getRandom(long variation)
    {
        long l=_random.nextLong()^variation;
        return l<0?-l:l;
    }

    /* ------------------------------------------------------------ */
    void clientOnBrowser(String browserId,String clientId)
    {
        List<String> clients=_browser2client.get(browserId);
        if (clients==null)
        {
            List<String> new_clients=new CopyOnWriteArrayList<String>();
            clients=_browser2client.putIfAbsent(browserId,new_clients);
            if (clients==null)
                clients=new_clients;
        }
        clients.add(clientId);
    }

    /* ------------------------------------------------------------ */
    void clientOffBrowser(String browserId,String clientId)
    {
        List<String> clients=_browser2client.get(browserId);
        if (clients!=null)
            clients.remove(clientId);
    }
    
    /* ------------------------------------------------------------ */
    List<String> clientsOnBrowser(String browserId)
    {
        List<String> clients=_browser2client.get(browserId);
        if (clients==null)
            return Collections.emptyList();
        return clients;
    }
    
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static class DefaultPolicy implements SecurityPolicy
    {
        public boolean canHandshake(Message message)
        {
            return true;
        }
        
        public boolean canCreate(Client client, String channel, Message message)
        {
            return client!=null && !channel.startsWith("/meta/");
        }

        public boolean canSubscribe(Client client, String channel, Message message)
        {
            return client!=null && !channel.startsWith("/meta/");
        }

        public boolean canPublish(Client client, String channel, Message message)
        {
            return client!=null && !channel.startsWith("/meta/");
        }

    }


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private abstract class Handler
    {
        abstract void handle(ClientImpl client, Transport transport, Message message) throws IOException;
        
        void unknownClient(Transport transport,String channel) throws IOException
        {
            Message reply=newMessage();
            
            reply.put(CHANNEL_FIELD,channel);
            reply.put(SUCCESSFUL_FIELD,Boolean.FALSE);
            reply.put(ERROR_FIELD,"402::Unknown client");
            reply.put("advice",new JSON.Literal("{\"reconnect\":\"handshake\"}"));
            transport.send(reply);
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class ConnectHandler extends Handler
    {
        protected String _metaChannel=META_CONNECT;
        
        public void handle(ClientImpl client, Transport transport, Message message) throws IOException
        {      
            if (client==null)
            {
                unknownClient(transport,_metaChannel);
                return;
            }

            // is this the first connect message?
            String type=client.getConnectionType();
            boolean polling=true;
            if (type==null)
            {
                type=(String)message.get("connectionType");
                client.setConnectionType(type);
                polling=false;
            }
            
            Object advice=null; 
        
            if (polling && _multiFrameInterval>0 && client.getBrowserId()!=null)
            {
                List<String> clients=clientsOnBrowser(client.getBrowserId());
                int count=clients.size();
                if (count>1)
                {
                    polling=clients.get(0).equals(client.getId());
                    advice=_multiFrameAdvice;
                    client.setAdviceVersion(-1);
                }
            }

            synchronized(this)
            {
                if (client.getAdviceVersion()!=_adviceVersion && (client.getAdviceVersion()>=0||advice==null))
                {
                    advice=_advice;
                    client.setAdviceVersion(_adviceVersion);
                }
            }
           
            // reply to connect message
            Object id=message.getId(); 

            Message reply=newMessage();
            reply.setAssociated(message);
            
            reply.put(CHANNEL_FIELD,META_CONNECT);
            reply.put(SUCCESSFUL_FIELD,Boolean.TRUE);
            if (advice!=null)
                reply.put(ADVICE_FIELD,advice);
            if (id!=null)
                reply.put(ID_FIELD,id);

            
            if (polling)
                transport.setPollReply(reply);
            else
            {
                for (Extension e:_extensions)
                    message=e.sendMeta(reply);
                transport.send(reply);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class ReConnectHandler extends ConnectHandler
    {
        {
            _metaChannel=META_RECONNECT;
        }
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class DisconnectHandler extends Handler
    {
        public void handle(ClientImpl client, Transport transport, Message message) throws IOException
        {
            if (client==null)
            {
                unknownClient(transport,META_DISCONNECT);
                return;
            }
            if (isLogInfo())
                logInfo("Disconnect "+client.getId());
                
            client.remove(false);
            
            Message reply=newMessage();
            reply.setAssociated(message);
            reply.put(CHANNEL_FIELD,META_DISCONNECT);
            reply.put(SUCCESSFUL_FIELD,Boolean.TRUE);
            Object id=message.getId(); 
            if (id!=null)
                reply.put(ID_FIELD,id.toString());

            for (Extension e:_extensions)
                message=e.sendMeta(reply);
            
            Message pollReply = transport.getPollReply();
            if (pollReply!=null)
            {
                for (Extension e:_extensions)
                    pollReply=e.sendMeta(pollReply);
                transport.send(pollReply);
                transport.setPollReply(null);
            }
            transport.send(reply);
        }
    }


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class HandshakeHandler extends Handler
    {
        public void handle(ClientImpl client, Transport transport, Message message) throws IOException
        {
            if (client!=null)
                throw new IllegalStateException();

            if (_securityPolicy!=null && !_securityPolicy.canHandshake(message))
            {
                Message reply=newMessage();
                reply.setAssociated(message);
                reply.put(CHANNEL_FIELD,META_HANDSHAKE);
                reply.put(SUCCESSFUL_FIELD,Boolean.FALSE);
                reply.put(ERROR_FIELD,"403::Handshake denied");

                for (Extension e:_extensions)
                    message=e.sendMeta(reply);
                
                transport.send(reply);
                return;
            }
            
            client=newRemoteClient();

            Map ext = (Map)message.get(EXT_FIELD);

            boolean commented=_JSONCommented && ext!=null && ((Boolean)ext.get("json-comment-filtered")).booleanValue();
            
            Message reply=newMessage();
            reply.setAssociated(message);
            reply.put(CHANNEL_FIELD,META_HANDSHAKE);
            reply.put("version","1.0");
            reply.put("minimumVersion","0.9");
            if (isJSONCommented())
                reply.put(EXT_FIELD,EXT_JSON_COMMENTED);

            if (client!=null)
            {
                reply.put("supportedConnectionTypes",TRANSPORTS);
                reply.put("successful",Boolean.TRUE);
                reply.put(CLIENT_FIELD,client.getId());
                if (_advice!=null)
                    reply.put(ADVICE_FIELD,_advice);
                client.setJSONCommented(commented);
                transport.setJSONCommented(commented);
            }
            else
            {
                reply.put("successful",Boolean.FALSE);
                if (_advice!=null)
                    reply.put(ADVICE_FIELD,_advice);
            }

            if (isLogDebug())
                logDebug("handshake.handle: reply="+reply);

            Object id=message.getId();
            if (id!=null)
                reply.put(ID_FIELD,id.toString());

            for (Extension e:_extensions)
                message=e.sendMeta(reply);
            transport.send(reply);
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class PublishHandler extends Handler
    {
        public void handle(ClientImpl client, Transport transport, Message message) throws IOException
        {
            String channel_id=message.getChannel();
            
            if (client==null && message.containsKey(CLIENT_FIELD))
            {
                unknownClient(transport,channel_id);
                return;
            }
            
            Object id=message.getId();

            ChannelId cid=getChannelId(channel_id);
            Object data=message.get("data");
            

            Message reply=newMessage();
            reply.setAssociated(message);
            reply.put(CHANNEL_FIELD,channel_id);
            if (id!=null)
                reply.put(ID_FIELD,id.toString());
                
            if (data!=null&&_securityPolicy.canPublish(client,channel_id,message))
            {
                reply.put(SUCCESSFUL_FIELD,Boolean.TRUE);

                for (Extension e:_extensions)
                    message=e.sendMeta(reply);
                transport.send(reply);
                publish(cid,client,data,id==null?null:id.toString());
            }
            else
            {
                reply.put(SUCCESSFUL_FIELD,Boolean.FALSE);
                reply.put(ERROR_FIELD,"403::Publish denied");

                for (Extension e:_extensions)
                    message=e.sendMeta(reply);
                transport.send(reply);
            }

        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class SubscribeHandler extends Handler
    {
        public void handle(ClientImpl client, Transport transport, Message message) throws IOException
        {
            if (client==null)
            {
                unknownClient(transport,META_SUBSCRIBE);
                return;
            }

            String subscribe_id=(String)message.get(SUBSCRIPTION_FIELD);

            // select a random channel ID if none specifified
            if (subscribe_id==null)
            {
                subscribe_id=Long.toString(getRandom(message.hashCode()^client.hashCode()),36);
                while (getChannel(subscribe_id)!=null)
                    subscribe_id=Long.toString(getRandom(message.hashCode()^client.hashCode()),36);
            }

            ChannelId cid=null;
            boolean can_subscribe=false;
            boolean service=false;
            
            if (subscribe_id.startsWith(Bayeux.SERVICE_SLASH))
            {
                can_subscribe=true;
                service=false;
            }
            else if (subscribe_id.startsWith(Bayeux.META_SLASH))
            {
                can_subscribe=false;
            }
            else
            {
                cid=getChannelId(subscribe_id);
                can_subscribe=_securityPolicy.canSubscribe(client,subscribe_id,message);
            }
                
            Message reply=newMessage();
            reply.setAssociated(message);
            reply.put(CHANNEL_FIELD,META_SUBSCRIBE);
            reply.put(SUBSCRIPTION_FIELD,subscribe_id);

            if (can_subscribe)
            {
                if (cid!=null)
                {
                    ChannelImpl channel=getChannel(cid);
                    if (channel==null&&_securityPolicy.canCreate(client,subscribe_id,message))
                        channel=(ChannelImpl)getChannel(subscribe_id, true);

                    if (channel!=null)
                        channel.subscribe(client);
                    else
                        can_subscribe=false;
                }
                        
                if (can_subscribe)
                {
                    reply.put(SUCCESSFUL_FIELD,Boolean.TRUE);
                }
                else 
                {
                    reply.put(SUCCESSFUL_FIELD,Boolean.FALSE);
                    reply.put(ERROR_FIELD,"403::cannot create");
                }
            }
            else
            {
                reply.put(SUCCESSFUL_FIELD,Boolean.FALSE);
                reply.put(ERROR_FIELD,"403::cannot subscribe");
                
            }

            Object id=message.getId(); 
            if (id!=null)
                reply.put(ID_FIELD,id.toString());
            for (Extension e:_extensions)
                message=e.sendMeta(reply);
            transport.send(reply);
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class UnsubscribeHandler extends Handler
    {
        public void handle(ClientImpl client, Transport transport, Message message) throws IOException
        {
            if (client==null)
            {
                unknownClient(transport,META_UNSUBSCRIBE);
                return;
            }

            String channel_id=(String)message.get(SUBSCRIPTION_FIELD);
            ChannelImpl channel=getChannel(channel_id);
            if (channel!=null)
                channel.unsubscribe(client);

            Message reply=newMessage();
            reply.setAssociated(message);
            reply.setAssociated(message);
            reply.put(CHANNEL_FIELD,META_UNSUBSCRIBE);
            if (channel!=null)
            {
                channel.unsubscribe(client);
                reply.put(SUBSCRIPTION_FIELD,channel.getId());
                reply.put(SUCCESSFUL_FIELD,Boolean.TRUE);
            }
            else
                reply.put(SUCCESSFUL_FIELD,Boolean.FALSE);
                
            Object id=message.getId(); 
            if (id!=null)
                reply.put(ID_FIELD,id.toString());
            for (Extension e:_extensions)
                message=e.sendMeta(reply);
            transport.send(reply);
        }
    }

    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class ServiceChannel extends ChannelImpl
    {
        ServiceChannel(String id)
        {
            super(id,AbstractBayeux.this);
        }

        /* ------------------------------------------------------------ */
        /* (non-Javadoc)
         * @see org.mortbay.cometd.ChannelImpl#addChild(org.mortbay.cometd.ChannelImpl)
         */
        public void addChild(ChannelImpl channel)
        {
            super.addChild(channel);
            setPersistent(true);
        }

        /* ------------------------------------------------------------ */
        public void subscribe(Client client)
        {
            if (client.isLocal())
                super.subscribe(client);
        }
        
    }

}
