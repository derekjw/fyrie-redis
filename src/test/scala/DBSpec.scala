package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

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

/*  describe("Multi exec commands") {
    it("should work with single commands") {
      r.multiexec(Seq(set("testkey1", "testvalue1"))) should be(Some(List(())))
    }
    it("should work with several commands") {
      r.multiexec(Seq(
        set("testkey1", "testvalue1"),
        dbsize,
        set("testkey2", "testvalue2"),
        dbsize,
        mget(Seq("testkey1", "testkey2")))) should be(Some(List((), 1, (), 2, Some(List(Some("testvalue1"), Some("testvalue2"))))))
    }
    it("should throw an error") {
      val thrown = evaluating {
        (r.multiexec(Seq(
          set("a", "abc"),
          lpop("a"),
          get("a")))).map(_.force)
      } should produce[Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("sort") {
    it("should do a simple sort") {
      List(6, 3, 5, 47, 1, 1, 4, 9) foreach (r.lpush("sortlist", _))
      (r.sort("sortlist")).map(_.flatMap(_.map(_.toInt))) should be(Some(List(1, 1, 3, 4, 5, 6, 9, 47)))
    }
    it("should do a lexical sort") {
      List("lorem", "ipsum", "dolor", "sit", "amet", 3, 7) foreach (r.lpush("sortlist", _))
      r.sort[String]("sortlist", alpha = true) map (_.flatten) should be(Some(List("3", "7", "amet", "dolor", "ipsum", "lorem", "sit")))
    }
    it("should return multiple items") {
      import serialization.Parse.Implicits._
      val list = List(("item1", "data1", 1, 4),
                      ("item2", "data2", 2, 8),
                      ("item3", "data3", 3, 1),
                      ("item4", "data4", 4, 6),
                      ("item5", "data5", 5, 3))
      for ((key, data, num, rank) <- list) {
        r.sadd("items", key)
        r.set("data::"+key, data)
        r.set("num::"+key, num)
        r.set("rank::"+key, rank)
      }
      r.del(List("num::item1"))
      r.sort3[String, String, Int]("items",
                                        get = ("#", "data::*", "num::*"),
                                        by = Some("rank::*"),
                                        limit = Some((1,3))) should be(Some(List((Some("item5"), Some("data5"), Some(5)),
                                                                                 (Some("item1"), Some("data1"), None),
                                                                                 (Some("item4"), Some("data4"), Some(4)))))
    }
  }*/
}
