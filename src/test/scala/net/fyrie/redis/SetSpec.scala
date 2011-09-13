package net.fyrie.redis

import org.specs2._

class SetSpec extends mutable.Specification with TestClient {

  "sadd" >> {
    "should add a non-existent value to the set" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
    }
    "should not add an existing value to the set" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "foo") === false
    }
    "should fail if the key points to a non-set" ! client { r ⇒
      r.sync.lpush("list-1", "foo") === (1)
      r.sync.sadd("list-1", "foo") must throwA[RedisErrorException]("ERR Operation against a key holding the wrong kind of value")
    }
  }

  "srem" >> {
    "should remove a value from the set" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.srem("set-1", "bar") === true
      r.sync.srem("set-1", "foo") === true
    }
    "should not do anything if the value does not exist" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.srem("set-1", "bar") === false
    }
    "should fail if the key points to a non-set" ! client { r ⇒
      r.sync.lpush("list-1", "foo") === 1
      r.sync.srem("list-1", "foo") must throwA[RedisErrorException]("ERR Operation against a key holding the wrong kind of value")
    }
  }

  "spop" >> {
    "should pop a random element" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.spop("set-1").parse[String] must beOneOf(Some("foo"), Some("bar"), Some("baz"))
    }
    "should return nil if the key does not exist" ! client { r ⇒
      r.sync.spop("set-1") === (None)
    }
  }

  "smove" >> {
    "should move from one set to another" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true

      r.sync.sadd("set-2", "1") === true
      r.sync.sadd("set-2", "2") === true

      r.sync.smove("set-1", "set-2", "baz") === true
      r.sync.sadd("set-2", "baz") === false
      r.sync.sadd("set-1", "baz") === true
    }
    "should return 0 if the element does not exist in source set" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.smove("set-1", "set-2", "bat") === false
      r.sync.smove("set-3", "set-2", "bat") === false
    }
    "should give error if the source or destination key is not a set" ! client { r ⇒
      r.sync.lpush("list-1", "foo") === (1)
      r.sync.lpush("list-1", "bar") === (2)
      r.sync.lpush("list-1", "baz") === (3)
      r.sync.sadd("set-1", "foo") === true
      r.sync.smove("list-1", "set-1", "bat") must throwA[RedisErrorException]("ERR Operation against a key holding the wrong kind of value")
    }
  }

  "scard" >> {
    "should return cardinality" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.scard("set-1") === (3)
    }
    "should return 0 if key does not exist" ! client { r ⇒
      r.sync.scard("set-1") === (0)
    }
  }

  "sismember" >> {
    "should return true for membership" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.sismember("set-1", "foo") === true
    }
    "should return false for no membership" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.sismember("set-1", "fo") === false
    }
    "should return false if key does not exist" ! client { r ⇒
      r.sync.sismember("set-1", "fo") === false
    }
  }

  "sinter" >> {
    "should return intersection" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true

      r.sync.sadd("set-2", "foo") === true
      r.sync.sadd("set-2", "bat") === true
      r.sync.sadd("set-2", "baz") === true

      r.sync.sadd("set-3", "for") === true
      r.sync.sadd("set-3", "bat") === true
      r.sync.sadd("set-3", "bay") === true

      r.sync.sinter(Set("set-1", "set-2")).parse[String] === (Set("foo", "baz"))
      r.sync.sinter(Set("set-1", "set-3")).parse[String] === (Set.empty)
    }
    "should return empty set for non-existing key" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.sinter(Set("set-1", "set-4")).parse[String] === (Set.empty)
    }
  }

  "sinterstore" >> {
    "should store intersection" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true

      r.sync.sadd("set-2", "foo") === true
      r.sync.sadd("set-2", "bat") === true
      r.sync.sadd("set-2", "baz") === true

      r.sync.sadd("set-3", "for") === true
      r.sync.sadd("set-3", "bat") === true
      r.sync.sadd("set-3", "bay") === true

      r.sync.sinterstore("set-r", Set("set-1", "set-2")) === (2)
      r.sync.scard("set-r") === (2)
      r.sync.sinterstore("set-s", Set("set-1", "set-3")) === (0)
      r.sync.scard("set-s") === (0)
    }
    "should return empty set for non-existing key" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.sinterstore("set-r", Seq("set-1", "set-4")) === (0)
      r.sync.scard("set-r") === (0)
    }
  }

  "sunion" >> {
    "should return union" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true

      r.sync.sadd("set-2", "foo") === true
      r.sync.sadd("set-2", "bat") === true
      r.sync.sadd("set-2", "baz") === true

      r.sync.sadd("set-3", "for") === true
      r.sync.sadd("set-3", "bat") === true
      r.sync.sadd("set-3", "bay") === true

      r.sync.sunion(Set("set-1", "set-2")).parse[String] === (Set("foo", "bar", "baz", "bat"))
      r.sync.sunion(Set("set-1", "set-3")).parse[String] === (Set("foo", "bar", "baz", "for", "bat", "bay"))
    }
    "should return empty set for non-existing key" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.sunion(Seq("set-1", "set-2")).parse[String] === (Set("foo", "bar", "baz"))
    }
  }

  "sunionstore" >> {
    "should store union" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true

      r.sync.sadd("set-2", "foo") === true
      r.sync.sadd("set-2", "bat") === true
      r.sync.sadd("set-2", "baz") === true

      r.sync.sadd("set-3", "for") === true
      r.sync.sadd("set-3", "bat") === true
      r.sync.sadd("set-3", "bay") === true

      r.sync.sunionstore("set-r", Set("set-1", "set-2")) === (4)
      r.sync.scard("set-r") === (4)
      r.sync.sunionstore("set-s", Set("set-1", "set-3")) === (6)
      r.sync.scard("set-s") === (6)
    }
    "should treat non-existing keys as empty sets" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.sunionstore("set-r", Set("set-1", "set-4")) === (3)
      r.sync.scard("set-r") === (3)
    }
  }

  "sdiff" >> {
    "should return diff" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true

      r.sync.sadd("set-2", "foo") === true
      r.sync.sadd("set-2", "bat") === true
      r.sync.sadd("set-2", "baz") === true

      r.sync.sadd("set-3", "for") === true
      r.sync.sadd("set-3", "bat") === true
      r.sync.sadd("set-3", "bay") === true

      r.sync.sdiff("set-1", Set("set-2", "set-3")).parse[String] === (Set("bar"))
    }
    "should treat non-existing keys as empty sets" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.sdiff("set-1", Set("set-2")).parse[String] === (Set("foo", "bar", "baz"))
    }
  }

  "smembers" >> {
    "should return members of a set" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.smembers("set-1").parse[String] === (Set("foo", "bar", "baz"))
    }
    "should return None for an empty set" ! client { r ⇒
      r.sync.smembers("set-1").parse[String] === (Set.empty)
    }
  }

  "srandmember" >> {
    "should return a random member" ! client { r ⇒
      r.sync.sadd("set-1", "foo") === true
      r.sync.sadd("set-1", "bar") === true
      r.sync.sadd("set-1", "baz") === true
      r.sync.srandmember("set-1").parse[String] must beOneOf(Some("foo"), Some("bar"), Some("baz"))
    }
    "should return None for a non-existing key" ! client { r ⇒
      r.sync.srandmember("set-1").parse[String] === (None)
    }
  }
}
