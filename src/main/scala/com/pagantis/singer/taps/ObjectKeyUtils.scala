package com.pagantis.singer.taps

trait ObjectKeyUtils {

  def buildS3Prefix(s3Prefix: Option[String], partitioningSubPath: Option[String]): Option[String] =
    s3Prefix match {
      case Some(prefix) if prefix.length > 0 =>
          partitioningSubPath match {
            case Some(subPath) => Some(s"$prefix/$subPath")
            case None =>  Some(s"$prefix")
        }
      case _ => partitioningSubPath
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
