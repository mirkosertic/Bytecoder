#!/bin/bash
export JAVA_HOME=~/$1
/home/travis/bin/install-jdk.sh --target "/home/travis/$1" --workspace "/home/travis/.cache/install-jdk" --feature "$2" --license "GPL" --cacerts
export PATH=$JAVA_HOME/bin:$PATH