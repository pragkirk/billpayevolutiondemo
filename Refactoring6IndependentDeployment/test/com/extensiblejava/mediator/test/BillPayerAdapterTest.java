package com.extensiblejava.mediator.test;

import java.util.*;
import junit.framework.*;
import junit.textui.*;
import com.extensiblejava.bill.*;
import com.extensiblejava.mediator.*;
import com.extensiblejava.financial.*;
import com.extensiblejava.bill.data.*;
import java.math.*;

public class BillPayerAdapterTest extends TestCase {

	public static void main(String[] args)
	{
		String[] testCaseName = { BillPayerAdapterTest.class.getName() };

		junit.textui.TestRunner.main(testCaseName);
	}

	protected void setUp() {

	}

	//Test the complete payment piece with BillPayerAdapter.
	public void testPay() {

		Bill bill = Bill.loadBill(new BillEntityLoader() {
			public Bill loadBill() {
				return new Bill(new BillDataBean(new Integer(1), new Integer(1), "ONE", new BigDecimal("25.00"), null, null));
			}
		});
		BillPayerAdapter payer = new BillPayerAdapter(bill);
		bill.pay(payer);
		assertEquals(bill.getPaidAmount(), bill.getAmount());
	}
}
