package com.pagantis.singer.taps.test

import com.pagantis.singer.taps.PartitioningUtils
import com.pagantis.singer.taps.exceptions.InvalidPrefixException
import org.scalatest._

class TestPartitioningUtils extends PartitioningUtils with Matchers with WordSpecLike {
  "PartitioningUtils" must {
    "build with S3 prefix" in {
      buildS3Prefix(Some("s3/prefix"), Some("date=2019-01-01")) shouldBe Some("s3/prefix/date=2019-01-01")
    }
    "build with S3 object key" in {
      buildS3Prefix(Some("8f3ac260-2d50-4a84-8e9b-eae8a6b79b7d"), None) shouldBe Some("8f3ac260-2d50-4a84-8e9b-eae8a6b79b7d")
    }
    "build without partitioning value" in {
      buildS3Prefix(Some("s3/prefix"), None) shouldBe Some("s3/prefix")
    }
    "fail on invalid prefix" in {
      an [InvalidPrefixException] should be thrownBy {
        buildS3Prefix(Some(""), Some("date=2019-01-01"))
      }
    }
  }
}
