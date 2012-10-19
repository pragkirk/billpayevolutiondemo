package org.mortbay.cometd;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.mortbay.util.ajax.JSON;

import dojox.cometd.Bayeux;
import dojox.cometd.Message;

public class MessageImpl extends HashMap<String, Object> implements Message, org.mortbay.util.ajax.JSON.Generator
{
    MessagePool _pool;
    String _clientId;
    String _json;
    String _channel;
    Object _id;
    Message _associated;
    AtomicInteger _refs=new AtomicInteger();

    /* ------------------------------------------------------------ */
    public MessageImpl()
    {
        super(8);
    }
    
    /* ------------------------------------------------------------ */
    public MessageImpl(MessagePool bayeux)
    {
        super(8);
        _pool=bayeux;
    }
    
    /* ------------------------------------------------------------ */
    public void incRef()
    {
        _refs.getAndIncrement();
    }

    /* ------------------------------------------------------------ */
    public void decRef()
    {
        int r= _refs.decrementAndGet();
        if (r==0 && _pool!=null)
            _pool.recycleMessage(this);
        else if (r<0)
            throw new IllegalStateException();
    }

    /* ------------------------------------------------------------ */
    public String getChannel()
    {
        return _channel;
    }
    
    /* ------------------------------------------------------------ */
    public String getClientId()
    {
        if (_clientId==null)
            _clientId=(String)get(Bayeux.CLIENT_FIELD);
        return _clientId;
    }

    /* ------------------------------------------------------------ */
    public Object getId()
    {
        return _id;
    }
    
    /* ------------------------------------------------------------ */
    public void addJSON(StringBuffer buffer)
    {
        buffer.append(getJSON());
    }

    /* ------------------------------------------------------------ */
    public String getJSON()
    {
        if (_json==null)
        {
            JSON json=_pool==null?JSON.getDefault():_pool.getMsgJSON();
            StringBuffer buf = new StringBuffer(json.getStringBufferSize());
            synchronized(buf)
            {
                json.appendMap(buf,this);
                _json=buf.toString();
            }
        }
        return _json;
    }
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see java.util.HashMap#clear()
     */
    public void clear()
    {
        _json=null;
        _id=null;
        _channel=null;
        _clientId=null;
        setAssociated(null);
        _refs.set(0);
        Iterator<Map.Entry<String,Object>> iterator=super.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry<String, Object> entry=iterator.next();
            String key=entry.getKey();
            if (Bayeux.CHANNEL_FIELD.equals(key))
                entry.setValue(null);
            else if (Bayeux.ID_FIELD.equals(key))
                entry.setValue(null);
            else if (Bayeux.TIMESTAMP_FIELD.equals(key))
                entry.setValue(null);
            else
                iterator.remove();
        }
        super.clear();
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
     */
    public Object put(String key, Object value)
    {
        _json=null;
        if (Bayeux.CHANNEL_FIELD.equals(key))
            _channel=(String)value;
        else if (Bayeux.ID_FIELD.equals(key))
            _id=value;
        else if (Bayeux.CLIENT_FIELD.equals(key))
            _clientId=(String)value;
        return super.put(key,value);
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see java.util.HashMap#putAll(java.util.Map)
     */
    public void putAll(Map<? extends String, ? extends Object> m)
    {
        _json=null;
        super.putAll(m);
        _channel=(String)get(Bayeux.CHANNEL_FIELD);
        _id=get(Bayeux.ID_FIELD);
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see java.util.HashMap#remove(java.lang.Object)
     */
    public Object remove(Object key)
    {
        _json=null;
        if (Bayeux.CHANNEL_FIELD.equals(key))
            _channel=null;
        else if (Bayeux.ID_FIELD.equals(key))
            _id=null;
        return super.remove(key);
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see java.util.HashMap#entrySet()
     */
    public Set<java.util.Map.Entry<String, Object>> entrySet()
    {
        return Collections.unmodifiableSet(super.entrySet());
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see java.util.HashMap#keySet()
     */
    public Set<String> keySet()
    {
        return Collections.unmodifiableSet(super.keySet());
    }

    /* ------------------------------------------------------------ */
    public Message getAssociated()
    {
        return _associated;
    }

    /* ------------------------------------------------------------ */
    public void setAssociated(Message message)
    {
        if (_associated!=message)
        {
            if (_associated!=null)
                ((MessageImpl)_associated).decRef();
            _associated=message;
            if (_associated!=null)
                ((MessageImpl)_associated).incRef();
        }
    }
    

}