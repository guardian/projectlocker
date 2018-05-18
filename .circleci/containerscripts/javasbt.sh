#!/bin/bash -e

yum -y install java-1.8.0-openjdk-devel
curl -L https://dl.bintray.com/sbt/rpm/sbt-0.13.15.rpm > /tmp/sbt-0.13.15.rpm
rpm -Uvh /tmp/sbt-0.13.15.rpm

echo exit | sbt
#mv /root/.ivy2 /home/circleci/.ivy2
#chown -R circleci /home/circleci/.ivy2