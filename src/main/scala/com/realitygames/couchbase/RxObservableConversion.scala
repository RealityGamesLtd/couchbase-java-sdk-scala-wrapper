package com.realitygames.couchbase

import java.util

import com.couchbase.client.java.query.AsyncN1qlQueryRow
import com.couchbase.client.java.view.AsyncViewRow
import play.api.libs.json.{Json, Reads}
import rx.SingleSubscriber

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.breakOut
import scala.concurrent.{ExecutionContext, Future, Promise}
object RxObservableConversion extends RowsConversions {

  implicit protected[couchbase] class ObservableConversions[T](underlying: rx.Observable[T]) {

    def asFuture(implicit ec: ExecutionContext): Future[T] = {
      val p = Promise[T]()
      underlying.toSingle.subscribe(new SingleSubscriber[T]{
        override def onError(error: Throwable): Unit = p.failure(error)
        override def onSuccess(value: T): Unit = p.success(value)
      })

      p.future
    }

    def mapAsFuture[A](f: T => A)(implicit ec: ExecutionContext): Future[Seq[A]] = {

      val p = Promise[List[A]]()
      underlying.toList.toSingle.subscribe(new SingleSubscriber[util.List[T]] {
        override def onError(error: Throwable): Unit = p.failure(error)
        override def onSuccess(value: util.List[T]): Unit = p.success(value.toList.map(f)(breakOut))
      })

      p.future
    }
  }
}
