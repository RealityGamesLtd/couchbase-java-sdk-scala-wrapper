package com.realitygames.couchbase

import com.couchbase.client.java.view.AsyncViewRow
import play.api.libs.json.{Json, Reads}

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.breakOut
import scala.concurrent.{ExecutionContext, Future}

object RxObservableConversion {

  protected[couchbase] implicit def asyncViewRow2x[T](
    view: AsyncViewRow
  )(
    implicit ec: ExecutionContext,
    reads: Reads[T]
  ): T = {
    Json.parse(view.value().toString).validate[T].get
  }

  implicit protected[couchbase] class ObservableConversions[T](underlying: rx.Observable[T]) {

    //TODO
    def asFuture(implicit ec: ExecutionContext): Future[T] = Future{ underlying.toBlocking.first() }

    def asFutureList[A](implicit unpack: T => A, ec: ExecutionContext): Future[Seq[A]] = {
      Future {
        underlying.toList.toBlocking.first().map(unpack)(breakOut)
      }
    }
  }
}
