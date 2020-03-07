Externalsortinginjava
==========================================================
[![Build Status](https://travis-ci.org/lemire/externalsortinginjava.png)](https://travis-ci.org/lemire/externalsortinginjava)
[![][maven img]][maven]
[![][license img]][license]
[![docs-badge][]][docs]
[![Coverage Status](https://coveralls.io/repos/github/lemire/externalsortinginjava/badge.svg?branch=master)](https://coveralls.io/github/lemire/externalsortinginjava?branch=master)

External-Memory Sorting in Java: useful to sort very large files using multiple cores and an external-memory algorithm.


The versions 0.1 of the library are compatible with Java 6 and above. Versions 0.2 and above
require at least Java 8.

This code is used in [Apache Jackrabbit Oak](https://github.com/apache/jackrabbit-oak) as well as in [Apache Beam](https://github.com/apache/beam).

Code sample
------------

```java
import com.google.code.externalsorting.ExternalSort;

//... inputfile: input file name
//... outputfile: output file name
// next command sorts the lines from inputfile to outputfile
ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(new File(inputfile)), new File(outputfile));
// you can also provide a custom string comparator, see API
```


Code sample (CSV)
------------

For sorting CSV files, it  might be more convenient to use `CsvExternalSort`.

```java
import com.google.code.externalsorting.CsvExternalSort;
import org.apache.commons.csv.CSVRecord;


// provide a comparator
Comparator<CSVRecord> comparator = (op1, op2) -> op1.get(0).compareTo(op2.get(0));
//... inputfile: input file name
//... outputfile: output file name
// next two lines sort the lines from inputfile to outputfile
List<File> sortInBatch = CsvExternalSort.sortInBatch(inputfile, comparator, CsvExternalSort.DEFAULTMAXTEMPFILES, Charset.defaultCharset(), null, false, 1, CSVFormat.DEFAULT);
CsvExternalSort.mergeSortedFiles(sortInBatch, outputfile, comparator, Charset.defaultCharset(), false, true, CSVFormat.DEFAULT);

```

API Documentation
-----------------

http://www.javadoc.io/doc/com.google.code.externalsortinginjava/externalsortinginjava/




Maven dependency
-----------------


You can download the jar files from the Maven central repository:
https://repo1.maven.org/maven2/com/google/code/externalsortinginjava/externalsortinginjava/

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
