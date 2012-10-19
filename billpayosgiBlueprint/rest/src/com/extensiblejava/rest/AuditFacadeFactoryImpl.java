package com.extensiblejava.rest;

//import org.osgi.framework.BundleActivator;
//import org.osgi.framework.BundleContext;
//import org.osgi.framework.ServiceReference;
import com.extensiblejava.bill.*;
import com.extensiblejava.audit.*;
import java.math.*;

//If I put this class in the same component as Bill, I've logically decoupled Bill from
//AuditFacade implementations, but have not physically decoupled them. If I put it in the UI
//I limit ability to use it in batch. Putting it in billpay.jar (ie. mediator) works well.
public class AuditFacadeFactoryImpl  {
	//private static AuditFacade auditFacade;
	//private static AuditFacadeFactoryImpl auditFactory;
	
	//private AuditFacadeFactoryImpl() { }
	
	public void start() throws Exception { }
	public void stop() throws Exception { }
	
	/*public static AuditFacadeFactoryImpl getInstance(AuditFacade auditFacade) { 
		if (auditFacade == null) {
			auditFactory = new AuditFacadeFactoryImpl();	
			auditFactory.setAuditFacade(auditFacade);
			return auditFactory;
		} else {
			return auditFactory;
		}
	}
	
	public static AuditFacadeFactoryImpl getInstance() {
		return auditFactory;
	}*/
	
	public void setAuditFacade(AuditFacade auditFacade) {
		/*if (auditFacade == null) { 
			System.out.println("audit facade null"); 
		} else {
			System.out.println("setting auditfacade");
		}
		auditFacade = auditFacade;*/
		RestTest.setAuditor(auditFacade);
	}
		
	//public static AuditFacade getAuditFacade(Bill bill) { return auditFacade;}

	/* public void start(BundleContext context) {

		ServiceReference ref = context.getServiceReference(AuditFacade.class.getName());
		auditor = (AuditFacade) context.getService(ref);
	 }

	 public void stop(BundleContext context) {
		// NOTE: The service is automatically unregistered.
    }

	public static AuditFacade getAuditFacade(Bill bill) {

        return auditor;

	}*/ 

}