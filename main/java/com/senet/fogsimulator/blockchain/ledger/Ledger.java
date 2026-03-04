package com.senet.fogsimulator.blockchain.ledger;

import com.senet.fogsimulator.blockchain.block.Block;
import com.senet.fogsimulator.blockchain.core.Transaction;
import com.senet.fogsimulator.blockchain.core.BlockchainException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Ledger implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int BLOCK_TRANSACTION_LIMIT = 10;
    private static final Logger logger = Logger.getLogger(Ledger.class.getName());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final List<Block> blockChain;
    private final List<Transaction> pendingTransactions;

    public Ledger() {
        this.blockChain = new ArrayList<>();
        this.pendingTransactions = Collections.synchronizedList(new ArrayList<>());

        // 初始化创世区块，确保金额大于0且发送方和接收方不相同
        Transaction genesisTransaction = new Transaction("Genesis", "InitialReceiver", 1000.0);
        byte[] nonce = new byte[4]; // 创建一个有效的随机数数组
        SECURE_RANDOM.nextBytes(nonce); // 填充随机数
        Block genesisBlock = new Block("", Collections.singletonList(genesisTransaction), nonce);
        blockChain.add(genesisBlock);
        logger.info("Genesis block added to the blockchain");
    }

    public synchronized void addTransaction(Transaction transaction) {
        if (isTransactionValid(transaction)) {
            pendingTransactions.add(transaction);
            logger.info("Added transaction: " + transaction.getTransactionHash());

            if (pendingTransactions.size() >= BLOCK_TRANSACTION_LIMIT) {
                mineBlock();
            }
        } else {
            logger.warning("Rejected invalid transaction: " + transaction);
        }
    }

    private void mineBlock() {
        String previousHash = getLatestBlock().getBlockHash();
        List<Transaction> transactionsToMine = new ArrayList<>(pendingTransactions);

        byte[] nonce = findValidNonce(transactionsToMine, previousHash);
        Block newBlock = new Block(previousHash, transactionsToMine, nonce);
        addBlock(newBlock); // 调用 addBlock 方法
        pendingTransactions.clear();
        logger.info("Mined new block: " + newBlock.getBlockHash());
    }

    private boolean isTransactionValid(Transaction tx) {
        return tx.isValid() && !isDoubleSpending(tx.getSender(), tx.getAmount());
    }

    private boolean isDoubleSpending(String sender, double amount) {
        double spent = pendingTransactions.stream()
                .filter(t -> t.getSender().equals(sender))
                .mapToDouble(Transaction::getAmount)
                .sum();
        return spent + amount > getBalance(sender);
    }

    public double getBalance(String address) {
        double received = blockChain.stream()
                .flatMap(b -> b.getTransactions().stream())
                .filter(t -> t.getReceiver().equals(address))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double sent = blockChain.stream()
                .flatMap(b -> b.getTransactions().stream())
                .filter(t -> t.getSender().equals(address))
                .mapToDouble(Transaction::getAmount)
                .sum();

        return received - sent;
    }

    public Block getLatestBlock() {
        if (blockChain.isEmpty()) {
            throw new IllegalStateException("Blockchain is empty");
        }
        return blockChain.get(blockChain.size() - 1);
    }

    public String getLatestBlockHash() {
        if (blockChain.isEmpty()) {
            return "0x0";
        }
        return blockChain.get(blockChain.size() - 1).getBlockHash();
    }

    public List<Block> getBlockChain() {
        return Collections.unmodifiableList(blockChain);
    }

    public List<Transaction> getPendingTransactions() {
        return Collections.unmodifiableList(pendingTransactions);
    }

    public List<Transaction> getTransactions() {
        List<Transaction> allTransactions = new ArrayList<>();
        blockChain.forEach(block -> allTransactions.addAll(block.getTransactions()));
        return Collections.unmodifiableList(allTransactions);
    }

    private byte[] findValidNonce(List<Transaction> transactions, String previousHash) {
        int difficulty = 2;
        byte[] nonce = new byte[4];
        long maxAttempts = 100_000;
        String target = "0".repeat(difficulty);

        for (long i = 0; i < maxAttempts; i++) {
            nonce[0] = (byte) (i >>> 24);
            nonce[1] = (byte) (i >>> 16);
            nonce[2] = (byte) (i >>> 8);
            nonce[3] = (byte) i;

            Block testBlock = new Block(previousHash, transactions, nonce);
            String hash = testBlock.getBlockHash();
            if (hash.substring(2, 2 + difficulty).equals(target)) {
                return nonce;
            }
        }
        throw new RuntimeException("Valid nonce not found");
    }

    // 新增 addBlock 方法
    public void addBlock(Block block) {
        blockChain.add(block);
        logger.info("Block added to the blockchain: " + block.getBlockHash());
    }

    public void sync(Ledger newLedger) {
        this.blockChain.clear();
        this.blockChain.addAll(newLedger.getBlockChain());
        this.pendingTransactions.clear();
        this.pendingTransactions.addAll(newLedger.getPendingTransactions());
        logger.info("Ledger synced with new ledger");
    }
}