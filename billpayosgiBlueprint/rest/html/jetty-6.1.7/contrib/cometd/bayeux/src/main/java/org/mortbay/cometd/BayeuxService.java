package org.mortbay.cometd;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dojox.cometd.Bayeux;
import dojox.cometd.Client;
import dojox.cometd.Listener;
import dojox.cometd.Message;

/* ------------------------------------------------------------ */
/** Abstract Bayeux Service class.
 * This is a base class to assist with the creation of server side {@ link Bayeux} 
 * clients that provide services to remote Bayeux clients.   The class provides
 * a Bayeux {@link Client} and {@link Listener} together with convenience methods to map
 * subscriptions to methods on the derived class and to send responses to those methods.
 *
 * @author gregw
 *
 */
public abstract class BayeuxService 
{
    private Bayeux _bayeux;
    private Client _client;
    private Map<String,Method> _methods = new ConcurrentHashMap<String,Method>();
    
    /* ------------------------------------------------------------ */
    /** Instantiate the service.
     * Typically the derived constructor will call {@ #subscribe(String, String)} to 
     * map subscriptions to methods.
     * @param bayeux The bayeux instance.
     * @param name The name of the service (used as client ID prefix).
     */
    public BayeuxService(Bayeux bayeux,String name)
    {
        _bayeux=bayeux;
        _client=_bayeux.newClient(name,new Listen());        
    }

    /* ------------------------------------------------------------ */
    public Bayeux getBayeux()
    {
        return _bayeux;
    }

    /* ------------------------------------------------------------ */
    public Client getClient()
    {
        return _client;
    }
    
    /* ------------------------------------------------------------ */
    /** Subscribe to a channel.
     * Subscribe to channel and map a method to handle received messages.
     * The method must have a unique name and take 2 or 3 arguments:
     * <code>myMethod(Client fromClient,Object data,String id)</code> with
     * the id parameter being optional.  The data parameter can be typed if
     * the type of the data object published by the client is known (typically 
     * Map<String,Object>) 
     * <p>
     * Typically a service will subscribe to a channel in the "/service/**" space
     * which is not a broadcast channel.  Messages published to these channels are
     * only delivered to server side clients like this service.  
     * 
     * <p>Any object returned by a mapped subscription method is delivered to the 
     * calling client and not broadcast. If the method returns void or null, then 
     * no response is sent. A mapped subscription method may also call {@link #send(Client, String, Object, String)}
     * to deliver a response message(s) to different clients and/or channels. It may
     * also publish methods via the normal {@link Bayeux} API.
     * <p>
     * 
     * 
     * @param channel The channel to subscribe to
     * @param methodName The name of the method on this object to call when messages are recieved.
     */
    protected void subscribe(String channel,String methodName)
    {
        if (channel.indexOf('*')>=0)
            throw new IllegalArgumentException("Wildcard not supported");
        Method method=null;
        
        Class<?> c=this.getClass();
        while (c!=null && c!=Object.class)
        {
            Method[] methods = c.getDeclaredMethods();
            for (int i=methods.length;i-->0;)
            {
                if (methodName.equals(methods[i].getName()))
                {
                    if (method!=null)
                        throw new IllegalArgumentException("Multiple methods called '"+methodName+"'");
                    method=methods[i];
                }
            }
            c=c.getSuperclass();
        }
        
        if (method==null)
            throw new NoSuchMethodError(methodName);
        int params=method.getParameterTypes().length;
        if (params!=2 && params!=3)
            throw new IllegalArgumentException("Method '"+methodName+"' does not have 2or3 parameters");
        if (!Client.class.isAssignableFrom(method.getParameterTypes()[0]))
            throw new IllegalArgumentException("Method '"+methodName+"' does not have Client as first parameter");
            
        _methods.put(channel,method);
        _bayeux.subscribe(channel,_client);
    }

    /* ------------------------------------------------------------ */
    /** Send data to a individual client.
     * The data passed is sent to the client as the "data" member of a message
     * with the given channel and id.  The message is not published on the channel and is
     * thus not broadcast to all channel subscribers.  However to the target client, the
     * message appears as if it was broadcast.
     * <p>
     * Typcially this method is only required if a service method sends response(s) to 
     * channels other than the subscribed channel. If the response is to be sent to the subscribed
     * channel, then the data can simply be returned from the subscription method.
     * 
     * @param toClient The target client
     * @param onChannel The channel the message is for
     * @param data The data of the message
     * @param id The id of the message (or null for a random id).
     */
    protected void send(Client toClient, String onChannel, Object data, String id)
    {
        // TODO recycle maps
        Message reply=_bayeux.newMessage();
        reply.put(Bayeux.DATA_FIELD,data);
        if (id!=null)   
            reply.put(Bayeux.ID_FIELD,id);
        _bayeux.deliver(_client,toClient,onChannel,reply);
        ((MessageImpl)reply).decRef();
    }    


    /* ------------------------------------------------------------ */
    /** Handle Exception.
     * This method is called when a mapped subscription method throws
     * and exception while handling a message.
     * @param fromClient
     * @param toClient
     * @param msg
     * @param th
     */
    protected void exception(Client fromClient, Client toClient, Map<String, Object> msg,Throwable th)
    {
        th.printStackTrace();
    }

    /* ------------------------------------------------------------ */
    private void deliver(Client fromClient, Client toClient, Map<String, Object> msg)
    {
        Object data=msg.get(Bayeux.DATA_FIELD);
        String channel=(String)msg.get(Bayeux.CHANNEL_FIELD);
        String id=(String)msg.get(Bayeux.ID_FIELD);
        Method method=_methods.get(channel);
        
        if (method!=null)
        {
            try
            {
                Object reply = (method.getParameterTypes().length==3)
                ?method.invoke(this,fromClient,data,id)
                :method.invoke(this,fromClient,data);
                if (reply!=null)
                    send(fromClient,channel,reply,id);
            }
            catch (Exception e)
            {
                exception(fromClient,toClient,msg,e);
            }
            catch (Error e)
            {
                exception(fromClient,toClient,msg,e);
            }
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Listen implements Listener
    {
        public void deliver(Client fromClient, Client toClient, Message msg)
        {
            BayeuxService.this.deliver(fromClient,toClient,msg);
        }

        public void removed(String clientId, boolean timeout)
        {
        }
    }

}

