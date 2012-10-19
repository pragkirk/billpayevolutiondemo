package org.mortbay.io;

import java.text.DateFormatSymbols;
import java.util.Locale;

import org.mortbay.util.DateCache;

public class BufferDateCache extends DateCache
{
    Buffer _buffer;
    String _last;
    
    public BufferDateCache()
    {
        super();
    }

    public BufferDateCache(String format, DateFormatSymbols s)
    {
        super(format,s);
    }

    public BufferDateCache(String format, Locale l)
    {
        super(format,l);
    }

    public BufferDateCache(String format)
    {
        super(format);
    }

    public synchronized Buffer formatBuffer(long date)
    {
        String d = super.format(date);
        if (d==_last)
            return _buffer;
        _last=d;
        _buffer=new ByteArrayBuffer(d);
        
        return _buffer;
    }
}
