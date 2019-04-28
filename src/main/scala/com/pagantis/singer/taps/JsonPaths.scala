package com.pagantis.singer.taps

import com.fasterxml.jackson.databind.ObjectMapper
import io.gatling.jsonpath.JsonPath

object JsonPaths {

  private def query(jsonObject : Object, jsonPath: String) = {

    JsonPath.query(jsonPath, jsonObject) match {
      // if the query was successful
      case Right(matches) if matches != null && matches.nonEmpty =>
        val firstMatch = matches.next
        // and the element found is not null, return it stringified
        if(firstMatch != null) Some(firstMatch.toString)
        else None
      case _ => None
    }

  }

  def asList(jsonValue: String, jsonPaths: List[String]): List[Option[String]] = {

    val jsonObject = (new ObjectMapper).readValue(jsonValue, classOf[Object])
    jsonPaths.map(
      query(jsonObject, _)
    )

  }

  def asMap(jsonValue: String, jsonPaths: List[String]): Map[String, Option[String]] = {

    val jsonObject = (new ObjectMapper).readValue(jsonValue, classOf[Object])
    jsonPaths.map(
      path => path -> query(jsonObject, path)
    )
    .toMap

  }

}
