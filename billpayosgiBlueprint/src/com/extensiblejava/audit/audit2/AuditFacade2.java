package com.extensiblejava.audit.audit2;

import java.util.Properties;
import com.extensiblejava.audit.*;
import java.math.*;

public class AuditFacade2 implements AuditFacade {

	public BigDecimal audit(Auditable auditable)  throws AuditException {
		System.out.println("USING Audit Subsystem #2 - Applying 15% discount!");
		BigDecimal amount = auditable.getAmount();
		BigDecimal auditedAmount = amount.multiply(new BigDecimal("0.85"));
		return auditedAmount.setScale(2);
	}
}