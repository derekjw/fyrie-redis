package net.fyrie.redis

import Commands._

import org.scalatest.Spec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll

trait RedisTestServer extends BeforeAndAfterEach  with BeforeAndAfterAll {
  self: Spec =>

  val r = new RedisClient

  override def beforeAll = {
    r send flushdb
  }

  override def afterEach = {
    r send flushdb
  }

  override def afterAll = {
    r.disconnect
  }
}
