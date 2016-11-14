package com.realitygames.couchbase.util

import java.util.{List => JavaList}

import com.realitygames.couchbase.query.RowConversions
import rx.SingleSubscriber

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.breakOut
import scala.concurrent.{ExecutionContext, Future, Promise}
object RxObservableConversion extends RowConversions {

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
      underlying.toList.toSingle.subscribe(new SingleSubscriber[JavaList[T]] {
        override def onError(error: Throwable): Unit = p.failure(error)
        override def onSuccess(value: JavaList[T]): Unit = p.success(value.toList.map(f)(breakOut))
      })

      p.future
    }
  }
}
