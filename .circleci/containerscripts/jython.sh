#!/usr/bin/env bash -e

curl -L http://search.maven.org/remotecontent?filepath=org/python/jython-installer/2.7.0/jython-installer-2.7.0.jar > /tmp/jython-installer-2.7.0.jar
java -jar /tmp/jython-installer-2.7.0.jar -s -d ~/jython -t standard -e doc
rm -f /tmp/jython-installer-2.7.0.jar