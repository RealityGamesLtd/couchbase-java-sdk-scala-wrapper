package com.realitygames.couchbase.query

import com.couchbase.client.java.query.AsyncN1qlQueryRow
import com.couchbase.client.java.view.AsyncViewRow
import com.realitygames.couchbase.model.Document
import com.realitygames.couchbase.util.JsonConversions
import play.api.libs.json._

import scala.concurrent.ExecutionContext

protected[couchbase] trait RowsConversions extends JsonConversions {

  implicit protected[couchbase] def asyncViewRow2document[T](
    view: AsyncViewRow
  )(
    implicit ec: ExecutionContext,
    reads: Reads[T]
  ): Document[T] = {

    value2jsValue(view.value()).validate[T] match {
      case JsError(errors) =>
        throw JsResultException(errors)
      case JsSuccess(content, _) =>
        Document(view.id(), 0l, content)
    }
  }

  implicit protected[couchbase] def asyncN1qlRow2document[T](
    view: AsyncN1qlQueryRow
  )(
    implicit ec: ExecutionContext,
    reads: Reads[T]
  ): Document[T] = {
    Document(
      id = "",
      cas = 0l,
      content = Json.parse(view.value().toString).\(???.toString).validate[T].get
    )
  }

}