package org.mortbay.terracotta.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mortbay.util.LazyList;


class ConcurrentMultiMap extends ConcurrentHashMap
{

    public void add(Object name, Object value) 
    {
        Object lo = super.get(name);
        Object ln = LazyList.add(lo,value);
        if (lo!=ln)
            super.put(name,ln);
    }
    
    public void addValues(Object name, List values) 
    {
        Object lo = super.get(name);
        Object ln = LazyList.addCollection(lo,values);
        if (lo!=ln)
            super.put(name,ln);
    }
    public void addValues(Object name, String[] values) 
    {
        Object lo = super.get(name);
        Object ln = LazyList.addCollection(lo,Arrays.asList(values));
        if (lo!=ln)
            super.put(name,ln);
    }
    
    public Object get(Object name) 
    {
        Object l=super.get(name);
        switch(LazyList.size(l))
        {
          case 0:
              return null;
          case 1:
              Object o=LazyList.get(l,0);
              return o;
          default:
              return LazyList.getList(l,true);
        }
    }
    public String getString(Object name)
    {
        Object l=super.get(name);
        switch(LazyList.size(l))
        {
          case 0:
              return null;
          case 1:
              Object o=LazyList.get(l,0);
              return o==null?null:o.toString();
          default:
              StringBuffer values=new StringBuffer(128);
              synchronized(values)
              {
                  for (int i=0; i<LazyList.size(l); i++)              
                  {
                      Object e=LazyList.get(l,i);
                      if (e!=null)
                      {
                          if (values.length()>0)
                              values.append(',');
                          values.append(e.toString());
                      }
                  }   
                  return values.toString();
              }
        }
    }
    
    public Object getValue(Object name,int i)
    {
        Object l=super.get(name);
        if (i==0 && LazyList.size(l)==0)
            return null;
        return LazyList.get(l,i);
    }
    
    public List getValues(Object name)
    {
        return LazyList.getList(super.get(name),true);
    }
    
    public Object put(Object name, Object value) 
    {
        return super.put(name,LazyList.add(null,value));
    }
    public void putAll(Map m)
    {
        Iterator i = m.entrySet().iterator();
        boolean multi=m instanceof ConcurrentMultiMap;
        while(i.hasNext())
        {
            Map.Entry entry =
                (Map.Entry)i.next();
            if (multi)
                super.put(entry.getKey(),LazyList.clone(entry.getValue()));
            else
                put(entry.getKey(),entry.getValue());
        }
    }
    
    public Object putValues(Object name, List values) 
    {
        return super.put(name,values);
    }
    
    public boolean removeValue(Object name,Object value)
    {
        Object lo = super.get(name);
        Object ln=lo;
        int s=LazyList.size(lo);
        if (s>0)
        {
            ln=LazyList.remove(lo,value);
            if (ln==null)
                super.remove(name);
            else
                super.put(name, ln);
        }
        return LazyList.size(ln)!=s;
    }
    
    public Map toStringArrayMap()
    {
        HashMap map = new HashMap(size()*3/2);
        
        Iterator i = super.entrySet().iterator();
        while(i.hasNext())
        {
            Map.Entry entry = (Map.Entry)i.next();
            Object l = entry.getValue();
            String[] a = LazyList.toStringArray(l);
            map.put(entry.getKey(),a);
        }
        return map;
    }
    
}