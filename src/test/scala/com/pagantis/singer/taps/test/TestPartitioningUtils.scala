package com.pagantis.singer.taps.test

import com.pagantis.singer.taps.PartitioningUtils
import com.pagantis.singer.taps.exceptions.InvalidPreffixException
import org.scalatest._

class TestPartitioningUtils extends FlatSpec with PartitioningUtils with Matchers {

  "PartitioningUtils" should "build with S3 prefix" in {
    buildS3Preffix(Some("s3/prefix"), Some("date=2019-01-01")) shouldBe Some("s3/prefix/date=2019-01-01")
  }
  "PartitioningUtils" should "build with S3 object key" in {
    buildS3Preffix(Some("8f3ac260-2d50-4a84-8e9b-eae8a6b79b7d"), None) shouldBe Some("8f3ac260-2d50-4a84-8e9b-eae8a6b79b7d")
  }
  "PartitioningUtils" should "build without S3 prefix" in {
    buildS3Preffix(None, Some("date=2019-01-01")) shouldBe Some("date=2019-01-01")
  }
  "PartitioningUtils" should "build without partitioning value" in {
    buildS3Preffix(Some("s3/prefix"), None) shouldBe Some("s3/prefix")
  }
  "PartitioningUtils" should "fail on invalid prefix" in {
    an [InvalidPreffixException] should be thrownBy {
      buildS3Preffix(Some(""), Some("date=2019-01-01"))
    }
  }

}
