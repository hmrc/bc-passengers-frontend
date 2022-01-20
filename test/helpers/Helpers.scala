/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package helpers

import java.io.InputStream

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.networknt.schema.{JsonSchema, JsonSchemaFactory, SpecVersion, ValidationMessage}
import play.api.libs.json.JsValue

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.io.Source

case class SchemaValidationResult(errors: Set[ValidationMessage]) {
  override def toString = {
      errors.map(x => x+"\n").mkString + "\n"
  }
  def isValid = errors.isEmpty
  def hasErrors = !isValid
}

object Helpers {

  def resourceAsString(resourcePath: String): Option[String] = {
    resourceAsInputStream(resourcePath) map { is =>
      Source.fromInputStream(is).getLines.mkString("\n")
    }
  }

  def resourceAsInputStream(resourcePath: String): Option[InputStream] = {
    Option(getClass.getResourceAsStream(resourcePath))
  }


  def resourceAsJsonNode(resourcePath: String): Option[JsonNode] = {
    Helpers.resourceAsInputStream(resourcePath) map { is =>
      try {
        new ObjectMapper().readTree(is)
      }
      catch {
        case e: Throwable => throw new RuntimeException(s"Exception mapping json for $resourcePath", e)
      }

    }
  }

  implicit def playToJackson(jsVal: JsValue): JsonNode = {
    om.readTree(jsVal.toString)
  }

  private val om = new ObjectMapper()

  class SchemaValidator(schemaResourcePath: String) {

    private lazy val schema: JsonSchema = {
      val schemaJson = Helpers.resourceAsJsonNode(schemaResourcePath).getOrElse(throw new RuntimeException("Missing schema: " + schemaResourcePath))
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaJson)
    }


    private def validateInternal(subject: JsonNode): Set[ValidationMessage] = {
      schema.validate(subject).asScala.toSet
    }

    def validate(subjectJson: JsonNode): SchemaValidationResult = {
      val errors: Set[ValidationMessage] = validateInternal(subjectJson)
      SchemaValidationResult(errors)
    }

  }

}
