package net.fyrie.redis

import org.specs2._

import net.fyrie.redis.types._
import net.fyrie.redis.serialization._

class ScriptsSpec extends mutable.Specification with UnstableClient {

  "eval" >> {
    "should eval simple script returning a number" ! client { r ⇒
      r.sync.eval("return ARGV[1] + ARGV[2]", args = List(4, 9)) === RedisInteger(13)
    }
  }

  "lua dsl" >> {
    "should produce valid lua" ! client { r =>
      import lua._

      val v1 = Var("v1")
      val v2 = Var("v2")
      val v3 = Var("v3")

      val script = {
        (v1 := 34) ::
        Do {
          (v2 := true) ::
          End
        } ::
        (v3 := "hello") ::
        Return(Table(v1, v2, v3))
      }.toLua

      script === "v1 = 34.0; do v2 = true; end; v3 = \"hello\"; return {v1, v2, v3}"
      r.sync.eval(script) === RedisMulti(Some(List(RedisInteger(34), RedisInteger(1), RedisBulk(Some(Store("hello"))))))
    }
  }

}
