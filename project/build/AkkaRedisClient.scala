import sbt._
import sbt.CompileOrder._

class AkkaRedisClientProject(info: ProjectInfo) extends DefaultProject(info) with AkkaProject
{
  override def compileOptions = Optimize :: Unchecked :: super.compileOptions.toList

  val fyrieredis = "net.fyrie" %% "fyrieredis" % "2.0-SNAPSHOT"
  val specs = "org.scala-tools.testing" %% "specs" % "1.6.5" % "test"
  val scalatest = "org.scalatest" % "scalatest" % "1.2" % "test"
  val scalacheck = "org.scala-tools.testing" %% "scalacheck" % "1.7" % "test"
  val junit = "junit" % "junit" % "4.8.1" % "test"

  override def managedStyle = ManagedStyle.Maven
  val publishUser = "derek"
  val publishKeyFile = new java.io.File("/home/derek/.ssh/id_rsa")
  val publishTo = projectVersion.value match {
    case BasicVersion(_,_,_,Some("SNAPSHOT")) =>
      Resolver.sftp("Fyrie Snapshots SFTP", "repo.fyrie.net", "/home/repo/snapshots") as(publishUser, publishKeyFile)
    case _ =>
      Resolver.sftp("Fyrie Releases SFTP", "repo.fyrie.net", "/home/repo/releases") as(publishUser, publishKeyFile)
  }

  val fyrieReleases           = "Fyrie releases" at "http://repo.fyrie.net/releases"
  val fyrieSnapshots          = "Fyrie snapshots" at "http://repo.fyrie.net/snapshots"
  val scalaToolsSnapshots     = ScalaToolsSnapshots
}
