package com.extensiblejava.financial.test;

import java.util.*;
import junit.framework.*;
import junit.textui.*;
import com.extensiblejava.financial.*;
import java.math.*;

public class FinancialTest extends TestCase
{
	public static void main(String[] args)
	{
		String[] testCaseName = { FinancialTest.class.getName() };

		junit.textui.TestRunner.main(testCaseName);
	}


	public void testPayment() {
		Payment payment = new Payment();
		BigDecimal paidAmount = payment.generateDraft(new Payable() {
			public BigDecimal getAuditedAmount() {
				return null;
			}

			public BigDecimal getAmount() {
				return new BigDecimal("100.00");
			}
		});

		assertEquals(paidAmount, new BigDecimal("100.00"));
	}

}
