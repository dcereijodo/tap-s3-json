# TapS3Json [![CircleCI](https://circleci.com/gh/dcereijodo/tap-s3-json.svg?style=svg)](https://circleci.com/gh/dcereijodo/tap-s3-json)
A [Singer.io](https://github.com/singer-io/getting-started) tap for extracting data from Json files on S3. Given an S3 bucket and optionally an S3 prefix, it will output the contents of the bucket to a singer stream called `raw`.

## Example
Given an S3 bucket `bucket-with-jsons` with two S3 objects

`sub-prefix/object1.json`
```json
  {
    "_id": "5cfcc6725ed2ba0bf86a3715",
    "isActive": false,
    "tags": ["proident","officia","aliquip"],
    "customerInfo": {
      "id": 0,
      "company": "DIGIFAD"
    }
  }
```
and `sub-prefix/object2.json`
```json
  {
    "_id": "5cfcc672d3f4e41fdce82464",
    "isActive": true,
    "tags": ["culpa","magna","consectetur"],
    "customerInfo": {
      "id": 1,
      "company": "PETIGEMS"
    }
  }
```

Provided a configuration file (HOCON or JSON) such as below
```hocon
# application.conf
tap {
  bucket_name = "bucket-with-jsons"
  s3_preffix = "sub-prefix" // optional
}
```
The complete JSON is extracted in the record
```bash
tap-s3-json --config application.conf
# will output two lines
# { "type": "RECORD", stream: "raw", "record": {"_id": "5cfcc6725ed2ba0bf86a3715", "isActive": false, "tags": ["proident","officia","aliquip"], "customerInfo": {"id": 0,"company": "DIGIFAD"}}}
# { "type": "RECORD", stream: "raw", "record": {"_id": "5cfcc672d3f4e41fdce82464", "isActive": true, "tags": ["culpa","magna","consectetur"], "customerInfo": {"id": 1,"company": "PETIGEMS"}}}
```

### ~~JsonPaths mode~~
This mode has been discontinued, as there are number of tools that can do a better job at munging the extracted
JSON data. [`jq`](https://stedolan.github.io/jq/) is one of such tools that supports a full featured query language for
streamline JSON data.
```bash
tap-s3-json --config application.conf | jq '{type: .type, stream: "jsonpath-matches", record: {"$._id": .["_id"], "$.tags[0]": .["tags[0]"], "$.customerInfo.company": .["customerInfo.company"]}}'
# will output two lines
# { "type": "RECORD", stream: "jsonpath-matches", "record": {"$._id": "5cfcc6725ed2ba0bf86a3715", "$.tags[0]": "proident", "$.customerInfo.company": "DIGIFAD"} }
# { "type": "RECORD", stream: "jsonpath-matches", "record": {"$._id": "5cfcc6725ed2ba0bf86a3715", "$.tags[0]": "culpa", "$.customerInfo.company": "PETIGEMS"} }
```

## Configuration
You can find examples and descriptions for all configurations supported by `tap-s3-json` in the [sample configuration file](src/main/resources/application.conf).
All this properties can be overridden when using the tap from the command line by providing appropriate arguments. Check the `tap-s3-json help` for more information.

The util uses `logback` for logging. The default logging configuration can be found in the [`logback.xml` file](src/main/resources/logback.xml).

## Build and Run
This is an [SBT](https://www.scala-sbt.org/) project. If you don't have sbt installed, do so by running `brew install sbt`
on Mac. Then you can compile and package the project with
```bash
sbt ";compile;universal:packageBin"
```
And run it with
```bash
sbt run
```

## CLI
The `universal:packageBin` sbt target from the previous section will generate zip file in `target/universal` with a packaged version of the tap. This wrapps the call to the tap with a set of defaults and options aliases so a friendly interface can be exposed to the user in the form of a CLI util.
For running the packaged tap (Java 8 required)
```bash
unzip target/universal/tap-s3-json-0.1-SNAPSHOT.zip
tap-s3-json-0.1-SNAPSHOT/bin/tap-s3-json help
```
The command uses defaults
* Logback `LOG_LEVEL` :arrow_right: `ERROR`
* Tap `ignore_headers` :arrow_right: `true`
* Tap `bucket_name` :arrow_right: `pmt-events-datalake-storage-prod`
* Tap `prefix` :arrow_right: `PMT_ORDER/ORDER_CREATED`
* Tap `limit` :arrow_right: `20`
* Tap `frame_length` :arrow_right: `262144` (enough to support the maximum SNS message size)
* Tap `worker_count` :arrow_right: `4`
* HTTP `max-connections` :arrow_right: `32`
* HTTP `max-open-requests` :arrow_right: `16`

Though all these can be overwritten using appropriate arguments to `tap-s3-json`. For more information check the
help with `tap-s3-json help`.

`bats/cli.bats` defines a few [bats tests](https://github.com/sstephenson/bats) that check some CLI basic functionallity after installation.

## Date-based tapping
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
will output all the records in the `my-table\date=PARTITION_VALUE` S3 prefix. This value can also be set in the command
line with the `--date` option.

## Integration Tests
Integration tests are based on [minio](https://github.com/minio/minio) public object store service. They are skipped by
default during build. If you want to run them, so can do so with `sbt it:test`.
