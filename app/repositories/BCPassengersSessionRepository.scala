package repositories

import javax.inject._
import play.api.Configuration
import play.api.libs.json.{Format, JsObject, Json, Reads, Writes}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.mongo.ReactiveRepository
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.indexes.IndexType
import reactivemongo.api.indexes.Index
import uk.gov.hmrc.http.HeaderCarrier
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}


class BCPassengersSessionRepository @Inject()(mongoComponent: ReactiveMongoComponent, configuration: Configuration)
  extends ReactiveRepository[JsObject, BSONObjectID](
    collectionName = "frontendCache",
    mongo          = mongoComponent.mongoConnector.db,
    domainFormat   = implicitly[Format[JsObject]]
  ) {

  lazy val ttlInSeconds = configuration.get[Int]("frontendcache.ttlinseconds")

  override def indexes: Seq[Index] = {
    Seq(
      Index(
        key = Seq("updatedAt" -> IndexType.Ascending),
        name = Some("updated-at-index"),
        options = BSONDocument("expireAfterSeconds" -> ttlInSeconds)
      )
    )
  }

  def fetch[T](key:String)(implicit hc: HeaderCarrier, rds: Reads[T], executionContext: ExecutionContext):Future[Option[T]] = {
    hc.sessionId match {
      case Some(id) =>
        collection.find(BSONDocument("_id" -> id.value), Some(BSONDocument(key -> "1"))).one[JsObject].map {
          obj => obj.flatMap(job => (job \ key).asOpt[T])
        }
      case _ => Future.failed[Option[T]](new Exception("Could not find sessionId in HeaderCarrier"))
    }
  }

  def store[T](
    key: String,
    body: T)(implicit wts: Writes[T], hc: HeaderCarrier, executionContext: ExecutionContext) : Future[T] = {
    hc.sessionId match {
      case Some(id) => collection.update(false)
        .one(
          BSONDocument("_id" -> id.value),
          BSONDocument("$set" ->
            BSONDocument(key -> Json.toJson(body), "updatedAt" -> new java.util.Date())
          ),
          upsert = true
        ).map(_ => body)
      case _ => Future.failed[T](new Exception("Could not find sessionId in HeaderCarrier"))
    }
  }

  def updateUpdatedAtTimestamp(implicit hc: HeaderCarrier, executionContext: ExecutionContext) : Future[UpdateWriteResult] = {
    hc.sessionId match {
      case Some(id) => collection.update(false)
        .one(
          BSONDocument("_id" -> id.value),
          BSONDocument("$set" ->
            BSONDocument("updatedAt" -> new java.util.Date())
          ),
          upsert = true
        )
      case _ => Future.failed[UpdateWriteResult](new Exception("Could not find sessionId in HeaderCarrier"))
    }
  }
}
