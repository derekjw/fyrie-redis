package net.fyrie.redis
package commands

import replies._
import Helpers._

// SAVE
// save the DB on disk now.
case object save extends Command[OkStatus]

// BGSAVE
// save the DB in the background.
case object bgsave extends Command[OkStatus]

// LASTSAVE
// return the UNIX TIME of the last DB SAVE executed with success.
case object lastsave extends Command[Int]

// SHUTDOWN
// Stop all the clients, save the DB, then quit the server.
case object shutdown extends Command[Nothing]

// BGREWRITEAOF
case object bgwriteaof extends Command[OkStatus]

// INFO
// the info command returns different information and statistics about the server.
case object info extends Command[Bulk]

// MONITOR
// is a debugging command that outputs the whole sequence of commands received by the Redis server.
// FIXME: Will probably cause much trouble as it has a non standard reply
case object monitor extends Command[OkStatus]

// SLAVEOF
// The SLAVEOF command can change the replication settings of a slave on the fly.
case class slaveof(hostPort: Option[(String, Int)]) extends Command[OkStatus] {
  override def args = hostPort match {
    case Some((h: String, p: Int)) => Seq(h.getBytes, p.toString.getBytes)
    case None => Seq("NO ONE".getBytes)
  }
}

object config {

  case class get(param: Any) extends Command[MultiBulk] {
    override def name = "CONFIG".getBytes
    override def args = getBytesSeq(Seq("GET", param))
  }

  case class set(param: Any, value: Any) extends Command[OkStatus] {
    override def name = "CONFIG".getBytes
    override def args = getBytesSeq(Seq("SET", param, value))
  }

}
