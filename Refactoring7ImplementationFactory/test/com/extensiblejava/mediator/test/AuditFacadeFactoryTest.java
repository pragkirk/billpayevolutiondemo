package com.extensiblejava.mediator.test;

import java.util.*;
import junit.framework.*;
import junit.textui.*;
import com.extensiblejava.bill.*;
import com.extensiblejava.mediator.*;
import com.extensiblejava.bill.data.*;
import com.extensiblejava.audit.*;
import com.extensiblejava.audit.audit1.*;
import com.extensiblejava.audit.audit2.*;
import java.math.*;

public class AuditFacadeFactoryTest extends TestCase {

	public static void main(String[] args)
	{
		String[] testCaseName = { AuditFacadeFactoryTest.class.getName() };

		junit.textui.TestRunner.main(testCaseName);
	}

	protected void setUp() {

	}

	//Test the complete payment piece with BillPayerAdapter.
	public void testAuditFacadeFactory() {
		AuditFacade auditor = AuditFacadeFactory.getAuditFacade();
		assertTrue(auditor instanceof AuditFacade1);
	}
}
