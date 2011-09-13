package net.fyrie.redis

import org.specs2._

class HashSpec extends mutable.Specification with TestClient {

  "hset" >> {
    "should set and get fields" ! client { r ⇒
      r.quiet.hset("hash1", "field1", "val")
      r.sync.hget("hash1", "field1").parse[String] must_== Some("val")
    }

    "should set and get maps" ! client { r ⇒
      r.quiet.hmset("hash2", Map("field1" -> "val1", "field2" -> "val2"))
      r.sync.hmget("hash2", Seq("field1")).parse[String] must_== List(Some("val1"))
      r.sync.hmget("hash2", Seq("field1", "field2")).parse[String] must_== List(Some("val1"), Some("val2"))
      r.sync.hmget("hash2", Seq("field1", "field2", "field3")).parse[String] must_== List(Some("val1"), Some("val2"), None)
    }

    "should increment map values" ! client { r ⇒
      r.sync.hincrby("hash3", "field1", 1) must_== 1
      r.sync.hget("hash3", "field1").parse[String] must_== Some("1")
    }

    "should check existence" ! client { r ⇒
      r.quiet.hset("hash4", "field1", "val")
      r.sync.hexists("hash4", "field1") must beTrue
      r.sync.hexists("hash4", "field2") must beFalse
    }

    "should delete fields" ! client { r ⇒
      r.quiet.hset("hash5", "field1", "val")
      r.sync.hexists("hash5", "field1") must beTrue
      r.quiet.hdel("hash5", "field1")
      r.sync.hexists("hash5", "field1") must beFalse
    }

    "should return the length of the fields" ! client { r ⇒
      r.quiet.hmset("hash6", Map("field1" -> "val1", "field2" -> "val2"))
      r.sync.hlen("hash6") must_== 2
    }

    "should return the aggregates" ! client { r ⇒
      r.quiet.hmset("hash7", Map("field1" -> "val1", "field2" -> "val2"))
      r.sync.hkeys("hash7").parse[String] must_== Set("field1", "field2")
      r.sync.hvals("hash7").parse[String] must_== Set("val1", "val2")
      r.sync.hgetall("hash7").parse[String, String] must_== Map("field1" -> "val1", "field2" -> "val2")
    }
  }
}

