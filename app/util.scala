import java.math.RoundingMode
import java.text.DecimalFormat

import play.api.libs.json.{JsNull, JsObject, JsValue}

package object util {

  val decimalFormat10 = {
    val df = new DecimalFormat("0.##########")
    df.setRoundingMode(RoundingMode.DOWN)
    df
  }


  implicit class EnhancedJsObject(jsObject: JsObject) {
    def stripNulls: JsObject = {
      alterFields {
        case (_, JsNull) => None
      }
    }

    def alterFields(customOperation: PartialFunction[(String, JsValue),Option[(String, JsValue)]]): JsObject = {
      val standardOperation: PartialFunction[(String, JsValue),Option[(String, JsValue)]] = {
        case (name, value: JsObject) => Some((name, value.alterFields(customOperation)))
        case (name, value) => Some((name, value))
      }
      val executePartials = customOperation orElse standardOperation
      JsObject(jsObject.fields.flatMap(executePartials(_).toList))
    }
  }
}
