FROM centos:7.4.1708


#ned to install shasum
COPY ./containerscripts/postgres.sh /tmp
RUN bash /tmp/postgres.sh

COPY ./containerscripts/javasbt.sh /tmp
RUN bash /tmp/javasbt.sh

COPY ./containerscripts/buildnode.sh /tmp
RUN bash /tmp/buildnode.sh
RUN cp /root/.bashrc /home/circleci

RUN yum -y install git
COPY ./containerscripts/jython.sh /tmp
RUN bash /tmp/jython.sh
