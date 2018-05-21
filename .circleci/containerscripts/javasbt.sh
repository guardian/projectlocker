#!/bin/bash -e

yum -y clean all
yum -y install java-1.8.0-openjdk-devel rpm-build postgresql
curl -L https://dl.bintray.com/sbt/rpm/sbt-0.13.15.rpm > /tmp/sbt-0.13.15.rpm
rpm -Uvh /tmp/sbt-0.13.15.rpm

yum -y clean all
rm -rf /var/cache/yum/*
echo exit | sbt
