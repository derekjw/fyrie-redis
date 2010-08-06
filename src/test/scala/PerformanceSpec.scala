package net.fyrie.redis.akka

import com.redis._
import commands._

import org.specs._
import specification.Context

class PerformanceSpec extends Specification {
  val rs = new RedisClient("localhost", 16379)
  val ra = new AkkaRedisClient("localhost", 16379)

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
        "100000 times" in { incrTest(100000) }
        "10000 times" in { incrTest(10000) }
        "1000 times" in { incrTest(1000, 10) }
        "100 times" in { incrTest(100, 100) }
        "10 times" in { incrTest(10, 1000) }
      }
    }
  }

  implicit def any2Bytes(in: Any): Array[Byte] = in.toString.getBytes
  implicit def bytes2String(in: Array[Byte]): String = new String(in)

  def reqs(iterations: Long, millis: Long): Double = iterations / (millis / 1000.0)

  def incrTest(iterations: Int, reps: Int = 1) = {
    println("Testing with "+iterations+" iterations " +reps+" times")

    val raRes = bm(reps){
      ra ! set("ra", 0)
      (1 to iterations) foreach {i => ra ! incr("ra")}
      new String(ra !! get("ra") getOrElse error("Timed Out") getOrElse error("Not Found")) must_== (iterations.toString)
    }

    val raReqs = reqs((iterations * reps),raRes).toInt

    println("akka:     " + raReqs + " req/s")

    val rsRes = bm(reps){
      rs send set("rs", 0)
      (1 to iterations) foreach {i => rs send incr("rs")}
      new String(rs send get("rs") getOrElse error("Not Found")) must_== (iterations.toString)
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
