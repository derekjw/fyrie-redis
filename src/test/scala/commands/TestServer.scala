package net.fyrie.redis

import Commands._

import net.fyrie.redis.akka._

import org.scalatest.Spec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll

trait RedisHelpers {
  val basePort = 16379
}

trait RedisTestSingleServer extends RedisHelpers{
  implicit val r = new AkkaRedisClient("localhost", basePort)
}

trait RedisTestServer extends RedisTestSingleServer with BeforeAndAfterEach  with BeforeAndAfterAll {
  self: Spec =>

  override def beforeAll = {
    if ((r send dbsize) > 0) error("Redis Database is not empty") // Try not to blow away the wrong database
  }

  override def afterEach = {
    r send flushdb
  }

  override def afterAll = {
    r.disconnect
  }
}
