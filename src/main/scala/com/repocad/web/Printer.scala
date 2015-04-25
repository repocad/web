package com.repocad.web

import com.repocad.web.evaluating.Evaluator

/**
 * A printer that can print objects on a medium
 */
trait Printer {

    //vars needed to update the drawing bounding box
    //harvest biggest and smallest Y-coordinates in order to dynamically scale the drawing paper
    protected val paper = new Paper()

  /*
  update the bounding box each time the drawing is evaluated.
   */
  def updateBoundingBox(x : Double, y: Double) : Vector2D = {

    if (x >= paper.maxX) paper.maxX = x
    if (x <= paper.minX) paper.minX = x
    if (y >= paper.maxY) paper.maxY = y
    if (y <= paper.minX) paper.minY = y

    println("MAX X;" + paper.maxX)


    //move the paper center to the center of the current artwork on the paper
    val cX = paper.minX + (paper.maxX - paper.minX) / 2
    val cY = paper.minY + (paper.maxY - paper.minY) / 2
    Vector2D(cX, cY)
  }

  /**
   * Draws an arc
   * @param x First coordinate
   * @param y Second coordinate
   * @param r Radius
   * @param sAngle start angle (3'o clock)
   * @param eAngle end angle
   */
  def arc(x : Double, y : Double, r : Double, sAngle : Double, eAngle : Double)

  /**
   * Draws a bezier curve
   * @param x1 start x
   * @param y1 start y
   * @param x2 control point1 x
   * @param y2 control point1 y
   * @param x3 control point2 x
   * @param y3 control point2 y
   * @param x4 end x
   * @param y4 start y
   */
  def bezierCurve(x1 : Double, y1 : Double, x2 : Double, y2 : Double, x3 : Double, y3 : Double, x4 : Double, y4 : Double)

  /**
   * Draws a circle
   * @param x First coordinate
   * @param y Second coordinate
   * @param r Radius
   */
  def circle(x : Double, y : Double, r : Double) : Unit

  def clear() : Unit

  /**
   * Renders a text string
   * @param x First coordinate
   * @param y Second coordinate
   * @param h Height
   * @param t Text
   */
  def text(x : Double, y : Double, h : Double, t : Any)

  /**
   * Renders a text box
   * @param x First coordinate
   * @param y Second coordinate
   * @param w Width
   * @param h Line height
   * @param t Text
   */
  def textBox(x : Double, y : Double, w: Double, h : Double, t : Any)

  /**
   * Draws a line
   * @param x1 First coordinate
   * @param y1 Second coordinate
   * @param x2 Third coordinate
   * @param y2 Fourth coordinate
   */
  def line(x1 : Double, y1 : Double, x2 : Double, y2 : Double)

  /**
   * Prepares the printer for drawing
   */
  def prepare() : Unit = {
    paper.resetBoundingBox()
  }

  lazy val toEnv : Evaluator.Env = {
    Map(
      "arc"  -> ((env : Evaluator.Env, x : Double, y : Double, r : Double, sAngle : Double, eAngle : Double) => arc(x, y, r, sAngle, eAngle)),
      "bezierCurve" -> ((env : Evaluator.Env, x1 : Double, y1 : Double, x2 : Double, y2 : Double, x3 : Double, y3 : Double, x4 : Double, y4 : Double) => bezierCurve(x1, y1, x2, y2, x3, y3, x4, y4)),
      "circle" -> ((env : Evaluator.Env, x : Double, y : Double, r : Double) => circle(x, y, r)),
      "line" -> ((env : Evaluator.Env, x1 : Double, y1 : Double, x2 : Double, y2 : Double) => line(x1, y1, x2, y2)),
      "text" -> ((env : Evaluator.Env, x : Double, y : Double, h : Double, t : Any) => text(x, y, h, t)),
      "textBox" -> ((env : Evaluator.Env, x : Double, y : Double, w: Double, h : Double, t : Any) => textBox(x, y, w, h, t))
    )
  }

}

object Printer {

  lazy val dummyEnv : Evaluator.Env = {
    Map(
      "arc"  -> ((env : Evaluator.Env, x : Double, y : Double, r : Double, sAngle : Double, eAngle : Double) => Unit),
      "bezierCurve" -> ((env : Evaluator.Env, x1 : Double, y1 : Double, x2 : Double, y2 : Double, x3 : Double, y3 : Double, x4 : Double, y4 : Double) => Unit),
      "circle" -> ((env : Evaluator.Env, x : Double, y : Double, r : Double) => Unit),
      "line" -> ((env : Evaluator.Env, x1 : Double, y1 : Double, x2 : Double, y2 : Double) => Unit),
      "text" -> ((env : Evaluator.Env, x : Double, y : Double, h : Double, t : Any) => Unit),
      "textBox" -> ((env : Evaluator.Env, x : Double, y : Double, w: Double, h : Double, t : Any) => Unit)
    )
  }
}