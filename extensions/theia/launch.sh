#!/usr/bin/env sh

BASEDIR=$(cd "$(dirname "$0")" && pwd)
cd $BASEDIR/../..
./gradlew packageExtension
cd $BASEDIR
mkdir -p plugins
cp ../vscode/build/vscode/streamblocks-extension.vsix plugins/
yarn
yarn theia build
yarn theia start ../../workspace --hostname 127.0.0.1 --port 8080 --plugins=local-dir:plugins