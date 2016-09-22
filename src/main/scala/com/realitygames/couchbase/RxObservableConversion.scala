package com.realitygames.couchbase

import java.util

import com.couchbase.client.java.query.AsyncN1qlQueryRow
import com.couchbase.client.java.view.AsyncViewRow
import play.api.libs.json.{Json, Reads}
import rx.SingleSubscriber

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.breakOut
import scala.concurrent.{ExecutionContext, Future, Promise}
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

    def asFuture(implicit ec: ExecutionContext): Future[T] = {
      val p = Promise[T]()
      underlying.toSingle.subscribe(new SingleSubscriber[T]{
        override def onError(error: Throwable): Unit = p.failure(error)
        override def onSuccess(value: T): Unit = p.success(value)
      })

      p.future
    }

    def asFutureList[A](implicit unpack: T => A, ec: ExecutionContext): Future[Seq[A]] = {

      val p = Promise[List[A]]()
      underlying.toList.toSingle.subscribe(new SingleSubscriber[util.List[T]] {
        override def onError(error: Throwable): Unit = p.failure(error)
        override def onSuccess(value: util.List[T]): Unit = p.success(value.toList.map(unpack)(breakOut))
      })

      p.future
    }
  }
}
