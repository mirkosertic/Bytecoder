#!/bin/bash

openssl aes-256-cbc -K $encrypted_41bc8c83cf90_key -iv $encrypted_41bc8c83cf90_iv -in .travis/gpg.all.enc -out ./all.gpg -d
gpg --import ./all.gpg

if [ ! -z "$TRAVIS_TAG" ]
then
    echo "on a tag -> set pom.xml <version> to $TRAVIS_TAG"
    mvn --settings .mvn/settings.xml org.codehaus.mojo:versions-maven-plugin:set -DnewVersion=$TRAVIS_TAG
else
    echo "not on a tag -> keep snapshot version in pom.xml"
fi

mvn --settings .mvn/settings.xml clean javadoc:jar deploy -P notest -P signed
