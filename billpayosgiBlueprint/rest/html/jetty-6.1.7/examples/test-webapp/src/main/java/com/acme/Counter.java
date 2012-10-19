package com.acme;


public class Counter
{
    int counter=0;
    String last;

    public int getCount()
    {
	counter++;
	return counter;
    }

    public void setLast(String uri) {
        last=uri;
    }

    public String getLast() {
        return last;
    }

}

