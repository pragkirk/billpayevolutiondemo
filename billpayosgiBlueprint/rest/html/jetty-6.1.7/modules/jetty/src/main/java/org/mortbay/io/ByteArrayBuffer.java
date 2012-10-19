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

package org.mortbay.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/* ------------------------------------------------------------------------------- */
/**
 * @author gregw
 */
public class ByteArrayBuffer extends AbstractBuffer
{
    private byte[] _bytes;

    public ByteArrayBuffer(byte[] bytes)
    {
        this(bytes, 0, bytes.length, READWRITE);
    }

    public ByteArrayBuffer(byte[] bytes, int index, int length)
    {
        this(bytes, index, length, READWRITE);
    }

    public ByteArrayBuffer(byte[] bytes, int index, int length, int access)
    {
        super(READWRITE, NON_VOLATILE);
        _bytes = bytes;
        setPutIndex(index + length);
        setGetIndex(index);
        _access = access;
    }

    public ByteArrayBuffer(byte[] bytes, int index, int length, int access, boolean isVolatile)
    {
        super(READWRITE, isVolatile);
        _bytes = bytes;
        setPutIndex(index + length);
        setGetIndex(index);
        _access = access;
    }

    public ByteArrayBuffer(int size)
    {
        this(new byte[size], 0, size, READWRITE);
        setPutIndex(0);
    }

    public ByteArrayBuffer(String value)
    {
        super(READWRITE,NON_VOLATILE);
        _bytes = Portable.getBytes(value);
        setGetIndex(0);
        setPutIndex(_bytes.length);
        _access=IMMUTABLE;
        _string = value;
    }

    public ByteArrayBuffer(String value,String encoding) throws UnsupportedEncodingException
    {
        super(READWRITE,NON_VOLATILE);
        _bytes = value.getBytes(encoding);
        setGetIndex(0);
        setPutIndex(_bytes.length);
        _access=IMMUTABLE;
        _string = value;
    }

    public byte[] array()
    {
        return _bytes;
    }

    public int capacity()
    {
        return _bytes.length;
    }

    public byte get()
    {
        return _bytes[_get++];
    }
    
    public byte peek(int index)
    {
        return _bytes[index];
    }

    public int peek(int index, byte[] b, int offset, int length)
    {
        int l = length;
        if (index + l > capacity()) l = capacity() - index;
        if (l <= 0) return -1;
        Portable.arraycopy(_bytes, index, b, offset, l);
        return l;
    }

    public void poke(int index, byte b)
    {
        if (isReadOnly()) throw new IllegalStateException(__READONLY);
        if (index < 0) throw new IllegalArgumentException("index<0: " + index + "<0");
        if (index > capacity())
                throw new IllegalArgumentException("index>capacity(): " + index + ">" + capacity());
        _bytes[index] = b;
    }
    
    public static class CaseInsensitive extends ByteArrayBuffer implements Buffer.CaseInsensitve
    {
        public CaseInsensitive(String s)
        {
            super(s);
        }

        public CaseInsensitive(byte[] b, int o, int l, int rw)
        {
            super(b,o,l,rw);
        }
    }


    /* ------------------------------------------------------------ */
    /** Wrap a byte array.
     * @param b
     * @param off
     * @param len
     */
    public void wrap(byte[] b, int off, int len)
    {
        if (isReadOnly()) throw new IllegalStateException(__READONLY);
        if (isImmutable()) throw new IllegalStateException(__IMMUTABLE);
        _bytes=b;
        clear();
        setGetIndex(off);
        setPutIndex(off+len);
    }

    /* ------------------------------------------------------------ */
    /** Wrap a byte array
     * @param b
     */
    public void wrap(byte[] b)
    {
        if (isReadOnly()) throw new IllegalStateException(__READONLY);
        if (isImmutable()) throw new IllegalStateException(__IMMUTABLE);
        _bytes=b;
        setGetIndex(0);
        setPutIndex(b.length);
    }

    /* ------------------------------------------------------------ */
    public void writeTo(OutputStream out)
        throws IOException
    {
        out.write(_bytes,getIndex(),length());
        clear();
    }
    
    /* ------------------------------------------------------------ */
    public int readFrom(InputStream in,int max) throws IOException
    {
        if (max<0||max>space())
            max=space();
        int p = putIndex();
        
        int len=0, total=0, available=max;
        while (total<max) 
        {
            len=in.read(_bytes,p,available);
            if (len<0)
                break;
            else if (len>0)
            {
                p += len;
                total += len;
                available -= len;
                setPutIndex(p);
            }
            if (in.available()<=0)
                break;
        }
        if (len<0 && total==0)
            return -1;
        return total;
    }
}
