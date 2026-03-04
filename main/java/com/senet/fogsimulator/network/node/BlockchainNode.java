package com.senet.fogsimulator.network.node;

import com.senet.fogsimulator.blockchain.consensus.ConsensusModule;
import com.senet.fogsimulator.blockchain.ledger.Ledger;
import com.senet.fogsimulator.blockchain.core.Transaction;
import com.senet.fogsimulator.network.message.EncryptedMessage;
import com.senet.fogsimulator.network.message.Message;
import com.senet.fogsimulator.network.message.MessageType;

import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;
import java.util.List;

public class BlockchainNode {
    private final String id;
    private final Ledger ledger;
    private final ConsensusModule consensusModule;
    private final List<BlockchainNode> neighbors;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public BlockchainNode(String id, Ledger ledger, ConsensusModule consensusModule, List<BlockchainNode> neighbors) {
        this.id = id;
        this.ledger = ledger;
        this.consensusModule = consensusModule;
        this.neighbors = neighbors;
    }

    public void start() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public Ledger getLedger() {
        return ledger;
    }

    public ConsensusModule getConsensusModule() {
        return consensusModule;
    }

    public EncryptedMessage encryptMessage(Message message, PublicKey recipientKey) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
            oos.writeObject(message);
            oos.close();
            byte[] dataToEncrypt = baos.toByteArray();
            
            int maxBlockSize = 245;
            if (dataToEncrypt.length <= maxBlockSize) {
                byte[] encryptedData = encryptData(dataToEncrypt, recipientKey);
                return new EncryptedMessage(encryptedData, publicKey);
            } else {
                java.io.ByteArrayOutputStream encryptedBaos = new java.io.ByteArrayOutputStream();
                int offset = 0;
                while (offset < dataToEncrypt.length) {
                    int blockSize = Math.min(maxBlockSize, dataToEncrypt.length - offset);
                    byte[] block = new byte[blockSize];
                    System.arraycopy(dataToEncrypt, offset, block, 0, blockSize);
                    byte[] encryptedBlock = encryptData(block, recipientKey);
                    encryptedBaos.write(encryptedBlock);
                    offset += blockSize;
                }
                return new EncryptedMessage(encryptedBaos.toByteArray(), publicKey);
            }
        } catch (Exception e) {
            throw new RuntimeException("加密消息失败", e);
        }
    }

    public EncryptedMessage encryptMessage(Message message) {
        return encryptMessage(message, publicKey);
    }

    private byte[] decryptMessage(byte[] encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            int blockSize = 256;
            if (encryptedData.length <= blockSize) {
                return cipher.doFinal(encryptedData);
            } else {
                java.io.ByteArrayOutputStream decryptedBaos = new java.io.ByteArrayOutputStream();
                int offset = 0;
                while (offset < encryptedData.length) {
                    int currentBlockSize = Math.min(blockSize, encryptedData.length - offset);
                    byte[] block = new byte[currentBlockSize];
                    System.arraycopy(encryptedData, offset, block, 0, currentBlockSize);
                    byte[] decryptedBlock = cipher.doFinal(block);
                    decryptedBaos.write(decryptedBlock);
                    offset += currentBlockSize;
                }
                return decryptedBaos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("解密消息失败", e);
        }
    }

    public void receiveMessage(EncryptedMessage encryptedMessage) {
        try {
            byte[] decryptedData = decryptMessage(encryptedMessage.getEncryptedData());
            java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(decryptedData));
            Message message = (Message) ois.readObject();

            if (message.getType() == MessageType.TRANSACTION) {
                Transaction transaction = (Transaction) message.getPayload();
                if (validateTransaction(transaction)) {
                    ledger.addTransaction(transaction);
                    consensusModule.onTransactionReceived(transaction);
                }
            } else if (message.getType() == MessageType.SYNC) {
                Ledger newLedger = (Ledger) message.getPayload();
                if (consensusModule.validateLedger(newLedger)) {
                    sync(newLedger);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("处理消息失败", e);
        }
    }

    public boolean validateTransaction(Transaction transaction) {
        if (transaction == null) {
            return false;
        }
        return transaction.isValid();
    }

    public void broadcastTransaction(Transaction transaction) throws GeneralSecurityException {
        try {
            transaction.sign(privateKey, publicKey);
            Message message = new Message(MessageType.TRANSACTION, transaction);
            for (BlockchainNode neighbor : neighbors) {
                EncryptedMessage encryptedMessage = encryptMessage(message, neighbor.getPublicKey());
                neighbor.receiveMessage(encryptedMessage);
            }
        } catch (Exception e) {
            throw new GeneralSecurityException("广播交易失败", e);
        }
    }

    private byte[] encryptData(byte[] data, PublicKey recipientKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, recipientKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("加密数据失败", e);
        }
    }

    public void sync(Ledger newLedger) {
        this.ledger.sync(newLedger);
    }

    public void syncLedger(Ledger newLedger) {
        sync(newLedger);
    }
}