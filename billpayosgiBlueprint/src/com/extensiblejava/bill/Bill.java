package com.extensiblejava.bill;

import com.extensiblejava.audit.*;
import java.math.*;
import com.extensiblejava.bill.data.*;

public class Bill implements Auditable {

	private BillDataBean billData;

	public static Bill loadBill(BillEntityLoader loader) {
		return loader.loadBill();
	}
	public Bill(BillDataBean billData) {
		this.billData = billData;
	}

	public String getBillId() {	return this.billData.getBillId().toString(); }
	public String getName() { return this.billData.getName(); }
	public BigDecimal getAmount() { return this.billData.getAmount(); }
	public BigDecimal getAuditedAmount() { return (this.billData.getAuditedAmount() == null ? null : this.billData.getAuditedAmount()); }
	public BigDecimal getPaidAmount() { return this.billData.getPaidAmount(); }
	public String getStatus() {
		if (this.billData.getPaidAmount() != null) {
			return "PAID";
		} else if (this.billData.getAuditedAmount() != null) {
			return "AUDITED";
		} else {
			return "NEW";
		}
	}

	public void audit(AuditFacade auditor) throws AuditException {
		this.billData.setAuditedAmount(auditor.audit(this));
		this.persist();
	}

	public void pay(BillPayer payer) {
		if (this.billData.getPaidAmount() == null) {
			this.billData.setPaidAmount(payer.generateDraft(this));
			this.persist();
		}
	}

	private void persist() {
		BillDb.update(billData);
	}
}