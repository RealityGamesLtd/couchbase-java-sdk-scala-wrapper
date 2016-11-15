package com.realitygames.couchbase.query

sealed abstract class RowValue

case class JsonRowValue(value: String) extends RowValue

case class PrimitiveRowValue(value: Any) extends RowValue
