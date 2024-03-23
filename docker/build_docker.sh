#!/bin/bash
cp -r ../build .
docker build -t http-proxy-encryption -f http-proxy-encryption.Dockerfile .
docker build -t http-proxy-forward -f http-proxy-forward.Dockerfile .
docker build -t http-proxy-reverse -f http-proxy-reverse.Dockerfile .
rm -rf ./build