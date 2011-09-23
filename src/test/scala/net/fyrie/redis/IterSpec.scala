package net.fyrie.redis

import org.specs2._

import akka.util.ByteString

class IterSpec extends Specification {

  def is = "iteratee" ! {
    import akka.util.iteratee._
    val iter = for {
      a ← takeUntil(ByteString(" "))
      b ← take(5)
      c ← takeAll
    } yield (a.utf8String, b.utf8String, c.utf8String)
    iter(ByteString("Hel"))(ByteString("lo W"))(ByteString("orld!")) === Done(("Hello", "World", "!"))
  }

}

