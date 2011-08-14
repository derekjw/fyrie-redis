

scalaVersion := "2.9.0-1"

name := "fyrie-redis"

organization := "net.fyrie"

version := "1.1-SNAPSHOT"

resolvers += "Akka Repo" at "http://akka.io/repository"

libraryDependencies ++= Seq("se.scalablesolutions.akka" % "akka-actor" % "1.1.3" % "compile",
                            "org.scalatest" % "scalatest_2.9.0" % "1.6.1" % "test")

autoCompilerPlugins := true

addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.0-1")

scalacOptions += "-P:continuations:enable"

parallelExecution in Test := false

seq(ScalariformPlugin.settings: _*)
