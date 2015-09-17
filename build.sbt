name := "akka-cluster-poc"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "theatr.us" at "http://repo.theatr.us"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.3.13"
libraryDependencies += "com.typesafe.akka" %% "akka-contrib" % "2.3.13"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.13"
libraryDependencies += "us.theatr" %% "akka-quartz" % "0.3.0"

