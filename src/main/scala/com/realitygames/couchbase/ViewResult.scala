package com.realitygames.couchbase

sealed abstract class ViewResult[+T]

object ViewResult {

  case object FailureViewResult extends ViewResult[Nothing]

  case class SuccessViewResult[T](
    values: Seq[Document[T]],
    totalResults: Int
  ) extends ViewResult[T]
}