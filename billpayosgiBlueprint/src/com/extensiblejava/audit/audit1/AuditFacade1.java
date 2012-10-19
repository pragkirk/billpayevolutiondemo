package com.extensiblejava.audit.audit1;

import java.util.Properties;
import com.extensiblejava.audit.*;
import java.math.*;

public class AuditFacade1 implements AuditFacade {

	public BigDecimal audit(Auditable auditable)  throws AuditException {
		System.out.println("USING Audit Subsystem #1 - Applying 25% discount!");
		BigDecimal amount = auditable.getAmount();
		BigDecimal auditedAmount = amount.multiply(new BigDecimal("0.75"));
		return auditedAmount.setScale(2);
	}
}