

scalaVersion := "2.9.0"

name := "fyrie-redis"

version := "2.0-SNAPSHOT"

libraryDependencies ++= Seq("se.scalablesolutions.akka" % "akka-actor" % "2.0-SNAPSHOT" % "compile",
                            "org.scalatest" %% "scalatest" % "1.6.1.RC1" % "test")

autoCompilerPlugins := true

addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.0")

scalacOptions += "-P:continuations:enable"

parallelExecution in Test := false
