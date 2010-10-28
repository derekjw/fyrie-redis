package net.fyrie.redis

object Response {
  implicit def response2Iterable[A](xr: Response[A]): Iterable[A] = xr.toList

  implicit def response2Option[A](xr: Response[A]): Option[A] = xr.toOption

  implicit def option2Response[A: Manifest](xo: Option[A]): Response[A] = apply(xo.get)

  def apply[A: Manifest](a: => A): Response[A] = try {Result(a)} catch {case e: Exception => Error(e)}
}

sealed abstract class Response[A] extends Product {
  self =>

  def isDefined: Boolean = !isError

  def isError: Boolean

  def get: A

  def manifest: Manifest[A]

  def asA[B](implicit m: Manifest[B]): Response[B] =
    if (manifest == m) this.asInstanceOf[Response[B]] else Error(new ClassCastException("Expected "+m.toString+" but got "+manifest.toString))

  def fold[X](ifResult: A => X, ifError: Throwable => X): X

  def getOrElse[B >: A](default: => B): B = if (isError) default else this.get

  def map[B: Manifest](f: A => B): Response[B]

  def foreach[U](f: A => U) {
    if (!isError) f(get)
  }

  def toList: List[A] = if (isError) Nil else List(get)

  def toOption: Option[A] = if (isError) None else Some(get)

  def toEither: Either[Throwable,A] = this match {
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

final case class Result[A](value: A)(implicit val manifest: Manifest[A]) extends Response[A] {
  def get = value
  def fold[X](ifResult: A => X, ifError: Throwable => X): X = ifResult(value)
  def map[B: Manifest](f: A => B): Response[B] = Result(f(value))
  def isError = false
  override def toString = "Result(" + value.toString + ")"
}

final case class Error[A](exception: Throwable)(implicit val manifest: Manifest[A]) extends Response[A] {
  def get = throw exception
  def fold[X](ifResult: A => X, ifError: Throwable => X): X = ifError(exception)
  def map[B: Manifest](f: A => B): Response[B] = Error(exception)
  def isError = true
  override def toString = "Error(" + exception.toString + ")"
}

class ResponseFuture[A](timeout: Long = 0)(implicit val manifest: Manifest[A]) extends se.scalablesolutions.akka.dispatch.DefaultCompletableFuture[A](timeout) {
  def toResponse: Response[A] =
    this.await.result.map(Result(_)(manifest)).orElse(this.exception.map(Error(_)(manifest))).getOrElse(Error(error("ERROR")))
}
