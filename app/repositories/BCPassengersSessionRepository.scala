/*
 * Copyright 2024 HM Revenue & Customs
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

package repositories

import com.google.inject.Singleton
import com.mongodb.client.model.Updates
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, ReturnDocument}
import play.api.libs.json.{Format, JsObject, Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BCPassengersSessionRepository @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[JsObject](
      collectionName = "frontendCache",
      mongoComponent = mongoComponent,
      domainFormat = implicitly[Format[JsObject]],
      indexes = Seq(
        IndexModel(
          ascending("updatedAt"),
          IndexOptions()
            .name("updated-at-index")
            .expireAfter(3600, TimeUnit.SECONDS)
        )
      )
    ) {

  def get()(implicit hc: HeaderCarrier): Future[Option[JsObject]] =
    hc.sessionId match {
      case Some(id) => collection.find(equal("_id", id.value)).headOption()
      case _        => Future.failed[Option[JsObject]](new Exception("Could not find sessionId in HeaderCarrier"))
    }

  def store[T](key: String, body: T)(implicit
    wts: Writes[T],
    hc: HeaderCarrier
  ): Future[T] =
    hc.sessionId match {
      case Some(id) =>
        collection
          .findOneAndUpdate(
            equal("_id", id.value),
            Updates.combine(Updates.set(key, Json.toJson(body)), Updates.set("updatedAt", new java.util.Date())),
            FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
          )
          .map(_ => body)
          .head()
      case _        => Future.failed[T](new Exception("Could not find sessionId in HeaderCarrier"))
    }

  def updateUpdatedAtTimestamp(implicit hc: HeaderCarrier): Future[JsObject] =
    hc.sessionId match {
      case Some(id) =>
        collection
          .findOneAndUpdate(
            equal("_id", id.value),
            Updates.set("updatedAt", new java.util.Date()),
            FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
          )
          .toFuture()
      case _        =>
        Future.failed(
          new Exception(
            "[BCPassengersSessionRepository][updateUpdatedAtTimestamp]Could not find sessionId in HeaderCarrier"
          )
        )
    }
}
