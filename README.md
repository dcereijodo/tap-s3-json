# TapS3Json
A [Singer.io](https://github.com/singer-io/getting-started) tap for extracting data from Json files on S3. The tap has
two modes, `jsonpaths` and `raw`. In the `raw` mode, given an S3 bucket and optionally an S3 prefix, it will output the
contents of the bucket to a singer stream called `raw`. If a set of `jsonpath`s is provided in the configuration,
the tap will output the first match for each key if any to an singer stream called `jsonpath-matches`.

# JsonPaths mode example
Given an S3 bucket `bucket-with-jsons` with two S3 objects
```json
# pseudo-folder/object1.json
{
  "action": "LOAN_CLOSED",
  "payload": {
    "loan_id": "20bcfd8e-7d08-4554-a31d-633d9fc7a760",
    "closed_at": "2019-04-25T14:44:21.000000Z"
  }
}

# pseudo-folder/object2.json
{
  "action": "LOAN_CLOSED",
  "payload": {
    "loan_id": "9044cfdd68e-7d08-4554-1d-633d9wsn302",
    "closed_at": "2018-04-25T04:24:22.000000Z"
  }
}
```
We can tap onto this bucket with the following configuration in `application.conf`
```hocon
tap {
  bucket_name = "bucket-with-jsons"
  s3_preffix = "pseudo-folder" // optional
  json {
    paths = [
      "$.action",
      "$.payload.loan_id"
    ]
  }
}
```
and we will get
```bash
tap-s3-json --config application.conf
# will output two lines
# { "type": "RECORD", stream: "jsonpath-matches", "record": {"$.action": "LOAN_CLOSED", "$.payload.loan_id": "20bcfd8e-7d08-4554-a31d-633d9fc7a760"} }
# { "type": "RECORD", stream: "jsonpath-matches", "record": {"$.action": "LOAN_CLOSED", "$.payload.loan_id": "9044cfdd68e-7d08-4554-1d-633d9wsn302"} }
```

# Raw mode example
If no `paths` key is provided in the configuration, the tap will run in `raw` mode
```bash
tap-s3-json --config application.conf
# will output two lines
# { "type": "RECORD", stream: "raw", "record": {"action": "LOAN_CLOSED", "payload": {"loan_id": "9044cfdd68e-7d08-4554-1d-633d9wsn302", "closed_at": "2018-04-25T04:24:22.000000Z" }}}
# { "type": "RECORD", stream: "raw", "record": {"action": "LOAN_CLOSED", "payload": {"loan_id": "9044cfdd68e-7d08-4554-1d-633d9wsn302", "closed_at": "2018-04-25T04:24:22.000000Z" }}}
```

# Build and Run
This is an [SBT](https://www.scala-sbt.org/) project. If you don't have sbt installed, do so by running `brew install sbt`
on Mac. Then you can compile and package the project with
```bash
sbt package && sbt assembly
```
And next run the tap like
```bash
java -jar target/scala-2.12/tap-s3-json-assembly-0.1-SNAPSHOT.jar -Dconfig.file=application.conf
```

# Testing
Integration tests are based on [minio](https://github.com/minio/minio) public object store service.

# Date-based tapping
One can tap using an environment variable `PARTITION_VALUE` provided the partitioning configuration is defined
in the `application.conf`
```hocon
tap {

  # some other configs
  s3_perefix = "my-table"
  
  partitioning {
    key = "date"
    value = ${?PARTITION_VALUE}
  }
}
```
will output all the records in the `my-table\date=PARTITION_VALUE` S3 prefix.