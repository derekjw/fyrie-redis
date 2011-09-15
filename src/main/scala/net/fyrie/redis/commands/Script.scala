package net.fyrie.redis
package commands

import serialization._
import akka.util.ByteString

private[redis] trait Scripts[Result[_]] {
  this: Commands[Result] ⇒
  import Protocol._

  def eval[S: Store, K: Store, A: Store](script: S, keys: Seq[K] = Seq.empty[Store.Dummy], args: Seq[A] = Seq.empty[Store.Dummy]): Result[Option[ByteString]] =
    send(EVAL :: Store(script) :: Store(keys.size) :: (keys.map(Store(_))(collection.breakOut): List[ByteString]) ::: (args.map(Store(_))(collection.breakOut): List[ByteString]))

  def evalNum[S: Store, K: Store, A: Store](script: S, keys: Seq[K] = Seq.empty[Store.Dummy], args: Seq[A] = Seq.empty[Store.Dummy]): Result[Long] =
    send(EVAL :: Store(script) :: Store(keys.size) :: (keys.map(Store(_))(collection.breakOut): List[ByteString]) ::: (args.map(Store(_))(collection.breakOut): List[ByteString]))

  def evalMulti[S: Store, K: Store, A: Store](script: S, keys: Seq[K] = Seq.empty[Store.Dummy], args: Seq[A] = Seq.empty[Store.Dummy]): Result[Option[List[Option[ByteString]]]] =
    send(EVAL :: Store(script) :: Store(keys.size) :: (keys.map(Store(_))(collection.breakOut): List[ByteString]) ::: (args.map(Store(_))(collection.breakOut): List[ByteString]))

  def evalStatus[S: Store, K: Store, A: Store](script: S, keys: Seq[K] = Seq.empty[Store.Dummy], args: Seq[A] = Seq.empty[Store.Dummy]): Result[String] =
    send(EVAL :: Store(script) :: Store(keys.size) :: (keys.map(Store(_))(collection.breakOut): List[ByteString]) ::: (args.map(Store(_))(collection.breakOut): List[ByteString]))

}
