package com.kirkk.extensiblejava.audit.restservice;

import static spark.Spark.*;
import com.google.gson.Gson;
import com.extensiblejava.audit.audit2.AuditFacade2;
import com.extensiblejava.audit.Auditable;
import static com.kirkk.extensiblejava.audit.restservice.JsonUtil.*;
import java.math.*;
 
public class Auditor {
    public static void main(String[] args) {
    	port(getHerokuAssignedPort());
    	get("/", (req, res) -> "Welcome to the Auditor Service!");
        //get("/hello", (req, res) -> "Hello, World!");
        get("/audit", "application/json", (req, res) -> {
        	final String amount = req.queryParams("amount");
        	
        	AuditFacade2 auditor = new AuditFacade2();
        	BigDecimal auditedAmt = auditor.audit(new Auditable() {
        		public BigDecimal getAmount() { return new BigDecimal(amount); }
        	});
        	
        	return new Payment().setPayment(auditedAmt.setScale(2, BigDecimal.ROUND_UP).toString());
        	
        }, json());
        /*get("/audit", "application/json", (req, res) -> {
        	String principle = req.queryParams("amount");
        	
        	//return new Payment().setPayment(new LoanCalculator().calculatePayment(principle, rate, term));
        }, json());*/
        
        /*get("/loan", (req, res) -> {        	
        	return new Payment().setPayment(new LoanCalculator().calculatePayment("15000","6.0","60"));
        }, json());*/
    }
    
    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567; //return default port if heroku-port isn't set (i.e. on localhost)
    }
}