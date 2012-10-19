package com.acme;

import javax.ejb.Remote;

@Remote
public interface Echo
{
 public String echo();
}
