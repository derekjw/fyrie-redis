package net.fyrie.redis
package commands

import handlers._
import serialization._
import Command._

trait NodeCommands {
  // SAVE
  // save the DB on disk now.
  case object save extends Command(OkStatus)

  // BGSAVE
  // save the DB in the background.
  case object bgsave extends Command(OkStatus)

  // LASTSAVE
  // return the UNIX TIME of the last DB SAVE executed with success.
  case object lastsave extends Command(ShortInt)

  // SHUTDOWN
  // Stop all the clients, save the DB, then quit the server.
  case object shutdown extends Command(NoHandler)

  // BGREWRITEAOF
  case object bgwriteaof extends Command(OkStatus)

  // INFO
  // the info command returns different information and statistics about the server.
  case class info[A](implicit parse: Parse[A]) extends Command(Bulk[A]()(implicitly, parse.manifest))

  // MONITOR
  // is a debugging command that outputs the whole sequence of commands received by the Redis server.
  // FIXME: Will probably cause much trouble as it has a non standard reply
  case object monitor extends Command(OkStatus)

  // SLAVEOF
  // The SLAVEOF command can change the replication settings of a slave on the fly.
  case class slaveof(hostPort: Option[(String, Int)])(implicit format: Format) extends Command(OkStatus) {
    override def args = hostPort map (x => Iterator(x._1, x._2)) getOrElse Iterator("NO ONE")
  }

  object config {

    case class get[A](param: Any)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulk[A]()(implicitly, parse.manifest)) {
      override def name = "CONFIG"
      override def args = Iterator("GET", param)
    }

    case class set(param: Any, value: Any)(implicit format: Format) extends Command(OkStatus) {
      override def name = "CONFIG"
      override def args = Iterator("SET", param, value)
    }

  }
}
