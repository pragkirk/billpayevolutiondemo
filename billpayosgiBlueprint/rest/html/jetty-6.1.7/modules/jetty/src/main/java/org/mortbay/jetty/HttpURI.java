//========================================================================
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty;

import java.io.UnsupportedEncodingException;

import org.mortbay.util.MultiMap;
import org.mortbay.util.StringUtil;
import org.mortbay.util.TypeUtil;
import org.mortbay.util.URIUtil;
import org.mortbay.util.UrlEncoded;


/* ------------------------------------------------------------ */
/** Http URI.
 * Parse a HTTP URI from a string or byte array.  Given a URI
 * <code>http://user@host:port/path/info;param?query#fragment</code>
 * this class will split it into the following undecoded optional elements:<ul>
 * <li>{@link #getScheme()} - http:</li>
 * <li>{@link #getAuthority()} - //name@host:port</li>
 * <li>{@link #getHost()} - host</li>
 * <li>{@link #getPort()} - port</li>
 * <li>{@link #getPath()} - /path/info</li>
 * <li>{@link #getParam()} - param</li>
 * <li>{@link #getQuery()} - query</li>
 * <li>{@link #getFragment()} - fragment</li>
 * </ul>
 * 
 */
public class HttpURI
{
    private static byte[] __empty={}; 
    private final static int 
    START=0,
    AUTH_OR_PATH=1,
    SCHEME_OR_PATH=2,
    AUTH=4,
    PORT=5,
    PATH=6,
    PARAM=7,
    QUERY=8;
    
    boolean _partial=false;
    byte[] _raw=__empty;
    String _rawString;
    int _scheme;
    int _authority;
    int _host;
    int _port;
    int _path;
    int _param;
    int _query;
    int _fragment;
    int _end;
    
    public HttpURI()
    {
        
    } 
    
    /* ------------------------------------------------------------ */
    /**
     * @param parsePartialAuth If True, parse auth without prior scheme, else treat all URIs starting with / as paths
     */
    public HttpURI(boolean parsePartialAuth)
    {
        _partial=parsePartialAuth;
    }
    
    public HttpURI(String raw)
    {
        _rawString=raw;
        byte[] b = raw.getBytes();
        parse(b,0,b.length);
    }
    
    public HttpURI(byte[] raw,int offset, int length)
    {
        parse2(raw,offset,length);
    }
    
    public void parse(String raw)
    {
        byte[] b = raw.getBytes();
        parse2(b,0,b.length);
        _rawString=raw;
    }
    
    public void parse(byte[] raw,int offset, int length)
    {
        _rawString=null;
        parse2(raw,offset,length);
    }
    
    private void parse2(byte[] raw,int offset, int length)
    {
        _raw=raw;
        int i=offset;
        int e=offset+length;
        int state=START;
        int m=offset;
        _end=offset+length;
        _scheme=offset;
        _authority=offset;
        _host=offset;
        _port=offset;
        _path=offset;
        _param=_end;
        _query=_end;
        _fragment=_end;
        while (i<e)
        {
            char c=(char)(0xff&_raw[i]);
            int s=i++;
            
            switch (state)
            {
                case START:
                {
                    m=s;
                    if (c=='/')
                    {
                        state=AUTH_OR_PATH;
                    }
                    else if (Character.isLetterOrDigit(c))
                    {
                        state=SCHEME_OR_PATH;
                    }
                    else if (c==';')
                    {
                        _param=s;
                        state=PARAM;
                    }
                    else if (c=='?')
                    {
                        _param=s;
                        _query=s;
                        state=QUERY;
                    }
                    else if (c=='#')
                    {
                        _param=s;
                        _query=s;
                        _fragment=s;
                        break;
                    }
                    else
                        throw new IllegalArgumentException(new String(_raw,offset,length));
                    
                    continue;
                }

                case AUTH_OR_PATH:
                {
                    if ((_partial||_scheme!=_authority) && c=='/')
                    {
                        _host=i;
                        _port=_end;
                        _path=_end;
                        state=AUTH;
                    }
                    else if (c==';' || c=='?' || c=='#')
                    {
                        i--;
                        state=PATH;
                    }  
                    else
                    {
                        _host=m;
                        _port=m;
                        state=PATH;
                    }  
                    continue;
                }
                
                case SCHEME_OR_PATH:
                {
                    // short cut for http and https
                    if (length>6 && c=='t')
                    {
                        if (_raw[offset+3]==':')
                        {
                            s=offset+3;
                            i=offset+4;
                            c=':';
                        }
                        else if (_raw[offset+4]==':')
                        {
                            s=offset+4;
                            i=offset+5;
                            c=':';
                        }
                        else if (_raw[offset+5]==':')
                        {
                            s=offset+5;
                            i=offset+6;
                            c=':';
                        }
                    }
                    
                    
                    if (c==':')
                    {
                        m=i++;
                        _authority=m;
                        _path=m;
                        c=(char)(0xff&_raw[i]);
                        if (c=='/')
                            state=AUTH_OR_PATH;
                        else 
                        {
                            _host=m;
                            _port=m;
                            state=PATH;
                        }
                    }
                    else if (c=='/')
                    {
                        state=PATH;
                    }
                    else if (c==';')
                    {
                        _param=s;
                        state=PARAM;
                    }
                    else if (c=='?')
                    {
                        _param=s;
                        _query=s;
                        state=QUERY;
                    }
                    else if (c=='#')
                    {
                        _param=s;
                        _query=s;
                        _fragment=s;
                        break;
                    }
                    continue;
                }
                
                case AUTH:
                {
                    if (c=='/')
                    {
                        m=s;
                        _path=m;
                        _port=_path;
                        state=PATH;
                    }
                    else if (c=='@')
                    {
                        _host=i;
                    }
                    else if (c==':')
                    {
                        _port=s;
                        state=PORT;
                    }
                    continue;
                }
                
                case PORT:
                {
                    if (c=='/')
                    {
                        m=s;
                        _path=m;
                        if (_port<=_authority)
                            _port=_path;
                        state=PATH;
                    }
                    continue;
                }
                
                case PATH:
                {
                    if (c==';')
                    {
                        _param=s;
                        state=PARAM;
                    }
                    else if (c=='?')
                    {
                        _param=s;
                        _query=s;
                        state=QUERY;
                    }
                    else if (c=='#')
                    {
                        _param=s;
                        _query=s;
                        _fragment=s;
                        break;
                    }
                    continue;
                }
                
                case PARAM:
                {
                    if (c=='?')
                    {
                        _query=s;
                        state=QUERY;
                    }
                    else if (c=='#')
                    {
                        _query=s;
                        _fragment=s;
                        break;
                    }
                    continue;
                }
                
                case QUERY:
                {
                    if (c=='#')
                    {
                        _fragment=s;
                        break;
                    }
                    continue;
                }
                
            }
        }
    }
    
    
    public String getScheme()
    {
        if (_scheme==_authority)
            return null;
        int l=_authority-_scheme;
        if (l==5 && 
            _raw[_scheme]=='h' && 
            _raw[_scheme+1]=='t' && 
            _raw[_scheme+2]=='t' && 
            _raw[_scheme+3]=='p' )
            return HttpSchemes.HTTP;
        if (l==6 && 
            _raw[_scheme]=='h' && 
            _raw[_scheme+1]=='t' && 
            _raw[_scheme+2]=='t' && 
            _raw[_scheme+3]=='p' && 
            _raw[_scheme+4]=='s' )
            return HttpSchemes.HTTPS;
        return new String(_raw,_scheme,_authority-_scheme-1);
    }
    
    public String getAuthority()
    {
        if (_authority==_path)
            return null;
        return new String(_raw,_authority,_path-_authority);
    }
    
    public String getHost()
    {
        if (_host==_port)
            return null;
        return new String(_raw,_host,_port-_host);
    }
    
    public int getPort()
    {
        if (_port==_path)
            return -1;
        return TypeUtil.parseInt(_raw, _port+1, _path-_port-1,10);
    }
    
    public String getPath()
    {
        if (_path==_param)
            return null;
        return StringUtil.toString(_raw,_path,_param-_path,URIUtil.__CHARSET);
    }
    
    public String getDecodedPath()
    {
        if (_path==_param)
            return null;
        return URIUtil.decodePath(_raw,_path,_param-_path);
    }
    
    public String getPathAndParam()
    {
        if (_path==_query)
            return null;
        return StringUtil.toString(_raw,_path,_query-_path,URIUtil.__CHARSET);
    }
    
    public String getCompletePath()
    {
        if (_path==_end)
            return null;
        return StringUtil.toString(_raw,_path,_end-_path,URIUtil.__CHARSET);
    }
    
    public String getParam()
    {
        if (_param==_query)
            return null;
        return StringUtil.toString(_raw,_param+1,_query-_param-1,URIUtil.__CHARSET);
    }
    
    public String getQuery()
    {
        if (_query==_fragment)
            return null;
        return StringUtil.toString(_raw,_query+1,_fragment-_query-1,URIUtil.__CHARSET);
    }
    
    public String getQuery(String encoding)
    {
        if (_query==_fragment)
            return null;
        return StringUtil.toString(_raw,_query+1,_fragment-_query-1,encoding==null?URIUtil.__CHARSET:encoding);
    }
    
    public String getFragment()
    {
        if (_fragment==_end)
            return null;
        return new String(_raw,_fragment+1,_end-_fragment-1);
    }

    public void decodeQueryTo(MultiMap parameters, String encoding) 
        throws UnsupportedEncodingException
    {
        if (_query==_fragment)
            return;
       
        if (encoding==null)
            encoding=URIUtil.__CHARSET;
        
        if (StringUtil.isUTF8(encoding))
            UrlEncoded.decodeUtf8To(_raw,_query+1,_fragment-_query-1,parameters);
        else
            UrlEncoded.decodeTo(StringUtil.toString(_raw,_query+1,_fragment-_query-1,encoding),parameters,encoding);
    }

    public void clear()
    {
        _scheme=_authority=_host=_port=_path=_param=_query=_fragment=_end=0;
        _raw=__empty;
        _rawString="";
    }
    
    public String toString()
    {
        if (_rawString==null)
            _rawString=new String(_raw,_scheme,_end-_scheme);
        return _rawString;
    }
    
}
