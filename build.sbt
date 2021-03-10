name := "dist-sys-server-scala"

version := "1.0"

scalaVersion := "2.12.2"

// https://mvnrepository.com/artifact/com.google.maps/google-maps-services
libraryDependencies += "com.google.maps" % "google-maps-services" % "0.1.20" from "file://" + baseDirectory.value + "/lib/google-maps-services-0.1.20.jar"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor_2.12
libraryDependencies += "com.typesafe.akka" % "akka-actor_2.12" % "2.4.18"

// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.0"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-http
libraryDependencies += "com.typesafe.akka" % "akka-http_2.12" % "10.0.9"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-http-spray-json
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.9"

// https://mvnrepository.com/artifact/org.mongodb.scala/mongo-scala-driver
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "4.2.2"

// https://mvnrepository.com/artifact/org.slf4j/slf4j-api
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.30"

// https://mvnrepository.com/artifact/org.slf4j/slf4j-log4j12
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.30"

// https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.14.0"
