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
    iter(IO Chunk ByteString("Hel"))._1(IO Chunk ByteString("lo W"))._1(IO Chunk ByteString("orld!"))._1.get === ("Hello", "World", "!")
  }

}

