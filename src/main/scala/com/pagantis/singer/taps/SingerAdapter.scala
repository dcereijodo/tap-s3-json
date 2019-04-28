package com.pagantis.singer.taps

import java.time.{LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

trait SingerMessage {
  def asJsonString: String
}

case class TapS3JsonRecord(
                            `type`: String = "RECORD",
                            stream: String =  "jsonpath-matches",
                            time_extracted: Option[ZonedDateTime],
                            record: Map[String,Option[String]]
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

  def toSingerRecord(matches: Map[String,Option[String]]) =
    TapS3JsonRecord(time_extracted = None, record = matches)

  def toJsonString(tapS3JsonRecord: TapS3JsonRecord): String = {
    import spray.json._
    import JsonProtocol._

    tapS3JsonRecord.toJson.compactPrint
  }

}


