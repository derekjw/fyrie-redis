package net.fyrie.redis

object Response {
  implicit def response2Iterable[A](xr: Response[A]): Iterable[A] = xr.toList

  implicit def response2Option[A](xr: Response[A]): Option[A] = xr.toOption

  implicit def option2Response[A](xo: Option[A]): Response[A] = apply(xo.get)

  def apply[A](a: => A): Response[A] = try {Result(a)} catch {case e: Exception => Error(e)}
}

sealed abstract class Response[+A] extends Product {
  self =>

  def isDefined: Boolean = !isError

  def isError: Boolean

  def get: A

  def getOrElse[B >: A](default: => B): B = if (isError) default else this.get

  def map[B](f: A => B): Response[B]

  def foreach[U](f: A => U) {
    if (!isError) f(get)
  }

  def toList: List[A] = if (isError) Nil else List(get)

  def toOption: Option[A] = if (isError) None else Some(get)

  def toEither: Either[Exception,A] = this match {
    case Error(e) => Left(e)
    case Result(r) => Right(r)
  }

  def productArity = if (isError) 0 else 1

  def productElement(n: Int) = toOption.productElement(n)

  def canEqual(that: Any) = that.isInstanceOf[Result[_]]

  override def equals(that: Any) = that match {
    case r: Result[_] => r.canEqual(this) && (r.productArity == productArity) &&
      (if (productArity == 1) (r.productElement(0) == productElement(0)) else true)
    case _ => false
  }

  override def hashCode = toOption.hashCode
}

final case class Result[A](value: A) extends Response[A] {
  def get = value
  def map[B](f: A => B): Response[B] = Result(f(value))
  def isError = false
  override def toString = "Result(" + value.toString + ")"
}

final case class Error(exception: Exception) extends Response[Nothing] {
  def get = throw exception
  def map[B](f: Nothing => B): Response[B] = this
  def isError = true
  override def toString = "Error(" + exception.toString + ")"
}

