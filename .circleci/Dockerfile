FROM centos:7

COPY ./containerscripts/javasbtdockergit.sh /tmp
RUN bash /tmp/javasbtdockergit.sh

COPY ./containerscripts/buildnode.sh /tmp
RUN bash /tmp/buildnode.sh
RUN cp /root/.bashrc /home/circleci

COPY ./containerscripts/jython.sh /tmp
RUN bash /tmp/jython.sh

COPY ./containerscripts/awstools.sh /tmp
RUN bash /tmp/awstools.sh

COPY ./containerscripts/sbt-repositories /root/.sbt