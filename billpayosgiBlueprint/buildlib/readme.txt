-----------------------------------------------

JarAnalyzer -	Kirk Knoernschild
		www.kirkk.com
		www.extensiblejava.com
		
		This file contains very basic usage information.
		More detailed documentation can be found at
		http://www.kirkk.com/main/Main/JarAnalyzer.
-----------------------------------------------

IF YOU'VE DOWNLOADED THE SRC VERSION...
-----------------------------------------------
First, unzip the contents of the zip file to any directory you wish.

Next, you'll need to build the project. The src version includes a build.xml file (but no binaries), and requires that you have ANT installed on your machine. Assuming you have ANT installed, and ANT is in your path, you should unzip JarAnalyzer. In the root directory, simply type ant. After the build runs, you'll find the following directories have been created:

* bin - The binary distribution. If you want to run the Analyzer, navigate to this directory and execute the runxmlsummary.bat or rundotsummary.bat file. It'll prompt you for an input directory followed by an output file name. For more detail, follow the instructions related to the BIN version below.
* deploy - This directory contains the binary and source distributions of Analyzer. If you've made changes to Analyzer, you can use the zip files in this directory to deploy the binary and source distributions. Each of the zip files will contain everything needed to run or build Analyzer.

If you have created a new BIN version with modifications, and want to easily explain to others how to use the BIN distribution, read the BIN version instructions below.

-----------------------------------------------

IF YOU'VE DOWNLOADED THE BIN VERSION
-----------------------------------------------
First, unzip the contents of the zip file to any directory you wish.

- Run with XML output.
To run the analyzer, execute the runxmlsummary.bat file. It'll prompt you for an input directory and an output file. The output file is stored in xml format.
- Run with DOT output.
To run the analyzer, execute the rundotsummary.bat. This creates a dot file that can be used with GraphViz (www.graphviz.org) to create a visual representation of the component relationships. Note that when running using dot, all "-" and "." will be stripped out of the jar file names.