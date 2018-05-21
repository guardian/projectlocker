#!/bin/bash

sha256sum "$1" > "$1".sha
aws s3 cp "$1" s3://${DEPLOY_BUCKET}/projectlocker/${CIRCLE_BUILD_NUM}/`basename $1`
aws s3 cp "$1".sha s3://${DEPLOY_BUCKET}/projectlocker/${CIRCLE_BUILD_NUM}/`basename $1`.sha