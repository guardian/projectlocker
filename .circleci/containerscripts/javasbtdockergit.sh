#!/bin/bash -e

yum -y clean all
yum -y install java-1.8.0-openjdk-devel rpm-build postgresql git docker
curl -L https://dl.bintray.com/sbt/rpm/sbt-1.1.4.rpm > /tmp/sbt-1.1.4.rpm
rpm -Uvh /tmp/sbt-1.1.4.rpm

yum -y clean all
rm -rf /var/cache/yum/*
echo exit | sbt
