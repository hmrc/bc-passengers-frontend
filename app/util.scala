import java.math.RoundingMode
import java.text.DecimalFormat

import play.api.libs.json.{JsArray, JsNull, JsObject, JsValue}

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
        case (name, jso: JsObject) => Some((name, jso.alterFields(customOperation)))
        case (name, jsa: JsArray) => Some((name, JsArray((jsa.value map {
          case JsNull => None
          case o: JsObject => Some(o.alterFields(customOperation))
          case v: JsValue => Some(v)
        }).flatten)))
        case (name, jsv) => Some((name, jsv))
      }
      val executePartials: PartialFunction[(String, JsValue), Option[(String, JsValue)]] = customOperation orElse standardOperation
      JsObject(jsObject.fields.flatMap(executePartials(_).toList))
    }
  }


}
