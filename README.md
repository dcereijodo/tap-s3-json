# TapS3Json
A [Singer.io](https://github.com/singer-io/getting-started) tap for extracting data from Json files on S3. The tap has
two modes, `raw` and `jsonpaths`. In the `raw` mode, given an S3 bucket and optionally an S3 prefix, it will output the
contents of the bucket to a singer stream called `raw`. If a set of `jsonpath`s is provided in the configuration,
the tap will output the first match for each key if any to an singer stream called `jsonpath-matches`.

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

### Raw mode
If no `paths` key is provided in the configuration the tap will run in `raw` mode
```hocon
# application.conf
tap {
  bucket_name = "bucket-with-jsons"
  s3_preffix = "sub-prefix" // optional
}
```
In `raw` mode the complete JSON is extracted in the record
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
Yo can find examples and descriptions for all configurations supported by `tap-s3-json` in the [sample configuration file](src/main/resources/application.conf).
All this properties can be overridden when using the tap from the command line by providing appropriate arguments. Check the `tap-s3-json help` for more information.

The util uses `logback` for logging. The default logging configuration can be found in the [`logback.xml` file](src/main/resources/logback.xml).

## Build and Run
This is an [SBT](https://www.scala-sbt.org/) project. If you don't have sbt installed, do so by running `brew install sbt`
on Mac. Then you can compile and package the project with
```bash
sbt package && sbt assembly
```
And next run the tap like
```bash
sbt run
```
or if you prefer using bare java
```bash
java -jar target/scala-2.12/tap-s3-json-assembly-0.1-SNAPSHOT.jar [-Dconfig.file=application.conf]
```

## CLI
In the `cli` folder a `stub.sh` wrapps the call to the tap with a set of defaults and options aliases so a friendly interface can be exposed to the user in the form of a CLI util. If you want to install this wrapped version of the tap as command on your system (tried on Mac)
```bash
sbt package && sbt assembly && sbt make && sbt install
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

`cli.bats` defines a few [bats tests](https://github.com/sstephenson/bats) that check some CLI basic functionallity after installation.

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
