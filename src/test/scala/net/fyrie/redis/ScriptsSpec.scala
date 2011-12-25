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
    "should produce valid lua" ! client { r ⇒
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
      }

      script.bytes.utf8String === "v1 = 34.0; do v2 = true; end; v3 = \"hello\"; return {v1, v2, v3}"
      r.sync.eval(script) === RedisMulti(Some(List(RedisInteger(34), RedisInteger(1), RedisBulk(Some(Store("hello"))))))
    }
    "should provide If/Then/Else statement" ! client { r ⇒
      import lua._

      def script(x: Exp) = {
        val v = Var("result")

        {
          If(x :== "a") Then {
            (v := 1) :: End
          } ElseIf (x :== "b") Then {
            (v := 6) :: End
          } ElseIf (x :== "c") Then {
            (v := 3) :: End
          } ElseIf (x :== "d") Then {
            (v := 8) :: End
          } Else {
            (v := "I don't know!") :: End
          }
        } :: Return(v)
      }

      r.sync.eval(script("a")) === RedisInteger(1)
      r.sync.eval(script("b")) === RedisInteger(6)
      r.sync.eval(script("c")) === RedisInteger(3)
      r.sync.eval(script("d")) === RedisInteger(8)
      r.sync.eval(script("e")) === RedisBulk(Some(Store("I don't know!")))
    }
    "should produce valid arithmetic expressions" ! client { r ⇒
      import lua._

      val n = Var("n")

      (n :+ 3 :* n :- 5).bytes.utf8String === "n + 3.0 * n - 5.0"
      (Exp(n) :+ 3 :* n :- 5).bytes.utf8String === "n + 3.0 * n - 5.0"
      (n :+ 3 :* (n :- 5)).bytes.utf8String === "n + 3.0 * (n - 5.0)"
      (Exp(n :+ 3) :* (n :- 5)).bytes.utf8String === "(n + 3.0) * (n - 5.0)"
    }
  }

}

