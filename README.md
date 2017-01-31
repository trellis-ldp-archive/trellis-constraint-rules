# trellis-constraint-rules

[![Build Status](https://travis-ci.org/acoburn/trellis-constraint-rules.png?branch=master)](https://travis-ci.org/acoburn/trellis-constraint-rules)

A set of constraints on a trellis repository defining the rules that govern valid RDF.

These rules consist of:

  * Single-subject rule -- RDF graphs must have a single subject, corresponding to the resource URI
  * No inappropriate LDP properties -- certain LDP properties can only be modified if the interation model permits it
  * LDP Resource types cannot be set or changed in the RDF

## Building

This code requires Java 8 and can be built with Gradle:

    ./gradlew install
