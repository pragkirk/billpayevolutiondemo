package com.extensiblejava.bill;

import com.extensiblejava.bill.data.*;

public class DefaultBillEntityLoader implements BillEntityLoader {

	private Integer billId;

	public DefaultBillEntityLoader(Integer billId) {
		this.billId = billId;
	}

	public Bill loadBill() {
		BillDataBean billBean = BillDb.getBill(this.billId);
		return new Bill(billBean);
	}
}