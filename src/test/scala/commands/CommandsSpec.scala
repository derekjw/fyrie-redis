package net.fyrie.redis
package commands

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class CommandsSpec extends Spec 
                   with ShouldMatchers
                   with RedisTestServer {

  describe("keys") {
    it("should fetch keys") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
      r send keys("anshin*") map (_.size) should equal(Some(2))
    }

    it("should fetch keys with spaces") {
      r send set("anshin 1", "debasish")
      r send set("anshin 2", "maulindu")
      r send keys("anshin*") map (_.size) should equal(Some(2))
    }
  }

  describe("randomkey") {
    it("should give") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
      r send randomkey map (mkString) getOrElse fail("No key returned") should startWith("anshin")
    }
  }

  describe("rename") {
    it("should give") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
      r send rename("anshin-2", "anshin-2-new")
      val thrown = evaluating { r send rename("anshin-2", "anshin-2-new") } should produce[Exception]
      thrown.getMessage should equal ("ERR no such key")
    }
  }

  describe("renamenx") {
    it("should give") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
      r send renamenx("anshin-2", "anshin-2-new") should equal(true)
      r send renamenx("anshin-1", "anshin-2-new") should equal(false)
    }
  }

  describe("dbsize") {
    it("should give") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
      r send dbsize should equal(2)
    }
  }

  describe("exists") {
    it("should give") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
      r send exists("anshin-2") should equal(true)
      r send exists("anshin-1") should equal(true)
      r send exists("anshin-3") should equal(false)
    }
  }

  describe("del") {
    it("should give") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
      r send del(Seq("anshin-2", "anshin-1")) should equal(2)
      r send del(Seq("anshin-2", "anshin-1")) should equal(0)
    }
  }

  describe("type") {
    it("should give") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
      r send getType("anshin-2") should equal("string")
    }
  }

  describe("expire") {
    it("should give") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
      r send expire("anshin-2", 1000) should equal(true)
      r send expire("anshin-3", 1000) should equal(false)
    }
  }

  describe("Multi exec commands") {
    it("should work with single commands") {
      r send multiexec(Seq(set("testkey1", "testvalue1"))) should be(Some(List(())))
    }
    it("should work with several commands") {
      r send multiexec(Seq(
        set("testkey1", "testvalue1"),
        dbsize,
        set("testkey2", "testvalue2"),
        dbsize,
        mget(Seq("testkey1", "testkey2"))
      )) map (_.map{
        case Some(List(Some(x: Array[Byte]), Some(y: Array[Byte]))) => Some(List(Some(new String(x)), Some(new String(y))))
        case x => x
      }) should be(Some(List((), 1, (), 2, Some(List(Some("testvalue1"), Some("testvalue2"))))))
    }
  }

  def mkList {
    List(6, 3, 5, 47, 1, 1, 4, 9) foreach (r send lpush("sortlist", _))
  }

  def mkStringList {
    List("lorem", "ipsum", "dolor", "sit", "amet", 3, 7) foreach (r send lpush("sortlist", _))
  }

  describe("sort") {
    it("should do a simple sort") {
      mkList
      (r send sort("sortlist")).map(mkString(_).map(_.toInt)) should be(Some(List(1, 1, 3, 4, 5, 6, 9, 47)))
    }
    it("should do a lexical sort") {
      mkStringList
      (r send sort("sortlist", alpha = true)).map(mkString) should be(Some(List("3", "7", "amet", "dolor", "ipsum", "lorem", "sit")))
    }
  }   
}
