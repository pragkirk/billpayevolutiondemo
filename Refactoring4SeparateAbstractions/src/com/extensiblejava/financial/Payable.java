package com.extensiblejava.financial;

import java.math.*;

public interface Payable {
	public BigDecimal getAmount();
	public BigDecimal getAuditedAmount();

}