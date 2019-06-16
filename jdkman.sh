#!/bin/bash
export JAVA_HOME=~/$1
~/install-jdk.sh -W /home/travis/$1 -F $2 -L GPL
export PATH=$JAVA_HOME/bin:$PATH