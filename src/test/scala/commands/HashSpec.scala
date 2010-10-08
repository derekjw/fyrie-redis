package net.fyrie.redis

import Commands._

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class HashOperationsSpec extends Spec with ShouldMatchers with RedisTestServer {

  describe("hset") {
    it("should set and get fields") {
      r send hset("hash1", "field1", "val")
      r send hget("hash1", "field1") should be(Result("val"))
    }
    
    it("should set and get maps") {
      r send hmset("hash2", Map("field1" -> "val1", "field2" -> "val2"))
      r send hmget("hash2", Seq("field1")) should be(Result(Seq(Some("val1"))))
      r send hmget("hash2", Seq("field1", "field2")) should be(Result(Seq(Some("val1"), Some("val2"))))
      r send hmget("hash2", Seq("field1", "field2", "field3")) should be(Result(Seq(Some("val1"), Some("val2"), None)))
    }
    
    it("should increment map values") {
      r send hincrby("hash3", "field1", 1) should be(1)
      r send hget("hash3", "field1") should be(Result("1"))
    }
    
    it("should check existence") {
      r send hset("hash4", "field1", "val")
      r send hexists("hash4", "field1") should equal(true)
      r send hexists("hash4", "field2") should equal(false)
    }
    
    it("should delete fields") {
      r send hset("hash5", "field1", "val")
      r send hexists("hash5", "field1") should equal(true)
      r send hdel("hash5", "field1")
      r send hexists("hash5", "field1") should equal(false)
    }
    
    it("should return the length of the fields") {
      r send hmset("hash6", Map("field1" -> "val1", "field2" -> "val2"))
      r send hlen("hash6") should be(2)
    }
    
    it("should return the aggregates") {
      r send hmset("hash7", Map("field1" -> "val1", "field2" -> "val2"))
      r send hkeys("hash7") map (_.toSet) should be(Result(Set("field1", "field2")))
      r send hvals("hash7") should be(Result(Seq("val1", "val2")))
      r send hgetall("hash7") map (_.toMap) should be(Result(Map("field1" -> "val1", "field2" -> "val2")))
    }
  }
}
