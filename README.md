Dojo Web Builder

Usage:

	- Clone the repo
	- run 'mvn jetty:run'
	- connect to http://localhost:8080/dwb

TODO:
	- get mvn jetty:run-war to work
	- switch to using system properties instead of the build properties file if possible
		I'd like to be able to do mvn -DdojoPath=/path/to/dojo and avoid doing the string replacement
		that we currently have to do, though this may still be required.  If so, we can trigger that
		by calling ant from within mvn or look for a different mvn plugin
	- trigger the build for dojo at the appropriate time via mvn.  Note what should happen here varies
		depending on whether you are running jetty:run, which looks to src/main/webapp for the webapp files
		and target/work/webapp when running jetty:run-war or jetty:run-exploded.
