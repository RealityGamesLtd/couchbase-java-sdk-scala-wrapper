package com.realitygames.couchbase

import com.couchbase.client.java.query.AsyncN1qlQueryRow
import com.couchbase.client.java.view.AsyncViewRow
import play.api.libs.json.{Json, Reads}

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.breakOut
import scala.concurrent.{ExecutionContext, Future}
object RxObservableConversion {

  protected[couchbase] implicit def asyncViewRow2document[T](
    view: AsyncViewRow
  )(
    implicit ec: ExecutionContext,
    reads: Reads[T]
  ): Document[T] = {

    Document(
      id = view.id(),
      cas = 0l,
      content = Conversion.value2jsValue(view.value()).validate[T].get
    )
  }

  implicit protected[couchbase] def asyncViewRow2document[T](
    view: AsyncN1qlQueryRow
  )(
    implicit ec: ExecutionContext,
    reads: Reads[T]
  ): Document[T] = {
    Document(
      id = "",
      cas = 0l,
      content = Json.parse(view.value().toString).\("auth-service").validate[T].get
    )
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
