package com.senet.fogsimulator.blockchain.block;

import com.senet.fogsimulator.blockchain.core.Transaction;
import com.senet.fogsimulator.blockchain.core.BlockchainException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Block implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(Block.class.getName());

    private final String previousBlockHash;
    private final List<Transaction> transactions;
    private final byte[] nonce;
    private final String blockHash; // 哈希固化

    public Block(String previousBlockHash, List<Transaction> transactions, byte[] nonce) {
        logger.log(Level.INFO, "Creating new block with previous hash: {0}", previousBlockHash);
        validateConstructorArgs(transactions, nonce);
        this.previousBlockHash = previousBlockHash != null ? previousBlockHash : "";
        this.transactions = new ArrayList<>(transactions); // 防御性拷贝
        this.nonce = Arrays.copyOf(nonce, nonce.length);   // 防御性拷贝
        this.blockHash = calculateBlockHash(); // 构造时计算哈希
        logger.log(Level.INFO, "Block created with hash: {0}", this.blockHash);
    }

    public String calculateBlockHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringJoiner txJoiner = new StringJoiner("\u0001"); // 修复分隔符问题

            for (Transaction tx : transactions) {
                txJoiner.add(tx.getTransactionHash());
            }

            // 统一编码格式
            String dataInput = previousBlockHash
                    + txJoiner.toString()
                    + bytesToHex(nonce); // 使用HEX编码替代Arrays.toString()

            digest.update(dataInput.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String hash = bytesToHex(digest.digest());
            logger.log(Level.FINE, "Calculated block hash: {0}", hash);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "哈希计算失败", e);
            throw new BlockchainException("哈希计算失败", e);
        }
    }

    // 复用同一HEX编码方法
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int v = b & 0xFF; // 处理字节符号问题
            hexString.append(Character.toUpperCase(Character.forDigit(v >>> 4, 16)));
            hexString.append(Character.toUpperCase(Character.forDigit(v & 0x0F, 16)));
        }
        return hexString.toString();
    }

    private void validateConstructorArgs(List<Transaction> transactions, byte[] nonce) {
        if (transactions == null || transactions.isEmpty()) {
            logger.log(Level.SEVERE, "交易列表不能为空且必须包含至少一笔交易");
            throw new BlockchainException("交易列表不能为空且必须包含至少一笔交易");
        }
        if (nonce == null || nonce.length == 0) {
            logger.log(Level.SEVERE, "随机数不能为空");
            throw new BlockchainException("随机数不能为空");
        }
    }

    // 防御性Getter方法
    public String getBlockHash() { return blockHash; }
    public String getPreviousBlockHash() { return previousBlockHash; }
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(transactions));
    }
    public byte[] getNonce() { return Arrays.copyOf(nonce, nonce.length); }

    // 新增测试辅助方法
    public String getNonceHex() {
        return bytesToHex(nonce);
    }
}