Externalsortinginjava
==========================================================
[![Build Status](https://travis-ci.org/lemire/externalsortinginjava.png)](https://travis-ci.org/lemire/externalsortinginjava)
[![][maven img]][maven]
[![][license img]][license]
[![docs-badge][]][docs]
[![Coverage Status](https://coveralls.io/repos/github/lemire/externalsortinginjava/badge.svg?branch=master)](https://coveralls.io/github/lemire/externalsortinginjava?branch=master)

External-Memory Sorting in Java: useful to sort very large files.


The versions 0.1 of the library are compatible with Java 6 and above. Versions 0.2 and above
require at least Java 8. 

Code sample
------------

```
                import com.google.code.externalsorting.ExternalSort;

                //...

                List<File> l = ExternalSort.sortInBatch(new File(inputfile));
                ExternalSort.mergeSortedFiles(l, new File(outputfile));
```

API Documentation
-----------------

http://www.javadoc.io/doc/com.google.code.externalsortinginjava/externalsortinginjava/




Maven dependency
-----------------


You can download the jar files from the Maven central repository:
http://repo1.maven.org/maven2/com/google/code/externalsortinginjava/externalsortinginjava/

You can also specify the dependency in the Maven "pom.xml" file:

```xml
    <dependencies>
         <dependency>
	     <groupId>com.google.code.externalsortinginjava</groupId>
	     <artifactId>externalsortinginjava</artifactId>
	     <version>[0.1.9,)</version>
         </dependency>
     </dependencies>
```

How to build
-----------------

- get the java jdk
- Install Maven 2
- mvn install - builds jar (requires signing)
- mvn test - runs tests



[maven img]:https://maven-badges.herokuapp.com/maven-central/com.googlecode.javaewah/JavaEWAH/badge.svg
[maven]:http://search.maven.org/#search%7Cga%7C1%7Cexternalsortinginjava

[license]:LICENSE.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg


[docs-badge]:https://img.shields.io/badge/API-docs-blue.svg?style=flat-square
[docs]:http://www.javadoc.io/doc/com.google.code.externalsortinginjava/externalsortinginjava/
