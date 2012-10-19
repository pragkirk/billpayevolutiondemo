package org.mortbay.cometd;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.mortbay.util.IO;
import org.mortbay.util.StringMap;
import org.mortbay.util.ajax.JSON;

import dojox.cometd.Bayeux;
import dojox.cometd.Message;

public class MessagePool 
{
    Stack<MessageImpl> _messagePool=new Stack<MessageImpl>();
    Stack<JSON.ReaderSource> _readerPool=new Stack<JSON.ReaderSource>();

    public MessagePool()
    {
        super();
    }

    
    
    
    /* ------------------------------------------------------------ */
    /**
     * @return the {@link JSON} instance used to convert data and ext fields
     */
    public JSON getJSON()
    {
        return _json;
    }


    /* ------------------------------------------------------------ */
    /**
     * @param json the {@link JSON} instance used to convert data and ext fields
     */
    public void setJSON(JSON json)
    {
        _json=json;
    }




    /* ------------------------------------------------------------ */
    /**
     * @return the {@link JSON} instance used to convert bayeux messages
     */
    public JSON getMsgJSON()
    {
        return _msgJSON;
    }




    /* ------------------------------------------------------------ */
    /**
     * @param msgJSON the {@link JSON} instance used to convert bayeux messages
     */
    public void setMsgJSON(JSON msgJSON)
    {
        _msgJSON=msgJSON;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the {@link JSON} instance used to convert batches of bayeux messages
     */
    public JSON getBatchJSON()
    {
        return _batchJSON;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param batchJSON the {@link JSON} instance used to convert batches of bayeux messages
     */
    public void setBatchJSON(JSON batchJSON)
    {
        _batchJSON=batchJSON;
    }


    /* ------------------------------------------------------------ */
    public Message newMessage()
    {
        MessageImpl message=null;
        synchronized (_messagePool)
        {
            if (!_messagePool.isEmpty())
                message=_messagePool.pop();
        }
        if (message==null)
            message=new MessageImpl(this);
        message.incRef();
        return message;
    }

    /* ------------------------------------------------------------ */
    public void recycleMessage(MessageImpl message)
    {
        message.clear();
        synchronized(_messagePool)
        {
            if (_messagePool.size()<100) // TODO configure
                _messagePool.push(message);
        }
    }

    public Message[] parse(Reader reader) throws IOException
    {
        JSON.ReaderSource source =null;
        synchronized(_readerPool)
        {
            if (!_readerPool.isEmpty())
                source=_readerPool.pop();
        }
        if (source==null)
            source=new JSON.ReaderSource(reader);
        else
            source.setReader(reader);
        
        Object batch=_batchJSON.parse(source);
        synchronized(_readerPool)
        {
            if (_readerPool.size()<100) // TODO configure
                _readerPool.push(source);
        }

        if (batch==null)
            return new Message[0]; 
        if (batch.getClass().isArray())
            return (Message[])batch;
        return new Message[]{(Message)batch};
    }

    public Message[] parse(String s) throws IOException
    {
        Object batch=_batchJSON.parse(new JSON.StringSource(s));
        if (batch==null)
            return new Message[0]; 
        if (batch.getClass().isArray())
            return (Message[])batch;
        return new Message[]{(Message)batch};
    }

    public void parseTo(String fodder, List<Message> messages)
    {
        Object batch=_batchJSON.parse(new JSON.StringSource(fodder));
        if (batch==null)
            return;
        if (batch.getClass().isArray())
        {
            Message[] msgs=(Message[])batch;
            for (int m=0;m<msgs.length;m++)
                messages.add(msgs[m]);
        }
        else
            messages.add((Message)batch);
    }

    
    private StringMap _fieldStrings = new StringMap();
    private StringMap _valueStrings = new StringMap();
    {
        _fieldStrings.put(Bayeux.ADVICE_FIELD,Bayeux.ADVICE_FIELD);
        _fieldStrings.put(Bayeux.CHANNEL_FIELD,Bayeux.CHANNEL_FIELD);
        _fieldStrings.put(Bayeux.CLIENT_FIELD,Bayeux.CLIENT_FIELD);
        _fieldStrings.put("connectionType","connectionType");
        _fieldStrings.put(Bayeux.DATA_FIELD,Bayeux.DATA_FIELD);
        _fieldStrings.put(Bayeux.ERROR_FIELD,Bayeux.ERROR_FIELD);
        _fieldStrings.put(Bayeux.EXT_FIELD,Bayeux.EXT_FIELD);
        _fieldStrings.put(Bayeux.ID_FIELD,Bayeux.ID_FIELD);
        _fieldStrings.put(Bayeux.SUBSCRIPTION_FIELD,Bayeux.SUBSCRIPTION_FIELD);
        _fieldStrings.put(Bayeux.SUCCESSFUL_FIELD,Bayeux.SUCCESSFUL_FIELD);
        _fieldStrings.put(Bayeux.TIMESTAMP_FIELD,Bayeux.TIMESTAMP_FIELD);
        _fieldStrings.put(Bayeux.TRANSPORT_FIELD,Bayeux.TRANSPORT_FIELD);
        
        _valueStrings.put(Bayeux.META_CLIENT,Bayeux.META_CLIENT);
        _valueStrings.put(Bayeux.META_CONNECT,Bayeux.META_CONNECT);
        _valueStrings.put(Bayeux.META_DISCONNECT,Bayeux.META_DISCONNECT);
        _valueStrings.put(Bayeux.META_HANDSHAKE,Bayeux.META_HANDSHAKE);
        _valueStrings.put(Bayeux.META_SUBSCRIBE,Bayeux.META_SUBSCRIBE);
        _valueStrings.put(Bayeux.META_UNSUBSCRIBE,Bayeux.META_UNSUBSCRIBE);
    }
    
    
    private JSON _json = new JSON()
    {
        protected String toString(char[] buffer, int offset, int length)
        {
            Map.Entry entry = _valueStrings.getEntry(buffer,offset,length);
            if (entry!=null)
                return (String)entry.getValue();
            String s= new String(buffer,offset,length);
            return s;
        }
    };
    
    private JSON _msgJSON = new JSON()
    {
        protected Map newMap()
        {
            return newMessage();
        }

        protected String toString(char[] buffer, int offset, int length)
        {
            Map.Entry entry = _fieldStrings.getEntry(buffer,offset,length);
            if (entry!=null)
                return (String)entry.getValue();
            String s= new String(buffer,offset,length);
            return s;
        }
        
        protected JSON contextFor(String field)
        {
            return _json;
        }
    };
    
    private JSON _batchJSON = new JSON()
    {
        protected Map newMap()
        {
            return newMessage();
        }
        
        protected Object[] newArray(int size)
        {
            return new Message[size]; // todo recycle
        }
        
        protected JSON contextFor(String field)
        {
            return _json;
        }

        protected JSON contextForArray()
        {
            return _msgJSON;
        }
    };

}
