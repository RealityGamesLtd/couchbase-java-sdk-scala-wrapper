package com.realitygames.couchbase.models

case class TestStructure(
  string: String,
  int: Int,
  long: Long,
  byte: Byte,
  boolean: Boolean,
  float: Float,
  double: Double
)

//object TestStructure {
//  implicit val format: Format[TestStructure] = Json.format[TestStructure]
//}
