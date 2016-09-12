package com.realitygames.couchbase

import com.couchbase.client.java.{AsyncBucket => JavaAsyncBucket}
import Conversion._
import RxObservableConversion.ObservableConversions
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class AsyncBucket(bucket: JavaAsyncBucket) {

  def close()(implicit ec: ExecutionContext): Future[Boolean] = {
    bucket.close().asFuture map { _.booleanValue }
  }

  def exists(id: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    bucket.exists(id).asFuture map { _.booleanValue }
  }

  def get[T](id: String)(implicit reads: Reads[T], ec: ExecutionContext): Future[Document[T]] = {
    bucket.get(id).asFuture map Document.fromCouchbaseDoc[T]
  }

  def getAndTouch[T](id: String, expiry: Expiration)(implicit reads: Reads[T], ec: ExecutionContext): Future[Document[T]] = {
    bucket.getAndTouch(id, expiry.seconds).asFuture map Document.fromCouchbaseDoc[T]
  }

  def insert[T](id: String, value: T)(implicit reads: Reads[T], writes: Writes[T], ec: ExecutionContext): Future[Document[T]] = {
    bucket.insert(value.createCouchbaseDocument(id)).asFuture map Document.fromRawCouchbaseDoc[T]
  }

  def remove(id: String)(implicit ec: ExecutionContext): Future[RemovedDocument] = {
    bucket.remove(id).asFuture map RemovedDocument.fromCouchbaseDoc
  }

  def replace[T](id: String, value: T)(implicit reads: Reads[T], writes: Writes[T], ec: ExecutionContext): Future[Document[T]] = {
    bucket.replace(value.createCouchbaseDocument(id)).asFuture map Document.fromRawCouchbaseDoc[T]
  }

  def upsert[T](id: String, value: T)(implicit reads: Reads[T], writes: Writes[T], ec: ExecutionContext): Future[Document[T]] = {
    bucket.upsert(value.createCouchbaseDocument(id)).asFuture map Document.fromRawCouchbaseDoc[T]
  }

  def atomicUpdate[T](id: String)(update: Document[T] => Future[T])(implicit reads: Reads[T], writes: Writes[T], ec: ExecutionContext): Future[Document[T]] = {

    def updateAndUnlockOnFailure(document: Document[T]): Future[T] = {
      val f = update(document)
      f.onFailure{ case t: Throwable => bucket.unlock(id, document.cas) }
      f
    }

    val documentF = bucket.getAndLock(id, 2).asFuture map Document.fromCouchbaseDoc[T]

    val replacedF = for {
      document <- documentF
      updated <- updateAndUnlockOnFailure(document)
    } yield {
      bucket.replace(updated.createCouchbaseDocument(id, document.cas)).asFuture map Document.fromRawCouchbaseDoc[T]
    }

    replacedF flatMap identity
  }

}
