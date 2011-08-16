package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

import akka.dispatch.Future
import akka.testkit.{ filterEvents, EventFilter }

class OperationsSpec extends Spec
  with ShouldMatchers
  with RedisTestServer {

  describe("keys") {
    it("should fetch keys") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.keys("anshin*").size should equal(2)
    }

    it("should fetch keys with spaces") {
      r.set("anshin 1", "debasish")
      r.set("anshin 2", "maulindu")
      r.sync.keys("anshin*").size should equal(2)
    }
  }

  describe("randomkey") {
    it("should give") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.randomkey().parse[String] getOrElse fail("No key returned") should startWith("anshin")
    }
  }

  describe("rename") {
    it("should give") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.rename("anshin-2", "anshin-2-new")
      val thrown = evaluating { r.sync.rename("anshin-2", "anshin-2-new") } should produce[Exception]
      thrown.getMessage should equal("ERR no such key")
    }
  }

  describe("renamenx") {
    it("should give") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.renamenx("anshin-2", "anshin-2-new") should equal(true)
      r.sync.renamenx("anshin-1", "anshin-2-new") should equal(false)
    }
  }

  describe("dbsize") {
    it("should give") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.dbsize() should equal(2)
    }
  }

  describe("exists") {
    it("should give") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.exists("anshin-2") should equal(true)
      r.sync.exists("anshin-1") should equal(true)
      r.sync.exists("anshin-3") should equal(false)
    }
  }

  describe("del") {
    it("should give") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.del(Set("anshin-2", "anshin-1")) should equal(2)
      r.sync.del(Set("anshin-2", "anshin-1")) should equal(0)
    }
  }

  describe("type") {
    it("should give") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.typeof("anshin-2") should equal("string")
    }
  }

  describe("expire") {
    it("should give") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.expire("anshin-2", 1000) should equal(true)
      r.sync.expire("anshin-3", 1000) should equal(false)
    }
  }

  describe("quit") {
    it("should reconnect") {
      r.quit
      r.set("key1", "value1")
      r.quit
      r.sync.get("key1").parse[String] should be(Some("value1"))
    }
    ignore("should reconnect with many commands") { // ignore until fixed
      (1 to 1000) foreach (_ => r.incr("incKey"))
      r.quit
      val result1 = r.get("incKey").parse[Int]
      (1 to 1000) foreach (_ => r.incr("incKey"))
      val result2 = r.get("incKey").parse[Int]
      r.quit
      (1 to 1000) foreach (_ => r.incr("incKey"))
      val result3 = r.get("incKey").parse[Int]
      result1.get should be(Some(1000))
      result2.get should be(Some(2000))
      result3.get should be(Some(3000))
    }
  }

  describe("Multi exec commands") {
    it("should work with single commands") {
      val result = r.multi { rq =>
        rq.set("testkey1", "testvalue1")
      }
      result.get should be(())
    }
    it("should work with several commands") {
      val result = r.multi { rq =>
        for {
          _ <- rq.set("testkey1", "testvalue1")
          _ <- rq.set("testkey2", "testvalue2")
          x <- rq.mget(List("testkey1", "testkey2")).parse[String]
        } yield x
      }
      result.get should be(List(Some("testvalue1"), Some("testvalue2")))
    }
    it("should work with a list of commands") {
      val values = List.range(1, 100)
      val result = r.multi { rq =>
        val setq = (Queued[Any](()) /: values)((q, v) => q flatMap (_ => rq.set(v, v * 2)))
        (setq.map(_ => List[Future[Option[Int]]]()) /: values)((q, v) => q flatMap (l => rq.get(v).parse[Int].map(_ :: l))).map(_.reverse)
      }
      Future.sequence(result).get.flatten should be(values.map(2*))
    }
    it("should throw an error") {
      filterEvents(EventFilter[RedisErrorException]("ERR Operation against a key holding the wrong kind of value")) {
        val result = r.multi { rq =>
          for {
            _ <- rq.set("a", "abc")
            x <- rq.lpop("a").parse[String]
            y <- rq.get("a").parse[String]
          } yield (x, y)
        }
        evaluating { result._1.get } should produce[RedisErrorException]
        result._2.get should be(Some("abc"))
      }
    }
    it("should handle invalid requests") {
      val result = r.multi { rq =>
        for {
          _ <- rq.set("testkey1", "testvalue1")
          _ <- rq.set("testkey2", "testvalue2")
          x <- rq.mget(List[String]()).parse[String]
          y <- rq.mget(List("testkey1", "testkey2")).parse[String]
        } yield (x, y)
      }
      evaluating { result._1.get } should produce[RedisErrorException]
      result._2.get should be(List(Some("testvalue1"), Some("testvalue2")))
    }
  }

  describe("watch") {
    it("should fail without watch") {
      r.sync.set("key", 0)
      val clients = List.fill(10)(RedisClient())
      val futures = for (client <- clients; _ <- 1 to 10) yield client.get("key").parse[Int] flatMap { n => client.set("key", n.get + 1) }
      Future.sequence(futures).await
      clients foreach (_.disconnect)
      r.sync.get("key").parse[Int] should not be (Some(100))
    }
    it("should succeed with watch") {
      r.sync.set("key", 0)
      val futures = for (_ <- 1 to 100) yield {
        r watch { rw =>
          for {
            _ <- rw watch "key"
            Some(n) <- rw.get("key").parse[Int]
          } yield rw multi { rq =>
            for {
              _ <- rq.set("key", n + 1)
            } yield n
          }
        }
      }
      Future.sequence(futures).await
      r.sync.get("key").parse[Int] should be(Some(100))
    }
    it("should handle complex request") {
      r.sync.rpush("mykey1", 5)
      r.set("mykey2", "hello")
      r.hset("mykey3", "hello", 7)
      val result = r watch { rw =>
        for {
          _ <- rw.watch("mykey1")
          _ <- rw.watch("mykey2")
          _ <- rw.watch("mykey3")
          Some(a) <- rw.lindex("mykey1", 0).parse[Int]
          Some(b) <- rw.get("mykey2").parse[String]
          Some(c) <- rw.hget("mykey3", b).parse[Int]
        } yield rw.multi { rq =>
          for {
            _ <- rq.rpush("mykey1", a + 1)
            _ <- rq.hset("mykey3", b, c + 1)
          } yield (a, b, c)
        }
      }
      result.get should be(5, "hello", 7)
      r.sync.lrange("mykey1").parse[Int] should be(Some(List(5, 6)))
      r.sync.hget("mykey3", "hello").parse[Int] should be(Some(8))
    }
  }

  describe("sort") {
    it("should do a simple sort") {
      List(6, 3, 5, 47, 1, 1, 4, 9) foreach (r.lpush("sortlist", _))
      r.sync.sort("sortlist").parse[Int].flatten should be(List(1, 1, 3, 4, 5, 6, 9, 47))
    }
    it("should do a lexical sort") {
      List("lorem", "ipsum", "dolor", "sit", "amet") foreach (r.lpush("sortlist", _))
      List(3, 7) foreach (r.lpush("sortlist", _))
      r.sync.sort("sortlist", alpha = true).parse[String].flatten should be(List("3", "7", "amet", "dolor", "ipsum", "lorem", "sit"))
    }
    it("should return an empty list if key not found") {
      r.sync.sort("sortnotfound") should be(Nil)
    }
    it("should return multiple items") {
      val list = List(("item1", "data1", 1, 4),
        ("item2", "data2", 2, 8),
        ("item3", "data3", 3, 1),
        ("item4", "data4", 4, 6),
        ("item5", "data5", 5, 3))
      for ((key, data, num, rank) <- list) {
        r.quiet.sadd("items", key)
        r.quiet.set("data::" + key, data)
        r.quiet.set("num::" + key, num)
        r.quiet.set("rank::" + key, rank)
      }
      r.quiet.del(List("num::item1"))
      r.sync.sort("items",
        get = Seq("#", "data::*", "num::*"),
        by = Some("rank::*"),
        limit = Limit(1, 3)).parse[String] should be(List(Some("item5"), Some("data5"), Some("5"),
          Some("item1"), Some("data1"), None,
          Some("item4"), Some("data4"), Some("4")))
      r.sync.sort("items",
        get = Seq("#", "data::*", "num::*"),
        by = Some("rank::*"),
        limit = Limit(1, 3)).parse[String, String, Int] should be(List((Some("item5"), Some("data5"), Some(5)),
          (Some("item1"), Some("data1"), None),
          (Some("item4"), Some("data4"), Some(4))))
    }
  }
}
