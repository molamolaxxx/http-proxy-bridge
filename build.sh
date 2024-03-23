#!/bin/bash

echo "start build http-proxy-bridge"

echo "1.start create target directory"
directory="build"
if [ ! -d "$directory" ]; then
  mkdir -p "$directory"
  echo "create $directory success"
else
  echo "$directory already exist"
fi

echo "2.start build maven"
mvn clean package

echo "3.start copy resource"
cp ./http-proxy-bridge-forward/target/http-proxy-bridge-forward-1.0.1-beta-jar-with-dependencies.jar ./$directory/http-proxy-forward.jar;
cp ./http-proxy-bridge-reverse/target/http-proxy-bridge-reverse-1.0.1-beta-jar-with-dependencies.jar ./$directory/http-proxy-reverse.jar;
cp ./http-proxy-bridge-encryption/target/http-proxy-bridge-encryption-1.0.1-beta-jar-with-dependencies.jar ./$directory/http-proxy-encryption.jar;

content="java -jar ./http-proxy-forward.jar &"
echo "$content" > $directory/start_forward.sh
chmod +x $directory/start_forward.sh

content="java -jar ./http-proxy-reverse.jar &"
echo "$content" > $directory/start_reverse.sh
chmod +x $directory/start_reverse.sh

content="java -jar ./http-proxy-encryption.jar &"
echo "$content" > $directory/start_encryption.sh
chmod +x $directory/start_encryption.sh

cp http-proxy-bridge.yml $directory/http-proxy-bridge.yml

cp client.cer $directory/client.cer
cp client_ks.jks $directory/client_ks.jks
cp clientTrust_ks.jks $directory/clientTrust_ks.jks

cp server.cer $directory/server.cer
cp server_ks.jks $directory/server_ks.jks
cp serverTrust_ks.jks $directory/serverTrust_ks.jks

cp shutdown_all.sh $directory/shutdown_all.sh