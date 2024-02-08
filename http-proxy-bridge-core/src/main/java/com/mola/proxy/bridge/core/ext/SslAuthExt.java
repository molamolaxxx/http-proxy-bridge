package com.mola.proxy.bridge.core.ext;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-10-22 18:36
 * 1、生成服务端密钥
 * keytool -genkey -keystore server_ks.jks -storepass server_password -keyalg RSA -keypass server_password
 * 2、生成服务端证书
 * keytool -export -keystore server_ks.jks -storepass server_password -file server.cer
 * 3、生成客户端密钥
 * keytool -genkey -keystore client_ks.jks -storepass client_password -keyalg RSA -keypass client_password
 * 4、生成客户端证书
 * keytool -export -keystore client_ks.jks -storepass client_password -file client.cer
 * 5、将server端证书添加到serverTrust_ks.jks文件中
 * keytool -import -keystore serverTrust_ks.jks -storepass client -file server.cer
 * 6、将client端证书添加到clientTrust_ks.jks文件中
 * keytool -import -keystore clientTrust_ks.jks -storepass server -file client.cer
 **/
public interface SslAuthExt {

    /**
     * 存放server_ks.jks/client_ks.jks的路径
     * @return
     */
    String keyStorePath();

    /**
     * server_ks.jks/client_ks.jks设置的密码
     * @return
     */
    String keyStorePassword();

    /**
     * 存放clientTrust_ks.jks/serverTrust_ks.jks的路径
     * @return
     */
    String trustStorePath();

    /**
     * clientTrust_ks.jks/serverTrust_ks.jks设置的密码
     * @return
     */
    String trustStorePassword();
}
