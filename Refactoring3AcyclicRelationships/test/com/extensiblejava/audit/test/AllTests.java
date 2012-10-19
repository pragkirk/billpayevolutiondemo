package com.extensiblejava.audit.test;

import junit.framework.*;
import junit.textui.*;

public class AllTests extends TestCase {

	public AllTests(String name) {
		super(name);
	}

	public static void main(java.lang.String[] args) {
		junit.textui.TestRunner.run(AllTests.suite());
	}

	public static Test suite() {
		TestSuite packageTests = new TestSuite(AllTests.class.getName());
		packageTests.addTestSuite(AuditFacade1Test.class);

		return packageTests;

	}

}