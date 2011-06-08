package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import serialization._

class StringOperationsSpec extends Spec 
                           with ShouldMatchers
                           with RedisTestServer {

  describe("set") {
    it("should set key/value pairs") {
      r.set("anshin-1", "debasish").get
      r.set("anshin-2", "maulindu").get
    }
  }

  describe("get") {
    it("should retrieve key/value pairs for existing keys") {
      r.set("anshin-1", "debasish")
      r.get("anshin-1").parse[String].get should equal(Some("debasish"))
    }
    it("should fail for non-existent keys") {
      r.get("anshin-2").parse[String].get should be(None)
    }
  }

  describe("getset") {
    it("should set new values and return old values") {
      r.set("anshin-1", "debasish")
      r.get("anshin-1").parse[String].get should equal(Some("debasish"))
      r.getset("anshin-1", "maulindu").parse[String].get should equal(Some("debasish"))
      r.get("anshin-1").parse[String].get should equal(Some("maulindu"))
    }
  }

  describe("setnx") {
    it("should set only if the key does not exist") {
      r.set("anshin-1", "debasish")
      r.setnx("anshin-1", "maulindu").get should equal(false)
      r.setnx("anshin-2", "maulindu").get should equal(true)
    }
  }

  describe("incr") {
    it("should increment by 1 for a key that contains a number") {
      r.set("anshin-1", "10")
      r.incr("anshin-1").get should equal(11)
    }
    it("should reset to 0 and then increment by 1 for a key that contains a diff type") {
      r.set("anshin-2", "debasish")
      try {
        r.incr("anshin-2").get
      } catch { case ex => ex.getMessage should startWith("ERR value is not an integer") }
    }
    it("should increment by 5 for a key that contains a number") {
      r.set("anshin-3", "10")
      r.incrby("anshin-3", 5).get should equal(15)
    }
    it("should reset to 0 and then increment by 5 for a key that contains a diff type") {
      r.set("anshin-4", "debasish")
      try {
        r.incrby("anshin-4", 5).get
      } catch { case ex => ex.getMessage should startWith("ERR value is not an integer") }
    }
  }

  describe("decr") {
    it("should decrement by 1 for a key that contains a number") {
      r.set("anshin-1", "10")
      r.decr("anshin-1").get should equal(9)
    }
    it("should reset to 0 and then decrement by 1 for a key that contains a diff type") {
      r.set("anshin-2", "debasish")
      try {
        r.decr("anshin-2").get
      } catch { case ex => ex.getMessage should startWith("ERR value is not an integer") }
    }
    it("should decrement by 5 for a key that contains a number") {
      r.set("anshin-3", "10")
      r.decrby("anshin-3", 5).get should equal(5)
    }
    it("should reset to 0 and then decrement by 5 for a key that contains a diff type") {
      r.set("anshin-4", "debasish")
      try {
        r.decrby("anshin-4", 5).get
      } catch { case ex => ex.getMessage should startWith("ERR value is not an integer") }
    }
  }

  describe("mget") {
    it("should get values for existing keys") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.set("anshin-3", "nilanjan")
      r.mget(Seq("anshin-1", "anshin-2", "anshin-3")).parse[String].get should equal(Some(List(Some("debasish"), Some("maulindu"), Some("nilanjan"))))
    }
    it("should give None for non-existing keys") {
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.mget(Seq("anshin-1", "anshin-2", "anshin-4")).parse[String].get should equal(Some(List(Some("debasish"), Some("maulindu"), None)))
    }
  }

  describe("mset") {
    it("should set all keys irrespective of whether they exist") {
      r.mset(Map(
        ("anshin-1", "debasish"), 
        ("anshin-2", "maulindu"),
        ("anshin-3", "nilanjan"))).get
    }

    it("should set all keys only if none of them exist") {
      r.msetnx(Map(
        ("anshin-4", "debasish"), 
        ("anshin-5", "maulindu"),
        ("anshin-6", "nilanjan"))).get should equal(true)
      r.msetnx(Map(
        ("anshin-7", "debasish"), 
        ("anshin-8", "maulindu"),
        ("anshin-6", "nilanjan"))).get should equal(false)
      r.msetnx(Map(
        ("anshin-4", "debasish"), 
        ("anshin-5", "maulindu"),
        ("anshin-6", "nilanjan"))).get should equal(false)
    }
  }

  describe("get with spaces in keys") {
    it("should retrieve key/value pairs for existing keys") {
      r.set("anshin software", "debasish ghosh")
      r.get("anshin software").parse[String].get should equal(Some("debasish ghosh"))

      r.set("test key with spaces", "I am a value with spaces")
      r.get("test key with spaces").parse[String].get should equal(Some("I am a value with spaces"))
    }
  }

  describe("set with newline values") {
    it("should set key/value pairs") {
      r.set("anshin-x", "debasish\nghosh\nfather")
      r.set("anshin-y", "maulindu\nchatterjee").get
    }
  }

  describe("get with newline values") {
    it("should retrieve key/value pairs for existing keys") {
      r.set("anshin-x", "debasish\nghosh\nfather")
      r.get("anshin-x").parse[String].get should equal(Some("debasish\nghosh\nfather"))
    }
  }
}
