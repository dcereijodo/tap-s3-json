package com.pagantis.singer.taps

import com.pagantis.singer.taps.exceptions.InvalidPrefixException

trait ObjectKeyUtils {

  def buildS3Prefix(s3Prefix: Option[String], partitioningSubPath: Option[String]): Option[String] =
    s3Prefix match {
      case Some(prefix) =>
        if(prefix.length < 1) throw new InvalidPrefixException
        else {
          partitioningSubPath match {
            case Some(subPath) => Some(s"$prefix/$subPath")
            case None =>  Some(s"$prefix")
          }
        }
      case None => partitioningSubPath
    }

  def buildPartitioningSubPath(partitioningKey: Option[String], partitioningValue: Option[String]): Option[String] =
    partitioningValue.flatMap(partVal => partitioningKey.map(partKey => s"$partKey=$partVal"))

  def filterKey(key: String, expression: String) = {
    val keyPattern = expression.r
    key match {
      case keyPattern(_*) => true
      case _ => false
    }
  }

}
