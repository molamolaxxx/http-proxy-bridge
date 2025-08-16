package com.mola.proxy.bridge.core.entity;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2025-08-16 20:05
 **/
public class EncryptionAuthPacket {

    public static final String OUT_OF_LIMIT = "OUT_OF_LIMIT";

    private transient int alreadyReadLength = 0;

    private String decodeErrorMessage;

    private String authKey;

    private String encryptionServerId;

    public EncryptionAuthPacket(String authKey, String encryptionServerId) {
        this.authKey = authKey;
        this.encryptionServerId = encryptionServerId;
    }

    public EncryptionAuthPacket(){}

    public String getDecodeErrorMessage() {
        return decodeErrorMessage;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getEncryptionServerId() {
        return encryptionServerId;
    }

    public void setEncryptionServerId(String encryptionServerId) {
        this.encryptionServerId = encryptionServerId;
    }

    public static EncryptionAuthPacket readFrom(ByteBuf in) {
        EncryptionAuthPacket result = new EncryptionAuthPacket();
        try {
            // auth length
            int packLength = in.readInt();
            if (packLength > 256) {
                result.decodeErrorMessage = OUT_OF_LIMIT;
                return result;
            }
            // body1: authKey
            readPart(in, result::setAuthKey, result, packLength);

            // body2: serverId
            readPart(in, result::setEncryptionServerId, result, packLength);

        } catch (Exception e) {
            result.decodeErrorMessage = e.getMessage();
        }
        return result;
    }

    private static void readPart(ByteBuf byteBuf, Consumer<String> setter, EncryptionAuthPacket result, int packLength) {
        if (result.decodeErrorMessage != null) {
            return;
        }
        if (result.alreadyReadLength == packLength) {
            return;
        }
        int partLength = byteBuf.readInt();
        result.alreadyReadLength += 4;
        if (partLength > 128) {
            result.decodeErrorMessage = OUT_OF_LIMIT;
            return;
        }
        byte[] partInfoArr = new byte[partLength];
        byteBuf.readBytes(partInfoArr);
        result.alreadyReadLength += partInfoArr.length;
        setter.accept(new String(partInfoArr, StandardCharsets.UTF_8));
    }

    public byte[] buildBody() {
        return buildAuthInfo(
                authKey.getBytes(StandardCharsets.UTF_8),
                encryptionServerId.getBytes(StandardCharsets.UTF_8)
        );
    }


    private static byte[] buildAuthInfo(byte[]... infoArr) {
        int totalLength = 0;
        for (byte[] bytes : infoArr) {
            totalLength += bytes.length + 4;
        }
        byte[] result = new byte[totalLength];
        int writeIdx = 0;
        for (byte[] bytes : infoArr) {
            int length = bytes.length;
            result[writeIdx++] = (byte) (length >> 24);   // 最高位字节
            result[writeIdx++] = (byte) (length >> 16);
            result[writeIdx++] = (byte) (length >> 8);
            result[writeIdx++] = (byte) (length);
            for (byte each : bytes) {
                result[writeIdx++] = each;
            }
        }
        return result;
    }
}
