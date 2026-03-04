package com.senet.fogsimulator.network.message;

import java.security.PublicKey;

public class EncryptedMessage {
    private final byte[] encryptedData;
    private final PublicKey senderKey;

    // 构造函数
    public EncryptedMessage(byte[] encryptedData, PublicKey senderKey) {
        this.encryptedData = encryptedData;
        this.senderKey = senderKey; // 初始化 senderKey
    }

    // Getter 方法
    public byte[] getEncryptedData() {
        return encryptedData;
    }

    public PublicKey getSenderKey() {
        return senderKey;
    }
}