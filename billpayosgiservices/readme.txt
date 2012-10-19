Main changes from No Services - 

Service enabled AuditFacade1, AuditFacade2, and AuditFacadeFactory. This required changing the .bnd files for the bundles to define a Bundle-Activator. By service enabling these bundles, a dependency has been created on org.osgi.framework from these bundles.

Also had to comment out the AuditFacadeFactoryTest due to dependency of of billpay.jar on osgi. I commented this out in AllTests.

I tried to *not* create a Bundle-Activator for AuditFacadeFactory, and instead pass in a BundleContest, but couldn't get it to work.