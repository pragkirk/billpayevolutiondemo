package com.extensiblejava.restaudit;

import com.extensiblejava.audit.*;
import java.math.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RestAuditFacade implements AuditFacade {

	public BigDecimal audit(Auditable auditable) throws AuditException {
		try {
		
			//URL url = new URL("http://localhost:4567/audit?amount=" + auditable.getAmount().setScale(2, BigDecimal.ROUND_UP).toString());
			URL url = new URL("http://morning-woodland-59913.herokuapp.com/audit?amount=" + auditable.getAmount().setScale(2, BigDecimal.ROUND_UP).toString());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));

			String output = br.readLine();
			int begLoc = output.indexOf(":");
			int endLoc = output.indexOf("}");
			String newString = output.substring(begLoc+2,endLoc-1);
			
			//System.out.println(output);
			//System.out.println("---- " + newString + "----");

			conn.disconnect();
			
			return new BigDecimal(newString);

		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new AuditException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new AuditException();
		}

	}
}