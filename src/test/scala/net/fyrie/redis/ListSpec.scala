package net.fyrie.redis

import org.specs2._

class ListSpec extends mutable.Specification with TestClient {

  "lpush" >> {
    "should add to the head of the list" ! client { r ⇒
      r.sync.lpush("list-1", "foo") === (1)
      r.sync.lpush("list-1", "bar") === (2)
    }
    "should throw if the key has a non-list value" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.sync.lpush("anshin-1", "bar") must throwA[RedisErrorException]("ERR Operation against a key holding the wrong kind of value")
    }
  }

  "rpush" >> {
    "should add to the head of the list" ! client { r ⇒
      r.sync.rpush("list-1", "foo") === (1)
      r.sync.rpush("list-1", "bar") === (2)
    }
    "should throw if the key has a non-list value" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.sync.rpush("anshin-1", "bar") must throwA[RedisErrorException]("ERR Operation against a key holding the wrong kind of value")
    }
  }

  "llen" >> {
    "should return the length of the list" ! client { r ⇒
      r.sync.lpush("list-1", "foo") === (1)
      r.sync.lpush("list-1", "bar") === (2)
      r.sync.llen("list-1") === (2)
    }
    "should return 0 for a non-existent key" ! client { r ⇒
      r.sync.llen("list-2") === (0)
    }
    "should throw for a non-list key" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.sync.llen("anshin-1") must throwA[RedisErrorException]("ERR Operation against a key holding the wrong kind of value")
    }
  }

  "lrange" >> {
    "should return the range" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.sync.lpush("list-1", "3") === (4)
      r.sync.lpush("list-1", "2") === (5)
      r.sync.lpush("list-1", "1") === (6)
      r.sync.llen("list-1") === (6)
      r.sync.lrange("list-1", 0, 4).parse[String] === (Some(List("1", "2", "3", "4", "5")))
    }
    "should return empty list if start > end" ! client { r ⇒
      r.sync.lpush("list-1", "3") === (1)
      r.sync.lpush("list-1", "2") === (2)
      r.sync.lpush("list-1", "1") === (3)
      r.sync.lrange("list-1", 2, 0).parse[String] === (Some(Nil))
    }
    "should treat as end of list if end is over the actual end of list" ! client { r ⇒
      r.sync.lpush("list-1", "3") === (1)
      r.sync.lpush("list-1", "2") === (2)
      r.sync.lpush("list-1", "1") === (3)
      r.sync.lrange("list-1", 0, 7).parse[String] === (Some(List("1", "2", "3")))
    }
  }

  "ltrim" >> {
    "should trim to the input size" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.sync.lpush("list-1", "3") === (4)
      r.sync.lpush("list-1", "2") === (5)
      r.sync.lpush("list-1", "1") === (6)
      r.ltrim("list-1", 0, 3)
      r.sync.llen("list-1") === (4)
    }
    "should should return empty list for start > end" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.ltrim("list-1", 6, 3)
      r.sync.llen("list-1") === (0)
    }
    "should treat as end of list if end is over the actual end of list" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.ltrim("list-1", 0, 12)
      r.sync.llen("list-1") === (3)
    }
  }

  "lindex" >> {
    "should return the value at index" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.sync.lpush("list-1", "3") === (4)
      r.sync.lpush("list-1", "2") === (5)
      r.sync.lpush("list-1", "1") === (6)
      r.sync.lindex("list-1", 2).parse[String] === (Some("3"))
      r.sync.lindex("list-1", 3).parse[String] === (Some("4"))
      r.sync.lindex("list-1", -1).parse[String] === (Some("6"))
    }
    "should return None if the key does not point to a list" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.sync.lindex("list-1", 0).parse[String] === (None)
    }
    "should return empty string for an index out of range" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.sync.lindex("list-1", 8).parse[String] === (None) // the protocol says it will return empty string
    }
  }

  "lset" >> {
    "should set value for key at index" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.sync.lpush("list-1", "3") === (4)
      r.sync.lpush("list-1", "2") === (5)
      r.sync.lpush("list-1", "1") === (6)
      r.lset("list-1", 2, "30")
      r.sync.lindex("list-1", 2).parse[String] === (Some("30"))
    }
    "should generate error for out of range index" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.sync.lset("list-1", 12, "30") must throwA[RedisErrorException]("ERR index out of range")
    }
  }

  "lrem" >> {
    "should remove count elements matching value from beginning" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "hello") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.sync.lpush("list-1", "hello") === (4)
      r.sync.lpush("list-1", "hello") === (5)
      r.sync.lpush("list-1", "hello") === (6)
      r.sync.lrem("list-1", "hello", 2) === (2)
      r.sync.llen("list-1") === (4)
    }
    "should remove all elements matching value from beginning" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "hello") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.sync.lpush("list-1", "hello") === (4)
      r.sync.lpush("list-1", "hello") === (5)
      r.sync.lpush("list-1", "hello") === (6)
      r.sync.lrem("list-1", "hello") === (4)
      r.sync.llen("list-1") === (2)
    }
    "should remove count elements matching value from end" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "hello") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.sync.lpush("list-1", "hello") === (4)
      r.sync.lpush("list-1", "hello") === (5)
      r.sync.lpush("list-1", "hello") === (6)
      r.sync.lrem("list-1", "hello", -2) === (2)
      r.sync.llen("list-1") === (4)
      r.sync.lindex("list-1", -2).parse[String] === (Some("4"))
    }
  }

  "lpop" >> {
    "should pop the first one from head" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.sync.lpush("list-1", "3") === (4)
      r.sync.lpush("list-1", "2") === (5)
      r.sync.lpush("list-1", "1") === (6)
      r.sync.lpop("list-1").parse[String] === (Some("1"))
      r.sync.lpop("list-1").parse[String] === (Some("2"))
      r.sync.lpop("list-1").parse[String] === (Some("3"))
      r.sync.llen("list-1") === (3)
    }
    "should give nil for non-existent key" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.lpop("list-2").parse[String] === (None)
      r.sync.llen("list-1") === (2)
    }
  }

  "rpop" >> {
    "should pop the first one from tail" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.lpush("list-1", "4") === (3)
      r.sync.lpush("list-1", "3") === (4)
      r.sync.lpush("list-1", "2") === (5)
      r.sync.lpush("list-1", "1") === (6)
      r.sync.rpop("list-1").parse[String] === (Some("6"))
      r.sync.rpop("list-1").parse[String] === (Some("5"))
      r.sync.rpop("list-1").parse[String] === (Some("4"))
      r.sync.llen("list-1") === (3)
    }
    "should give nil for non-existent key" ! client { r ⇒
      r.sync.lpush("list-1", "6") === (1)
      r.sync.lpush("list-1", "5") === (2)
      r.sync.rpop("list-2").parse[String] === (None)
      r.sync.llen("list-1") === (2)
    }
  }

  "rpoplpush" >> {
    "should do" ! client { r ⇒
      r.sync.rpush("list-1", "a") === (1)
      r.sync.rpush("list-1", "b") === (2)
      r.sync.rpush("list-1", "c") === (3)
      r.sync.rpush("list-2", "foo") === (1)
      r.sync.rpush("list-2", "bar") === (2)
      r.sync.rpoplpush("list-1", "list-2").parse[String] === (Some("c"))
      r.sync.lindex("list-2", 0).parse[String] === (Some("c"))
      r.sync.llen("list-1") === (2)
      r.sync.llen("list-2") === (3)
    }

    "should rotate the list when src and dest are the same" ! client { r ⇒
      r.sync.rpush("list-1", "a") === (1)
      r.sync.rpush("list-1", "b") === (2)
      r.sync.rpush("list-1", "c") === (3)
      r.sync.rpoplpush("list-1", "list-1").parse[String] === (Some("c"))
      r.sync.lindex("list-1", 0).parse[String] === (Some("c"))
      r.sync.lindex("list-1", 2).parse[String] === (Some("b"))
      r.sync.llen("list-1") === (3)
    }

    "should give None for non-existent key" ! client { r ⇒
      r.sync.rpoplpush("list-1", "list-2").parse[String] === (None)
      r.sync.rpush("list-1", "a") === (1)
      r.sync.rpush("list-1", "b") === (2)
      r.sync.rpoplpush("list-1", "list-2").parse[String] === (Some("b"))
    }
  }

  "lpush with newlines in strings" >> {
    "should add to the head of the list" ! client { r ⇒
      r.sync.lpush("list-1", "foo\nbar\nbaz") === (1)
      r.sync.lpush("list-1", "bar\nfoo\nbaz") === (2)
      r.sync.lpop("list-1").parse[String] === (Some("bar\nfoo\nbaz"))
      r.sync.lpop("list-1").parse[String] === (Some("foo\nbar\nbaz"))
    }
  }

}

