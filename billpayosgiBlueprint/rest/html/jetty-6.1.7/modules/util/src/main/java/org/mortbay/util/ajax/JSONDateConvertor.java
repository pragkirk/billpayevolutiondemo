package org.mortbay.util.ajax;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.mortbay.log.Log;
import org.mortbay.util.DateCache;
import org.mortbay.util.ajax.JSON.Output;

/* ------------------------------------------------------------ */
/**
* Convert a {@link Date} to JSON.
* If fromJSON is true in the constructor, the JSON generated will
* be of the form {class="java.util.Date",value="1/1/1970 12:00 GMT"}
* If fromJSON is false, then only the string value of the date is generated.
*/
public class JSONDateConvertor implements JSON.Convertor
{
    private boolean _fromJSON;
    DateCache _dateCache;
    SimpleDateFormat _format;

    public JSONDateConvertor()
    {
        this(false);
    }

    public JSONDateConvertor(boolean fromJSON)
    {
        this(DateCache.DEFAULT_FORMAT,TimeZone.getTimeZone("GMT"),fromJSON);
    }
    
    public JSONDateConvertor(String format,TimeZone zone,boolean fromJSON)
    {
        _dateCache=new DateCache(format);
        _dateCache.setTimeZone(zone);
        _fromJSON=fromJSON;
        _format=new SimpleDateFormat(format);
        _format.setTimeZone(zone);
    }
    
    public Object fromJSON(Map map)
    {
        if (!_fromJSON)
            throw new UnsupportedOperationException();
        try
        {
            synchronized(_format)
            {
                return _format.parseObject((String)map.get("value"));
            }
        }
        catch(Exception e)
        {
            Log.warn(e);  
        }
        return null;
    }

    public void toJSON(Object obj, Output out)
    {
        String date = _dateCache.format((Date)obj);
        if (_fromJSON)
        {
            out.addClass(obj.getClass());
            out.add("value",date);
        }
        else
        {
            out.add(date);
        }
    }

}
