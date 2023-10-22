# 1、生成服务端密钥
keytool -genkey -keystore server_ks.jks -storepass 123456 -keyalg RSA -keypass 123456
# 2、生成服务端证书
keytool -export -keystore server_ks.jks -storepass 123456 -file server.cer
# 3、生成客户端密钥
keytool -genkey -keystore client_ks.jks -storepass 123456 -keyalg RSA -keypass 123456
# 4、生成客户端证书
keytool -export -keystore client_ks.jks -storepass 123456 -file client.cer
# 5、将server端证书添加到serverTrust_ks.jks文件中
keytool -import -keystore serverTrust_ks.jks -storepass 123456 -file server.cer
# 6、将client端证书添加到clientTrust_ks.jks文件中
keytool -import -keystore clientTrust_ks.jks -storepass 123456 -file client.cer