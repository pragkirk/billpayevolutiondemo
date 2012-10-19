package com.extensiblejava.financial;

import java.math.*;

public class Payment {
	//Should this logic be here, or should it be in Bill and have Payable define a
	//generic getPayAmount method.
	public BigDecimal generateDraft(Payable payable) {
		if (payable.getAuditedAmount() == null) {
			return payable.getAmount();
		} else {
			return payable.getAuditedAmount();
		}
	}
}