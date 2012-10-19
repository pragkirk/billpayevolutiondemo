package com.extensiblejava.audit.audit2;

import com.extensiblejava.audit.*;
import java.math.*;

public class AuditFacade2 implements AuditFacade {
	public BigDecimal audit(Auditable auditable) {
		BigDecimal amount = auditable.getAmount();
		BigDecimal auditedAmount = amount.multiply(new BigDecimal("0.85"));
		return auditedAmount.setScale(2);
	}
}