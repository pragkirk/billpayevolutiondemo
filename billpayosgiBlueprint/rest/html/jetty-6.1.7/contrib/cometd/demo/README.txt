
This is a demo of the java servlet for cometd


Simplest thing to do is to run 

  mvn jetty:run

and then point TWO different instances of browsers at http://localhost:8080


There is also rudamentary support now for terracotta clustering (totally unoptimized).
Currently this needs to run within a checkout and build of Jetty and it uses an
embedded jetty server to avoid classloader issues for now.

   1) run the terracotta server(s)
   2) bin/startTerracottaNode.sh 8081
   3) bin/startTerracottaNode.sh 8082
   4) point a browser window at localhost:8081
   5) point another browser window at localhost:8082

There is a bit of latency at the moment, but we will see what optimization of the
configuraton and locks does for that.

cheers


