package com.realitygames.couchbase

import java.util.concurrent.TimeUnit

import com.couchbase.client.java.query.N1qlQuery
import com.couchbase.client.java.view.ViewQuery
import com.couchbase.client.java.{CouchbaseCluster, AsyncBucket => JavaAsyncBucket}
import com.realitygames.couchbase.model.{Document, Expiration, RemovedDocument}
import com.realitygames.couchbase.query.QueryResult.{FailureQueryResult, SuccessQueryResult}
import com.realitygames.couchbase.util.RxObservableConversion.ObservableConversions
import com.realitygames.couchbase.query.{ParseFailedDocument, QueryResult, RowsConversions}
import com.realitygames.couchbase.util.{DocumentUtil, JsonConversions}
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ScalaAsyncBucket(bucket: JavaAsyncBucket) extends RowsConversions with JsonConversions {

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
      viewResult.rows().mapAsFuture[Either[ParseFailedDocument, Document[T]]](asyncViewRow2document) map { documents =>

        SuccessQueryResult(
          documents collect { case Right(document) => document },
          viewResult.totalRows(),
          documents collect { case Left(failedDocument) => failedDocument }
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
                SuccessQueryResult(docs, docs.size, Seq.empty /*TODO*/)
              }
            } else {
              Future.successful(FailureQueryResult("query failed"))
            }

          }

      } else Future.successful(FailureQueryResult("Parse error - instant fail. Check your N1ql syntax."))
    }
  }
}

object ScalaAsyncBucket {
  def apply(configuration: BucketConfiguration): ScalaAsyncBucket = {
    val cluster = CouchbaseCluster.create(configuration.hosts:_*)

    configuration.timeout.map(_.toMillis) match {
      case Some(timeout) =>
        cluster
          .openBucket(configuration.bucket, configuration.password.orNull, timeout, TimeUnit.MILLISECONDS)
          .scalaAsync()
      case None =>
        cluster
          .openBucket(configuration.bucket, configuration.password.orNull)
          .scalaAsync()
    }


  }
}
