package net.fyrie.redis

import org.specs2._

import akka.dispatch.Future

class KeysSpec extends mutable.Specification with TestClient {

  "keys" >> {
    "should fetch keys" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.keys("anshin*").size must_== 2
    }

    "should fetch keys with spaces" ! client { r ⇒
      r.set("anshin 1", "debasish")
      r.set("anshin 2", "maulindu")
      r.sync.keys("anshin*").size must_== 2
    }
  }

  "randomkey" >> {
    "should give" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.randomkey().parse[String].get must startWith("anshin")
    }
  }

  "rename" >> {
    "should give" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.rename("anshin-2", "anshin-2-new")
      r.sync.rename("anshin-2", "anshin-2-new") must throwA[RedisErrorException]("ERR no such key")
    }
  }

  "renamenx" >> {
    "should give" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.renamenx("anshin-2", "anshin-2-new") must_== true
      r.sync.renamenx("anshin-1", "anshin-2-new") must_== false
    }
  }

  "dbsize" >> {
    "should give" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.dbsize() must_== 2
    }
  }

  "exists" >> {
    "should give" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.exists("anshin-2") must_== true
      r.sync.exists("anshin-1") must_== true
      r.sync.exists("anshin-3") must_== false
    }
  }

  "del" >> {
    "should give" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.del(Set("anshin-2", "anshin-1")) must_== 2
      r.sync.del(Set("anshin-2", "anshin-1")) must_== 0
    }
  }

  "type" >> {
    "should give" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.typeof("anshin-2") must_== "string"
    }
  }

  "expire" >> {
    "should give" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.expire("anshin-2", 1000) must_== true
      r.sync.expire("anshin-3", 1000) must_== false
    }
  }

  "quit" >> {
    "should reconnect" ! client { r ⇒
      r.quit
      r.set("key1", "value1")
      r.quit
      r.sync.get("key1").parse[String] must_== Some("value1")
    }
    /*"should reconnect with many commands" ! client { r ⇒
      (1 to 1000) foreach (_ ⇒ r.incr("incKey"))
      r.quit
      val result1 = r.get("incKey").parse[Int]
      (1 to 1000) foreach (_ ⇒ r.incr("incKey"))
      val result2 = r.get("incKey").parse[Int]
      r.quit
      (1 to 1000) foreach (_ ⇒ r.incr("incKey"))
      val result3 = r.get("incKey").parse[Int]
      result1.get must_== Some(1000)
      result2.get must_== Some(2000)
      result3.get must_== Some(3000)
    }*/
  }

  "Multi exec commands" >> {
    "should work with single commands" ! client { r ⇒
      val result = r.multi { rq ⇒
        rq.set("testkey1", "testvalue1")
      }
      result.get must_== ()
    }
    "should work with several commands" ! client { r ⇒
      val result = r.multi { rq ⇒
        for {
          _ ← rq.set("testkey1", "testvalue1")
          _ ← rq.set("testkey2", "testvalue2")
          x ← rq.mget(List("testkey1", "testkey2")).parse[String]
        } yield x
      }
      result.get must_== List(Some("testvalue1"), Some("testvalue2"))
    }
    "should work with a list of commands" ! client { r ⇒
      val values = List.range(1, 100)
      val result = r.multi { rq ⇒
        val setq = (Queued[Any](()) /: values)((q, v) ⇒ q flatMap (_ ⇒ rq.set(v, v * 2)))
        (setq.map(_ ⇒ List[Future[Option[Int]]]()) /: values)((q, v) ⇒ q flatMap (l ⇒ rq.get(v).parse[Int].map(_ :: l))).map(_.reverse)
      }
      Future.sequence(result).get.flatten must_== values.map(2*)
    }
    "should throw an error" ! client { r ⇒
      val result = r.multi { rq ⇒
        for {
          _ ← rq.set("a", "abc")
          x ← rq.lpop("a").parse[String]
          y ← rq.get("a").parse[String]
        } yield (x, y)
      }
      result._1.get must throwA[RedisErrorException]
      result._2.get must_== Some("abc")
    }
    "should handle invalid requests" ! client { r ⇒
      val result = r.multi { rq ⇒
        for {
          _ ← rq.set("testkey1", "testvalue1")
          _ ← rq.set("testkey2", "testvalue2")
          x ← rq.mget(List[String]()).parse[String]
          y ← rq.mget(List("testkey1", "testkey2")).parse[String]
        } yield (x, y)
      }
      result._1.get must throwA[RedisErrorException]
      result._2.get must_== List(Some("testvalue1"), Some("testvalue2"))
    }
  }

  "watch" >> {
    "should fail without watch" ! client { r ⇒
      r.sync.set("key", 0)
      val clients = List.fill(10)(RedisClient())
      val futures = for (client ← clients; _ ← 1 to 10) yield client.get("key").parse[Int] flatMap { n ⇒ client.set("key", n.get + 1) }
      Future.sequence(futures).await
      clients foreach (_.disconnect)
      r.sync.get("key").parse[Int] must_!= Some(100)
    }
    "should succeed with watch" ! client { r ⇒
      r.sync.set("key", 0)
      val futures = for (_ ← 1 to 100) yield {
        r atomic { rw ⇒
          rw watch "key"
          for {
            Some(n: Int) ← rw.get("key").parse[Int]
          } yield rw multi (_.set("key", n + 1))
        }
      }
      Future.sequence(futures).await
      r.sync.get("key").parse[Int] must_== (Some(100))
    }
    "should handle complex request" ! client { r ⇒
      r.sync.rpush("mykey1", 5)
      r.set("mykey2", "hello")
      r.hset("mykey3", "hello", 7)
      val result = r atomic { rw ⇒
        for {
          _ ← rw.watch("mykey1")
          _ ← rw.watch("mykey2")
          _ ← rw.watch("mykey3")
          Some(a: Int) ← rw.lindex("mykey1", 0).parse[Int]
          Some(b: String) ← rw.get("mykey2").parse[String]
          Some(c: Int) ← rw.hget("mykey3", b).parse[Int]
        } yield rw.multi { rq ⇒
          for {
            _ ← rq.rpush("mykey1", a + 1)
            _ ← rq.hset("mykey3", b, c + 1)
          } yield (a, b, c)
        }
      }
      result.get must_== (5, "hello", 7)
      r.sync.lrange("mykey1").parse[Int] must_== Some(List(5, 6))
      r.sync.hget("mykey3", "hello").parse[Int] must_== Some(8)
    }
  }

  "sort" >> {
    "should do a simple sort" ! client { r ⇒
      List(6, 3, 5, 47, 1, 1, 4, 9) foreach (r.lpush("sortlist", _))
      r.sync.sort("sortlist").parse[Int].flatten must_== List(1, 1, 3, 4, 5, 6, 9, 47)
    }
    "should do a lexical sort" ! client { r ⇒
      List("lorem", "ipsum", "dolor", "sit", "amet") foreach (r.lpush("sortlist", _))
      List(3, 7) foreach (r.lpush("sortlist", _))
      r.sync.sort("sortlist", alpha = true).parse[String].flatten must_== List("3", "7", "amet", "dolor", "ipsum", "lorem", "sit")
    }
    "should return an empty list if key not found" ! client { r ⇒
      r.sync.sort("sortnotfound") must beEmpty
    }
    "should return multiple items" ! client { r ⇒
      val list = List(("item1", "data1", 1, 4),
        ("item2", "data2", 2, 8),
        ("item3", "data3", 3, 1),
        ("item4", "data4", 4, 6),
        ("item5", "data5", 5, 3))
      for ((key, data, num, rank) ← list) {
        r.quiet.sadd("items", key)
        r.quiet.set("data::" + key, data)
        r.quiet.set("num::" + key, num)
        r.quiet.set("rank::" + key, rank)
      }
      r.quiet.del(List("num::item1"))
      r.sync.sort("items",
        get = Seq("#", "data::*", "num::*"),
        by = Some("rank::*"),
        limit = Limit(1, 3)).parse[String] must_== List(Some("item5"), Some("data5"), Some("5"),
          Some("item1"), Some("data1"), None,
          Some("item4"), Some("data4"), Some("4"))
      r.sync.sort("items",
        get = Seq("#", "data::*", "num::*"),
        by = Some("rank::*"),
        limit = Limit(1, 3)).parse[String, String, Int] must_== (List((Some("item5"), Some("data5"), Some(5)),
          (Some("item1"), Some("data1"), None),
          (Some("item4"), Some("data4"), Some(4))))
    }
  }

}
