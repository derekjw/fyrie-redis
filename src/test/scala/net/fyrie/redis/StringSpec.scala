package net.fyrie.redis

import org.specs2._

class StringSpec extends mutable.Specification with TestClient {

  "set" >> {
    "should set key/value pairs" ! client { r ⇒
      for {
        a ← r.set("anshin-1", "debasish")
        b ← r.set("anshin-2", "maulindu")
      } yield {
        a === ()
        b === ()
      }
    }
  }

  "get" >> {
    "should retrieve key/value pairs for existing keys" ! client { r ⇒
      r.quiet.set("anshin-1", "debasish")
      r.get("anshin-1").parse[String] map (_ === Some("debasish"))
    }
    "should fail for non-existent keys" ! client { r ⇒
      r.get("anshin-2").parse[String] map (_ === None)
    }
  }

  "getset" >> {
    "should set new values and return old values" ! client { r ⇒
      for {
        _ ← r.set("anshin-1", "debasish")
        a ← r.get("anshin-1").parse[String]
        b ← r.getset("anshin-1", "maulindu").parse[String]
        c ← r.get("anshin-1").parse[String]
      } yield {
        a === Some("debasish")
        b === Some("debasish")
        c === Some("maulindu")
      }
    }
  }

  "setnx" >> {
    "should set only if the key does not exist" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.sync.setnx("anshin-1", "maulindu") === (false)
      r.sync.setnx("anshin-2", "maulindu") === (true)
    }
  }

  "incr" >> {
    "should increment by 1 for a key that contains a number" ! client { r ⇒
      r.set("anshin-1", "10")
      r.sync.incr("anshin-1") === (11)
    }
    "should reset to 0 and then increment by 1 for a key that contains a diff type" ! client { r ⇒
      r.set("anshin-2", "debasish")
      r.sync.incr("anshin-2") must throwA[RedisErrorException].like {
        case e ⇒ e.getMessage must startWith("ERR value is not an integer")
      }
    }
    "should increment by 5 for a key that contains a number" ! client { r ⇒
      r.set("anshin-3", "10")
      r.sync.incrby("anshin-3", 5) === (15)
    }
    "should reset to 0 and then increment by 5 for a key that contains a diff type" ! client { r ⇒
      r.set("anshin-4", "debasish")
      r.sync.incrby("anshin-4", 5) must throwA[RedisErrorException].like {
        case e ⇒ e.getMessage must startWith("ERR value is not an integer")
      }
    }
  }

  "decr" >> {
    "should decrement by 1 for a key that contains a number" ! client { r ⇒
      r.set("anshin-1", "10")
      r.sync.decr("anshin-1") === (9)
    }
    "should reset to 0 and then decrement by 1 for a key that contains a diff type" ! client { r ⇒
      r.set("anshin-2", "debasish")
      r.sync.decr("anshin-2") must throwA[RedisErrorException].like {
        case e ⇒ e.getMessage must startWith("ERR value is not an integer")
      }
    }
    "should decrement by 5 for a key that contains a number" ! client { r ⇒
      r.set("anshin-3", "10")
      r.sync.decrby("anshin-3", 5) === (5)
    }
    "should reset to 0 and then decrement by 5 for a key that contains a diff type" ! client { r ⇒
      r.set("anshin-4", "debasish")
      r.sync.decrby("anshin-4", 5) must throwA[RedisErrorException].like {
        case e ⇒ e.getMessage must startWith("ERR value is not an integer")
      }
    }
  }

  "mget" >> {
    "should get values for existing keys" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.set("anshin-3", "nilanjan")
      r.sync.mget(Seq("anshin-1", "anshin-2", "anshin-3")).parse[String] === (List(Some("debasish"), Some("maulindu"), Some("nilanjan")))
    }
    "should give None for non-existing keys" ! client { r ⇒
      r.set("anshin-1", "debasish")
      r.set("anshin-2", "maulindu")
      r.sync.mget(Seq("anshin-1", "anshin-2", "anshin-4")).parse[String] === (List(Some("debasish"), Some("maulindu"), None))
    }
  }

  "mset" >> {
    "should set all keys irrespective of whether they exist" ! client { r ⇒
      r.sync.mset(Map(
        ("anshin-1", "debasish"),
        ("anshin-2", "maulindu"),
        ("anshin-3", "nilanjan"))) === ()
    }

    "should set all keys only if none of them exist" ! client { r ⇒
      r.sync.msetnx(Map(
        ("anshin-4", "debasish"),
        ("anshin-5", "maulindu"),
        ("anshin-6", "nilanjan"))) === (true)
      r.sync.msetnx(Map(
        ("anshin-7", "debasish"),
        ("anshin-8", "maulindu"),
        ("anshin-6", "nilanjan"))) === (false)
      r.sync.msetnx(Map(
        ("anshin-4", "debasish"),
        ("anshin-5", "maulindu"),
        ("anshin-6", "nilanjan"))) === (false)
    }
  }

  "get with spaces in keys" >> {
    "should retrieve key/value pairs for existing keys" ! client { r ⇒
      r.set("anshin software", "debasish ghosh")
      r.sync.get("anshin software").parse[String] === (Some("debasish ghosh"))

      r.set("test key with spaces", "I am a value with spaces")
      r.sync.get("test key with spaces").parse[String] === (Some("I am a value with spaces"))
    }
  }

  "set with newline values" >> {
    "should set key/value pairs" ! client { r ⇒
      r.sync.set("anshin-x", "debasish\nghosh\nfather") === ()
      r.sync.set("anshin-y", "maulindu\nchatterjee") === ()
    }
  }

  "get with newline values" >> {
    "should retrieve key/value pairs for existing keys" ! client { r ⇒
      r.set("anshin-x", "debasish\nghosh\nfather")
      r.sync.get("anshin-x").parse[String] === (Some("debasish\nghosh\nfather"))
    }
  }
}

