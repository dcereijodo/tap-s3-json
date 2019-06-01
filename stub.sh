#!/bin/bash
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
        --config: Provide a HOCON or JSON configuration file

    All other arguments provided will be passed to the application as a JVM parameter.
    For example --some.key myval will translate to JVM parameter -Dsome.key=myval.

HELP

}

# Set defaults for environment variables
: ${AWS_REGION=eu-west-1}
: ${AWS_PROFILE=default}
if [ $# -eq 0 ]; then
    exec "$java" -jar $MYSELF
    exit 1
else
    # Parse options
    parsed=()
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|-\?|--help|help)
                show_help
                exit 0
                ;;
            -v)
                verbose=1
                shift
                ;;
            --config)
                parsed+=("-Dconfig.file=$2")
                shift 2
                ;;
            *)
                option=$1; value=$2
                parsed+=("-D$option=$value")
                shift 2
                ;;
        esac
    done

    exec "$java" "${parsed[*]}" -jar $MYSELF
    exit
fi
