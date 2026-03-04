package com.senet.fogsimulator.network.message;

import com.senet.fogsimulator.blockchain.core.Transaction;
import com.senet.fogsimulator.network.message.MessageType;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private MessageType type;
    private Object payload;

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    // Getter 方法
    public MessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    // 将消息转换为字符串格式（用于加密）
    @Override
    public String toString() {
        return type + "," + payload.toString();
    }

    // 从字符串解析消息
    public static Message fromString(String data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("消息数据不能为空");
        }
        String[] parts = data.split(",", 2);
        if (parts.length < 1) {
            throw new IllegalArgumentException("消息格式无效");
        }
        MessageType type = MessageType.valueOf(parts[0]);
        Object payload = null;

        if (type == MessageType.TRANSACTION && parts.length > 1) {
            String[] transactionParts = parts[1].split(",", 3);
            if (transactionParts.length == 3) {
                String sender = transactionParts[0];
                String receiver = transactionParts[1];
                double amount = Double.parseDouble(transactionParts[2]);
                payload = new Transaction(sender, receiver, amount);
            }
        } else if (type == MessageType.SYNC && parts.length > 1) {
            payload = parts[1];
        }

        return new Message(type, payload);
    }
}