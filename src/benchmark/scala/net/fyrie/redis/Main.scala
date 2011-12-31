package net.fyrie.redis

import akka.actor.ActorSystem
import akka.dispatch.{ Future, Await }
import akka.util.{ ByteString, Duration }

import annotation.tailrec
import com.google.caliper.{
  Runner => CaliperRunner,
  Param,
  SimpleBenchmark
}

object Main extends App {
  //CaliperRunner.main(classOf[Benchmark], (args :+ "--vm" :+ "java -XX:+TieredCompilation"): _*)

  CaliperRunner.main(classOf[Benchmark], (args :+ "--vm" :+ "java -XX:+UseParallelOldGC -XX:+TieredCompilation -XX:SurvivorRatio=1 -Xmn1g -Xms2g -Xmx2g"): _*)

  //CaliperRunner.main(classOf[Benchmark], (args :+ "-Jgc=-XX:+UseParallelOldGC,-XX:+UseConcMarkSweepGC,-XX:+UseG1GC"  :+ "--vm" :+ "java -XX:+TieredCompilation -XX:SurvivorRatio=1 -Xmn1g -Xms2g -Xmx2g"): _*)

/*
  val bm = new Benchmark
  bm.setUp

  while(true) {
    val ops = 100000
    val start = System.nanoTime
    bm.timeScatterGather(ops)
    val end = System.nanoTime
    println("ops/sec: " + (ops.toDouble / ((end - start).toDouble / 1000000000.0)).toInt)
    System.gc
  }

  bm.system.shutdown
*/
}

class Benchmark extends SimpleScalaBenchmark {

  // default key: 

  // amount of unique keys to use, not implemented
  // val keyspace = 1

  val dataSize = 3

  val data = ByteString("x" * dataSize)

  implicit val system: ActorSystem = TestSystem.system

  var client: RedisClient = _

  override def setUp() {
    client = RedisClient(config = RedisClientConfig(retryOnReconnect = false))
    client.sync.flushdb
  }

  override def tearDown() {
    // client.sync.flushdb
    client.disconnect
    client = null
  }

  def timeSet(reps: Int) = {
    repeat(reps) { client set ("foo:rand:000000000000", data) }

    val result = client.sync get "foo:rand:000000000000"

    assert(result == Some(data))

    result
  }

  def timeGet(reps: Int) = {
    client.quiet set ("foo:rand:000000000000", data)
    repeat(reps) { client get "foo:rand:000000000000" }

    val result = client.sync get "foo:rand:000000000000"

    assert(result == Some(data))

    result
  }

  def timeIncrFast(reps: Int) = {
    repeat(reps) { client.quiet incr "counter:rand:000000000000" }

    val result = (client.sync getset ("counter:rand:000000000000", 0)).parse[Int]

    assert(result == Some(reps))

    result
  }

  def timeIncr(reps: Int) = {
    repeat(reps) { client incr "counter:rand:000000000000" }

    val result = (client.sync getset ("counter:rand:000000000000", 0)).parse[Int]

    assert(result == Some(reps))

    result
  }

  def timeScatterGather(reps: Int) = {
    val keys = (1 to 100).toList map ("list_" + _)

    val ops = reps / 200 // 2 ops per rep

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

    assert(result == ((1 to ops).sum * 100))

  }

}

trait SimpleScalaBenchmark extends SimpleBenchmark {
  
  def repeat[@specialized A](reps: Int)(f: => A): A = {
    def run(i: Int, result: A): A = {
      if (i == 0) result else run(i - 1, f)
    }
    run(reps - 1, f)
  }

}
