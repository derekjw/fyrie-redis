package net.fyrie.redis

import org.specs2._

class SortedSetSpec extends mutable.Specification with TestClient {

  private def add(r: RedisClient) = {
    r.sync.zadd("hackers", "yukihiro matsumoto", 1965) === (true)
    r.sync.zadd("hackers", "richard stallman", 1953) === (true)
    r.sync.zadd("hackers", "claude shannon", 1916) === (true)
    r.sync.zadd("hackers", "linus torvalds", 1969) === (true)
    r.sync.zadd("hackers", "alan kay", 1940) === (true)
    r.sync.zadd("hackers", "alan turing", 1912) === (true)
  }

  "zadd" >> {
    "should add based on proper sorted set semantics" ! client { r ⇒
      add(r)
      r.sync.zadd("hackers", "alan turing", 1912) === (false)
      r.sync.zcard("hackers") === (6)
    }
  }

  "zrange" >> {
    "should get the proper range" ! client { r ⇒
      add(r)
      r.sync.zrange("hackers", 0, -1) must have size (6)
      r.sync.zrangeWithScores("hackers", 0, -1) must have size (6)
    }
  }

  "zrank" >> {
    "should give proper rank" ! client { r ⇒
      add(r)
      r.sync.zrank("hackers", "yukihiro matsumoto") === (Some(4))
      r.sync.zrevrank("hackers", "yukihiro matsumoto") === (Some(1))
    }
  }

  "zremrangebyrank" >> {
    "should remove based on rank range" ! client { r ⇒
      add(r)
      r.sync.zremrangebyrank("hackers", 0, 2) === (3)
    }
  }

  "zremrangebyscore" >> {
    "should remove based on score range" ! client { r ⇒
      add(r)
      r.sync.zremrangebyscore("hackers", 1912, 1940) === (3)
      r.sync.zremrangebyscore("hackers", 0, 3) === (0)
    }
  }

  "zunion" >> {
    "should do a union" ! client { r ⇒
      r.sync.zadd("hackers 1", "yukihiro matsumoto", 1965) === (true)
      r.sync.zadd("hackers 1", "richard stallman", 1953) === (true)
      r.sync.zadd("hackers 2", "claude shannon", 1916) === (true)
      r.sync.zadd("hackers 2", "linus torvalds", 1969) === (true)
      r.sync.zadd("hackers 3", "alan kay", 1940) === (true)
      r.sync.zadd("hackers 4", "alan turing", 1912) === (true)

      // union with weight = 1
      r.sync.zunionstore("hackers", Set("hackers 1", "hackers 2", "hackers 3", "hackers 4")) === (6)
      r.sync.zcard("hackers") === (6)

      r.sync.zrangeWithScores("hackers").parse[String] === (List(("alan turing", 1912), ("claude shannon", 1916), ("alan kay", 1940), ("richard stallman", 1953), ("yukihiro matsumoto", 1965), ("linus torvalds", 1969)))

      // union with modified weights
      r.sync.zunionstoreWeighted("hackers weighted", Map(("hackers 1", 1), ("hackers 2", 2), ("hackers 3", 3), ("hackers 4", 4))) === (6)
      r.sync.zrangeWithScores("hackers weighted").parse[String] map (_._2) must_== (List(1953, 1965, 3832, 3938, 5820, 7648))
    }
  }
}

