#!/bin/bash
# https://coderwall.com/p/ssuaxa/how-to-make-a-jar-file-linux-executable
MYSELF=`which "$0" 2>/dev/null`
[ $? -gt 0 -a -f "$0" ] && MYSELF="./$0"
java=java
if test -n "$JAVA_HOME"; then
    java="$JAVA_HOME/bin/java"
fi

show_help() {

    cat << HELP

    Tap JSON contents of an S3 bucket to the standard output

    Optional arguments:
        -h,--help, help: Will display this
        -v, -d, --debug: Sets the logging level to DEBUG
        --config: Provide a HOCON or JSON configuration file
        --unbounded: Sets 'tap.limit' to 0 (no limit)
        --date: Specifies a date to be tapped. When specified, mode will be 'unbounded'.
        --profile: AWS profile. Overrides environment variable AWS_PROFILE

    All other arguments provided will be passed to the application as a JVM parameter.
    For example --some.key myval will translate to JVM parameter -Dsome.key=myval.

HELP

}

# Set defaults for environment variables
AWS_REGION=${AWS_REGION:=eu-west-1}
AWS_PROFILE=${AWS_PROFILE:=default}
LOG_LEVEL=${LOG_LEVEL:=ERROR}

# ------------------------
# Interpret user arguments
# ------------------------

# By default we want to set the tap to ignore headers, so we add them to the argument list
parsed=(
    "-Dtap.ignore_headers=true" '-Dtap.json=""'
    '-Dtap.bucket_name=pmt-events-datalake-storage-prod'
    '-Dtap.prefix=PMT_ORDER/ORDER_CREATED'
    '-Dtap.limit=20'
)
# and then we loop through the user arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|-\?|--help|help)
            show_help
            exit 0
            ;;
        -d|--debug|-v)
            LOG_LEVEL=DEBUG
            shift
            ;;
        --config)
            parsed+=("-Dconfig.file=$2")
            shift 2
            ;;
        --unbounded)
            parsed+=("-Dtap.limit=0")
            shift
            ;;
        --profile)
            AWS_PROFILE=$2
            shift 2
            ;;
        --date)
            parsed+=("-Dtap.partitioning.value=$2")
            parsed+=("-Dtap.limit=0") # when a --date parameter is provided assume 'unbounded' mode
            shift 2
            ;;
        *)
            option=${1##*-}; value=$2
            parsed+=("-D$option=$value")
            shift 2
            ;;
    esac
done
parsed+=("-jar")
parsed+=($MYSELF)

echo "[tap-s3] profile -> $AWS_PROFILE - region -> $AWS_REGION - loglevel -> $LOG_LEVEL" 1>&2

export AWS_PROFILE=$AWS_PROFILE
export AWS_REGION=$AWS_REGION
export LOG_LEVEL=$LOG_LEVEL

java ${parsed[*]}

exit
