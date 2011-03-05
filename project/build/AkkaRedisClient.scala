import sbt._
import sbt.CompileOrder._

class FyrieRedisProject(info: ProjectInfo) extends DefaultProject(info) with AkkaProject with Boilerplate
{
  override def compileOptions = Optimize :: Unchecked :: super.compileOptions.toList /* ++ Seq( "-verbose", "-Ydebug" ).map(CompileOption(_)) */
  override def mainSourceRoots = super.mainSourceRoots +++ srcManagedScala
  override def compileAction = super.compileAction dependsOn(generateSortTuple)

  val specs = "org.scala-tools.testing" %% "specs" % "1.6.6" % "test"
  val scalatest = "org.scalatest" % "scalatest" % "1.3" % "test"
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
}
