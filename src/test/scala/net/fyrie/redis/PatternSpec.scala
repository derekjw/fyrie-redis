package net.fyrie.redis

import org.specs2._

import akka.actor.Actor.Timeout
import akka.dispatch.{ Future, Futures }

class PatternsSpec extends mutable.Specification with TestClient {

  implicit val timeout = Timeout(60000)

  val timeoutMs = timeout.duration.toMillis

  def scatterGatherWithList(ops: Int, client: RedisClient) = {

    client.sync.flushdb

    val start = System.nanoTime

    val keys = (1 to 100).toList map ("list_" + _)

    val scatter = { (key: String) ⇒
      (1 to ops) foreach (i ⇒ client.quiet rpush (key, i))
      client llen key map (x ⇒ assert(x == ops))
    }

    val gather = { (key: String) ⇒
      val sum = (Future(0) /: (1 to ops)) { (fr, _) ⇒
        for {
          n ← client lpop key
          r ← fr
        } yield n.parse[Int].get + r
      }
      client llen key map (x ⇒ assert(x == 0)) flatMap (_ ⇒ sum)
    }

    val future = for {
      _ ← Future.traverse(keys, timeoutMs)(scatter)
      n ← Future.traverse(keys, timeoutMs)(gather)
    } yield n.sum

    val result = future.get

    val elapsed = (System.nanoTime - start) / 1000000000.0
    val opsPerSec = (100 * ops * 2) / elapsed

    println("Operations per run: " + ops * 100 * 2 + " elapsed: " + elapsed + " ops per second: " + opsPerSec)

    result === ((1 to ops).sum * 100)
  }

  "Scatter/Gather" >> {
    "100 lists x 2000 items" ! client { r ⇒ scatterGatherWithList(2000, r) }
    "100 lists x 5000 items" ! client { r ⇒ scatterGatherWithList(5000, r) }
    "100 lists x 10000 items" ! client { r ⇒ scatterGatherWithList(10000, r) }
  }

}
