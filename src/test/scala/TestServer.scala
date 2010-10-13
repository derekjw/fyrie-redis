package net.fyrie.redis

import Commands._

import org.scalatest.Spec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll

trait RedisHelpers {
  val basePort = 16379
}

trait RedisTestSingleServer extends RedisHelpers{
  implicit val r = new RedisClient("localhost", basePort)
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
/*
package cluster {
  trait RedisTestCluster extends BeforeAndAfterEach with BeforeAndAfterAll with RedisByteHelpers {
    self: Spec =>

    val addresses = (0 to 2).map(n => "localhost:"+(basePort + n))

    val r = new RedisCluster(addresses: _*) {
      val keyTag = Some(RegexKeyTag)
    }

    override def afterEach = {
      r.flushdb
    }

    override def afterAll = {
      r.flushdb
    }
  }
}

package ds {
  trait RedisTestDeque extends BeforeAndAfterEach with BeforeAndAfterAll with RedisByteHelpers {
    self: Spec =>

    val r = new RedisDequeClient("localhost", basePort).mkDeque("td")

    override def beforeEach = {
      r.clear
    }

    override def afterEach = {
      r.clear
    }

    override def afterAll = {
      r.clear
    }
  }
}
*/
