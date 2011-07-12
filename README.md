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
at [http://localhost:8080/](http://localhost:8080/).

In addition, the application is not tied to these
platforms, allowing you to build a WAR file and deploy to the application
server of your choice. 

### Quickstart with Jetty
	$ mvn -Ddojo.source=/some/path/to/dojo-1.6-source jetty:run

### Quickstart with Tomcat
	$ mvn -Ddojo.source=/some/path/to/dojo-1.6-source tomcat:run

### Quickstart with Jetty, using older Dojo release
	$ mvn -Ddojo.source=/some/path/to/dojo-1.5-source -Ddojo.version=1.5.0 jetty:run

### Quickstart with Jetty, setting build cache path 
    $ mvn -Dcachepath=/tmp/ -Ddojo.source=/some/path/to/dojo-source jetty:run

### Deploying to remote Tomcat server
    Edit ~/.m2/settings.xml and fill out server details for server id, "dwb"
	$ mvn -Ddojo.source=/some/path/to/dojo-source tomcat:deploy

Configuration 
-----

Configurable application parameters are set using Java system properties. Users 
can modify these values using the -D command line argument. _The only mandatory 
property is the dojo source location_. Both the _dojo.source_ and _cachepath_ parameters
support the use of environment variables, using the %env\_name% format.

### Mandatory Parameters 

* _dojo.source_ - Directory location for the source release of the Dojo Toolkit
  version you want to build. Source folder must contain the correct version of dojo expected
  by the project, see _dojo.version_ parameter below.

### Optional Parameters 

* _dojo.version_ - Change the version of the Dojo Toolkit being exposed by the
project. Currently supported versions are: 1.6.0 (default), 1.5.0. Default version
is set in the _pom.xml_ file. 

* _cachepath_ - Specify the custom build cache directory. By default, this
is a different temporary directory each time the application is started. This 
value can also be set using a context parameter in the application's _web.xml_.

Supporting custom modules
----

The Dojo Web Builder supports serving up custom modules you've created alongside
modules from the Dojo Toolkit. These modules will be exposed alongside the normal
Dojo Toolkit modules in the web application, allowing users to create custom
builds using your own modules. To configure the tool to serve up your local module 
files, follow the steps below...

### Generate package descriptor for custom modules
    $ cd src/main/resources/package_scripts/
    $ rhino generateNewPackageMetaData.js ~/code/js/custom_modules_root/

### Create package & version directory in package repository
    $ mkdir ../../config/packages/custom_modules
    $ mkdir ../../config/packages/custom_modules/1.0.0

### Copy custom package descriptor to package repository 
    $ cp package.json ../../config/packages/custom_modules/1.0.0

### Run Dojo Web Builder as normal

The _src/main/config/packages/_ directory contains the package meta-data files. Packages to be served
are identified by the presence of a child directory. Each package directory must contain at least one 
version directory. Version directories contain the package modules descriptor files. Multiple versions 
of the same package are allowed, the web application automatically uses the most recent version.

To support a different version of Dojo, use the steps above to generate the package descriptor and place
in a new version directory under the _src/main/config/packages/dojo_ path. Then use the _dojo.version_ 
parameter to modify the default version of Dojo served.

Deploying Multiple Versions of Dojo 
----

The Dojo Web Builder also supports building for multiple versions of The Dojo
Toolkit concurrently, allowing the user to choose the version they need
dynamically. To achieve this, ensure you have downloaded local copies of all the
versions of the toolkit you want to expose and then configure the application using 
the steps below.... 

Currently, package descriptor files are included for Dojo 1.6.0, 1.5.0 and
1.4.3. If you need to expose an older version, please generate the package
descriptor, as per the instructions above. 

### (Optional) Generate package descriptors for older versions of Dojo 
    See description above and package in src/main/config/packages/dojo/X.Y.Z/

### Fill in location parameter for all version packages
    $ vim src/main/config/packages/dojo/${dojo.version}/package.json
    $ <replace location field value with directory path>

### Run Dojo Web Builder WITHOUT normal dojo.source parameter 
    $ mvn jetty:run

Development notes
---

### Layout 

The project uses Maven's 
[standard directory layout](http://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html) 
to organise source files. Front-end application code lives in _src/main/webapp_ and the 
backend API code lives in _src/main/java_. 

### Profiles

Maven profiles are used to assist with development, details below. To change the 
current profile, use the -PprofileName parameter when running the application. 

* _standard (default)_ - Quick start using optimised client-side code layer, debug mode turned off.

* _release_ - Same as _standard_ but triggers a full-rebuild of the client-side code, copying generated
resources to JS directories before starting application.

* _dev_ - Debug mode turned on, uses source JavaScript modules uncompressed and unoptimised.

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
or contact [James Thomas](http://github.com/jthomas) [(thomasj @ twitter)](http://twitter.com/thomasj) for anything else.

TODO
--

* Support AMD module format natively within build and analysis tools. At the moment we use a build time 
transformation. 

* Extract out Dojo dependent functionality, transforming the tool into a generic AMD module builder. 
