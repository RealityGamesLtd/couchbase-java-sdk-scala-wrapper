package com.realitygames.couchbase

import com.couchbase.client.java.query.AsyncN1qlQueryRow
import com.couchbase.client.java.view.AsyncViewRow
import play.api.libs.json.{Json, Reads}

import scala.concurrent.ExecutionContext

protected[couchbase] trait RowsConversions extends JsonConversions {

  implicit protected[couchbase] def asyncViewRow2document[T](
    view: AsyncViewRow
  )(
    implicit ec: ExecutionContext,
    reads: Reads[T]
  ): Document[T] = {

    Document(
      id = view.id(),
      cas = 0l,
      content = value2jsValue(view.value()).validate[T].get
    )
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
