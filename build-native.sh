#!/bin/bash
sh build.sh
cd ./build
echo "start build native image"
# use version : graalvm-jdk-17.0.11+7.1
native-image \
  --no-fallback \
  -march=compatibility \
  -jar ./http-proxy-reverse.jar http-proxy-reverse

native-image \
  --no-fallback \
  -march=compatibility \
  -jar ./http-proxy-forward.jar http-proxy-forward

# use version : graalvm-ee-java17-21.3.10
native-image \
  --no-fallback \
  --gc=G1 \
  --allow-incomplete-classpath \
  --initialize-at-build-time=sun.instrument.InstrumentationImpl \
  -jar ./http-proxy-reverse.jar http-proxy-reverse

native-image \
  --no-fallback \
  --gc=G1 \
  --allow-incomplete-classpath \
  --initialize-at-build-time=sun.instrument.InstrumentationImpl \
  -jar ./http-proxy-forward.jar http-proxy-forward