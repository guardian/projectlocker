#!/usr/bin/env bash

#from http://stackoverflow.com/questions/3915040/bash-fish-command-to-print-absolute-path-to-a-file
function abspath() {
    # generate absolute path from relative path
    # $1     : relative filename
    # return : absolute path
    if [ -d "$1" ]; then
        # dir
        (cd "$1"; pwd)
    elif [ -f "$1" ]; then
        # file
        if [[ $1 == */* ]]; then
            echo "$(cd "${1%/*}"; pwd)/${1##*/}"
        else
            echo "$(pwd)/$1"
        fi
    fi
}

BASEPATH=$(abspath "${BASH_SOURCE%/*}")

#Run the script to set up the test db as the local postgres user
if [ "${CIRCLECI}" != "" ]; then
    ${BASEPATH}/blank_test_db_actions.sh #if we're in circleci the running user already has what we need
else
    sudo su postgres -c "${BASEPATH}/blank_test_db_actions.sh" #otherwise run as the postgres user. this will probably prompt for password.
fi
