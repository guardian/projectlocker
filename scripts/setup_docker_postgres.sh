#!/usr/bin/env bash

docker run --rm -p 5432:5432 -v ${PWD}/docker-init:/docker-entrypoint-initdb.d postgres:9.6