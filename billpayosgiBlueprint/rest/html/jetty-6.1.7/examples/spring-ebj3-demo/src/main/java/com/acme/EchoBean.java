package com.acme;


import java.util.Date;
import javax.annotation.PostConstruct;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;



@Stateless
@Remote(Echo.class)
public class EchoBean implements Echo 
{
 
    public String echo() 
    {
        return "Hello "+new Date(); 
    }
    
    @PostConstruct
    public void init() 
    {
        System.err.println("EchoBean init called");
    }
}
