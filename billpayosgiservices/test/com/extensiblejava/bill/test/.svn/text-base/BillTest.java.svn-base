package com.extensiblejava.bill.test;

import java.util.*;
import junit.framework.*;
import junit.textui.*;
import com.extensiblejava.bill.*;
import com.extensiblejava.audit.*;
import com.extensiblejava.audit.audit1.*;
import com.extensiblejava.audit.audit2.*;
import com.extensiblejava.bill.data.*;
import java.math.*;

public class BillTest extends TestCase {
	private static class BillPayerTest implements BillPayer {
		private Bill bill;
		public BillPayerTest(Bill bill) {
			this.bill = bill;
		}

		public BigDecimal generateDraft(Bill bill) {
			return bill.getAmount();
		}

		public BigDecimal getAmount() {
			return this.bill.getAmount();
		}

		public BigDecimal getAuditedAmount() {
			return this.bill.getAuditedAmount();
		}
	};

	public static void main(String[] args)
	{
		String[] testCaseName = { BillTest.class.getName() };

		junit.textui.TestRunner.main(testCaseName);
	}

	protected void setUp() {

	}

	public void testCustomerLoader() {
		Customer cust = Customer.loadCustomer(new DefaultCustomerEntityLoader(new Integer(1)));
		assertNotNull(cust.getName());

		Iterator bills = cust.getBills().iterator();
		while (bills.hasNext()) {
			assertNotNull(bills.next());
		}
	}

	public void testBillLoader() {
		Bill bill = Bill.loadBill(new BillEntityLoader() {
			public Bill loadBill() {
				return new Bill(new BillDataBean(new Integer(1), new Integer(1), "ONE", new BigDecimal("25.00"), null, null));
			}
		});
		assertNotNull(bill);
	}

	public void testAudit1() throws Exception {
		Bill bill = Bill.loadBill(new BillEntityLoader() {
			public Bill loadBill() {
				return new Bill(new BillDataBean(new Integer(1), new Integer(1), "ONE", new BigDecimal("25.00"), null, null));
			}
		});
		bill.audit(new AuditFacade1());
		BigDecimal auditedAmount = bill.getAuditedAmount();
		assertEquals(new BigDecimal("18.75"),auditedAmount);
		assertTrue(true);
	}

	public void testAudit2() throws Exception {
		Bill bill = Bill.loadBill(new BillEntityLoader() {
			public Bill loadBill() {
				return new Bill(new BillDataBean(new Integer(1), new Integer(1), "ONE", new BigDecimal("25.00"), null, null));
			}
		});
		bill.audit(new AuditFacade2());
		BigDecimal auditedAmount = bill.getAuditedAmount();
		assertEquals(new BigDecimal("21.25"),auditedAmount);
	}

	//Here, I'm no longer testing the financial piece. That's done in the financial test cases. Instead,
	//I'm only testing the pay logic associated with Bill. This is evident when looking at BillPayerTest
	//above, since the generateDraft is degenerate.
	public void testPay() {

		Bill bill = Bill.loadBill(new BillEntityLoader() {
			public Bill loadBill() {
				return new Bill(new BillDataBean(new Integer(1), new Integer(1), "ONE", new BigDecimal("25.00"), null, null));
			}
		});
		BillPayerTest payer = new BillPayerTest(bill);
		bill.pay(payer);
		assertEquals(bill.getPaidAmount(), bill.getAmount());
	}

	public void testAudit1AfterPay() throws Exception {
		Bill bill = Bill.loadBill(new BillEntityLoader() {
			public Bill loadBill() {
				return new Bill(new BillDataBean(new Integer(1), new Integer(1), "ONE", new BigDecimal("25.00"), null, null));
			}
		});
		BillPayerTest payer = new BillPayerTest(bill);
		bill.pay(payer);
		BigDecimal paidAmount = bill.getPaidAmount();
		bill.audit(new AuditFacade1());
		BigDecimal paidAmountAfter = bill.getPaidAmount();
		assertEquals(paidAmount, paidAmountAfter);
	}
}
