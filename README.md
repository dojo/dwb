Dojo Web Builder
================

The Dojo Web Builder provides a web interface to the dojo build system,
allowing a user to generate custom toolkit builds using just their web browser. 

This project contains the source code behind the hosted solution provided by
The Dojo Toolkit at [http://build.dojotoolkit.org](http://build.dojotoolkit.org).

The project is divided into two logical components, a front-end Dojo application
and the backend RESTful interface to the build system. For usage details see the 
sections below. 

Usage
-----

Maven is used as the build manager for the project. It has been configured for
quickstart with minimal configuration using either embedded Jetty or Tomcat
plugins, see details below. 

To connect the application, once running, point your web browser 
at [http://localhost:8080/)](http://localhost:8080/).

In addition, the application is not tied to these
platforms, allowing you to build a WAR file and deploy to the application
server of your choice. 

### Quickstart with Jetty
	$ mvn -Ddojo.source=/some/path/to/dojo-source jetty:run

### Quickstart with Tomcat
	$ mvn -Ddojo.source=/some/path/to/dojo-source tomcat:run

### Quickstart with Jetty, using custom Dojo release
	$ mvn -Ddojo.source=/some/path/to/dojo-source -Ddojo.version=1.5.0 jetty:run

### Quickstart with Jetty, setting build cache path 
    $ mvn -Dcachepath=/tmp/ -Ddojo.source=/some/path/to/dojo-source jetty:run

### Deploying to remote Tomcat server
    Edit ~/.m2/settings.xml and fill out server details for server id, "dwb"
	$ mvn -Ddojo.source=/some/path/to/dojo-source tomcat:deploy

Configuration 
-----

Configurable application parameters are set using Java system properties. Users 
can modify these values using the -D command line argument. _The only mandatory 
property is the dojo source location_. 

### Mandatory Parameters 

* _dojo.source_ - Directory location for the source release of the Dojo Toolkit
  version you want to build.

### Optional Parameters 

* _dojo.version_ - Change the version of the Dojo Toolkit being exposed by the
project. Currently supported versions are: 1.6.0 (default), 1.5.0. 

* _dwbcachepath_ - Specify the custom build cache directory. By default, this
is a different temporary directory each time the application is started. 

Development notes
---

### Layout 

The project uses Maven's 
[http://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html](standard directory layout) 
to organise source files. Front-end application code lives in _src/main/webapp_ and the 
backend API code lives in _src/main/java_. 

### Profiles

Maven profiles are used to assist with development, details below. To change the 
current profile, use the -PprofileName parameter when running the application. 

* _standard (default)_ Quick start using optimised client-side code layer, debug mode turned off.

* _release_ Same as _standard_ but triggers a full-rebuild of the client-side code, copying generated
resources to JS directories before starting application.

* _dev_ Debug mode turned on, uses source JavaScript modules uncompressed and unoptimised.

Use the -Pdebug mode during front-end development or module changes won't show up! 

### Testing

The project has an integration test suite used for verifying the API works as expected. These tests 
must be run against a deployed version of the application. To run the the tests against localhost, use
the following commands 

    $ mvn -Ptest test

The following system parameters (-D) can be used to modify the API endpoint used for testing. 

* _test.host_ - Hostname (default: "localhost")

* _test.port_ - Port (default: "8080")

* _test.path_ - Path to application (default: "")

* _test.protocol_ - Protocol (default: "http")


Dependencies
------------

### Backend 

* Java, version 1.5.0 or above

* Application server, supporting _servlet-2.5_ container 

### Frontend 

The Dojo Web Builder is tested on the following web browsers 

* Chrome 10+

* Firefox 3.6+

* IE7+ 

Feedback
--------

File bug reports at [http://bugs.dojotoolkit.org](http://bugs.dojotoolkit.org) 
or contact James Thomas (twitter.com/thomasj) for anything else.

TODO
--

* Allow the application to support multiple versions of the Dojo Toolkit simultaneously. Backend already
supports this in theory, UI needs to allow selection of Dojo Toolkit version to display. 

* Support AMD module format natively within build and analysis tools. At the moment we use a build time 
transformation. 

* Extract out Dojo dependent functionality, transforming the tool into a generic AMD module builder. 

