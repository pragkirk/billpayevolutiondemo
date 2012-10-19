package com.extensiblejava.mediator;

import com.extensiblejava.financial.*;
import com.extensiblejava.bill.*;
import java.math.*;

//Escalate the dependency upon financial to BillPayerAdapter.
public class BillPayerAdapter implements BillPayer, Payable {
	private Bill bill;
	public BillPayerAdapter(Bill bill) {
		this.bill = bill;
	}

	public BigDecimal generateDraft(Bill bill) {
		Payment payer = new Payment();
		return payer.generateDraft(this);
	}

	public BigDecimal getAmount() {
		return this.bill.getAmount();
	}

	public BigDecimal getAuditedAmount() {
		return this.bill.getAuditedAmount();
	}
}