#!/bin/bash

docker run -p 22222:22222 -p 22223:22223 -p 22224:22224 molamolaxxx/http-proxy-encryption &

docker run -p 20432:20432 -p 20434:20434 -p 20435:20435  molamolaxxx/http-proxy-forward &

docker run  molamolaxxx/http-proxy-reverse &