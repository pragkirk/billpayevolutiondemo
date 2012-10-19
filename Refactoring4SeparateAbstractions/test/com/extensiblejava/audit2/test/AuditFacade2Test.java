package com.extensiblejava.audit2.test;

import java.util.*;
import junit.framework.*;
import junit.textui.*;
import com.extensiblejava.audit.audit2.*;
import com.extensiblejava.audit.*;
import java.math.*;

public class AuditFacade2Test extends TestCase
{
	public static void main(String[] args)
	{
		String[] testCaseName = { AuditFacade2Test.class.getName() };

		junit.textui.TestRunner.main(testCaseName);
	}

	public void testAudit() {
		AuditFacade2 a1 = new AuditFacade2();

		BigDecimal amount = a1.audit(new Auditable() {
			public BigDecimal getAmount() { return new BigDecimal("100.00"); };
		});
		assertEquals(amount, new BigDecimal("85.00"));
	}
}
