package com.acme;

import javax.ejb.EJB;

public class EchoTest
{

  @EJB private static EchoBean echoBean;

  public static String echo ()
  {
      return echoBean.echo();
  }

};
