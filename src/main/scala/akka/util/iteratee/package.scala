package akka.util

package object iteratee {
  def takeUntil(delimiter: ByteString, inclusive: Boolean = false): Iteratee[ByteString] = {
    def step(bytes: ByteString, start: Int): Iteratee[ByteString] = {
      val idx = bytes.indexOfSlice(delimiter, start)
      if (idx >= 0) {
        val index = if (inclusive) idx + delimiter.length else idx
        Done(bytes take index, bytes drop (index + delimiter.length))
      } else {
        Cont(more ⇒ step(bytes ++ more, math.max(bytes.length - delimiter.length, 0)))
      }
    }

    Cont(step(_, 0))
  }

  def take(length: Int): Iteratee[ByteString] = {
    def step(bytes: ByteString): Iteratee[ByteString] =
      if (bytes.length >= length)
        Done(bytes.take(length), bytes.drop(length))
      else
        Cont(more ⇒ step(bytes ++ more))

    Cont(step)
  }

  val takeAll: Iteratee[ByteString] = Cont { bytes ⇒
    if (bytes.nonEmpty) Done(bytes) else takeAll
  }

}
