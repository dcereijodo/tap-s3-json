package com.pagantis.singer.taps.test

import com.pagantis.singer.taps.JsonPaths
import org.scalatest._

class TestJsonPaths extends FlatSpec with Matchers {

  "JsonPaths" should "extract a single string value" in {
    val jsonPaths = List(
      "$.keyA.keyA1"
    )
    JsonPaths.asList(
      """
        | {
        |   "keyA" : {
        |     "keyA1" : "valueA1"
        |   }
        | }
      """.stripMargin,
      jsonPaths
    ) shouldBe List(
      Some("valueA1")
    )
  }

  "JsonPaths" should "extract two single string values" in {
    val jsonPaths = List(
      "$.keyA.keyA1",
      "$.keyA.keyA2"
    )
    JsonPaths.asList(
      """
        | {
        |   "keyA" : {
        |     "keyA1" : "valueA1",
        |     "keyA2" : "valueA2"
        |   }
        | }
      """.stripMargin,
      jsonPaths
    ) shouldBe List(
      Some("valueA1"),
      Some("valueA2")
    )
  }

  "JsonPaths" should "extract one single integer value" in {
    val jsonPaths = List(
      "$.keyA.keyA3"
    )
    JsonPaths.asList(
      """
        | {
        |   "keyA" : {
        |     "keyA3" : 23
        |   }
        | }
      """.stripMargin,
      jsonPaths
    ) shouldBe List(
      Some("23")
    )
  }

  "JsonPaths" should "extract first element from an array" in {
    val jsonPaths = List(
      "$.keyA.keyA4[0]"
    )
    JsonPaths.asList(
      """
        | {
        |   "keyA" : {
        |     "keyA4" : [
        |       "valueA4"
        |     ]
        |   }
        | }
      """.stripMargin,
      jsonPaths
    ) shouldBe List(
      Some("valueA4")
    )
  }

  "JsonPaths" should "extract non existent array element as None" in {
    val jsonPaths = List(
      "$.keyA.keyA4[2]"
    )
    JsonPaths.asList(
      """
        | {
        |   "keyA" : {
        |     "keyA4" : [
        |       "valueA4"
        |     ]
        |   }
        | }
      """.stripMargin,
      jsonPaths
    ) shouldBe List(
      None
    )
  }

  "JsonPaths" should "extract non existent key as None" in {
    val jsonPaths = List(
      "$.keyB.keyA3[2]"
    )
    JsonPaths.asList(
      """
        | {
        |   "keyA" : {
        |     "keyA4" : [
        |       "valueA4"
        |     ]
        |   }
        | }
      """.stripMargin,
      jsonPaths
    ) shouldBe List(
      None
    )
  }

  "JsonPaths" should "extract null value as None" in {
    val jsonPaths = List(
      "$.keyA.keyA4[0]"
    )
    JsonPaths.asList(
      """
        | {
        |   "keyA" : {
        |     "keyA4" : [
        |       null
        |     ]
        |   }
        | }
      """.stripMargin,
      jsonPaths
    ) shouldBe List(
      None
    )
  }

}
