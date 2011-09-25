package net.fyrie.redis

import org.specs2._

import akka.util.ByteString
import akka.actor.IO

class IterSpec extends Specification {

  def is = "iteratee" ! {
    val iter = for {
      a ← IO takeUntil ByteString(" ")
      b ← IO take 5
      c ← IO takeAll
    } yield (a.utf8String, b.utf8String, c.utf8String)
    iter(ByteString("Hel"))(ByteString("lo W"))(ByteString("orld!")) === IO.Done(("Hello", "World", "!"))
  }

}

