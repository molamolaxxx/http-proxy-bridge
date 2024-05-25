#!/bin/bash
sh build.sh
cd ./build
native-image --no-fallback --allow-incomplete-classpath -jar ./http-proxy-reverse.jar http-proxy-reverse