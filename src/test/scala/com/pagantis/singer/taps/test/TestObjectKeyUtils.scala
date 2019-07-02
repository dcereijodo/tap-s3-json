package com.pagantis.singer.taps.test

import com.pagantis.singer.taps.ObjectKeyUtils
import com.pagantis.singer.taps.exceptions.InvalidPrefixException
import org.scalatest._

class TestObjectKeyUtils extends ObjectKeyUtils with Matchers with WordSpecLike {
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
    "match a key by regex" in {
      filterKey("group/action/date=2001", ".*date=2001") shouldBe true
      filterKey("group/action/date=2002", ".*date=2001") shouldBe false
      filterKey("group/other-action/date=2002", "group/.*/date=2002") shouldBe true
      filterKey("other-group/other-action/date=2002", "group/.*/date=2002") shouldBe false
    }
    "filter by date two levels deep" in {
      List(
        "group/some-action/date=2001/object1.json",
        "group/some-action/date=2002/object2.json",
        "group/an-action/date=2001/object3.json",
        "group/an-action/date=2002/object4.json",
      ).filter(filterKey(_,"group/.*/date=2001/.*")) shouldBe List(
        "group/some-action/date=2001/object1.json",
        "group/an-action/date=2001/object3.json"
      )
    }
  }
}
