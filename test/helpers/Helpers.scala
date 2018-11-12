package helpers

import java.io.InputStream

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.networknt.schema.{JsonSchema, JsonSchemaFactory, ValidationMessage}
import play.api.libs.json.JsValue

import scala.collection.JavaConversions._
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
      (new JsonSchemaFactory).getSchema(schemaJson)
    }


    private def validateInternal(subject: JsonNode): Set[ValidationMessage] = {
      schema.validate(subject).toSet
    }

    def validate(subjectJson: JsonNode): SchemaValidationResult = {
      val errors: Set[ValidationMessage] = validateInternal(subjectJson)
      SchemaValidationResult(errors)
    }

  }

}
