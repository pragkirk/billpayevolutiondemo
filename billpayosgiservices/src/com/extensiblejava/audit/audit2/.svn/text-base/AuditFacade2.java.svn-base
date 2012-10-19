package com.extensiblejava.audit.audit2;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import java.util.Properties;
import com.extensiblejava.audit.*;
import java.math.*;

public class AuditFacade2 implements AuditFacade, BundleActivator {

	public void start(BundleContext context) {

		Properties props = new Properties();
		props.put("Language", "English");
		context.registerService(AuditFacade.class.getName(), this, props);
    }

    public void stop(BundleContext context) {
		// NOTE: The service is automatically unregistered.
    }

	public BigDecimal audit(Auditable auditable)  throws AuditException {
		System.out.println("USING Audit Subsystem #2 - Applying 15% discount!");
		BigDecimal amount = auditable.getAmount();
		BigDecimal auditedAmount = amount.multiply(new BigDecimal("0.85"));
		return auditedAmount.setScale(2);
	}
}