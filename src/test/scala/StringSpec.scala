package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import serialization._

class StringOperationsSpec extends Spec 
                           with ShouldMatchers
                           with RedisTestServer {

  describe("set") {
    it("should set key/value pairs") {
      r.sync.set("anshin-1", "debasish")
      r.sync.set("anshin-2", "maulindu")
    }
  }

  describe("get") {
    it("should retrieve key/value pairs for existing keys") {
      r.quiet.set("anshin-1", "debasish")
      r.sync.get("anshin-1").parse[String] should equal(Some("debasish"))
    }
    it("should fail for non-existent keys") {
      r.sync.get("anshin-2").parse[String] should be(None)
    }
  }

  describe("getset") {
    it("should set new values and return old values") {
      r.set("anshin-1", "debasish")
      r.sync.get("anshin-1").parse[String] should equal(Some("debasish"))
      r.sync.getset("anshin-1", "maulindu").parse[String] should equal(Some("debasish"))
      r.sync.get("anshin-1").parse[String] should equal(Some("maulindu"))
    }
  }

  describe("setnx") {
    it("should set only if the key does not exist") {
      r.set("anshin-1", "debasish")
      r.sync.setnx("anshin-1", "maulindu") should equal(false)
      r.sync.setnx("anshin-2", "maulindu") should equal(true)
    }
  }

  describe("incr") {
    it("should increment by 1 for a key that contains a number") {
      r.set("anshin-1", "10")
      r.sync.incr("anshin-1") should equal(11)
    }
    it("should reset to 0 and then increment by 1 for a key that contains a diff type") {
      r.set("anshin-2", "debasish")
      try {
        r.sync.incr("anshin-2")
      } catch { case ex => ex.getMessage should startWith("ERR value is not an integer") }
    }
    it("should increment by 5 for a key that contains a number") {
      r.set("anshin-3", "10")
      r.sync.incrby("anshin-3", 5) should equal(15)
    }
    it("should reset to 0 and then increment by 5 for a key that contains a diff type") {
      r.set("anshin-4", "debasish")
      try {
        r.sync.incrby("anshin-4", 5)
      } catch { case ex => ex.getMessage should startWith("ERR value is not an integer") }
    }
  }

  describe("decr") {
    it("should decrement by 1 for a key that contains a number") {
      r.set("anshin-1", "10")
      r.sync.decr("anshin-1") should equal(9)
    }
    it("should reset to 0 and then decrement by 1 for a key that contains a diff type") {
      r.set("anshin-2", "debasish")
      try {
        r.sync.decr("anshin-2")
      } catch { case ex => ex.getMessage should startWith("ERR value is not an integer") }
    }
    it("should decrement by 5 for a key that contains a number") {
      r.set("anshin-3", "10")
      r.sync.decrby("anshin-3", 5) should equal(5)
    }
    it("should reset to 0 and then decrement by 5 for a key that contains a diff type") {
      r.set("anshin-4", "debasish")
      try {
        r.sync.decrby("anshin-4", 5)
      } catch { case ex => ex.getMessage should startWith("ERR value is not an integer") }
    }
  }

  describe("mget") {
    it("should get values for existing keys") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.set("anshin-3", "nilanjan")
      r.sync.mget(Seq("anshin-1", "anshin-2", "anshin-3")).parse[String] should equal(Some(List(Some("debasish"), Some("maulindu"), Some("nilanjan"))))
    }
    it("should give None for non-existing keys") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.mget(Seq("anshin-1", "anshin-2", "anshin-4")).parse[String] should equal(Some(List(Some("debasish"), Some("maulindu"), None)))
    }
  }

  describe("mset") {
    it("should set all keys irrespective of whether they exist") {
      r.sync.mset(Map(
        ("anshin-1", "debasish"), 
        ("anshin-2", "maulindu"),
        ("anshin-3", "nilanjan")))
    }

    it("should set all keys only if none of them exist") {
      r.sync.msetnx(Map(
        ("anshin-4", "debasish"), 
        ("anshin-5", "maulindu"),
        ("anshin-6", "nilanjan"))) should equal(true)
      r.sync.msetnx(Map(
        ("anshin-7", "debasish"), 
        ("anshin-8", "maulindu"),
        ("anshin-6", "nilanjan"))) should equal(false)
      r.sync.msetnx(Map(
        ("anshin-4", "debasish"), 
        ("anshin-5", "maulindu"),
        ("anshin-6", "nilanjan"))) should equal(false)
    }
  }

  describe("get with spaces in keys") {
    it("should retrieve key/value pairs for existing keys") {
      r.set("anshin software", "debasish ghosh")
      r.sync.get("anshin software").parse[String] should equal(Some("debasish ghosh"))

      r.set("test key with spaces", "I am a value with spaces")
      r.sync.get("test key with spaces").parse[String] should equal(Some("I am a value with spaces"))
    }
  }

  describe("set with newline values") {
    it("should set key/value pairs") {
      r.sync.set("anshin-x", "debasish\nghosh\nfather")
      r.sync.set("anshin-y", "maulindu\nchatterjee")
    }
  }

  describe("get with newline values") {
    it("should retrieve key/value pairs for existing keys") {
      r.set("anshin-x", "debasish\nghosh\nfather")
      r.sync.get("anshin-x").parse[String] should equal(Some("debasish\nghosh\nfather"))
    }
  }
}
