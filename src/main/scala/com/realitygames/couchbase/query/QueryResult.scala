package com.realitygames.couchbase.query

import com.realitygames.couchbase.model.Document
import play.api.libs.json.{JsObject, JsString}

sealed abstract class QueryResult[+T]

object QueryResult {

  case class FailureQueryResult(errors: JsObject) extends QueryResult[Nothing]

  object FailureQueryResult {
    def apply(errorMessage: String): FailureQueryResult = FailureQueryResult(JsObject(Map(
      "msg" -> JsString(errorMessage)
    )))
  }

  case class SuccessQueryResult[T](
    values: Seq[Document[T]],
    totalResults: Int,
    parseFailedDocuments: Seq[ParseFailedDocument]
  ) extends QueryResult[T]
}
