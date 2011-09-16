package net.fyrie
package redis

package object lua {
  sealed trait Chunk
  sealed trait Block extends Chunk {
    def ::(stat: Stat) = StatList(stat, this)
    def toLua: String
  }

  case class StatList(stat: Stat, next: Block) extends Block {
    def toLua = stat.toLua + "; " + next.toLua
  }

  sealed trait Stat {
    def toLua: String
  }
  //case class Call(fun: String) extends Stat
  case class Assign(name: Var, exp: Exp) extends Stat {
    def toLua = name.toLua + " = " + exp.toLua
  }
  //case class Local(name: Var, exp: Exp) extends Stat
  case class Do(block: Block) extends Stat {
    def toLua = "do " + block.toLua + "end"
  }
  //case class While(test: Exp, block: Block) extends Stat
  //case class Repeat(block: Block, test: Exp) extends Stat
  //sealed trait If extends Stat
  //case class IfThenElse(test: Exp, then: Block, elseIf: Option[IfThen], orElse: Block) extends If
  //case class IfThen(test: Exp, then: Block, elseIf: Option[IfThen]) extends If

  sealed trait LastStat extends Block
  case class Return(exps: Exp*) extends LastStat {
    def toLua = "return" + (if (exps.isEmpty) "" else (" " + exps.map(_.toLua).mkString(", ")))
  }
  case object Break extends LastStat {
    def toLua = "break"
  }
  case object End extends LastStat {
    def toLua = ""
  }

  object Exp {
    implicit def boolToExp(b: Boolean): Exp = if (b) True else False
    implicit def intToExp(n: Int): Exp = Num(n)
    implicit def strToExp(s: String): Exp = Str(s)
  }
  sealed trait Exp {
    def toLua: String
  }
  //case class Func
  case class Var(name: String) extends Exp {
    def toLua = name
    def :=(exp: Exp) = Assign(this, exp)
  }
  case object True extends Exp {
    def toLua = "true"
  }
  case object False extends Exp {
    def toLua = "false"
  }
  case object nil extends Exp {
    def toLua = "nil"
  }
  case class Num(value: Double) extends Exp {
    def toLua = value.toString
  }
  case class Str(value: String) extends Exp {
    def toLua = "\"" + value + "\""
  }
  case class Table(fields: Field*) extends Exp {
    def toLua = "{" + fields.map(_.toLua).mkString(", ") + "}"
  }
  object Field {
    implicit def expToField(exp: Exp): Field = FieldV(exp)
  }
  sealed trait Field {
    def toLua: String
  }
  case class FieldV(value: Exp) extends Field {
    def toLua = value.toLua
  }
/*  case class Fun extends Exp {
    def toLua = this.toString
  }

  sealed trait BinOp extends Exp
  case class Add(a: Exp,b: Exp) extends BinOp

  sealed trait UnOp extends Exp
  case class Minus(exp: Exp) extends UnOp
  case class Not(exp: Exp) extends UnOp*/

}

