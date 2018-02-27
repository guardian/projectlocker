#!/usr/bin/env bash

FAILED=0

for checkfile in `ls conf/evolutions/default/*.sql`; do
    diff ${checkfile} conf/evolutions/test/`basename ${checkfile}`
    if [ "$?" -ne "0" ]; then
        echo $checkfile is not in sync with conf/evolutions/test/`basename ${checkfile}` - this means that test results can\'t be
        echo relied on.
        FAILED=1
    fi
done

if [ "$FAILED" -ne "0" ]; then
    exit 1
else
    exit 0
fi