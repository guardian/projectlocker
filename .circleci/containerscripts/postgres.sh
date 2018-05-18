#!/usr/bin/env bash

yum -y clean all
rpm -Uvh https://download.postgresql.org/pub/repos/yum/9.2/redhat/rhel-7-x86_64/pgdg-centos92-9.2-3.noarch.rpm
yum -y install postgresql92-server sudo
echo export PATH=/usr/pgsql-9.2/bin:\$PATH >> /var/lib/pgsql/.bash_profile

sudo -u postgres /usr/pgsql-9.2/bin/initdb -D /var/lib/pgsql/9.2/data