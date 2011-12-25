package net.fyrie.redis

import org.specs2._

import akka.dispatch.{ Future, Await }
import akka.util.Duration

class PatternsSpec extends mutable.Specification {

  implicit val system = TestSystem.system

  implicit val arguments = args(sequential = true)

  def scatterGatherWithList(ops: Int) = {
    val client = RedisClient(config = RedisClientConfig(retryOnReconnect = false))

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
      _ ← Future.traverse(keys)(scatter)
      n ← Future.traverse(keys)(gather)
    } yield n.sum

    val result = Await.result(future, Duration.Inf)

    val elapsed = (System.nanoTime - start) / 1000000000.0
    val opsPerSec = (100 * ops * 2) / elapsed

    println("Operations per run: " + ops * 100 * 2 + " elapsed: " + elapsed + " ops per second: " + opsPerSec)

    client.disconnect

    result === ((1 to ops).sum * 100)
  }

  def incrBench(ops: Int) = {
    val client = RedisClient(config = RedisClientConfig(retryOnReconnect = false))

    client.sync.flushdb

    val start = System.nanoTime

    (1 to ops) foreach (_ => client.quiet incr "inctest")
    val result = client.sync get "inctest"

    val elapsed = (System.nanoTime - start) / 1000000000.0
    val opsPerSec = ops / elapsed

    println("Operations per run: " + ops + " elapsed: " + elapsed + " ops per second: " + opsPerSec.toInt)

    client.disconnect

    result.parse[Int] === Some(ops)
  }

  "Scatter/Gather" >> {
    "100 lists x 2000 items" ! { scatterGatherWithList(2000) }
    "100 lists x 5000 items" ! { scatterGatherWithList(5000) }
    "1000000" ! { incrBench(1000000) }
  }

}
