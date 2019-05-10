package com.pagantis.singer.taps

import com.pagantis.singer.taps.exceptions.InvalidPreffixException

trait PartitioningUtils {

  def buildS3Preffix(s3Preffix: Option[String], partitioningSubPath: Option[String]) =
    s3Preffix match {
      case Some(preffix) => {
        if(preffix.size < 1) throw new InvalidPreffixException
        else {
          partitioningSubPath match {
            case Some(subPath) => Some(s"$preffix/$subPath")
            case None =>  Some(s"$preffix")
          }
        }
      }
      case None => partitioningSubPath
    }

  def buildPartitioningSubPath(partitioningKey: Option[String], partitioningValue: Option[String]) =
    partitioningValue.flatMap(partVal => partitioningKey.map(partKey => s"$partKey=$partVal"))
}
