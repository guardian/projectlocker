FROM centos:7.4.1708

COPY ./containerscripts/javasbt.sh /tmp
RUN bash /tmp/javasbt.sh

COPY ./containerscripts/buildnode.sh /tmp
RUN bash /tmp/buildnode.sh
RUN cp /root/.bashrc /home/circleci

RUN yum -y install git
COPY ./containerscripts/jython.sh /tmp
RUN bash /tmp/jython.sh

COPY ./containerscripts/awstools.sh /tmp
RUN bash /tmp/awstools.sh