#!/bin/bash
sh build.sh
cd ./build
# use version : graalvm-jdk-17.0.11+7.1
native-image --no-fallback -march=compatibility -jar ./http-proxy-reverse.jar http-proxy-reverse