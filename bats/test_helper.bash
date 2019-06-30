setup() {
  # set an AWS profile for testing
  BATS_AWS_PROFILE=${BATS_AWS_PROFILE:-default}
  TAP_START_SCRIPT=${TAP_START_SCRIPT:-target/universal/stage/bin/tap-s3-json}
}
teardown() {
  unset BATS_AWS_PROFILE
}
