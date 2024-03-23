#!/bin/bash
cp -r ../build .
docker build -t molamolaxxx/http-proxy-encryption -f http-proxy-encryption.Dockerfile .
docker build -t molamolaxxx/http-proxy-forward -f http-proxy-forward.Dockerfile .
docker build -t molamolaxxx/http-proxy-reverse -f http-proxy-reverse.Dockerfile .
rm -rf ./build