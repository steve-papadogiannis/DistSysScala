# Directions Map Reduce Server #

[![Build Status](https://travis-ci.com/steve-papadogiannis/dist-sys-server-scala.svg?branch=master)](https://travis-ci.com/steve-papadogiannis/dist-sys-server-scala)
[![Known Vulnerabilities](https://snyk.io/test/github/steve-papadogiannis/dist-sys-server-scala/badge.svg)](https://snyk.io/test/github/steve-papadogiannis/dist-sys-server-scala)

A small project that takes directions queries and produces directions results in polynomial representation.

## Versions ##

* JDK: 1.8.0_251
* SBT: 0.13.18
* Scala: 2.12.2
* google-maps-services SDK: 0.1.20
* Akka Actor: 2.4.18

## Sequence Diagram ## 


## Build ##

Below command should be issued inside project's directory:

```
sbt clean package
```

## Run ##

Below environment variable should be set before running the project:

```shell
export API_KEY=<the value of the API Key>
```

Below command should be issued inside project's directory:

```sbt
sbt run
```