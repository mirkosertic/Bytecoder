#!/usr/bin/env bash
test "$1" == "openjdk11" && (~/install-jdk.sh -W /home/travis/openjdk11 -F 11 -L GPL;)
test "$1" == "openjdk12" && (~/install-jdk.sh -W /home/travis/openjdk12 -F 12 -L GPL;)