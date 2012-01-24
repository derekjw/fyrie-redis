import sbt._
import Keys._

import com.typesafe.sbtscalariform.ScalariformPlugin
import com.typesafe.sbtscalariform.ScalariformPlugin.ScalariformKeys

object FyrieRedisBuild extends Build {
  lazy val core = (Project("fyrie-redis", file("."))
                   configs(Benchmark)
                   settings(coreSettings: _*))

  val coreSettings = Defaults.defaultSettings ++ inConfig(Benchmark)(Defaults.configSettings) ++ ScalariformPlugin.scalariformSettings ++ Seq(
    scalaVersion := "2.9.1",
    crossScalaVersions := Seq("2.9.0-1", "2.9.1", "2.10.0-M1"),
    name := "fyrie-redis",
    organization := "net.fyrie",
    version := "2.0-SNAPSHOT",
    resolvers ++= Seq("Sonatype OSS Repo" at "http://oss.sonatype.org/content/repositories/snapshots",
                      "Akka Snapshot Repo" at "http://akka.io/snapshots"),
    libraryDependencies ++= Seq("com.typesafe.akka" % "akka-actor" % "2.0-20120124-000638",
                                "com.typesafe.akka" % "akka-testkit" % "2.0-20120124-000638" % "test",
                                "org.specs2" % "specs2_2.9.1" % "1.6.1",
                                "org.specs2" % "specs2-scalaz-core_2.9.1" % "6.0.1" % "test"),
                                "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT" % "benchmark",
                                "com.google.code.gson" % "gson" % "1.7.1" % "benchmark"),
    parallelExecution := false,
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-optimize"),
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test :=  formattingPreferences,
    runner in Benchmark in run <<= (thisProject, taskTemporaryDirectory, scalaInstance, baseDirectory, javaOptions, outputStrategy, javaHome, connectInput) map {
      (tp, tmp, si, base, options, strategy, javaHomeDir, connectIn) =>
        new BenchmarkRunner(tp.id, ForkOptions(scalaJars = si.jars, javaHome = javaHomeDir, connectInput = connectIn, outputStrategy = strategy,
          runJVMOptions = options, workingDirectory = Some(base)) )
    },
    publishTo <<= (version) { version: String =>
      val repo = (s: String) =>
        Resolver.ssh(s, "repo.fyrie.net", "/home/repo/" + s + "/") as("derek", file("/home/derek/.ssh/id_rsa")) withPermissions("0644")
      Some(if (version.trim.endsWith("SNAPSHOT")) repo("snapshots") else repo("releases"))
    })

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
    .setPreference(RewriteArrowSymbols, true)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
  }

  lazy val Benchmark = config("benchmark") extend(Test)

}

class BenchmarkRunner(subproject: String, config: ForkScalaRun) extends sbt.ScalaRun {
  def run(mainClass: String, classpath: Seq[File], options: Seq[String], log: Logger): Option[String] = {
    log.info("Running " + subproject + " " + mainClass + " " + options.mkString(" "))

    val javaOptions = classpathOption(classpath) ::: mainClass :: options.toList
    val strategy = config.outputStrategy getOrElse LoggedOutput(log)
    val process =  Fork.java.fork(config.javaHome,
                                  /* Seq("-XX:+UseConcMarkSweepGC", "-XX:+TieredCompilation", "-XX:SurvivorRatio=1", "-Xmn1g", "-Xms2g", "-Xmx2g") ++ */ config.runJVMOptions ++ javaOptions,
                                  config.workingDirectory,
                                  Map.empty,
                                  config.connectInput,
                                  strategy)
    def cancel() = {
      log.warn("Run canceled.")
      process.destroy()
      1
    }
    val exitCode = try process.exitValue() catch { case e: InterruptedException => cancel() }
    processExitCode(exitCode, "runner")
  }
  private def classpathOption(classpath: Seq[File]) = "-classpath" :: Path.makeString(classpath) :: Nil
  private def processExitCode(exitCode: Int, label: String) = {
    if(exitCode == 0) None
    else Some("Nonzero exit code returned from " + label + ": " + exitCode)
  }
}
