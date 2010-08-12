package com.redis
package commands

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class StringOperationsSpec extends Spec 
                           with ShouldMatchers
                           with RedisTestServer {

  describe("set") {
    it("should set key/value pairs") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
    }
  }

  describe("get") {
    it("should retrieve key/value pairs for existing keys") {
      r send set("anshin-1", "debasish")
      r send get("anshin-1") map (mkString) match {
        case Some(s: String) => s should equal("debasish")
        case None => fail("should return debasish")
      }
    }
    it("should fail for non-existent keys") {
      r send get("anshin-2") map (mkString) should be(None)
    }
  }

  describe("getset") {
    it("should set new values and return old values") {
      r send set("anshin-1", "debasish")
      r send get("anshin-1") map (mkString) match {
        case Some(s: String) => s should equal("debasish")
        case None => fail("should return debasish")
      }
      r send getset("anshin-1", "maulindu") map (mkString) match {
        case Some(s: String) => s should equal("debasish")
        case None => fail("should return debasish")
      }
      r send get("anshin-1") map (mkString) match {
        case Some(s: String) => s should equal("maulindu")
        case None => fail("should return maulindu")
      }
    }
  }

  describe("setnx") {
    it("should set only if the key does not exist") {
      r send set("anshin-1", "debasish")
      r send setnx("anshin-1", "maulindu") should equal(false)
      r send setnx("anshin-2", "maulindu") should equal(true)
    }
  }

  describe("incr") {
    it("should increment by 1 for a key that contains a number") {
      r send set("anshin-1", "10")
      r send incr("anshin-1") should equal(11)
    }
    it("should reset to 0 and then increment by 1 for a key that contains a diff type") {
      r send set("anshin-2", "debasish")
      try {
        r send incr("anshin-2")
      } catch { case ex => ex.getMessage should equal("ERR value is not an integer") }
    }
    it("should increment by 5 for a key that contains a number") {
      r send set("anshin-3", "10")
      r send incrby("anshin-3", 5) should equal(15)
    }
    it("should reset to 0 and then increment by 5 for a key that contains a diff type") {
      r send set("anshin-4", "debasish")
      try {
        r send incrby("anshin-4", 5)
      } catch { case ex => ex.getMessage should equal("ERR value is not an integer") }
    }
  }

  describe("decr") {
    it("should decrement by 1 for a key that contains a number") {
      r send set("anshin-1", "10")
      r send decr("anshin-1") should equal(9)
    }
    it("should reset to 0 and then decrement by 1 for a key that contains a diff type") {
      r send set("anshin-2", "debasish")
      try {
        r send decr("anshin-2")
      } catch { case ex => ex.getMessage should equal("ERR value is not an integer") }
    }
    it("should decrement by 5 for a key that contains a number") {
      r send set("anshin-3", "10")
      r send decrby("anshin-3", 5) should equal(5)
    }
    it("should reset to 0 and then decrement by 5 for a key that contains a diff type") {
      r send set("anshin-4", "debasish")
      try {
        r send decrby("anshin-4", 5)
      } catch { case ex => ex.getMessage should equal("ERR value is not an integer") }
    }
  }

  describe("mget") {
    it("should get values for existing keys") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
      r send set("anshin-3", "nilanjan")
      r send mget(Seq("anshin-1", "anshin-2", "anshin-3")) map (_.map(mkString)) should equal(Some(List(Some("debasish"), Some("maulindu"), Some("nilanjan"))))
    }
    it("should give None for non-existing keys") {
      r send set("anshin-1", "debasish")
      r send set("anshin-2", "maulindu")
      r send mget(Seq("anshin-1", "anshin-2", "anshin-4")) map (_.map(mkString)) should equal(Some(List(Some("debasish"), Some("maulindu"), None)))
    }
  }

  describe("mset") {
    it("should set all keys irrespective of whether they exist") {
      r send mset(Map(
        ("anshin-1", "debasish"), 
        ("anshin-2", "maulindu"),
        ("anshin-3", "nilanjan")))
    }

    it("should set all keys only if none of them exist") {
      r send msetnx(Map(
        ("anshin-4", "debasish"), 
        ("anshin-5", "maulindu"),
        ("anshin-6", "nilanjan"))) should equal(true)
      r send msetnx(Map(
        ("anshin-7", "debasish"), 
        ("anshin-8", "maulindu"),
        ("anshin-6", "nilanjan"))) should equal(false)
      r send msetnx(Map(
        ("anshin-4", "debasish"), 
        ("anshin-5", "maulindu"),
        ("anshin-6", "nilanjan"))) should equal(false)
    }
  }

  describe("get with spaces in keys") {
    it("should retrieve key/value pairs for existing keys") {
      r send set("anshin software", "debasish ghosh")
      r send get("anshin software") map (mkString) match {
        case Some(s: String) => s should equal("debasish ghosh")
        case None => fail("should return debasish ghosh")
      }

      r send set("test key with spaces", "I am a value with spaces")
      r send get("test key with spaces") map (mkString) should equal(Some("I am a value with spaces"))
    }
  }

  describe("set with newline values") {
    it("should set key/value pairs") {
      r send set("anshin-x", "debasish\nghosh\nfather")
      r send set("anshin-y", "maulindu\nchatterjee")
    }
  }

  describe("get with newline values") {
    it("should retrieve key/value pairs for existing keys") {
      r send set("anshin-x", "debasish\nghosh\nfather")
      r send get("anshin-x") map (mkString) match {
        case Some(s: String) => s should equal("debasish\nghosh\nfather")
        case None => fail("should return debasish")
      }
    }
  }
}

