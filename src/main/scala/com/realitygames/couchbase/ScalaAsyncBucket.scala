package com.realitygames.couchbase

import java.util.concurrent.TimeUnit

import com.couchbase.client.java.view.ViewQuery
import com.couchbase.client.java.{CouchbaseCluster, AsyncBucket => JavaAsyncBucket}
import com.realitygames.couchbase.RxObservableConversion.{ObservableConversions, asyncViewRow2document}
import com.realitygames.couchbase.ViewResult.{FailureViewResult, SuccessViewResult}
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ScalaAsyncBucket(bucket: JavaAsyncBucket) {

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

  def query[T](query: ViewQuery)(implicit reads: Reads[T], ec: ExecutionContext): Future[ViewResult[T]] = bucket.query(query).asFuture flatMap { viewResult =>

    if (viewResult.success()) {
      viewResult.rows().asFutureList[Document[T]](asyncViewRow2document(_)(ec, reads), ec) map { documents =>

        SuccessViewResult(
          documents,
          viewResult.totalRows()
        )
      }
    } else {
      Future.successful(FailureViewResult)
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
