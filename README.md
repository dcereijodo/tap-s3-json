# TapS3Json
A [Singer.io](https://github.com/singer-io/getting-started) tap for extracting flattened data from Json files on S3.
Given a set of `jsonpath`s, an S3 bucket and optionally an S3 prefix, outputs to `stdout` singer records with the first
match for each key if any.

# Example
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
  json_paths = [
    "$.action",
    "$.payload.loan_id"
  ]
}
```
and we will get
```bash
tap-s3-json --config application.conf
# will output two lines
# { "type": "RECORD",  "record": {"$.action": "LOAN_CLOSED", "$.payload.loan_id": "20bcfd8e-7d08-4554-a31d-633d9fc7a760"} }
# { "type": "RECORD",  "record": {"$.action": "LOAN_CLOSED", "$.payload.loan_id": "9044cfdd68e-7d08-4554-1d-633d9wsn302"} }
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
One can tap using an environment variable `EXECUTION_DATE` provided the partitioning configuration is defined
in the `application.conf`
```hocon
tap {

  # some other configs
  s3_perefix = "my-table"
  
  partitioning {
    key = "date"
  }
}
```
will output all the records in the `my-table\date=$EXECUTION_DATE` S3 prefix.