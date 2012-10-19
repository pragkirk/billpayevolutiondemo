package com.extensiblejava.mediator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import com.extensiblejava.bill.*;
import com.extensiblejava.audit.*;
import java.math.*;

//If I put this class in the same component as Bill, I've logically decoupled Bill from
//AuditFacade implementations, but have not physically decoupled them. If I put it in the UI
//I limit ability to use it in batch. Putting it in billpay.jar (ie. mediator) works well.
public class AuditFacadeFactory implements BundleActivator {
	private static AuditFacade auditor;

	public void start(BundleContext context) {

		ServiceReference ref = context.getServiceReference(AuditFacade.class.getName());
		auditor = (AuditFacade) context.getService(ref);
	 }

	 public void stop(BundleContext context) {
		// NOTE: The service is automatically unregistered.
    }

	public static AuditFacade getAuditFacade(Bill bill) {

        return auditor;

	}

}