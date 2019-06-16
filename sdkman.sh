#!/bin/bash
export JAVA_HOME=$HOME/$1
$TRAVIS_BUILD_DIR/install-jdk.sh --install $1 --target $JAVA_HOME
export PATH=$JAVA_HOME/bin:$PATH