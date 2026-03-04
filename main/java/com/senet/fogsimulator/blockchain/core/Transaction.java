package com.senet.fogsimulator.blockchain.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String sender;
    private final String receiver;
    private final double amount;
    private final long timestamp;
    private byte[] signature;
    private transient PublicKey senderPublicKey;
    private boolean isSigned = false;

    private static final Logger logger = Logger.getLogger(Transaction.class.getName());

    public Transaction(String sender, String receiver, double amount) {
        validateParameters(sender, receiver, amount);
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public synchronized void sign(PrivateKey privateKey, PublicKey publicKey) throws Exception {
        if (isSigned) {
            throw new IllegalStateException("交易已签名，不可重复签名");
        }
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(getDataToSign());
        this.signature = sig.sign();
        this.senderPublicKey = publicKey;
        this.isSigned = true; // 标记为已签名
    }

    public boolean isValid() {
        if (!isSigned || signature == null || senderPublicKey == null) {
            return false;
        }
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(senderPublicKey);
            sig.update(getDataToSign());
            return sig.verify(signature);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "验证签名时出错: " + e.getMessage(), e);
            return false;
        }
    }

    public String getTransactionHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String rawData = String.format(Locale.US, "%s|%s|%.8f|%d",
                    sender, receiver, amount, timestamp);
            byte[] hashBytes = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes); // 移除"0x"前缀保持格式统一
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("交易哈希生成失败", e);
        }
    }

    // 使用大写HEX编码（Block类一致）
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }

    private void validateParameters(String sender, String receiver, double amount) {
        if (amount <= 0) {
            logger.log(Level.SEVERE, "无效的交易金额: " + amount);
            throw new IllegalArgumentException("金额必须大于0");
        }
        if (sender == null || sender.isEmpty()) {
            logger.log(Level.SEVERE, "无效的发送方: " + sender);
            throw new IllegalArgumentException("无效发送方");
        }
        if (receiver == null || receiver.isEmpty()) {
            logger.log(Level.SEVERE, "无效的接收方: " + receiver);
            throw new IllegalArgumentException("无效接收方");
        }
        if (sender.equals(receiver)) {
            logger.log(Level.SEVERE, "发送方和接收方不能相同: " + sender + " 和 " + receiver);
            throw new IllegalArgumentException("发送方和接收方不能相同");
        }
    }

    // Getters（增加防御性拷贝）
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public double getAmount() { return amount; }
    public byte[] getDataToSign() {
        return String.format(Locale.US, "%s|%s|%.8f|%d", sender, receiver, amount, timestamp)
                .getBytes(StandardCharsets.UTF_8);
    }
    public PublicKey getSenderPublicKey() { return senderPublicKey; }
    public byte[] getSignature() { return Arrays.copyOf(signature, signature.length); }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (senderPublicKey != null) {
            out.writeObject(senderPublicKey.getEncoded());
        } else {
            out.writeObject(null);
        }
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        byte[] keyBytes = (byte[]) in.readObject();
        if (keyBytes != null) {
            try {
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                senderPublicKey = keyFactory.generatePublic(spec);
            } catch (Exception e) {
                throw new IOException("Failed to deserialize PublicKey", e);
            }
        }
    }
}