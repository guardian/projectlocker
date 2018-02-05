#!/usr/bin/env bash

#This script should be run as the postgres user
createuser projectlocker
createuser projectlocker_test

createdb projectlocker -O projectlocker