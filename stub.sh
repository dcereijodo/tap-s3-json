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
        -v: Sets the logging level to DEBUG
        --config: Provide a HOCON or JSON configuration file

    All other arguments provided will be passed to the application as a JVM parameter.
    For example --some.key myval will translate to JVM parameter -Dsome.key=myval.

HELP

}

# Set defaults for environment variables
: ${AWS_REGION=eu-west-1}
: ${AWS_PROFILE=default}
: ${LOG_LEVEL=ERROR}

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
        -v)
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
        *)
            option=${1##*-}; value=$2
            parsed+=("-D$option=$value")
            shift 2
            ;;
    esac
done
parsed+=("-jar")
parsed+=($MYSELF)

java ${parsed[*]}
exit
