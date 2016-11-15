package com.realitygames.couchbase

import java.util.concurrent.TimeUnit

import com.couchbase.client.java.query.N1qlQuery
import com.couchbase.client.java.view.ViewQuery
import com.couchbase.client.java.{CouchbaseCluster, AsyncBucket => JavaAsyncBucket}
import com.realitygames.couchbase.json.{JsonFormatter, JsonReader, JsonWriter}
import com.realitygames.couchbase.model.{Document, Expiration, RemovedDocument}
import com.realitygames.couchbase.query.N1qlQueryResult.{FailureN1qlQueryResult, SuccessN1qlQueryResult}
import com.realitygames.couchbase.query.QueryResult.{FailureQueryResult, SuccessQueryResult}
import com.realitygames.couchbase.query._
import com.realitygames.couchbase.util.DocumentUtil
import com.realitygames.couchbase.util.RxObservableConversion.ObservableConversions

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.Failure

class ScalaAsyncBucket(bucket: JavaAsyncBucket) extends RowConversions {

  def atomicUpdate[A, B](
    id: String,
    lockTime: Duration = 3.seconds
  )(
    update: Document[B] => Future[Either[A, B]]
  )(
    implicit format: JsonFormatter[B],
    ec: ExecutionContext
  ): Future[Either[A, Document[B]]] = {

    assert(lockTime >= 1.seconds && lockTime <= 30.seconds, "Lock time must be between 1 and 30 seconds")

    val documentF = bucket.getAndLock(DocumentUtil.createCouchbaseDocument(id), lockTime.toSeconds.toInt).asFuture map DocumentUtil.fromCouchbaseDocument[B]

    documentF flatMap { document =>

      update(document) flatMap {
        case Left(l) =>

          bucket.unlock(id, document.cas)
          Future.successful(Left(l))

        case Right(updatedValue) =>

          val updatedDocument = bucket.replace(DocumentUtil.createCouchbaseDocument(id, Some(updatedValue), cas = document.cas))
            .asFuture map DocumentUtil.fromCouchbaseDocument[B]
          updatedDocument onComplete {
            case Failure(_) =>
              bucket.unlock(id, document.cas)
            case _ =>
          }
          updatedDocument map { Right(_) }

      }
    }
  }

  def atomicUpdateOpt[T](
    id: String,
    lockTime: Duration = 3.seconds
  )(
    update: Document[T] => Future[Option[T]]
  )(
    implicit format: JsonFormatter[T],
    ec: ExecutionContext
  ): Future[Option[Document[T]]] = {

    atomicUpdate[Unit, T](id, lockTime){doc => update(doc) map {_ toRight ()}} map {_.toOption}
  }

  def atomicUpdateSimple[T](
    id: String,
    lockTime: Duration = 3.seconds
  )(
    update: Document[T] => Future[T]
  )(
    implicit format: JsonFormatter[T],
    ec: ExecutionContext
  ): Future[Document[T]] = {
    atomicUpdate[Unit, T](id, lockTime){doc => update(doc) map { Right(_) } } map {_.toOption.get }
  }

  def close()(implicit ec: ExecutionContext): Future[Boolean] = {
    bucket.close().asFuture map { _.booleanValue }
  }

  def exists(id: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    bucket.exists(id).asFuture map { _.booleanValue }
  }

  def get[T](id: String)(implicit format: JsonFormatter[T], ec: ExecutionContext): Future[Document[T]] = {
    val document = DocumentUtil.createCouchbaseDocument(id)
    bucket.get(document).asFuture map DocumentUtil.fromCouchbaseDocument[T]
  }

  def getAndTouch[T](id: String, expiry: Expiration)(implicit format: JsonFormatter[T], ec: ExecutionContext): Future[Document[T]] = {
    val document = DocumentUtil.createCouchbaseDocument(id, expiry = expiry.seconds)
    bucket.getAndTouch(document).asFuture map DocumentUtil.fromCouchbaseDocument[T]
  }

  def insert[T](id: String, value: T, expiration: Expiration = Expiration.none)(implicit format: JsonFormatter[T], writes: JsonWriter[T], ec: ExecutionContext): Future[Document[T]] = {
    val document = DocumentUtil.createCouchbaseDocument(id, Some(value), expiration.seconds)
    bucket.insert(document).asFuture map DocumentUtil.fromCouchbaseDocument[T]
  }

  def remove(id: String)(implicit ec: ExecutionContext): Future[RemovedDocument] = {
    bucket.remove(id).asFuture map RemovedDocument.fromCouchbaseDoc
  }

  def replace[T](id: String, value: T, expiration: Expiration = Expiration.none)(implicit format: JsonFormatter[T], ec: ExecutionContext): Future[Document[T]] = {
    val document = DocumentUtil.createCouchbaseDocument(id, Some(value), expiry = expiration.seconds)
    bucket.replace(document).asFuture map DocumentUtil.fromCouchbaseDocument[T]
  }

  def upsert[T](id: String, value: T, expiration: Expiration = Expiration.none)(implicit format: JsonFormatter[T], ec: ExecutionContext): Future[Document[T]] = {
    val document = DocumentUtil.createCouchbaseDocument(id, Some(value), expiry = expiration.seconds)
    bucket.upsert(document).asFuture map DocumentUtil.fromCouchbaseDocument[T]
  }

  def query[T](query: ViewQuery)(implicit reads: JsonReader[T], ec: ExecutionContext): Future[QueryResult[T]] = bucket.query(query).asFuture flatMap { viewResult =>

    if (viewResult.success()) {
      viewResult.rows().mapAsFuture[Either[ParseFailedDocument, Document[T]]](asyncViewRow2document) map { documents =>

        SuccessQueryResult(
          values = documents collect { case Right(document) => document },
          totalResults = viewResult.totalRows(),
          parseFailedDocuments = documents collect { case Left(failedDocument) => failedDocument }
        )
      }
    } else {

      Future.successful(FailureQueryResult("error"))
    }
  }

  def query[T](query: N1qlQuery)(implicit reads: JsonReader[T], ec: ExecutionContext): Future[N1qlQueryResult[T]] = {
    bucket.query(query).asFuture flatMap { queryResult =>
      if(queryResult.parseSuccess) {
          queryResult.finalSuccess.asFuture.flatMap { success =>
            if(success) {
              queryResult.rows().mapAsFuture[Option[Either[ParseFailedN1ql, T]]](asyncN1qlRow2document(this.bucket.name, _)) map { docs =>
                SuccessN1qlQueryResult(
                  values = docs collect { case Some(Right(x)) => x },
                  totalResults = docs.size,
                  parseFailedDocuments = docs collect { case Some(Left(y)) => y }
                )
              }
            } else {
              queryResult.errors().toList.asFuture map { errors =>
                ???
//                FailureN1qlQueryResult(errors.toList.map(couchbaseJsonObject2playJsObject))
              }
            }

          }

      } else Future.successful(FailureN1qlQueryResult("Query parse error - instant fail. Check your N1ql syntax."))
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
