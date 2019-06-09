#!/usr/bin/env bats

@test "error is printed when no profile is provided" {
  run tap-s3-json --profile ""
  [ "$status" -eq 1 ]
  echo $output | grep "Unable to load AWS credentials from any provider in the chain"
}

@test "error is printed when bucket does not exist" {
  run tap-s3-json --profile ${VALID_PROFILE} --tap.bucket_name "this-bucket-should-not-really-exist"
  [ "$status" -eq 1 ]
  echo $output | grep "The specified bucket does not exist"
}

@test "records (20) are extracted when valid bucket is tapped" {
  run \
    tap-s3-json \
      --profile ${VALID_PROFILE} \
      --region us-east-1 \
      --tap.bucket_name ryft-public-sample-data \
      --tap.prefix esRedditJson/
  [ "$status" -eq 0 ]
  [ "${#lines[@]}" -eq 21 ]
}
