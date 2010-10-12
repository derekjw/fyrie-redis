package net.fyrie.redis

object Result {
  implicit def result2Iterable[A](xr: Result[A]): Iterable[A] = xr.toList

  implicit def result2Option[A](xr: Result[A]): Option[A] = xr.toOption

  implicit def option2Result[A](xo: Option[A]): Result[A] = xo match {
    case Some(s) => new ValidResult(s)
    case None => NotFound
  }

  def apply[A](a: => A): Result[A] = new LazyResult(try {new ValidResult(a)} catch {case e: Exception => Error(e)})

  def unapply[A](r: Result[A]): Option[A] = r.toOption

  final class LazyResult[A](a: => Result[A]) extends Result[A] {
    protected var resultDefined = false
    lazy val force: Result[A] = {
      resultDefined = true
      a
    }
    def get = force.get
    def isError = force.isError
    def map[B](f: A => B) = new LazyResult(force.map(f))
    override def toString = if (resultDefined) force.toString else "Result(?)"
  }

  final class ValidResult[A](value: A) extends Result[A] {
    def get = value
    def force = this
    def isError = false
    def map[B](f: A => B) = Result(f(value))
    override def toString = "Result(" + value.toString + ")"
  }

  val NotFound = Error(new java.util.NoSuchElementException)

}

sealed abstract class Result[+A] extends Product {
  self =>

  def isValid: Boolean = !isError

  def isError: Boolean

  def get: A

  def getOrElse[B >: A](default: => B): B = if (isError) default else this.get

  def map[B](f: A => B): Result[B]

  def foreach[U](f: A => U) {
    if (!isError) f(get)
  }

  def force: Result[A]

  def toList: List[A] = if (isError) Nil else List(get)

  def toOption: Option[A] = if (isError) None else Some(get)

  def toEither: Either[Exception,A] = force match {
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

final case class Error(exception: Exception) extends Result[Nothing] {
  def get = throw exception
  def force = this
  def isError = true
  def map[B](f: Nothing => B) = this
  override def toString = "Error(" + exception.toString + ")"
}

