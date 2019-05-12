package com.pagantis.singer.taps

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

trait SingerMessage {
  def asJsonString: String
}

case class TapS3JsonRecord(
                            `type`: String = "RECORD",
                            stream: String =  "jsonpath-matches",
                            time_extracted: Option[ZonedDateTime],
                            record: JsValue
                          )

// json protocol
object JsonProtocol extends DefaultJsonProtocol {
  implicit object DateJsonFormat extends RootJsonFormat[ZonedDateTime] {
    override def write(datetime: ZonedDateTime): JsValue = {
      JsString(datetime.format(DateTimeFormatter.ISO_INSTANT))
    }
    //noinspection NotImplementedCode
    override def read(json: JsValue): ZonedDateTime = ??? //TODO: we won't read dates in the stream for now
  }
  implicit val recordSerde: RootJsonFormat[TapS3JsonRecord] = jsonFormat4(TapS3JsonRecord)
}

object SingerAdapter {

  def fromConfig =
    new SingerAdapter(JsonPaths.fromConfig)

}

class SingerAdapter(jsonPaths: Option[JsonPaths]) {
  import spray.json._
  import JsonProtocol._

  def toSingerRecord(line: String) = {
    jsonPaths match {
      case Some(paths) => TapS3JsonRecord(time_extracted = None, record = paths.asMap(line).toJson)
      case None => TapS3JsonRecord(stream = "raw", time_extracted = None, record = line.parseJson)
    }
  }

  def toJsonString(tapS3JsonRecord: TapS3JsonRecord): String =
    tapS3JsonRecord.toJson.compactPrint

}


