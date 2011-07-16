

scalaVersion := "2.9.0-1"

name := "fyrie-redis"

organization := "net.fyrie"

version := "2.0-SNAPSHOT"

libraryDependencies ++= Seq("se.scalablesolutions.akka" % "akka-actor" % "2.0-SNAPSHOT" % "compile",
                            "org.scalatest" % "scalatest_2.9.0" % "1.6.1" % "test")

autoCompilerPlugins := true

addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.0-1")

scalacOptions += "-P:continuations:enable"

parallelExecution in Test := false
