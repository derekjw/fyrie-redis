package net.fyrie.redis
package serialization

import akka.util.ByteString

trait Store[A] {
  def apply(value: A): ByteString
}

object Store extends StoreDefaults {

  def apply[A](value: A)(implicit store: Store[A]): ByteString = store(value)

  def asString[A]: Store[A] = new Store[A] { def apply(value: A) = ByteString(value.toString) }

  def asDouble(d: Double, inclusive: Boolean = true): Array[Byte] = {
    (if (inclusive) ("") else ("(")) + {
      if (d.isInfinity) {
        if (d > 0.0) "+inf" else "-inf"
      } else {
        d.toString
      }
    }
  }.getBytes

}

trait StoreDefaults {
  implicit val storeByteString = new Store[ByteString] { def apply(value: ByteString) = value }
  implicit val storeByteArray = new Store[Array[Byte]] { def apply(value: Array[Byte]) = ByteString(value) }
  implicit val storeString = new Store[String] { def apply(value: String) = ByteString(value) }
  implicit val storeInt = Store.asString[Int]
  implicit val storeLong = Store.asString[Long]
  implicit val storeFloat = Store.asString[Float]
  implicit val storeDouble = Store.asString[Double]
  implicit val storeScore = new Store[RedisScore] {
    def apply(score: RedisScore) = score match {
      case _ if score.value == Double.PositiveInfinity => Protocol.INFPOS
      case _ if score.value == Double.NegativeInfinity => Protocol.INFNEG
      case InclusiveScore(d) => ByteString(d.toString)
      case ExclusiveScore(d) => ByteString("(" + d.toString)
    }
  }
  implicit val storeAggregate = new Store[Aggregate] {
    def apply(value: Aggregate) = value match {
      case Aggregate.Sum => Protocol.SUM
      case Aggregate.Min => Protocol.MIN
      case Aggregate.Max => Protocol.MAX
    }
  }
}

trait Parse[A] {
  def apply(bytes: ByteString): A
}

object Parse {
  def apply[A](bytes: ByteString)(implicit parse: Parse[A]): A = parse(bytes)

  implicit val parseByteString = new Parse[ByteString] { def apply(bytes: ByteString) = bytes.compact }
  implicit val parseByteArray = new Parse[Array[Byte]] { def apply(bytes: ByteString) = bytes.toArray }
  implicit val parseString = new Parse[String] { def apply(bytes: ByteString) = bytes.utf8String }
  implicit val parseInt = new Parse[Int] { def apply(bytes: ByteString) = bytes.utf8String.toInt }
  implicit val parseLong = new Parse[Long] { def apply(bytes: ByteString) = bytes.utf8String.toLong }
  implicit val parseFloat = new Parse[Float] { def apply(bytes: ByteString) = bytes.utf8String.toFloat }
  implicit val parseDouble = new Parse[Double] { def apply(bytes: ByteString) = bytes.utf8String.toDouble }
}
