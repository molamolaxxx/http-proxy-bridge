FROM openjdk:17

COPY ./build/http-proxy-encryption.jar /app/http-proxy-encryption.jar

COPY ./build/http-proxy-bridge.yml /app/http-proxy-bridge.yml

COPY ./build/client.cer /app/client.cer
COPY ./build/client_ks.jks /app/client_ks.jks
COPY ./build/clientTrust_ks.jks /app/clientTrust_ks.jks

COPY ./build/server.cer /app/server.cer
COPY ./build/server_ks.jks /app/server_ks.jks
COPY ./build/serverTrust_ks.jks /app/serverTrust_ks.jks

# 设置工作目录
WORKDIR /app

# 运行Java包
CMD ["java", "-jar","./http-proxy-encryption.jar"]