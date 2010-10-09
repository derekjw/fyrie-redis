import sbt._
 
class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val akkaPlugin = "se.scalablesolutions.akka" % "akka-sbt-plugin" % "1.0-SNAPSHOT"
}
