package com.realitygames.couchbase

import scala.concurrent.{ExecutionContext, Future}

object RxObservableConversion {

  implicit class ObservableConversions[T](underlying: rx.Observable[T]) {

    //TODO
    def asFuture(implicit ec: ExecutionContext): Future[T] = Future{ underlying.toBlocking.first() }
  }
}
