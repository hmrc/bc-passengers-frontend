package binders

import models.ProductPath
import play.api.mvc.PathBindable

object Binders {

  implicit def productPathBinder(implicit stringBinder: PathBindable[String]): PathBindable[ProductPath] = new PathBindable[ProductPath] {

    override def bind(key: String, value: String): Either[String, ProductPath] = {
      val re = """^[a-z-/]+$""".r
      value match {
        case re() => Right(ProductPath(value.split("/").toList))
        case _ => Left("Invalid product path component")
      }
    }

    override def unbind(key: String, value: ProductPath): String = {
      value.toString
    }
  }

}
