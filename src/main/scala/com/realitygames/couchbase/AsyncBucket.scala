package com.realitygames.couchbase

import com.couchbase.client.java.query.{AsyncN1qlQueryRow, N1qlQuery, N1qlQueryResult}
import com.couchbase.client.java.view.ViewQuery
import com.couchbase.client.java.{AsyncBucket => JavaAsyncBucket}
import com.realitygames.couchbase.RxObservableConversion._
import com.realitygames.couchbase.QueryResult.{FailureQueryResult, SuccessQueryResult}
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class AsyncBucket(bucket: JavaAsyncBucket) extends RowsConversions with JsonConversions {

  def atomicUpdate[T](id: String, lockTime: Duration = 3.seconds)(update: Document[T] => Future[T])(implicit format: Format[T], ec: ExecutionContext): Future[Document[T]] = {

    assert(lockTime >= 1.seconds && lockTime <= 30.seconds, "Lock time must be between 1 and 30 seconds")

    def updateAndUnlockOnFailure(document: Document[T]): Future[T] = {
      val f = update(document)
      f.onFailure{ case t: Throwable => bucket.unlock(id, document.cas) }
      f
    }

    val documentF = bucket.getAndLock(DocumentUtil.createCouchbaseDocument(id), lockTime.toSeconds.toInt).asFuture map DocumentUtil.fromCouchbaseDocument[T]

    val replacedF = for {
      document <- documentF
      updated <- updateAndUnlockOnFailure(document)
    } yield {
      bucket.replace(DocumentUtil.createCouchbaseDocument(id, Some(updated), cas = document.cas)).asFuture map DocumentUtil.fromCouchbaseDocument[T]
    }

    replacedF flatMap identity
  }

  def close()(implicit ec: ExecutionContext): Future[Boolean] = {
    bucket.close().asFuture map { _.booleanValue }
  }

  def exists(id: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    bucket.exists(id).asFuture map { _.booleanValue }
  }

  def get[T](id: String)(implicit format: Format[T], ec: ExecutionContext): Future[Document[T]] = {
    val document = DocumentUtil.createCouchbaseDocument(id)
    bucket.get(document).asFuture map DocumentUtil.fromCouchbaseDocument[T]
  }

  def getAndTouch[T](id: String, expiry: Expiration)(implicit format: Format[T], ec: ExecutionContext): Future[Document[T]] = {
    val document = DocumentUtil.createCouchbaseDocument(id, expiry = expiry.seconds)
    bucket.getAndTouch(document).asFuture map DocumentUtil.fromCouchbaseDocument[T]
  }

  def insert[T](id: String, value: T, expiration: Expiration = Expiration.none)(implicit format: Format[T], writes: Writes[T], ec: ExecutionContext): Future[Document[T]] = {
    val document = DocumentUtil.createCouchbaseDocument(id, Some(value), expiration.seconds)
    bucket.insert(document).asFuture map DocumentUtil.fromCouchbaseDocument[T]
  }

  def remove(id: String)(implicit ec: ExecutionContext): Future[RemovedDocument] = {
    bucket.remove(id).asFuture map RemovedDocument.fromCouchbaseDoc
  }

  def replace[T](id: String, value: T)(implicit format: Format[T], ec: ExecutionContext): Future[Document[T]] = {
    val document = DocumentUtil.createCouchbaseDocument(id, Some(value))
    bucket.replace(document).asFuture map DocumentUtil.fromCouchbaseDocument[T]
  }

  def upsert[T](id: String, value: T)(implicit format: Format[T], ec: ExecutionContext): Future[Document[T]] = {
    val document = DocumentUtil.createCouchbaseDocument(id, Some(value))
    bucket.upsert(document).asFuture map DocumentUtil.fromCouchbaseDocument[T]
  }

  def query[T](query: ViewQuery)(implicit reads: Reads[T], ec: ExecutionContext): Future[QueryResult[T]] = bucket.query(query).asFuture flatMap { viewResult =>

    if (viewResult.success()) {
      viewResult.rows().mapAsFuture[Document[T]](asyncViewRow2document) map { documents =>

        SuccessQueryResult(
          documents,
          viewResult.totalRows()
        )
      }
    } else {
//      viewResult.error().asFutureNoUnpack(ec) map (e => FailureQueryResult(e)) TODO
      Future.successful(FailureQueryResult("error"))
    }
  }

  def query[T](query: N1qlQuery)(implicit reads: Reads[T], ec: ExecutionContext): Future[QueryResult[T]] = {
    bucket.query(query).asFuture flatMap { queryResult =>
      if(queryResult.parseSuccess()) {
          queryResult.finalSuccess().asFuture flatMap { success =>
            if(success) {
              queryResult.rows().mapAsFuture[Document[T]](asyncN1qlRow2document).map { docs =>
                SuccessQueryResult(docs, docs.size)
              }
            } else {
              Future.successful(FailureQueryResult("query failed"))
            }

          }

      } else Future.successful(FailureQueryResult("Parse error - instant fail. Check your N1ql syntax."))
    }
  }
}
