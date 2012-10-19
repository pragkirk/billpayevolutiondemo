package com.extensiblejava.ui;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

import javax.servlet.Servlet;

import org.apache.struts.action.ActionServlet;
import org.eclipse.equinox.http.helper.BundleEntryHttpContext;
import org.eclipse.equinox.http.helper.ContextPathServletAdaptor;
import org.eclipse.equinox.jsp.jasper.JspServlet;



public class Activator implements BundleActivator {

	private ServiceTracker httpServiceTracker;

	public void start(BundleContext context) throws Exception {
		httpServiceTracker = new HttpServiceTracker(context);
		httpServiceTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		httpServiceTracker.close();
		httpServiceTracker = null;
	}

	private class HttpServiceTracker extends ServiceTracker {

		public HttpServiceTracker(BundleContext context) {
			super(context, HttpService.class.getName(), null);
		}

		public Object addingService(ServiceReference reference) {
			final HttpService httpService = (HttpService) context.getService(reference);
			try {
				HttpContext commonContext = new BundleEntryHttpContext(context.getBundle(), "/web");
				httpService.registerResources("/bill-pay", "/", commonContext); //$NON-NLS-1$ //$NON-NLS-2$

				Servlet adaptedJspServlet = new ContextPathServletAdaptor(new JspServlet(context.getBundle(), "/web"), "/bill-pay"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				httpService.registerServlet("/bill-pay/*.jsp", adaptedJspServlet, null, commonContext); //$NON-NLS-1$

				Dictionary initparams = new Hashtable();
				initparams.put("servlet-name", "action"); //Note: requires servlet-name support in Http Service Implementation
				initparams.put("config", "/WEB-INF/struts-config.xml");
				initparams.put("debug", "2");
				initparams.put("detail", "2");
				Servlet adaptedActionServlet = new ContextPathServletAdaptor(new ActionServlet(), "/bill-pay");
				httpService.registerServlet("/bill-pay/*.do", adaptedActionServlet, initparams, commonContext);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return httpService;
		}

		public void removedService(ServiceReference reference, Object service) {
			final HttpService httpService = (HttpService) service;
			httpService.unregister("/bill-pay"); //$NON-NLS-1$
			httpService.unregister("/bill-pay/*.jsp"); //$NON-NLS-1$
			httpService.unregister("/bill-pay/*.do"); //$NON-NLS-1$
			super.removedService(reference, service);
		}



	}
}
