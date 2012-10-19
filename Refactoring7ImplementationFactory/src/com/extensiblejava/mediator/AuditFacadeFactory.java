package com.extensiblejava.mediator;

import com.extensiblejava.bill.*;
import com.extensiblejava.audit.audit1.*;
import com.extensiblejava.audit.audit2.*;
import com.extensiblejava.audit.*;
import java.math.*;

//If I put this class in the same component as Bill, I've logically decoupled Bill from
//AuditFacade implementations, but have not physically decoupled them. If I put it in the UI
//I limit ability to use it in batch. Putting it in billpay.jar (ie. mediator) works well.
public class AuditFacadeFactory {
	public static AuditFacade getAuditFacade(Bill bill) {
		BigDecimal amount = bill.getAmount();
		int val = amount.compareTo(new BigDecimal("1000.00"));
		if (val == -1) {
			return new AuditFacade2();
		} else {
			return new AuditFacade1();
		}

	}

}