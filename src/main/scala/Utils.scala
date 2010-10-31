package net.fyrie.redis
package object utils {
  def ifType[A: Manifest, B, C](a: A, b: Manifest[B])(t: (B) => C)(f: (A) => C) =
    if (manifest[A] == b) t(a.asInstanceOf[B]) else f(a)

  def checkType[A: Manifest, B](a: A, b: Manifest[B]): Option[B] =
    if (manifest[A] == b) Some(a.asInstanceOf[B]) else None

  def requireType[A: Manifest, B](a: A, b: Manifest[B]): B =
    if (manifest[A] == b) a.asInstanceOf[B] else throw new ClassCastException("Expected "+b.toString+" but got "+manifest[A].toString)
}
