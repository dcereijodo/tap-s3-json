package com.pagantis.singer.taps.test

import java.time.ZoneId

import com.pagantis.singer.taps.{JsonPaths, JsonProtocol, SingerAdapter, TapS3JsonRecord}
import org.scalatest._

class TestSingerModel extends WordSpecLike with Matchers {

  import spray.json._
  import JsonProtocol._

  "TapRecord" must {
    "serialize two matches to Singer Specification" in {

      val tapRecord = TapS3JsonRecord(
        time_extracted = Some(java.time.LocalDateTime.of(2019, 4, 23, 12, 0, 0).atZone(ZoneId.of("UTC"))),
        record =
          Map(
            "path1" -> Some("match1"),
            "path2" -> Some("match2")
          ).asInstanceOf[Map[String, Option[String]]].toJson
      )
      val tapRecordAsJson = tapRecord.toJson.compactPrint

      tapRecordAsJson shouldBe """{"type":"RECORD","stream":"jsonpath-matches","time_extracted":"2019-04-23T12:00:00Z","record":{"path1":"match1","path2":"match2"}}"""

    }
    "serialize no match to Singer Specification" in {
      val tapRecord = TapS3JsonRecord(
        time_extracted = Some(java.time.LocalDateTime.of(2019, 4, 23, 12, 0, 0).atZone(ZoneId.of("UTC"))),
        record = Map().asInstanceOf[Map[String, Option[String]]].toJson
      )

      val tapRecordAsJson = tapRecord.toJson.compactPrint

      //noinspection ScalaUnnecessaryParentheses
      tapRecordAsJson shouldBe(
        """{"type":"RECORD","stream":"jsonpath-matches","time_extracted":"2019-04-23T12:00:00Z","record":{}}"""
        )

    }
    "serialize nones to Singer Specification" in {
      val tapRecord = TapS3JsonRecord(
        time_extracted = Some(java.time.LocalDateTime.of(2019, 4, 23, 12, 0, 0).atZone(ZoneId.of("UTC"))),
        record = Map(
          "path1" -> None,
          "path2" -> Some("match2")
        ).toJson
      )

      import com.pagantis.singer.taps.JsonProtocol._
      import spray.json._
      val tapRecordAsJson = tapRecord.toJson.compactPrint

      tapRecordAsJson shouldBe """{"type":"RECORD","stream":"jsonpath-matches","time_extracted":"2019-04-23T12:00:00Z","record":{"path1":null,"path2":"match2"}}"""

    }
    "generate json-path matches" in {
      val singerAdapter = new SingerAdapter(
        Some(new JsonPaths(
          List("$.key1.sub-key")
        )
        )
      )

      singerAdapter.toSingerRecord(
        """
          | {
          |   "key1": {
          |     "sub-key": 34
          |   },
          |   "key2": false
          | }
        """.stripMargin) shouldBe TapS3JsonRecord(
        stream = "jsonpath-matches",
        time_extracted = None,
        record = JsObject(
          "$.key1.sub-key" -> JsString("34")
        ).toJson
      )

    }
    "generate raw records" in {

      val singerAdapter = new SingerAdapter(jsonPaths = None)
      val inputJson =
        """
          | {
          |   "key1": {
          |     "sub-key": 34
          |   },
          |   "key2": false
          | }
        """.stripMargin

      singerAdapter.toSingerRecord(inputJson) shouldBe TapS3JsonRecord(
        stream = "raw",
        time_extracted = None,
        record = inputJson.parseJson
      )

    }
    "ignore headers when printing records" in {
      val singerAdapter = new SingerAdapter(jsonPaths = None, ignoreHeaders = true)
      val inputJson =
        """
          | {
          |   "key1": {
          |     "sub-key": 34
          |   },
          |   "key2": false
          | }
        """.stripMargin.parseJson.compactPrint
      singerAdapter.toJsonString(singerAdapter.toSingerRecord(inputJson)) shouldBe inputJson

    }
  }
}
