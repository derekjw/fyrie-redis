package net.fyrie.redis

object ByteStringHelpers {
  implicit def toBytes(in: Any): Array[Byte] = in.toString.getBytes
  implicit def fromBytes(in: Array[Byte]): String = new String(in)
}
