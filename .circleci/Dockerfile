FROM centos:7.4.1708

COPY ./containerscripts/postgres.sh /tmp
RUN bash /tmp/postgres.sh

COPY ./containerscripts/javasbt.sh /tmp
RUN bash /tmp/javasbt.sh

COPY ./containerscripts/buildnode.sh /tmp
RUN bash /tmp/buildnode.sh
RUN cp /root/.bashrc /home/circleci

COPY ./containerscripts/jython.sh /tmp
RUN bash /tmp/jython.sh
