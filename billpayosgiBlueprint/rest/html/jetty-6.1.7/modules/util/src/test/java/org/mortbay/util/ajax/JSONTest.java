package org.mortbay.util.ajax;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.mortbay.util.ajax.JSON;
import org.mortbay.util.ajax.JSON.Output;

import junit.framework.TestCase;

public class JSONTest extends TestCase
{
    String test="\n\n\n\t\t    "+
    "// ignore this ,a [ \" \n"+
    "/* and this \n" +
    "/* and * // this \n" +
    "*/" +
    "{ "+
    "\"onehundred\" : 100  ,"+
    "\"name\" : \"fred\"  ," +
    "\"empty\" : {}  ," +
    "\"map\" : {\"a\":-1.0e2}  ," +
    "\"array\" : [\"a\",-1.0e2,[],null,true,false]  ," +
    "\"w0\":{\"class\":\"org.mortbay.util.ajax.JSONTest$Woggle\",\"name\":\"woggle0\",\"nested\":{\"class\":\"org.mortbay.util.ajax.JSONTest$Woggle\",\"name\":\"woggle1\",\"nested\":null,\"number\":101},\"number\":100}" +
    "}";
    
    public void testToString()
    {
        HashMap map = new HashMap();
        HashMap obj6 = new HashMap();
        HashMap obj7 = new HashMap();
        
        Woggle w0 = new Woggle();
        Woggle w1 = new Woggle();
        
        w0.name="woggle0";
        w0.nested=w1;
        w0.number=100;
        w1.name="woggle1";
        w1.nested=null;
        w1.number=101;
        
        map.put("n1",null);
        map.put("n2",new Integer(2));
        map.put("n3",new Double(-0.00000000003));
        map.put("n4","4\n\r\t\"4");
        map.put("n5",new Object[]{"a",new Character('b'),new Integer(3),new String[]{},null,Boolean.TRUE,Boolean.FALSE});
        map.put("n6",obj6);
        map.put("n7",obj7);
        map.put("n8",new int[]{1,2,3,4});
        map.put("n9",new JSON.Literal("[{},  [],  {}]"));
        map.put("w0",w0);
        
        obj7.put("x","value");
        
        String s = JSON.toString(map);
        assertTrue(s.indexOf("\"n1\":null")>=0);
        assertTrue(s.indexOf("\"n2\":2")>=0);
        assertTrue(s.indexOf("\"n3\":-3.0E-11")>=0);
        assertTrue(s.indexOf("\"n4\":\"4\\n")>=0);
        assertTrue(s.indexOf("\"n5\":[\"a\",\"b\",")>=0);
        assertTrue(s.indexOf("\"n6\":{}")>=0);
        assertTrue(s.indexOf("\"n7\":{\"x\":\"value\"}")>=0);
        assertTrue(s.indexOf("\"n8\":[1,2,3,4]")>=0);
        assertTrue(s.indexOf("\"n9\":[{},  [],  {}]")>=0);
        assertTrue(s.indexOf("\"w0\":{\"class\":\"org.mortbay.util.ajax.JSONTest$Woggle\",\"name\":\"woggle0\",\"nested\":{\"class\":\"org.mortbay.util.ajax.JSONTest$Woggle\",\"name\":\"woggle1\",\"nested\":null,\"number\":101},\"number\":100}")>=0);

        Gadget gadget = new Gadget();
        gadget.setShields(42);
        gadget.setWoggles(new Woggle[]{w0,w1});
        
        s = JSON.toString(new Gadget[]{gadget});
        assertTrue(s.startsWith("["));
        assertTrue(s.indexOf("\"modulated\":false")>=0);
        assertTrue(s.indexOf("\"shields\":42")>=0);
        assertTrue(s.indexOf("\"name\":\"woggle0\"")>=0);
        assertTrue(s.indexOf("\"name\":\"woggle1\"")>=0);

    }
    
    
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        JSON.registerConvertor(Gadget.class,new JSONObjectConvertor(false));
    }



    /* ------------------------------------------------------------ */
    public void testParse()
    {
        Map map = (Map)JSON.parse(test);
        assertEquals(new Long(100),map.get("onehundred"));
        assertEquals("fred",map.get("name"));
        assertTrue(map.get("array").getClass().isArray());
        assertTrue(map.get("w0") instanceof Woggle);
        assertTrue(((Woggle)map.get("w0")).nested instanceof Woggle);
        
        test="{\"data\":{\"source\":\"15831407eqdaawf7\",\"widgetId\":\"Magnet_8\"},\"channel\":\"/magnets/moveStart\",\"connectionId\":null,\"clientId\":\"15831407eqdaawf7\"}";
        map = (Map)JSON.parse(test);

        
    }

    /* ------------------------------------------------------------ */
    public void testParseReader() throws Exception
    {
        Map map = (Map)JSON.parse(new StringReader(test));
   
        assertEquals(new Long(100),map.get("onehundred"));
        assertEquals("fred",map.get("name"));
        assertTrue(map.get("array").getClass().isArray());
        assertTrue(map.get("w0") instanceof Woggle);
        assertTrue(((Woggle)map.get("w0")).nested instanceof Woggle);
        
        test="{\"data\":{\"source\":\"15831407eqdaawf7\",\"widgetId\":\"Magnet_8\"},\"channel\":\"/magnets/moveStart\",\"connectionId\":null,\"clientId\":\"15831407eqdaawf7\"}";
        map = (Map)JSON.parse(test);
    }
    
    /* ------------------------------------------------------------ */
    public void testStripComment()
    {
        String test="\n\n\n\t\t    "+
        "// ignore this ,a [ \" \n"+
        "/* "+
        "{ "+
        "\"onehundred\" : 100  ,"+
        "\"name\" : \"fred\"  ," +
        "\"empty\" : {}  ," +
        "\"map\" : {\"a\":-1.0e2}  ," +
        "\"array\" : [\"a\",-1.0e2,[],null,true,false]  ," +
        "} */";
        
        Object o = JSON.parse(test,false);
        assertTrue(o==null);
        o = JSON.parse(test,true);
        assertTrue(o instanceof Map);
        assertEquals("fred",((Map)o).get("name"));
        
    }

    /* ------------------------------------------------------------ */
    public static class Gadget 
    {
        private boolean modulated;
        private long shields;
        private Woggle[] woggles;
        /* ------------------------------------------------------------ */
        /**
         * @return the modulated
         */
        public boolean isModulated()
        {
            return modulated;
        }
        /* ------------------------------------------------------------ */
        /**
         * @param modulated the modulated to set
         */
        public void setModulated(boolean modulated)
        {
            this.modulated=modulated;
        }
        /* ------------------------------------------------------------ */
        /**
         * @return the shields
         */
        public long getShields()
        {
            return shields;
        }
        /* ------------------------------------------------------------ */
        /**
         * @param shields the shields to set
         */
        public void setShields(long shields)
        {
            this.shields=shields;
        }
        /* ------------------------------------------------------------ */
        /**
         * @return the woggles
         */
        public Woggle[] getWoggles()
        {
            return woggles;
        }
        /* ------------------------------------------------------------ */
        /**
         * @param woggles the woggles to set
         */
        public void setWoggles(Woggle[] woggles)
        {
            this.woggles=woggles;
        }
    }

    /* ------------------------------------------------------------ */
    public void testConvertor()
    {
        JSON json = new JSON();
        json.addConvertor(Date.class,new JSONDateConvertor());
        json.addConvertor(Object.class,new JSONObjectConvertor());

        Woggle w0 = new Woggle();
        Gizmo g0 = new Gizmo();
        
        w0.name="woggle0";
        w0.nested=g0;
        w0.number=100;
        g0.name="woggle1";
        g0.nested=null;
        g0.number=101;
        g0.tested=true;
        
        HashMap map = new HashMap();
        map.put("date",new Date(1));
        map.put("w0",w0);

        StringBuffer buf = new StringBuffer();
        json.append(buf,map);
        String js=buf.toString();
        
        System.err.println(js);
        assertTrue(js.indexOf("\"date\":\"Thu Jan 01 00:00:00 GMT 1970\"")>=0);
        assertTrue(js.indexOf("org.mortbay.util.ajax.JSONTest$Woggle")>=0);
        assertTrue(js.indexOf("org.mortbay.util.ajax.JSONTest$Gizmo")<0);
        assertTrue(js.indexOf("\"tested\":true")>=0);

        json.addConvertor(Date.class,new JSONDateConvertor(true));
        w0.nested=null;
        buf = new StringBuffer();
        json.append(buf,map);
        js=buf.toString();
        System.err.println(js);
        assertTrue(js.indexOf("\"date\":\"Thu Jan 01 00:00:00 GMT 1970\"")<0);
        assertTrue(js.indexOf("org.mortbay.util.ajax.JSONTest$Woggle")>=0);
        assertTrue(js.indexOf("org.mortbay.util.ajax.JSONTest$Gizmo")<0);
        
        map=(HashMap)json.parse(new JSON.StringSource(js));
        
        assertTrue(map.get("date") instanceof Date);
        assertTrue(map.get("w0") instanceof Woggle);
        
           
    }

    /* ------------------------------------------------------------ */
    public static class Gizmo
    {
        String name;
        Gizmo nested;
        long number;
        boolean tested;
        
        public String getName()
        {
            return name;
        }
        public Gizmo getNested()
        {
            return nested;
        }
        public long getNumber()
        {
            return number;
        }
        public boolean isTested()
        {
            return tested;
        }
    }
    
    /* ------------------------------------------------------------ */
    public static class Woggle extends Gizmo implements JSON.Convertible
    {
        
        public Woggle()
        {
        }
        
        public void fromJSON(Map object)
        {
            name=(String)object.get("name");
            nested=(Gizmo)object.get("nested");
            number=((Number)object.get("number")).intValue();
        }

        public void toJSON(Output out)
        {
            out.addClass(Woggle.class);
            out.add("name",name);
            out.add("nested",nested);
            out.add("number",number);
        }
        
        public String toString()
        {
            return name+"<<"+nested+">>"+number;
        }
        
    }
}
