package com.pagantis.singer.taps.test

import com.pagantis.singer.taps.JsonPaths
import org.scalatest._

class TestJsonPaths extends WordSpecLike with Matchers {

  "JsonPaths" must {
    "extract a single string value" in {
      val jsonPaths = new JsonPaths(
        List("$.keyA.keyA1")
      )
      jsonPaths.asList(
        """
          | {
          |   "keyA" : {
          |     "keyA1" : "valueA1"
          |   }
          | }
        """.stripMargin
      ) shouldBe List(
        Some("valueA1")
      )
    }
    "extract two single string values" in {
      val jsonPaths =  new JsonPaths(
        List(
          "$.keyA.keyA1",
          "$.keyA.keyA2"
        )
      )
      jsonPaths.asList(
        """
          | {
          |   "keyA" : {
          |     "keyA1" : "valueA1",
          |     "keyA2" : "valueA2"
          |   }
          | }
        """.stripMargin
      ) shouldBe List(
        Some("valueA1"),
        Some("valueA2")
      )
    }
    "extract one single integer value" in {
      val jsonPaths = new JsonPaths(
        List("$.keyA.keyA3")
      )
      jsonPaths.asList(
        """
          | {
          |   "keyA" : {
          |     "keyA3" : 23
          |   }
          | }
        """.stripMargin
      ) shouldBe List(
        Some("23")
      )
    }
    "extract first element from an array" in {
      val jsonPaths = new JsonPaths(
        List("$.keyA.keyA4[0]")
      )
      jsonPaths.asList(
        """
          | {
          |   "keyA" : {
          |     "keyA4" : [
          |       "valueA4"
          |     ]
          |   }
          | }
        """.stripMargin
      ) shouldBe List(
        Some("valueA4")
      )
    }
    "extract non existent array element as None" in {
      val jsonPaths = new JsonPaths(
        List("$.keyA.keyA4[2]")
      )
      jsonPaths.asList(
        """
          | {
          |   "keyA" : {
          |     "keyA4" : [
          |       "valueA4"
          |     ]
          |   }
          | }
        """.stripMargin
      ) shouldBe List(
        None
      )
    }
    "extract non existent key as None" in {
      val jsonPaths = new JsonPaths(
        List("$.keyB.keyA3[2]")
      )
      jsonPaths.asList(
        """
          | {
          |   "keyA" : {
          |     "keyA4" : [
          |       "valueA4"
          |     ]
          |   }
          | }
        """.stripMargin
      ) shouldBe List(
        None
      )
    }
    "extract null value as None" in {
      val jsonPaths = new JsonPaths(
        List("$.keyA.keyA4[0]")
      )
      jsonPaths.asList(
        """
          | {
          |   "keyA" : {
          |     "keyA4" : [
          |       null
          |     ]
          |   }
          | }
        """.stripMargin
      ) shouldBe List(
        None
      )
    }
  }

}
