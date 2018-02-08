#!/usr/bin/env bash

#This script should be run as the postgres user
createuser projectlocker
createuser projectlocker_test

psql << EOF
alter user projectlocker password 'projectlocker';
alter user projectlocker_test password 'projectlocker';
EOF

createdb projectlocker -O projectlocker
createdb projectlocker_test -O projectlocker