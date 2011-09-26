import sbt._
import Keys._

import com.typesafe.sbtscalariform.ScalariformPlugin
import ScalariformPlugin.{ format, formatPreferences }

object FyrieRedisBuild extends Build {
  lazy val core = Project("fyrie-redis",
                          file("."),
                          dependencies = Seq(akkaActor, akkaTestKit % "test"),
                          settings = coreSettings)

  lazy val akkaActor = ProjectRef(uri("git://github.com/jboner/akka.git"), "akka-actor")

  lazy val akkaTestKit = ProjectRef(uri("git://github.com/jboner/akka.git"), "akka-testkit")

  val coreSettings = Defaults.defaultSettings /* ++ ScalariformPlugin.settings */ ++ Seq(
    scalaVersion := "2.9.1",
    crossScalaVersions := Seq("2.9.0-1", "2.9.1"),
    name := "fyrie-redis",
    organization := "net.fyrie",
    version := "2.0-SNAPSHOT",
    libraryDependencies ++= Seq("org.specs2" % "specs2_2.9.1" % "1.6.1",
                                "org.specs2" % "specs2-scalaz-core_2.9.1" % "6.0.1" % "test"),
    parallelExecution in Test := false,
/*    formatPreferences in Compile := formattingPreferences,
    formatPreferences in Test :=  formattingPreferences, */
    publishTo <<= (version) { version: String =>
      val repo = (s: String) =>
        Resolver.ssh(s, "repo.fyrie.net", "/home/repo/" + s + "/") as("derek", file("/home/derek/.ssh/id_rsa")) withPermissions("0644")
      Some(if (version.trim.endsWith("SNAPSHOT")) repo("snapshots") else repo("releases"))
    })

  val formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
    .setPreference(RewriteArrowSymbols, true)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
  }

}

