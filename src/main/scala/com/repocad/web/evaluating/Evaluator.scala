package com.repocad.web.evaluating

import com.repocad.web.{Vector2D, Printer}

import com.repocad.web.lexing.Lexer
import com.repocad.web.parsing._
import org.scalajs.dom
import org.scalajs.dom.extensions.Ajax
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.Success

/**
 * An evaluator to evaluate a list of [[Expr]]
 */
object Evaluator {
  //vars needed to update the drawing bounding box
  //harvest biggest and smallest Y-coordinates in order to dynamically scale the drawing paper
  var minX : Option[Double] = None
  var maxX : Option[Double] = None
  var minY : Option[Double] = None
  var maxY : Option[Double] = None
  var center = Vector2D(0,0)

  /*
  update the bounding box each time the drawing is evaluated.
   */
  def updateBoundingBox(x : Double, y: Double) : Vector2D = {
    if(minX.isDefined && maxX.isDefined && minY.isDefined && maxY.isDefined) {
      if (x > maxX.get) maxX = Some(x)
      if (x < minX.get) minX = Some(x)
      if (x > maxY.get) maxY = Some(y)
      if (x < minY.get) minY = Some(y)

      val cX = (maxX.get - minX.get) / 2 - minX.get
      val cY = (maxY.get - minY.get) / 2 - minY.get
      val center = Vector2D(cX, cY) //move the paper center to the center of the current artwork on the paper
      center
      //first run: set the bounding box.
    } else {
        maxX = Some(x)
        minX = Some(x)
        maxY = Some(y)
        minY = Some(y)
      center
    }
  }
  /* run once before the evaluation loop to ensure the paper is scaled down again
     if the drawing extends are smaller after user editing of the drawing.
  */
  def resetBoundingBox() = {
    maxX = None
    minX = None
    maxY = None
    minY = None
  }

  type Env = Map[String, Any]

  type Value = Either[String, (Env, Any)]

  def eval(expr: Expr, env : Env, printer : Printer) : Value = {

    expr match {

      case ArcExpr(centerX, centerY, radius, sAngle, eAngle) =>
        getValue[Double](centerX, env, printer).right.flatMap(x =>
          getValue[Double](centerY, env, printer).right.flatMap(y =>
            getValue[Double](radius, env, printer).right.flatMap(radiusValue =>
              getValue[Double](sAngle, env, printer).right.flatMap(startAngle =>
                getValue[Double](eAngle, env, printer).right.flatMap(endAngle => {
                  center = updateBoundingBox(x + radiusValue,y + radiusValue)
                  printer.arc(x,y,radiusValue,startAngle,endAngle)
                  Right(env -> Unit)
              })
            )
          )
        )
      )

      case BezierExpr(sx1, sy1, sx2, sy2, sx3, sy3, sx4, sy4) =>
        getValue[Double](sx1, env, printer).right.flatMap(x1 =>
          getValue[Double](sy1, env, printer).right.flatMap(y1 =>
            getValue[Double](sx2, env, printer).right.flatMap(x2 =>
              getValue[Double](sy2, env, printer).right.flatMap(y2 =>
                getValue[Double](sx3, env, printer).right.flatMap(x3 =>
                  getValue[Double](sy3, env, printer).right.flatMap(y3 =>
                    getValue[Double](sx4, env, printer).right.flatMap(x4 =>
                      getValue[Double](sy4, env, printer).right.flatMap(y4 => {
                        center = updateBoundingBox(x1,y1)
                        center = updateBoundingBox(x2,y2)
                        center = updateBoundingBox(x3,y3)
                        center = updateBoundingBox(x4,y4)
                        printer.bezierCurve(x1, y1, x2, y2, x3 , y3 , x4 , y4)
                      Right(env -> Unit)
                    })
                  )
                )
              )
            )
          )
        )
      )


      case CircleExpr(centerX, centerY, radius) =>
        getValue[Double](centerX, env, printer).right.flatMap(x =>
          getValue[Double](centerY, env, printer).right.flatMap(y =>
            getValue[Double](radius, env, printer).right.flatMap(radiusValue => {
              center = updateBoundingBox(x + radiusValue,y + radiusValue)
              center = updateBoundingBox(x - radiusValue,y - radiusValue)
              printer.circle(x,y,radiusValue)
              Right(env -> Unit)
            })
          )
        )

      case FunctionExpr(name, params, body) =>
        val function = params.size match {
          case 0 => () => eval(body, env, printer)
          case 1 => (a : Any) => {
            eval(body, env.+(params(0) -> a), printer)
          }
          case 2 => (a : Any, b : Any) => eval(body, env.+(params(0) -> a, params(1) -> b), printer)
          case 3 => (a : Any, b : Any, c : Any) => eval(body, env.+(params(0) -> a, params(1) -> b, params(2) -> c), printer)
          case 4 => (a : Any, b : Any, c : Any, d : Any) => eval(body, env.+(params(0) -> a, params(1) -> b, params(2) -> c, params(3) -> d), printer)
          case x => Left("Unsupported number of arguments: " + x)
        }
        Right(env.+(name -> function), function)

      case LineExpr(e1, e2, e3, e4) =>
        getValue[Double](e1, env, printer).right.flatMap(x1 =>
          getValue[Double](e2, env, printer).right.flatMap(y1 =>
            getValue[Double](e3, env, printer).right.flatMap(x2 => {
              getValue[Double](e4, env, printer).right.flatMap(y2 => {
                center = updateBoundingBox(x1,y1)
                center = updateBoundingBox(x2,y2)
                printer.line(x1, y1, x2, y2)
                Right(env -> Unit)
              })
            })
          )
        )

      case TextExpr(centerX, centerY, height, text) =>
        getValue[Double](centerX, env, printer).right.flatMap(x =>
          getValue[Double](centerY, env, printer).right.flatMap(y =>
            getValue[Double](height, env, printer).right.flatMap(heightValue =>
              getValue[Any](text, env, printer).right.flatMap(textValue => {
                //TODO: calculate text bounding box and add that to the center to get correct extends of the text
                center = updateBoundingBox(x + heightValue * 2,y + heightValue * 2)
                center = updateBoundingBox(x - heightValue * 2,y - heightValue * 2)
                printer.text(x,y,heightValue,textValue)
                Right(env -> Unit)
              })
            )
          )
        )

      case ConstantExpr(value) => Right(env -> value)

      case CompExpr(e1, e2, op) =>
        eval(e1, env, printer).fold(e => Left(e), v1 => eval(e2, v1._1, printer).fold(e => Left(e), v2 => {
          val n1 = v1._2.asInstanceOf[Double]
          val n2 = v2._2.asInstanceOf[Double]
          op match {
            case ">" => Right(env -> (n1 > n2))
            case "<" => Right(env -> (n1 < n2))
            case x => Left(s"Unknown comparison operator $x")
          }
        }))

      case OpExpr(e1, e2, op) =>
        eval(e1, env, printer).right.flatMap(v1 => eval(e2, v1._1, printer).right.flatMap(v2 => {
          val n1 = v1._2.asInstanceOf[Double]
          val n2 = v2._2.asInstanceOf[Double]
          op match {
            case "-" => Right(env -> (n1 - n2))
            case "+" => Right(env -> (n1 + n2))
            case "*" => Right(env -> (n1 * n2))
            case "/" => Right(env -> (n1 / n2))
            case x => Left(s"Unknown arithmetic operator $x")
          }
        }))

      case ImportExpr(name) =>
        Ajax.get("http://siigna.com:20004/get/" + name.name).map(xhr => {
          Parser.parse(Lexer.lex(xhr.responseText)) match {
            case Right(e) => eval(e, env, printer)
            case Left(error) => Left(s"Script $name failed to compile with error: $error")
          }
        }).value.get.get

      case RangeExpr(name, from, to) =>
        val fromOption : Either[String, Double] = env.get(name).map {
          case i: Int => Right(i + 1d)
          case i: Double => Right(i + 1)
          case x => Left(s"Cannot parse $x to int")
        }.getOrElse(getValue[Double](from, env, printer))

        val toOption = getValue[Double](to, env, printer)
        fromOption.right.flatMap(fromValue => toOption.right.flatMap(toValue => {
          Right((env + (name -> fromValue)) -> (fromValue < toValue))
        }))

      case RefExpr(name, params) =>
        val x = env.get(name)
        x.fold[Value](Left(s"Failed to find function '$name'. Please check if it has been declared.")){
          case f : Function0[Any] => Right(env -> f())
          case f : Function1[Any, Any] => {
            eval(params(0), env, printer).right.flatMap(a => Right(a._1 -> f.apply(a._2)))
          }
          case f : Function2[Any, Any, Any] => {
            eval(params(0), env, printer).right.flatMap(a =>
              eval(params(1), a._1, printer).right.flatMap(b =>
                Right(b._1 -> f.apply(a._2, b._2))
              )
            )
          }
          case x => Left("Expected callable function, got " + x)
        }

      case RefExpr(name) =>
        env.get(name).fold[Value](Left(s"Failed to find function '$name'. Please check if it has been declared."))(x => Right(env -> x))

      case seq : SeqExpr =>
        def foldRecursive(it : Iterator[Expr], foldEnv : Env) : Value = {
          eval(it.next(), foldEnv, printer).fold(error => Left(error), t => {
            if (it.hasNext) {
              foldRecursive(it, t._1)
            } else {
              Right(t._1 -> t._2)
            }
          })
        }
        foldRecursive(seq.expr.iterator, env)

      case UnitExpr => Right(env -> Unit)

      case ValExpr(name, value) =>
        eval(value, env, printer).fold(Left(_), value => Right(env.+(name -> value._2) -> value._2))

      case LoopExpr(condition : Expr, body : Expr) =>
        /* Note to self: Too much recursion error when looping recursively */
        var loopEnv : Map[String, Any] = env
        var lastResult : Any = Unit
        var lastError : Option[String] = None
        def getCondition = eval(condition, loopEnv, printer).fold(error => {lastError = Some(error); false}, v => {
          loopEnv = v._1
          v._2.asInstanceOf[Boolean]
        })
        while (lastError.isEmpty && getCondition) {
          eval(body, loopEnv, printer).fold(s => {lastError = Some(s); s}, x => {
            lastResult = x._2
            loopEnv = x._1
          })
        }
        lastError.map(Left(_)).getOrElse(Right(loopEnv.filter(t => env.contains(t._1)) -> lastResult))

      case x => Left(s"Unknown expression $x")
    }

  }

  def getValue[T](expr : Expr, env : Env, printer : Printer) : Either[String, T] = {
    eval(expr, env, printer) match {
      case Right((_, t)) if (t.isInstanceOf[Int]) => Right(t.asInstanceOf[Int].toDouble.asInstanceOf[T])
      case Right((_, t : T)) => Right(t)
      case fail => {
        Left(s"Failed to read value from $expr, failed with: $fail")
      }
    }
  }

}