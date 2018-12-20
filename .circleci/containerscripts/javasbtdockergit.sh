#!/bin/bash -e

yum -y clean all
yum -y install java-1.8.0-openjdk-devel rpm-build postgresql git yum-utils
curl -L https://dl.bintray.com/sbt/rpm/sbt-1.1.4.rpm > /tmp/sbt-1.1.4.rpm
rpm -Uvh /tmp/sbt-1.1.4.rpm
rm -f /tmp/sbt-1.1.4.rpm

curl -L https://download.docker.com/linux/static/stable/x86_64/docker-18.06.1-ce.tgz > /tmp/docker-18.06.1-ce.tgz
tar xvzf /tmp/docker-18.06.1-ce.tgz
mv docker/docker /usr/local/bin
rm -rf docker

yum -y clean all
rm -rf /var/cache/yum/*
echo exit | sbt

if [ ! -d ~/.sbt ]; then mkdir ~/.sbt; fi