package org.mortbay.jetty;

import java.util.ArrayList;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.io.Buffer;
import org.mortbay.io.Buffers;

/* ------------------------------------------------------------ */
/** Abstract Buffer pool.
 * simple unbounded pool of buffers.
 * @author gregw
 *
 */
public abstract class AbstractBuffers extends AbstractLifeCycle implements Buffers
{
    protected static int BUFFER_LOSS_RATE=256; // Leak buffers to shrink pools
    
    private int _headerBufferSize=4*1024;
    private int _requestBufferSize=8*1024;
    private int _responseBufferSize=24*1024;

    // Use and array of buffers to avoid contention
    private transient ArrayList _headerBuffers=new ArrayList();
    protected transient int _loss;
    private transient ArrayList _requestBuffers;
    private transient ArrayList _responseBuffers;

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the headerBufferSize.
     */
    public int getHeaderBufferSize()
    {
        return _headerBufferSize;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param headerBufferSize The headerBufferSize to set.
     */
    public void setHeaderBufferSize(int headerBufferSize)
    {
        _headerBufferSize = headerBufferSize;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the requestBufferSize.
     */
    public int getRequestBufferSize()
    {
        return _requestBufferSize;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param requestBufferSize The requestBufferSize to set.
     */
    public void setRequestBufferSize(int requestBufferSize)
    {
        _requestBufferSize = requestBufferSize;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the responseBufferSize.
     */
    public int getResponseBufferSize()
    {
        return _responseBufferSize;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param responseBufferSize The responseBufferSize to set.
     */
    public void setResponseBufferSize(int responseBufferSize)
    {
        _responseBufferSize = responseBufferSize;
    }

    
    /* ------------------------------------------------------------ */
    protected abstract Buffer newBuffer(int size);

    
    /* ------------------------------------------------------------ */
    public Buffer getBuffer(int size)
    {
        if (size==_headerBufferSize)
        {   
            synchronized(_headerBuffers)
            {
                if (_headerBuffers.size()>0)
                    return (Buffer) _headerBuffers.remove(_headerBuffers.size()-1);
            }
            return newBuffer(size);
        }
        else if (size==_responseBufferSize)
        {
            synchronized(_responseBuffers)
            {
                if (_responseBuffers.size()==0)
                    return newBuffer(size);
                return (Buffer) _responseBuffers.remove(_responseBuffers.size()-1);
            }
        }
        else if (size==_requestBufferSize)
        {
            synchronized(_requestBuffers)
            {
                if (_requestBuffers.size()==0)
                    return newBuffer(size);
                return (Buffer) _requestBuffers.remove(_requestBuffers.size()-1);
            }   
        }
        
        return newBuffer(size);    
    }


    /* ------------------------------------------------------------ */
    public void returnBuffer(Buffer buffer)
    {
        buffer.clear();
        if (_loss++>BUFFER_LOSS_RATE)
        {
            _loss=0;
            return;
        }

        buffer.clear();
        if (!buffer.isVolatile() && !buffer.isImmutable())
        {
            int c=buffer.capacity();
            if (c==_headerBufferSize)
            {
                synchronized(_headerBuffers)
                {
                    _headerBuffers.add(buffer);
                }
            }
            else if (c==_responseBufferSize)
            {
                synchronized(_responseBuffers)
                {
                    _responseBuffers.add(buffer);
                }
            }
            else if (c==_requestBufferSize)
            {
                synchronized(_requestBuffers)
                {
                    _requestBuffers.add(buffer);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    protected void doStart() throws Exception
    {
        super.doStart();

        if (_headerBuffers!=null)
            _headerBuffers.clear();
        else
            _headerBuffers=new ArrayList();

        if (_requestBuffers!=null)
            _requestBuffers.clear();
        else
            _requestBuffers=new ArrayList();
        
        if (_responseBuffers!=null)
            _responseBuffers.clear();
        else
            _responseBuffers=new ArrayList(); 
    }
    
    
}
