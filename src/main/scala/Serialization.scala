package net.fyrie.redis
package serialization

import akka.util.ByteString

trait Store[A] {
  def apply(value: A): ByteString
}

object Store extends StoreDefaults {

  def apply[A](value: A)(implicit store: Store[A]): ByteString = store(value)

  def asString[A]: Store[A] = new Store[A] { def apply(value: A) = ByteString(value.toString) }

}

trait StoreDefaults {
  implicit val storeByteString = new Store[ByteString] { def apply(value: ByteString) = value }
  implicit val storeByteArray = new Store[Array[Byte]] { def apply(value: Array[Byte]) = ByteString(value) }
  implicit val storeString = new Store[String] { def apply(value: String) = ByteString(value) }
  implicit val storeInt = Store.asString[Int]
  implicit val storeLong = Store.asString[Long]

  // TODO: add special formatting
  implicit val storeFloat = Store.asString[Float]
  implicit val storeDouble = Store.asString[Double]
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
