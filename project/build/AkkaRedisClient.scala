import sbt._
import sbt.CompileOrder._

class AkkaRedisClientProject(info: ProjectInfo) extends DefaultProject(info) 
{
  override def compileOptions = Optimize :: Unchecked :: super.compileOptions.toList

  val akka = "se.scalablesolutions.akka" %% "akka-core"  % "0.10-SNAPSHOT" % "compile"

  val logback = "ch.qos.logback" % "logback-classic" % "0.9.24" % "test"
  val specs = "org.scala-tools.testing" %% "specs" % "1.6.5" % "test"
  val scalatest = "org.scalatest" % "scalatest" % "1.2" % "test"
  val scalacheck = "org.scala-tools.testing" %% "scalacheck" % "1.7" % "test"

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

  val akkaEmbedded            = "Akka embedded repo" at "http://repo.fyrie.net/akka-embedded-repo"
  def guiceyFruitRepo         = "GuiceyFruit Repo" at "http://guiceyfruit.googlecode.com/svn/repo/releases/"
  val guiceyFruitModuleConfig = ModuleConfiguration("org.guiceyfruit", guiceyFruitRepo)
  def jbossRepo               = "JBoss Repo" at "https://repository.jboss.org/nexus/content/groups/public/"
  val jbossModuleConfig       = ModuleConfiguration("org.jboss", jbossRepo)
  val nettyModuleConfig       = ModuleConfiguration("org.jboss.netty", jbossRepo)
  val jgroupsModuleConfig     = ModuleConfiguration("jgroups", jbossRepo)
  val liftModuleConfig        = ModuleConfiguration("net.liftweb", ScalaToolsSnapshots)
  def codehausSnapshotRepo    = "Codehaus Snapshots" at "http://snapshots.repository.codehaus.org"
  val multiverseModuleConfig  = ModuleConfiguration("org.multiverse", codehausSnapshotRepo)
}
