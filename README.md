Dojo Web Builder

Usage (jetty): 

	- Clone the repo
	- run 'mvn -Ddojo.source=/some/path/to/dojo-source jetty:run'
	- connect to http://localhost:8080/dwb

Usage (tomcat-embedded): 

	- Clone the repo
	- run 'mvn -Ddojo.source=/some/path/to/dojo-source tomcat:run'
	- connect to http://localhost:8080/dwb

Usage (tomcat-remote): 

	- Clone the repo
    - Edit ~/.m2/settings.xml and fill out server details for server id, "dwb"
	- run 'mvn -Ddojo.source=/some/path/to/dojo-source tomcat:deploy'
	- connect to http://remote.server:8080/dwb

Profiles (set using -P):

    - standard (default):
        quick start using optimised client-side code layer, debug mode turned off

    - release: 
        same as standard profile but triggers a full-rebuild of the client-side code, 

    - dev: 
        debug mode turned on, uses client-side modules from source, uncompressed and unoptimised.

Mandatory Parameters (set using -D):
    
    - dojo.source: location for the source release for the latest Dojo toolkit.

Optional Parameters (set using -D):
    
    - dojo.cachepath: directory to use as the build result cache, if not specified appliation will
        create and use a temporary directory.

TODO:

    - Remove all handled dependencies from WEB-INF/lib directory, try using public WINK JAR rather 
        than IBM's version.

    - Add an optional parameter, dojo.version, to switch the default version of dojo served by the
        backend API, which would cause front-end app to show modules for this version. Would need to 
        set location value in the package meta-data for that version and set this module as the
        default.

    - Set up maven dojo dependencies so that if source location isn't set, it's pulled down 
        automatically. 
