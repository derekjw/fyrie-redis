import sbt._
import xsbt.FileUtilities.write

// thank you scalaz
trait Boilerplate {
  self: DefaultProject =>

  def srcManagedScala = "src_managed" / "main" / "scala"

  lazy val generateSortTuple = {
    val cleanSrcManaged = cleanTask(srcManagedScala) named ("clean src_managed")
    task {
      val arities = 2 to 12

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

          """|case class MultiBulkAsTuple%d[%s](implicit %s) extends Handler[Option[Stream[(%s)]]] {
             |  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
             |    val futures = Stream.fill[Option[CompletableFuture[Any]]](string(data).toInt)(if (future.isDefined) Some(new DefaultCompletableFuture[Any](5000)) else None)
             |    complete(future, Some(futures.collect{case Some(f) => f.await.result}.grouped(%d).collect{case Seq(%s) => (%s)}.toStream))
             |    Stream.continually(Stream(%s)).flatten.zip(futures).map{ case (h,f) => (h, f)}
             |  }
             |}
             |""".stripMargin.format(arity,
                                     mapMkString { n => n.alpha},
                                     mapMkString { n => "%s: Parse[%s]".format(n.element, n.alpha) },
                                     mapMkString { n => "Option[%s]".format(n.alpha) },
                                     arity,
                                     mapMkString { n => "Some(Result(%s))".format(n.seqElem)},
                                     mapMkString { n => n.seqElem},
                                     mapMkString { n => "Bulk[%s]()".format(n.alpha)})
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
              "import se.scalablesolutions.akka.dispatch.{CompletableFuture, DefaultCompletableFuture}\n" +
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
