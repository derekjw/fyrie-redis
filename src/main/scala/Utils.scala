package net.fyrie.redis
package object utils {
  def checkType[T](in: AnyRef)(implicit m: Manifest[T]): Option[T] =
    if (m.erasure.isAssignableFrom(in.getClass)) Some(in.asInstanceOf[T]) else None

  def requireType[T](in: AnyRef)(implicit m: Manifest[T]): T =
    checkType(in)(m).getOrElse(throw new ClassCastException("Expected "+m.erasure))
}
