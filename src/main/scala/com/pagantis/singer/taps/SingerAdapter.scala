package com.pagantis.singer.taps

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.config.ConfigFactory
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

trait SingerMessage {
  def asJsonString: String
}

case class TapS3JsonRecord(
                            `type`: String = "RECORD",
                            stream: String =  "raw",
                            time_extracted: Option[ZonedDateTime],
                            key: Option[String] = None,
                            version: Option[String] = None,
                            last_modified_at: Option[String] = None,
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
  implicit val recordSerde: RootJsonFormat[TapS3JsonRecord] = jsonFormat7(TapS3JsonRecord)
}

object SingerAdapter {

  def fromConfig: SingerAdapter = {
    val config = ConfigFactory.load().getConfig("tap")
    new SingerAdapter(config.getBoolean("ignore_headers"))
  }

}

class SingerAdapter(ignoreHeaders: Boolean = false) {
  import spray.json._
  import JsonProtocol._

  def toSingerRecord(line: String, objectKey: Option[String] = None, version: Option[String] = None, lastModifiedAt: Option[String] = None): TapS3JsonRecord =
    TapS3JsonRecord(time_extracted = None, record = line.parseJson, key = objectKey, version = version, last_modified_at = lastModifiedAt)

  def toJsonString(tapS3JsonRecord: TapS3JsonRecord): String = {
    if(ignoreHeaders) tapS3JsonRecord.record.compactPrint
    else tapS3JsonRecord.toJson.compactPrint
  }

}


