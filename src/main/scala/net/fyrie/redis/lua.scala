package net.fyrie
package redis
package lua

import akka.util.ByteString

private[lua] object Literals {
  val ADD = ByteString(" + ")
  val ASSIGN = ByteString(" = ")
  val COMMA = ByteString(", ")
  val DIV = ByteString(" / ")
  val DO = ByteString("do ")
  val ELSE = ByteString("else ")
  val ELSEIF = ByteString("elseif ")
  val END = ByteString("end")
  val EQ = ByteString(" == ")
  val GT = ByteString(" > ")
  val GTEQ = ByteString(" >= ")
  val IF = ByteString("if ")
  val LT = ByteString(" < ")
  val LTEQ = ByteString(" <= ")
  val MULT = ByteString(" * ")
  val PARENL = ByteString("(")
  val PARENR = ByteString(")")
  val REPEAT = ByteString("repeat ")
  val RETURN = ByteString("return ")
  val SEMICOLON = ByteString("; ")
  val SUB = ByteString(" - ")
  val THEN = ByteString(" then ")
  val UNTIL = ByteString("until ")
  val WHILE = ByteString("while ")
  val CURLYL = ByteString("{")
  val CURLYR = ByteString("}")
}

import Literals._

sealed trait Chunk {
  def bytes: ByteString
}
sealed trait Block extends Chunk {
  def ::(stat: Stat) = StatList(stat, this)
}

case class StatList(stat: Stat, next: Block) extends Block {
  val bytes = stat.bytes ++ SEMICOLON ++ next.bytes
}

sealed trait Stat {
  def bytes: ByteString
}
//case class Call(fun: String) extends Stat
case class Assign(name: Var, exp: Exp) extends Stat {
  val bytes = name.bytes ++ ASSIGN ++ exp.bytes
}
//case class Local(name: Var, exp: Exp) extends Stat
case class Do(block: Block) extends Stat {
  val bytes = DO ++ block.bytes ++ END
}
case class While(test: Exp, block: Block) extends Stat {
  val bytes = WHILE ++ test.bytes ++ DO ++ block.bytes ++ END
}
case class Repeat(block: Block, test: Exp) extends Stat {
  val bytes = REPEAT ++ block.bytes ++ UNTIL ++ test.bytes
}
case class If(test: Exp) {
  def Then(block: Block) = IfThen(test, block, Vector.empty)
}
case class IfThen(test: Exp, then: Block, elseIf: Vector[ElseIfThen]) extends Stat {
  def ElseIf(test: Exp) = lua.ElseIf(test, this)
  def Else(block: Block) = IfThenElse(test, then, elseIf, block)
  val bytes = ((IF ++ test.bytes ++ THEN ++ then.bytes) /: elseIf)(_ ++ _.bytes) ++ END
}
case class IfThenElse(test: Exp, then: Block, elseIf: Vector[ElseIfThen], `else`: Block) extends Stat {
  val bytes = ((IF ++ test.bytes ++ THEN ++ then.bytes) /: elseIf)(_ ++ _.bytes) ++ ELSE ++ `else`.bytes ++ END
}
case class ElseIf(test: Exp, parent: IfThen) {
  def Then(block: Block) = parent.copy(elseIf = parent.elseIf :+ ElseIfThen(test, block))
}
case class ElseIfThen(test: Exp, block: Block) {
  val bytes = ELSEIF ++ test.bytes ++ THEN ++ block.bytes
}

sealed trait LastStat extends Block
case class Return(exps: Exp*) extends LastStat {
  val bytes = RETURN ++ (if (exps.nonEmpty) (exps.head.bytes /: exps.tail)(_ ++ COMMA ++ _.bytes) else ByteString.empty)
}
case object Break extends LastStat {
  def bytes = ByteString("break")
}
case object End extends LastStat {
  def bytes = ByteString.empty
}

object Exp {
  def apply(exp: Exp) = exp match {
    case _: BinOp ⇒ Par(exp)
    case _        ⇒ exp
  }
  implicit def boolToExp(b: Boolean): Exp = if (b) True else False
  implicit def intToExp(n: Int): Exp = Num(n)
  implicit def strToExp(s: String): Exp = Str(s)
}

sealed trait Exp {
  def bytes: ByteString
  def :+(that: Exp) = Add(this, Exp(that))
  def :-(that: Exp) = Sub(this, Exp(that))
  def :*(that: Exp) = Mult(this, Exp(that))
  def :\(that: Exp) = Div(this, Exp(that))
  def :<(that: Exp) = Lt(this, Exp(that))
  def :<=(that: Exp) = LtEq(this, Exp(that))
  def :>(that: Exp) = Lt(this, Exp(that))
  def :>=(that: Exp) = LtEq(this, Exp(that))
  def :==(that: Exp) = Eq(this, Exp(that))
}
case class Par(exp: Exp) extends Exp {
  val bytes = PARENL ++ exp.bytes ++ PARENR
}
//case class Func
case class Var(name: String) extends Exp {
  val bytes = ByteString(name)
  def :=(exp: Exp) = Assign(this, exp)
}
case object True extends Exp {
  val bytes = ByteString("true")
}
case object False extends Exp {
  val bytes = ByteString("false")
}
case object nil extends Exp {
  val bytes = ByteString("nil")
}
case class Num(value: Double) extends Exp {
  val bytes = ByteString(value.toString)
}
case class Str(value: String) extends Exp {
  val bytes = ByteString("\"" + value + "\"")
}
case class Table(fields: Field*) extends Exp {
  val bytes = CURLYL ++ (if (fields.nonEmpty) (fields.head.bytes /: fields.tail)(_ ++ COMMA ++ _.bytes) else ByteString.empty) ++ CURLYR
}
object Field {
  implicit def expToField(exp: Exp): Field = FieldV(exp)
}
sealed trait Field {
  def bytes: ByteString
}
case class FieldV(value: Exp) extends Field {
  val bytes = value.bytes
}
/*  case class Fun extends Exp {
 def bytes = this.toString
 }
*/

sealed trait BinOp extends Exp {
  def a: Exp
  def b: Exp
  def bytes: ByteString
}
case class Add(a: Exp, b: Exp) extends BinOp {
  val bytes = a.bytes ++ ADD ++ b.bytes
}
case class Sub(a: Exp, b: Exp) extends BinOp {
  val bytes = a.bytes ++ SUB ++ b.bytes
}
case class Mult(a: Exp, b: Exp) extends BinOp {
  val bytes = a.bytes ++ MULT ++ b.bytes
}
case class Div(a: Exp, b: Exp) extends BinOp {
  val bytes = a.bytes ++ DIV ++ b.bytes
}
case class Lt(a: Exp, b: Exp) extends BinOp {
  val bytes = a.bytes ++ LT ++ b.bytes
}
case class LtEq(a: Exp, b: Exp) extends BinOp {
  val bytes = a.bytes ++ LTEQ ++ b.bytes
}
case class Gt(a: Exp, b: Exp) extends BinOp {
  val bytes = a.bytes ++ GT ++ b.bytes
}
case class GtEq(a: Exp, b: Exp) extends BinOp {
  val bytes = a.bytes ++ GTEQ ++ b.bytes
}
case class Eq(a: Exp, b: Exp) extends BinOp {
  val bytes = a.bytes ++ EQ ++ b.bytes
}

/*
sealed trait UnOp extends Exp
case class Minus(exp: Exp) extends UnOp
case class Not(exp: Exp) extends UnOp*/
