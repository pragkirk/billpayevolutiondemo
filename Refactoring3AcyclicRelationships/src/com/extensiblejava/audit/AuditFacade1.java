package com.extensiblejava.audit;

import java.math.*;

public class AuditFacade1 implements AuditFacade {
	public BigDecimal audit(Auditable auditable) { 
		BigDecimal amount = auditable.getAmount();
		BigDecimal auditedAmount = amount.multiply(new BigDecimal("0.75"));
		return auditedAmount.setScale(2);
	}
}