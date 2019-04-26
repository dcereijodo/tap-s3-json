package com.pagantis.singer.taps.test

import java.time.ZoneId

import com.pagantis.singer.taps.TapS3JsonRecord
import org.scalatest._

class TestSingerModel extends FlatSpec with Matchers {

  "TapRecord" should "serialize two match to Singer Specification" in {
    val tapRecord = TapS3JsonRecord(
      time_extracted = Some(java.time.LocalDateTime.of(2019, 4, 23, 12, 0, 0).atZone(ZoneId.of("UTC"))),
      record =
        Map(
          "path1" -> Some("match1"),
          "path2" -> Some("match2")
        )
    )

    import spray.json._
    import com.pagantis.singer.taps.JsonProtocol._
    val tapRecordAsJson = tapRecord.toJson.compactPrint

    tapRecordAsJson shouldBe """{"type":"RECORD","stream":"jsonpath-matches","time_extracted":"2019-04-23T12:00:00Z","record":{"path1":"match1","path2":"match2"}}"""

  }

  "TapRecord" should "serialize no match to Singer Specification" in {
    val tapRecord = TapS3JsonRecord(
      time_extracted = Some(java.time.LocalDateTime.of(2019, 4, 23, 12, 0, 0).atZone(ZoneId.of("UTC"))),
      record = Map()
    )

    import spray.json._
    import com.pagantis.singer.taps.JsonProtocol._
    val tapRecordAsJson = tapRecord.toJson.compactPrint

    tapRecordAsJson shouldBe(
      """{"type":"RECORD","stream":"jsonpath-matches","time_extracted":"2019-04-23T12:00:00Z","record":{}}"""
    )

  }

  "TapRecord" should "serialize nones to Singer Specification" in {
    val tapRecord = TapS3JsonRecord(
      time_extracted = Some(java.time.LocalDateTime.of(2019, 4, 23, 12, 0, 0).atZone(ZoneId.of("UTC"))),
      record = Map(
          "path1" -> None,
          "path2" -> Some("match2")
        )
    )

    import spray.json._
    import com.pagantis.singer.taps.JsonProtocol._
    val tapRecordAsJson = tapRecord.toJson.compactPrint

    tapRecordAsJson shouldBe(
      """{"type":"RECORD","stream":"jsonpath-matches","time_extracted":"2019-04-23T12:00:00Z","record":{"path1":null,"path2":"match2"}}"""
    )

  }

}
