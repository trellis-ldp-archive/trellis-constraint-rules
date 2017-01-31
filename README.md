# trellis-constraint-rules

[![Build Status](https://travis-ci.org/trellis-ldp/trellis-constraint-rules.png?branch=master)](https://travis-ci.org/trellis-ldp/trellis-constraint-rules)

A set of constraints on a trellis repository defining the rules that govern valid RDF.

These rules consist of:

  * Single-subject rule -- RDF graphs must have a single subject, corresponding to the resource URI (hashURIs are allowed)
  * No inappropriate LDP properties -- certain LDP properties can only be modified if the interation model permits it
  * LDP Resource types cannot be set or changed in the RDF
  * Certain properties (`acl:accessControl`, `ldp:membershipResource`) must be used with "in-domain" resources
  * Certain properties must have a range of a URI and have a max-cardinality of 1 (`ldp:membershipResource`, `ldp:hasMemberRelation`, `ldp:isMemberOfRelation`, `ldp:insertedContentRelation`, `ldp:inbox`, `acl:accessControl`)

## Building

This code requires Java 8 and can be built with Gradle:

    ./gradlew install
