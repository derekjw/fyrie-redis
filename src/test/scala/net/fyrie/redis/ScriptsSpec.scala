package net.fyrie.redis

import org.specs2._

class ScriptsSpec extends mutable.Specification with UnstableClient {

  "evalNum" >> {
    "should eval simple script returning a number" ! client { r ⇒
      r.sync.evalNum("return ARGV[1] + ARGV[2]", args = List(4, 9)) === 13
    }
  }

}
