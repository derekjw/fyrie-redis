package net.fyrie.redis

import Commands._
import serialization._

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith


@RunWith(classOf[JUnitRunner])
class SerializationSpec extends Spec with ShouldMatchers with RedisTestServer {

  it("should not conflict when using all built in parsers") {
    import Parse.Implicits._
    r send hmset("hash", Map("field1" -> "1", "field2" -> 2))
    r send hmget[String]("hash", Seq("field1", "field2")) should be(Some(Seq(Some("1"), Some("2"))))
    r send hmget[Int]("hash", Seq("field1", "field2")) should be(Some(Seq(Some(1), Some(2))))
    r send hmget[Int]("hash", Seq("field1", "field2", "field3")) should be(Some(Seq(Some(1), Some(2), None)))
  }

  it("should use a provided implicit parser") {
    r send hmset("hash", Map("field1" -> "1", "field2" -> 2))

    r send hmget("hash", Seq("field1", "field2")) should be(Some(Seq(Some("1"), Some("2"))))

    implicit val parseInt = Parse[Int](new String(_).toInt)

    r send hmget[Int]("hash", Seq("field1", "field2")) should be(Some(Seq(Some(1), Some(2))))
    r send hmget[String]("hash", Seq("field1", "field2")) should be(Some(Seq(Some("1"), Some("2"))))
    r send hmget[Int]("hash", Seq("field1", "field2", "field3")) should be(Some(Seq(Some(1), Some(2), None)))
  }

  it("should use a provided implicit string parser") {
    import Parse.Implicits.parseInt
    implicit val parseString = Parse[String](new String(_).toInt.toBinaryString)
    r send hmset("hash", Map("field1" -> "1", "field2" -> 2))
    r send hmget[Int]("hash", Seq("field1", "field2")) should be(Some(Seq(Some(1), Some(2))))
    r send hmget[String]("hash", Seq("field1", "field2")) should be(Some(Seq(Some("1"), Some("10"))))
  }

  it("should use a seperate parser for key/values with Map") {
    r send hmset("hash7", Map("field1" -> 1, "field2" -> 2))
    r send hgetall("hash7") map (_.toMap) should be(Some(Map("field1" -> "1", "field2" -> "2")))    
    import Parse.Implicits._
    r send hgetall[String,Int]("hash7") map (_.toMap) should be(Some(Map("field1" -> 1, "field2" -> 2)))
  }


  it("should use a provided implicit formatter") {
    case class Upper(s: String)
    r send hmset("hash1", Map("field1" -> Upper("val1"), "field2" -> Upper("val2")))
    implicit val format = Format{case Upper(s) => s.toUpperCase}
    r send hmset("hash2", Map("field1" -> Upper("val1"), "field2" -> Upper("val2")))
    r send hmget("hash1", Seq("field1", "field2")) should be(Some(Seq(Some("Upper(val1)"), Some("Upper(val2)"))))
    r send hmget("hash2", Seq("field1", "field2")) should be(Some(Seq(Some("VAL1"), Some("VAL2"))))
  }

}
