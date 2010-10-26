import sbt._
import xsbt.FileUtilities.write

// thank you scalaz
trait Boilerplate {
  self: DefaultProject =>

  def srcManagedScala = "src_managed" / "main" / "scala"

  lazy val generateSortTuple = {
    val cleanSrcManaged = cleanTask(srcManagedScala) named ("clean src_managed")
    task {
      val arities = 2 to 10

      def writeFile(fileName: String, source: String): Unit = {
        val file = (srcManagedScala / fileName).asFile
        write(file, source)
      }

      case class N(n: Int) {
        val alpha: String = "P" + n
        val element: String = "p" + n
        val seqElem: String = "s" + n
      }

      val tupleHandlers = for (arity: Int <- arities) yield {
          val ns = (1 to arity) map N.apply
          def mapMkString(f: N => String): String = ns.map(f).mkString(", ")

          """|final case class MultiBulkAsTuple%d[%s](implicit %s) extends MultiHandler[(%s)] {
             |
             |  def handlers = Stream.continually(Stream(%s)).flatten
             |
             |  def parse[T[_]](in: Option[Stream[T[Any]]]): Option[Stream[Any]] =
             |    in.map(_.map{
             |      case f: Future[_] =>
             |        f.await.result.getOrElse(throw f.exception.get)
             |      case r: Response[_] =>
             |        r.get
             |      case x => x
             |    }.grouped(%d).toStream.flatMap{
             |      case Stream(%s) => Some((%s))
             |      case _ => None
             |    })
             |}
             |""".stripMargin.format(arity,
                                     mapMkString { n => n.alpha},
                                     mapMkString { n => "%s: Parse[%s]".format(n.element, n.alpha) },
                                     mapMkString { n => "Option[%s]".format(n.alpha) },
                                     mapMkString { n => "Bulk[%s]()".format(n.alpha) },
                                     arity,
                                     mapMkString { n => n.seqElem },
                                     mapMkString { n => n.seqElem })
      }

      val tupleSortCommands = for (arity: Int <- arities) yield {
          val ns = (1 to arity) map N.apply
          def mapMkString(f: N => String): String = ns.map(f).mkString(", ")

          "case class sort%d[%s](key: Any, by: Option[Any] = None, limit: Option[(Int, Int)] = None, get: Product%d[%s], order: Option[SortOrder] = None, alpha: Boolean = false)(implicit format: Format, %s) extends Command(MultiBulkAsTuple%d[%s]) with SortTupled\n".format(
            arity,
            mapMkString {n => n.alpha},
            arity,
            mapMkString {n => "Any"},
            mapMkString { n => "%s: Parse[%s]".format(n.element, n.alpha) },
            arity,
            mapMkString {n => n.alpha})
      }

      val source = "package net.fyrie.redis\n" +
              "package handlers {\n\n" +
              "import serialization.Parse\n" +
              "import se.scalablesolutions.akka.dispatch.{Future, CompletableFuture, DefaultCompletableFuture}\n" +
              tupleHandlers.mkString("\n") +
              "}\n\n" +
              "package commands {\n\n" +
              "import Command._\n" +
              "import handlers._\n" +
              "import serialization.{Parse, Format}\n\n" +
              "trait SortTupleCommands {\n" +
              tupleSortCommands.mkString("\n") +
              "}\n" +
              "}"

      writeFile("SortTuple.scala", source)
      None
    } dependsOn (cleanSrcManaged)
  }
}
