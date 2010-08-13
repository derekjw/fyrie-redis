package net.fyrie.redis
package akka

import commands._
import net.fyrie.redis.akka.collection._

import org.specs._
import specification.Context

class PerformanceSpec extends Specification {
  val rs = new RedisClient("localhost", 16379)
  implicit val ra = new AkkaRedisClient("localhost", 16379)

  val empty = new Context {
    before {
      rs send flushdb
    }
    after {
      rs send flushdb
    }
  }

  "Compared to not using actors, the akka redis client" ->- empty should {
    "Process more req/s" in {
      "Incrementing an integer then returning it" in {
        "100000 times" in { incrTest(100000) } // Overkill, but akka implementation needs good warmup
        "10000 times" in { incrTest(10000) }
        "1000 times" in { incrTest(1000, 10) }
        "100 times" in { incrTest(100, 100) }
        "10 times" in { incrTest(10, 1000) }
      }
    }
  }

  implicit def toBytes(in: Any): Array[Byte] = in.toString.getBytes
  implicit def fromBytes(in: Array[Byte]): String = new String(in)

  def reqs(iterations: Long, millis: Long): Double = iterations / (millis / 1000.0)

  def incrTest(iterations: Int, reps: Int = 1) = {
    println("Testing with "+iterations+" iterations " +reps+" times")

    val raNum = RedisLongVar("ra")

    val raRes = bm(reps){
      raNum set 0
      (1 to iterations) foreach {i => raNum.incrFast}
      raNum.get must_== iterations
    }

    val raReqs = reqs((iterations * reps),raRes).toInt

    println("akka:     " + raReqs + " req/s")

    val rsRes = bm(reps){
      rs send set("rs", 0)
      (1 to iterations) foreach {i => rs send incr("rs")}
      fromBytes(rs send get("rs") getOrElse error("Not Found")) must_== (iterations.toString)
    }

    val rsReqs = reqs((iterations * reps),rsRes).toInt

    println("standard: " + rsReqs + " req/s")

    raReqs must be_>= (rsReqs)
  }

  def bm(reps: Int)(f: => Unit): Long = {
    val start = System.currentTimeMillis
    (1 to reps) foreach (i => f)
    System.currentTimeMillis - start
  }

}
