setup() {
  # set an AWS profile for testing
  BATS_AWS_PROFILE=${BATS_AWS_PROFILE:-default}
}
teardown() {
  unset BATS_AWS_PROFILE
}
