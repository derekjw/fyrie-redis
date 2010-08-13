package net.fyrie.redis

import commands._

import net.fyrie.redis.akka._

import org.scalatest.Spec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll

trait RedisByteHelpers {
  val basePort = 16379

  implicit def str2Bytes(in: String): Array[Byte] = in.getBytes
  implicit def int2Bytes(in: Int): Array[Byte] = in.toString.getBytes
  implicit def double2Bytes(in: Double): Array[Byte] = in.toString.getBytes
  implicit def strPair2BytesPair(in: (String, String)): (Array[Byte], Array[Byte]) = (in._1.getBytes, in._2.getBytes)
  def mkString(in: Seq[Array[Byte]]): Seq[String] = in.map(new String(_))
  def mkString(in: Map[Array[Byte], Array[Byte]]): Map[String, String] = in.map{case (k,v) => (new String(k), new String(v))}
  implicit def mkString(in: Array[Byte]): String = new String(in)
  def mkString(in: Option[Array[Byte]]): Option[String] = in.map(new String(_))
  def mkString(in: Set[Array[Byte]]): Set[String] = in.map(mkString)

}

trait RedisTestSingleServer extends RedisByteHelpers{
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
