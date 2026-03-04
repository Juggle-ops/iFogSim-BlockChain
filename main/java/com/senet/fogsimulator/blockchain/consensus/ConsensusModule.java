package com.senet.fogsimulator.blockchain.consensus;

import com.senet.fogsimulator.blockchain.block.Block;
import com.senet.fogsimulator.blockchain.core.BlockchainException;
import com.senet.fogsimulator.blockchain.ledger.Ledger;
import com.senet.fogsimulator.network.node.BlockchainNode;
import com.senet.fogsimulator.blockchain.core.Transaction;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ConsensusModule {
    private static final Logger logger = Logger.getLogger(ConsensusModule.class.getName());
    private static final int INITIAL_DIFFICULTY = 2;
    private static final long TARGET_BLOCK_TIME_MS = 10_000; // 目标出块时间（10秒）

    private final Ledger ledger;
    private final BlockchainNode node;
    private final AtomicInteger difficulty = new AtomicInteger(INITIAL_DIFFICULTY);
    private volatile Instant lastBlockTime = Instant.now();

    public ConsensusModule(Ledger ledger, BlockchainNode node) {
        this.ledger = ledger;
        this.node = node;
    }

    public Block mineBlock(List<Transaction> transactions) {
        try {
            Block latestBlock = ledger.getLatestBlock();
            String previousHash = latestBlock != null ? latestBlock.getBlockHash() : "0x0";
            int currentDifficulty = difficulty.get();

            byte[] nonce = findValidNonce(previousHash, transactions, currentDifficulty);
            Block newBlock = new Block(previousHash, transactions, nonce);
            adjustDifficulty();
            return newBlock;
        } catch (Exception e) {
            throw new BlockchainException("挖矿过程中发生错误", e);
        }
    }

    public boolean isBlockValid(Block block) {
        return validateBlock(block);
    }

    public void startConsensus() {
        if (ledger.getPendingTransactions().isEmpty()) {
            throw new IllegalStateException("No pending transactions");
        }
        Block newBlock = mineBlock(ledger.getPendingTransactions());
        ledger.addBlock(newBlock);
    }

    public boolean shouldStartConsensus(Transaction tx) {
        if (tx == null) {
            throw new BlockchainException("交易不能为空");
        }
        return tx.getAmount() > 500.0 && tx.isValid();
    }

    private boolean validateBlock(Block block) {
        return isHashValid(block.getBlockHash(), difficulty.get()) &&
                isPreviousHashValid(block.getPreviousBlockHash());
    }

    private boolean validateFullChain(List<Block> chain) {
        String previousHash = "0x0";
        for (Block block : chain) {
            if (!block.getPreviousBlockHash().equals(previousHash) || !validateBlock(block)) {
                return false;
            }
            previousHash = block.getBlockHash();
        }
        return true;
    }

    private byte[] findValidNonce(String previousHash, List<Transaction> transactions, int difficulty) {
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

    private synchronized void adjustDifficulty() {
        Instant now = Instant.now();
        long timeElapsed = Duration.between(lastBlockTime, now).toMillis();
        lastBlockTime = now;

        if (timeElapsed < TARGET_BLOCK_TIME_MS / 2) {
            difficulty.set(clamp(difficulty.get() + 1, INITIAL_DIFFICULTY, 8));
        } else if (timeElapsed > TARGET_BLOCK_TIME_MS * 2) {
            difficulty.set(clamp(difficulty.get() - 1, INITIAL_DIFFICULTY, 8));
        }
    }

    private boolean isHashValid(String hash, int difficulty) {
        return hash.substring(2, 2 + difficulty).equals("0".repeat(difficulty));
    }

    private boolean isPreviousHashValid(String prevHash) {
        return prevHash.equals(ledger.getLatestBlockHash());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private String getLatestBlockHash() {
        return ledger.getLatestBlockHash();
    }

    public int getDifficultyLevel() {
        return difficulty.get();
    }

    public boolean validateLedger(Ledger ledger) {
        return validateFullChain(ledger.getBlockChain());
    }

    public void onTransactionReceived(Transaction transaction) {
        if (shouldStartConsensus(transaction)) {
            try {
                startConsensus();
            } catch (IllegalStateException e) {
                logger.info("待处理交易不足，无法启动共识");
            }
        }
    }
}