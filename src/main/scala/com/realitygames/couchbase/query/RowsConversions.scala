package com.realitygames.couchbase.query

import com.couchbase.client.java.query.AsyncN1qlQueryRow
import com.couchbase.client.java.view.AsyncViewRow
import com.realitygames.couchbase.model.Document
import com.realitygames.couchbase.util.JsonConversions
import play.api.libs.json._

import scala.concurrent.ExecutionContext

protected[couchbase] trait RowsConversions extends JsonConversions {

  protected[couchbase] def asyncViewRow2document[T](
    view: AsyncViewRow
  )(
    implicit ec: ExecutionContext,
    reads: Reads[T]
  ): Either[ParseFailedDocument, Document[T]] = {

    val jsValue = value2jsValue(view.value())

    jsValue.validate[T] match {
      case JsError(errors) =>
        Left(ParseFailedDocument(view.id(), jsValue, errors))
      case JsSuccess(content, _) =>
        Right(Document(
          id = view.id(),
          cas = 0l,
          content = content
        ))
    }
  }

  implicit protected[couchbase] def asyncN1qlRow2document[T](
    bucketName: String,
    view: AsyncN1qlQueryRow
  )(
    implicit ec: ExecutionContext,
    reads: Reads[T]
  ): Option[Either[ParseFailedN1ql, T]] = {

    val jsValueLookup = value2jsValue(view.value()) \ bucketName


    jsValueLookup.toOption map { jsValue =>
      jsValue.validate[T] match {
      case JsError(errors) =>
        Left(ParseFailedN1ql(jsValue, errors))
      case JsSuccess(content, _) =>
        Right(content)
    }}
  }

}
