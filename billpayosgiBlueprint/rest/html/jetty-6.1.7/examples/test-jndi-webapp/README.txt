After Building
--------------
After you have built the example, you will need to edit a couple
of files to uncomment the appropriate  configuration for the
transaction manager you are using.

+ edit contexts/test-jndi.xml and uncomment one of the transaction
  manager setups.

+ edit contexts/test-jndi.d/WEB-INF/jetty-env.xml and uncomment
  one of the transaction manager setups.


Running the Demo
----------------
You will need to copy a derby.jar to the jetty lib/ directory, as well
as copy all the necessary jars for the flavour of transaction manager
you are using. There are instructions for some of the popular 
transaction managers on the wiki at:

http://docs.codehaus.org/display/JETTY/Jetty+User+Guides

You run the demo like so:
   
   java -jar start.jar 


Adding Support for a Different Transaction Manager
--------------------------------------------------

1. Edit the src/etc/templates/filter.properties file and add
   a new set of token and replacement strings following the
   pattern established for ATOMIKOS and JOTM.

2. Edit the src/etc/templates/jetty-env.xml file and add
   configuration for new transaction manager following the
   pattern established for the other transaction managers.

3. Edit the src/etc/templates/jetty-test-jndi.xml file and
   add configuration for the new transaction manager following
   the pattern established for the other transaction managers.
