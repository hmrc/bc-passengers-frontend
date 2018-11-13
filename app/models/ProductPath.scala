package models

import play.api.libs.json._

case class ProductPath(components: List[String]) {
  override def toString = components.mkString("/")
  def addingComponent(component: String) = ProductPath(components :+ component)

  def categoryComponent = components.dropRight(1)
  def toMessageKey = components.mkString(".")
}

object ProductPath {
  def apply(path: String): ProductPath = ProductPath(path.split("/").toList)

  implicit val formats = new Format[ProductPath] {
    override def writes(o: ProductPath): JsValue = JsString(o.toString)
    override def reads(json: JsValue): JsResult[ProductPath] = json match {
      case JsString(value) => JsSuccess[ProductPath](ProductPath(value))
      case _ => JsError("Invalid ProductPath json")
    }
  }
}
