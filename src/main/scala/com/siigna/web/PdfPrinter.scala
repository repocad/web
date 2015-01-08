package com.siigna.web

import scala.scalajs.js

/**
 * A printer that can generate pdf files
 */
class PdfPrinter extends Printer {

  val document = js.Dynamic.global.jsPDF()

  /**
   * create an arc.
   * @param x The x-coordinate of the center of the arc
   * @param y y	The y-coordinate of the center of the arc
   * @param r r	The radius of the arc
   * @param sAngle	The starting angle, in radians (0 is at the 3 o'clock position of the arc's circle)
   * @param eAngle	The ending angle, in radians
   */
  def arc(x: Double,y: Double,r: Double,sAngle: Double,endAngle: Double) : Unit = {
    val v = transform(Vector2D(x, y))
    //document.lines(v1.x,v1.y,v2.x,v2.y,v3.x,v3.y,v4.x,v4.y)
  }

  def bezierCurve(x1: Double,y1: Double,x2: Double,y2: Double,x3: Double,y3: Double,x4: Double,y4: Double) : Unit = {
    val v1 = transform(Vector2D(x1, y1))
    val v2 = transform(Vector2D(x2, y2))
    val v3 = transform(Vector2D(x3, y3))
    val v4 = transform(Vector2D(x4, y4))
    document.lines(v1.x,v1.y,v2.x,v2.y,v3.x,v3.y,v4.x,v4.y)
  }

  def circle(x : Double, y : Double, r : Double) : Unit = {
    val v = transform(Vector2D(x, y))
    document.circle(v.x,v.y,r)
  }

  def line(x1 : Double, y1 : Double, x2 : Double, y2 : Double) : Unit = {
    val v1 = transform(Vector2D(x1, y1))
    val v2 = transform(Vector2D(x2, y2))
    document.setLineWidth(0.02)
    document.line(v1.x, v1.y, v2.x, v2.y)
  }

  def text(x : Double, y : Double, h : Double, t : Any) : Unit = {
    val v = transform(Vector2D(x, y))
    //document.font("Arial")
    document.setFontSize(h * 1.92)
    //document("test")
    document.text(v.x,v.y,t.toString())
  }

  private def transform(v : Vector2D): Vector2D = {
    //TODO: use a calculated paper scale...
    val paperScale = 1
    //scale the artwork based on the current paper scale
    //NOTE: Y is flipped.
    val vec =  Vector2D(v.x / paperScale,-v.y / paperScale)
    //Transform by moving (0, 0) to center of paper
    vec + Vector2D(105,147)

  }

  def save(name : String): Unit = {
    document.save(name)
  }

}
