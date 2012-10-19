Build the rest system and then deploy the billpayrest module to the server. Note that we didn't need to modify anything when doing this.

The URLs for the service are:

localhost:8080/bill-pay-rest/rs/bills/bill/{billed}
localhost:8080/bill-pay-rest/rs/bills/audit/{billed}
localhost:8080/bill-pay-rest/rs/bills/pay/{billed}

now startup the jetty server by navigating to the ./html/jetty-6.1.7 directory. Start by doing:
	java -jar start.jar

in the browser, navigate to http://localhost:8888/newbillclient/billpay.html

startup the iOS simulator and launch safari and do the same.

Notes: 
1.) This uses JSONP to access the rest service. JSON P allows you to define a callback method that the captures the data. We do this so we don't run into the same origin policy that restricts browsers from calling a service from another domain.
2.) All calls are asynchronous.



