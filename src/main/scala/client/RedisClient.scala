package net.fyrie.redis

trait SortOrder
object SortOrder {
  case object ASC extends SortOrder
  case object DESC extends SortOrder
}

trait AggregateScore {
  def getBytes: Array[Byte]
}
object AggregateScore {
  case object SUM extends AggregateScore {
    val getBytes = "SUM".getBytes
  }
  case object MIN extends AggregateScore {
    val getBytes = "MIN".getBytes
  }
  case object MAX extends AggregateScore {
    val getBytes = "MAX".getBytes
  }
}


trait Redis extends IO {
  def send[T](cmd: Command[T]): T = {
    writer.write(cmd.toBytes)
    reader.read(cmd.replyHandler)
  }
}

class RedisClient(override val host: String, override val port: Int) extends Redis {
  connect

  def this() = this("localhost", 6379)
  override def toString = host + ":" + String.valueOf(port)
}
