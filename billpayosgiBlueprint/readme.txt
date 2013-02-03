Main changes from No Services - 

Service enabled AuditFacade1, AuditFacade2, and AuditFacadeFactory using Spring DM. Spring DM now manages the service references so it updates for me when I disable/enable different auditors. No need to refresh the billpay module like I had to when coding my own Activators.

The rest directory also include an example that shows how we can build a new API atop the existing module and REST enable it. In the rest/html directory, we build a new HTML only client using JSONP that is responsive and renders well on desktop and mobile devices. To test this, launch an mobile device simulator after starting jetty. See the readme.txt in that directory for more information.

To use billpaybuild.xml, use the following commands.

ant -f billpaybuild.xml 		#Compiles and runs JarAnalyzer

ant clean -f billpaybuild.xml	#Cleans the project

ant -Dmodule=auditspec deploy -f billpaybuild.xml #Deploys auditspec.jar. Substitute other module names for auditspec

ant -Dmodule=auditspec undeploy -f billpaybuild.xml #Undeployes auditspec.jar. Substitute other module names for auditspec.
