#!/bin/bash
if [$1 eq 'openjdk11']; then
    echo "Installing OpenJDK 11"
    ~/install-jdk.sh -W /home/travis/openjdk11 -F 11 -L GPL
fi
if [$1 eq 'openjdk12']; then
    echo "Installing OpenJDK 12"
    ~/install-jdk.sh -W /home/travis/openjdk12 -F 12 -L GPL
fi