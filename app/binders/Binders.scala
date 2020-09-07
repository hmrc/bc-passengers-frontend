/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package binders

import models.ProductPath
import play.api.mvc.{JavascriptLiteral, PathBindable}

object Binders {

  implicit val productPathBinder: PathBindable[ProductPath] = new PathBindable[ProductPath] {

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

  implicit val productPathJSLBinder: JavascriptLiteral[ProductPath] = new JavascriptLiteral[ProductPath] {
    override def to(value: ProductPath): String = s"""'${value.toString}'"""
  }

}
