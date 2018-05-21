#!/usr/bin/env bash

curl "https://bootstrap.pypa.io/get-pip.py" -o "/tmp/get-pip.py"
chmod a+x /tmp/get-pip.py
python /tmp/get-pip.py
rm -f /tmp/get-pip.py

pip install awscli
