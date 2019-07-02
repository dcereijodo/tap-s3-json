#!/usr/bin/env bats

load test_helper

@test "error is printed when no profile is provided" {
  skip "This is currently not passing when triggered from Circle CI"
  run $TAP_START_SCRIPT --profile "DOES_NOT_EXIST"
  [ "$status" -eq 1 ]
  echo $output | grep "Unable to load AWS credentials from any provider in the chain"
}

@test "error is printed when the user does not have access" {
  run $TAP_START_SCRIPT --profile ${BATS_AWS_PROFILE}
  [ "$status" -eq 1 ]
  echo $output | grep "Access Denied"
}

@test "error is printed when bucket does not exist" {
  run $TAP_START_SCRIPT --profile ${BATS_AWS_PROFILE} --tap.bucket_name "this-bucket-should-not-really-exist"
  [ "$status" -eq 1 ]
  echo $output | grep "The specified bucket does not exist"
}

@test "records (20) are extracted when valid bucket is tapped" {
  run \
    $TAP_START_SCRIPT \
      --profile ${BATS_AWS_PROFILE} \
      --region us-east-1 \
      --tap.bucket_name ryft-public-sample-data \
      --tap.prefix esRedditJson/
  [ "$status" -eq 0 ]
  [ "${#lines[@]}" -eq 20 ]
}

@test "extend tap parallelism" {
  run \
  $TAP_START_SCRIPT \
    --profile ${BATS_AWS_PROFILE} \
    --region us-east-1 \
    --tap.worker_count 128 \
    --akka.http.host-connection-pool.max-open-requests 128 \
    --tap.bucket_name ryft-public-sample-data \
    --tap.prefix esRedditJson/
  [ "$status" -eq 0 ]
  [ "${#lines[@]}" -eq 20 ]
}

@test "keys are filtered using a regular expression" {
  run \
    $TAP_START_SCRIPT \
    --profile ${BATS_AWS_PROFILE} \
    --region us-east-1 \
    --tap.bucket_name ryft-public-sample-data \
    --tap.prefix esRedditJson \
    --filter '.*esRedditJson6[12]' \
    --unbounded
  [ "$status" -eq 0 ]
  [ "${#lines[@]}" -eq 72000 ]
}
