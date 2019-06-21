#!/usr/bin/env bats

load test_helper

@test "error is printed when no profile is provided" {
  run tap-s3-json --profile "DOES_NOT_EXIST"
  [ "$status" -eq 1 ]
  echo $output | grep "Unable to load AWS credentials from any provider in the chain"
}

@test "error is printed when the user does not have access" {
  run tap-s3-json
  [ "$status" -eq 1 ]
  echo $output | grep "Access Denied"
}

@test "error is printed when bucket does not exist" {
  run tap-s3-json --profile ${BATS_AWS_PROFILE} --tap.bucket_name "this-bucket-should-not-really-exist"
  [ "$status" -eq 1 ]
  echo $output | grep "The specified bucket does not exist"
}

@test "records (20) are extracted when valid bucket is tapped" {
  run \
    tap-s3-json \
      --profile ${BATS_AWS_PROFILE} \
      --region us-east-1 \
      --tap.bucket_name ryft-public-sample-data \
      --tap.prefix esRedditJson/
  [ "$status" -eq 0 ]
  [ "${#lines[@]}" -eq 21 ]
}
