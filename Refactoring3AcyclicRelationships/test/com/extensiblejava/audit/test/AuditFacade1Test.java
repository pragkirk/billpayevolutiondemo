package com.extensiblejava.audit.test;

import java.util.*;
import junit.framework.*;
import junit.textui.*;
import com.extensiblejava.audit.*;
import java.math.*;

public class AuditFacade1Test extends TestCase
{
	public static void main(String[] args)
	{
		String[] testCaseName = { AuditFacade1Test.class.getName() };

		junit.textui.TestRunner.main(testCaseName);
	}

	public void testAudit() {
		AuditFacade1 a1 = new AuditFacade1();

		BigDecimal amount = a1.audit(new Auditable() {
			public BigDecimal getAmount() { return new BigDecimal("100.00"); };
		});
		assertEquals(amount, new BigDecimal("75.00"));
	}
}
