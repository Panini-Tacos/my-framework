#!/usr/bin/env bash
set -e

rm -rf test
mkdir -p test
trap 'rm -rf test' EXIT

javac -parameters -Xlint -cp "lib/*" -d test $(find src -name "*.java")
java -cp "lib/*:test" JSF.Main "$@"
