# This is a Dockerfile for testing the Ontologizer .deb file

FROM debian:jessie

ARG apt_proxy=""
RUN if [ -n "$apt_proxy" ]; then echo 'Acquire::http { Proxy "'$apt_proxy'"; }' >>/etc/apt/apt.conf; fi #

RUN echo "deb http://httpredir.debian.org/debian jessie-backports main contrib non-free" >>/etc/apt/sources.list

RUN apt-get update
RUN apt-get dist-upgrade -y
RUN apt-get install -t jessie-backports -y --no-install-recommends \
	ca-certificates-java \
	openjdk-8-jre-headless

RUN mkdir /target
COPY target/*.deb /target/
WORKDIR /target
