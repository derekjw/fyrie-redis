package net.fyrie.redis

import akka.actor.{ActorSystem,Timeout}
import akka.dispatch.Future
import akka.util.ByteString

private[redis] sealed abstract class ResultFunctor[R[_]] {
  def fmap[A, B](a: R[A])(f: A ⇒ B)(implicit timeout: Timeout): R[B]
}

private[redis] object ResultFunctor {
  implicit val async: ResultFunctor[Future] = new ResultFunctor[Future] {
    def fmap[A, B](a: Future[A])(f: A ⇒ B)(implicit timeout: Timeout): Future[B] = a map f
  }
  implicit val sync: ResultFunctor[({ type λ[α] = α })#λ] = new ResultFunctor[({ type λ[α] = α })#λ] {
    def fmap[A, B](a: A)(f: A ⇒ B)(implicit timeout: Timeout): B = f(a)
  }
  implicit val quiet: ResultFunctor[({ type λ[_] = Unit })#λ] = new ResultFunctor[({ type λ[_] = Unit })#λ] {
    def fmap[A, B](a: Unit)(f: A ⇒ B)(implicit timeout: Timeout): Unit = ()
  }
  implicit val multi: ResultFunctor[({ type λ[α] = Queued[Future[α]] })#λ] = new ResultFunctor[({ type λ[α] = Queued[Future[α]] })#λ] {
    def fmap[A, B](a: Queued[Future[A]])(f: A ⇒ B)(implicit timeout: Timeout): Queued[Future[B]] = a map (_ map f)
  }
  implicit val raw: ResultFunctor[({ type X[_] = ByteString })#X] = new ResultFunctor[({ type X[_] = ByteString })#X] {
    def fmap[A, B](a: ByteString)(f: A ⇒ B)(implicit timeout: Timeout): ByteString = a
  }
}
