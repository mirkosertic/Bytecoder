#!/bin/bash
export JAVA_HOME=$HOME/$1
~/bin/install-jdk.sh --install $1 --target $JAVA_HOME --workspace "/home/travis/.cache/install-jdk"
export PATH=$JAVA_HOME/bin:$PATH