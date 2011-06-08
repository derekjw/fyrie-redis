package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll

trait RedisTestServer extends BeforeAndAfterEach  with BeforeAndAfterAll {
  self: Spec =>

  val r = new RedisClient

  override def beforeAll = {
    r.flushall
  }

  override def afterEach = {
    r.flushall
  }
/*
  override def afterAll = {
    r.disconnect
  }*/
}
