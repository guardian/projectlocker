#!/usr/bin/env bash

#This assumes that postgres utilities are on your PATH
#You should run this script via the main setup_test_db.sh which will su you to the postgres user
TEST_DB_NAME=projectlocker_test

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

CREATEDB=`which createdb`
PSQL=`which psql`
if [ "${CREATEDB}" == "" ] || [ ! -x ${CREATEDB} ]; then
    echo "Could not find the createdb command.  Running as user ${UID}, best I got was ${CREATEDB}."
    exit 1
fi

#START MAIN
CREATE_RESULT=`${CREATEDB} ${TEST_DB_NAME} 2>&1`
if [ "$?" != "0" ]; then
    echo ${CREATE_RESULT} | grep "already exists" >/dev/null 2>&1
    if [ "$?" != "0" ]; then
        echo Unable to create database ${TEST_DB_NAME} and it does not already exist.  Try running the createdb command manually.
        exit 1
    fi
fi

psql ${TEST_DB_NAME} << EOF
drop table "PlutoCommission" cascade;
drop table "PlutoWorkingGroup" cascade;
drop table "PostrunDependency" cascade;
drop table "PostrunAssociationRow" cascade;
drop table "PostrunAction" cascade;
drop table "ProjectFileAssociation" cascade;
drop table "FileEntry" cascade;
drop table "ProjectEntry" cascade;
drop table "ProjectTemplate" cascade;
drop table "ProjectType" cascade;
drop table "StorageEntry" cascade;
drop table "play_evolutions" cascade;
EOF
