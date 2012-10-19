package com.extensiblejava.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.codehaus.jettison.json.*;
import com.sun.jersey.api.json.JSONWithPadding;

import java.util.*;

import com.extensiblejava.bill.*;
import com.extensiblejava.bill.data.*;
import com.extensiblejava.audit.*;
import com.extensiblejava.mediator.*;

@Path("/bills")
public class RestTest {
	private static AuditFacade auditFacade;
	
	@Path("{custid}")
	@GET
	@Produces("application/x-javascript")
	public JSONWithPadding showBills(@PathParam("custid") int id, @QueryParam("callback") String callback) throws Exception {
	  // Return all Bills.
		Customer customer = Customer.loadCustomer(new DefaultCustomerEntityLoader(new Integer(id)));
		String customerName = customer.getName().getFullName();
		List bills = customer.getBills();
		Iterator i = bills.iterator();
		JSONObject custObj = new JSONObject();
		custObj.put("customer", customerName);
		while (i.hasNext()) {
			//billsObj.JSONObject billObj = new JSONObject();
			try {
				Bill bill = (Bill) i.next();
				String billID = bill.getBillId();
				String billName = bill.getName();
				custObj.accumulate("billid", billID);
				custObj.accumulate("billname", billName);
				} catch (JSONException e) {
					throw e;
				}
		}
		return new JSONWithPadding(custObj,callback);
		//return custObj.toString();
		/*String xmlString = "<customerbills id=\""+ id + "\">";
		xmlString = xmlString + "<link rel=\"self\" href=\"/bills/" + id + "\" />";
		while (i.hasNext()) {
			Bill bill = (Bill) i.next();
			String billID = bill.getBillId();
			xmlString = xmlString + "<bill>" + billID + "</bill>";		
		}
		xmlString = xmlString + "</customerbills>";
		return xmlString;*/
	  }
	
	@Path("/bill/{billid}")
	@GET
	@Produces("application/x-javascript")
	public JSONWithPadding showBillDetail(@PathParam("billid") int id, @QueryParam("callback") String callback) throws Exception {
	  // Return Bill detail.
		Bill bill = Bill.loadBill(new DefaultBillEntityLoader(new Integer(id)));
		String name = bill.getName();
		String amount = (bill.getAmount() == null ? "" : bill.getAmount().toString());
		String status = bill.getStatus();
		String auditedAmount = (bill.getAuditedAmount() == null ? "not audited" : bill.getAuditedAmount().toString());
		String paidAmount = (bill.getPaidAmount() == null ? "not paid" : bill.getPaidAmount().toString());
		
		JSONObject billObj = new JSONObject();
		billObj.put("name", name);
		billObj.put("amount",amount);
		billObj.put("auditedamount",auditedAmount);
		billObj.put("paidamount", paidAmount);
		billObj.put("status", status);
		
		//return billObj.toString();
		return new JSONWithPadding(billObj, callback);
	  }
	
	@Path("/audit/{billid}")
	@GET
	@Produces("application/x-javascript")
	public JSONWithPadding auditBill(@PathParam("billid") int id, @QueryParam("callback") String callback) throws Exception {
	  // Return Bill detail.
		Bill bill = Bill.loadBill(new DefaultBillEntityLoader(new Integer(id)));
		bill.audit(auditFacade);
		String name = bill.getName();
		String amount = (bill.getAmount() == null ? "" : bill.getAmount().toString());
		String status = bill.getStatus();
		String auditedAmount = (bill.getAuditedAmount() == null ? "not audited" : bill.getAuditedAmount().toString());
		String paidAmount = (bill.getPaidAmount() == null ? "not paid" : bill.getPaidAmount().toString());

		JSONObject billObj = new JSONObject();
		billObj.put("name", name);
		billObj.put("amount",amount);
		billObj.put("auditedamount",auditedAmount);
		billObj.put("paidamount", paidAmount);
		billObj.put("status", status);

		//return billObj.toString();
		return new JSONWithPadding(billObj, callback);
	  }
	
	@Path("/pay/{billid}")
	@GET
	@Produces("application/x-javascript")
	public JSONWithPadding payBill(@PathParam("billid") int id, @QueryParam("callback") String callback) throws Exception {
	  // Return Bill detail.
		Bill bill = Bill.loadBill(new DefaultBillEntityLoader(new Integer(id)));
		BillPayerAdapter billAdapter = new BillPayerAdapter(bill);
		bill.pay(billAdapter);
		
		String name = bill.getName();
		String amount = (bill.getAmount() == null ? "" : bill.getAmount().toString());
		String status = bill.getStatus();
		String auditedAmount = (bill.getAuditedAmount() == null ? "not audited" : bill.getAuditedAmount().toString());
		String paidAmount = (bill.getPaidAmount() == null ? "not paid" : bill.getPaidAmount().toString());

		JSONObject billObj = new JSONObject();
		billObj.put("name", name);
		billObj.put("amount",amount);
		billObj.put("auditedamount",auditedAmount);
		billObj.put("paidamount", paidAmount);
		billObj.put("status", status);

		//return billObj.toString();
		return new JSONWithPadding(billObj, callback);
	  }
	
	public static void setAuditor(AuditFacade facade) {
		auditFacade = facade;
	}

  /*@Path("{custid}")
  @GET
  @Produces("application/xml")
  public String showBills(@PathParam("custid") int id) {
    // Return all Bills.
	Customer customer = Customer.loadCustomer(new DefaultCustomerEntityLoader(new Integer(1)));
	List bills = customer.getBills();
	Iterator i = bills.iterator();
	String xmlString = "<customerbills id=\""+ id + "\">";
	xmlString = xmlString + "<link rel=\"self\" href=\"/bills/" + id + "\" />";
	while (i.hasNext()) {
		Bill bill = (Bill) i.next();
		String billID = bill.getBillId();
		xmlString = xmlString + "<bill>" + billID + "</bill>";		
	}
	xmlString = xmlString + "</customerbills>";
	return xmlString;
  }*/

  /*@Path("{id}")
  @GET
  @Produces("application/xml")
  public String showDepartment(@PathParam("id") int id) {
    // Return department by ID.
    return "<department id=\"" + id + "\">"
      + "<link rel=\"self\" href=\"/departments/" + id + "\" />"
      + "<name>Solutions Development</name>"
      + "</department>";
  }*/
}
